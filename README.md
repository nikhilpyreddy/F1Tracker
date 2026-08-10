# F1 Tracker

A personal Formula 1 companion app for Android, built for a Pixel 10 Pro. Not published
to the Play Store — this is a sideloaded, personal-use project.

Data comes from [Jolpica-F1](https://github.com/jolpica/jolpica-f1), the actively
maintained, Ergast-compatible successor API (the same source the
[FastF1](https://pypi.org/project/fastf1/) Python library now uses internally).

## Features

- **Home** — next race countdown (tap through to its Grand Prix detail), plus your
  favorite drivers' and teams' current standings
- **Favorites** — pick up to 4 favorite drivers and 2 favorite teams
- **Standings** — full current-season driver and constructor tables, with pull-to-refresh
- **Driver / Team detail** — points-by-season trend chart across the last 4 years, plus
  race-by-race results for any of those seasons
- **Grand Prix detail** — pick any Grand Prix on the calendar and any driver to see their
  results at that circuit across the last 6 years; switch between GPs and drivers freely
- **Compare** — overlay two drivers' or two teams' season-points trends on one chart

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
