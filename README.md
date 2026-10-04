# F1 Tracker

A personal Formula 1 companion app for Android, built for a Pixel 10 Pro. Not published
to the Play Store — this is a sideloaded, personal-use project.

Data comes from [Jolpica-F1](https://github.com/jolpica/jolpica-f1) (results, qualifying,
standings), [OpenF1](https://openf1.org) (telemetry, tyre stints, race control, team colours;
2023+) and [Open-Meteo](https://open-meteo.com) (forecasts). All are free and used for
personal, non-commercial purposes. See `docs/knowledge/data-sources.md` for details.

## Features

Built to inform F1 prediction-market decisions: every rate shows its sample size.

- **Home**: next race, a season calendar (any Grand Prix opens its Race Weekend screen), and your
  favourite drivers' and teams' standings with team colours
- **Race Weekend**, per Grand Prix:
  - **Circuit**: last year's podium and full results (any year); 10-season base rates (won from
    pole, front row or top 3, podium starts, top 10 from outside the top 10, retirements, fastest
    lap). Tap a stat to see the races behind it
  - **Qualifying**: this weekend's results with gaps to pole, pole history and pole-to-win rate,
    season qualifying form, teammate qualifying head-to-heads
  - **Track**: speed-coloured map and profile of the latest pole lap (full throttle, longest
    straight, slow/medium/fast braking corners), circuit type, weekend forecast
  - **Strategy**: tyre stints for the whole field each year since 2023, common strategies, stint
    lengths, Safety Car / VSC / red-flag rates, overtakes, pit-lane time
  - **Form**: last 5 races per driver as coloured finish chips, teammate head-to-heads
- **Driver pages**: points by season, plus finishing position race by race with the grid line
- **Standings, Compare, Favorites**: this season's grid with team colours and headshots

## Tech stack

- Kotlin + Jetpack Compose (Material 3), MVVM
- [Hilt](https://dagger.dev/hilt/) for dependency injection
- [Room](https://developer.android.com/training/data-storage/room) for local caching,
  [Retrofit](https://square.github.io/retrofit/) + kotlinx.serialization for the network layer
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for
  favorites
- Navigation Compose with a bottom nav bar (Home / Standings / Compare / Favorites) plus
  pushed detail screens (Driver / Team / Grand Prix)
- A small in-memory TTL guard in the repository layer avoids re-fetching data that was
  synced within the last 15 minutes

## Requirements

- Android Studio (current stable)
- JDK 17+
- minSdk 36 / compileSdk 37

## Building

```bash
./gradlew assembleDebug
```

Install directly to a connected device or emulator:

```bash
./gradlew installDebug
```

## Testing

```bash
./gradlew testDebugUnitTest
```

## Data source notes

The [Jolpica-F1](https://api.jolpi.ca/ergast/f1/) public API requires no API key but does
require a custom `User-Agent` header (already set in `NetworkModule`), and is rate-limited
to 4 requests/second and 500/hour. Historical data is cached permanently once fetched;
only the current season's schedule and standings are periodically refreshed.
