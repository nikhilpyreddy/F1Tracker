# Architecture

Single `app` module, package `com.nikhil.f1tracker`. MVVM with an offline-first data flow:
**screens read from Room, and syncs only write to Room.** Cached data therefore shows instantly,
and a sync failure leaves the last good data on screen.

```
ui (Compose screens + ViewModels)
 └─ observes Flows from ──> data/repository ──> data/local (Room DAOs)
                                  │
                                  └─ sync*() ──> data/remote (Retrofit: Jolpica, OpenF1, Open-Meteo)
domain/stats   pure functions turning cached rows into numbers (no Android, no I/O)
domain/model   small shared models and constants (seasons, identities, circuit types)
```

## Packages

| Package | What lives there |
|---|---|
| `data/remote` | Retrofit services and DTOs. `JolpicaApiService`, `openf1/OpenF1ApiService`, `openmeteo/OpenMeteoApiService`, `RequestSpacer` (rate limiting) |
| `data/local` | `F1Database` (Room, **destructive migrations**: everything is re-downloadable), entities, DAOs |
| `data/mapper` | DTO → entity mapping |
| `data/repository` | `F1Repository` (schedule, results, standings, circuit history), `QualifyingRepository`, `OpenF1Repository` (telemetry, stints, incidents; cached as JSON in `api_cache`), `IdentityRepository` (team colours and headshots), `FavoritesRepository` (DataStore) |
| `domain/stats` | `CircuitStats` and `CircuitStatDetails`, `DriverForm` (form and teammate head-to-head), `QualifyingStats`, `TrackProfile`, `StrategySummary`, `Rate` |
| `ui/common` | `SyncCatching`, `SeasonSync`, `LineChart`, `PositionChart` |
| `ui/common/identity` | `LocalF1Identities` (provided once in `F1App`), `DriverAvatar`, `CodeBadge`, `TeamDot`, `teamStripe`, `StandingRow`, `FractionBar`, `SplitBar`, `FinishBadge`, `FormStrip`, `finishColor` |
| `ui/weekend` | The Race Weekend screen. Tabs: Circuit, Qualifying, Track, Strategy, Form. Each newer tab has its own ViewModel in a sub-package (`qualifying/`, `track/`, `strategy/`), created when the tab first shows, so its data loads lazily |
| `ui/*` | Home (calendar), Standings, Compare, Favorites, Driver detail (Seasons / Race by race), Team detail, Grand Prix detail (driver history at a circuit) |
| `di` | Hilt modules. `NetworkModule` builds one base `OkHttpClient` and per-API clients (`@JolpicaClient`, `@OpenF1Client`) |

## Navigation (`ui/navigation/F1App.kt`)

- **Bottom bar:** Home, Standings, Compare, Favorites.
- **Pushed screens:** `weekend/{season}/{round}/{circuitId}`, `driverDetail/{driverId}`,
  `teamDetail/{constructorId}`, `grandPrixDetail/{circuitId}?driverId=`.
- **Arguments:** tab ViewModels read the route arguments from the shared `SavedStateHandle`.

## Conventions worth keeping

- **ViewModel state:** private `MutableStateFlow`s for status, combined with repository Flows into
  one `StateFlow<…UiState>` via `stateIn(WhileSubscribed(5_000))`.
- **Syncs:** run in `viewModelScope` inside `syncCatching { }.onFailure { message }`.
- **Two-state UI:** show cached content with a progress bar and an error banner, rather than
  replacing the screen, when data already exists.
- **Rows that represent a driver or team:** get `teamStripe(...)` plus `DriverAvatar`, `TeamDot` or
  `CodeBadge`.
- **New Room table:** bump `F1Database.version`; destructive fallback rebuilds the database.
- **Lists of "current" drivers or teams:** use `getSeasonDrivers`/`getSeasonConstructors`, which fall
  back to the previous season before round 1. Don't use `getAll*`.
