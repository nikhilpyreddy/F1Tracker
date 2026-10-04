# F1 Tracker

Personal, sideloaded Android app (Pixel 10 Pro) for following Formula 1. Its main purpose is to
inform **F1 prediction-market decisions**, so features are judged by predictive value: base rates
with sample sizes, qualifying, form, track character, tyre strategy. It is read-only, has no
accounts, and is not on the Play Store.

## Build & run

```bash
./gradlew assembleDebug                 # build
./gradlew testDebugUnitTest             # unit tests (JVM)
ANDROID_SERIAL=<serial> ./gradlew installDebug   # install on the phone (see docs/knowledge/device.md)
```

minSdk 36 / compileSdk 37, Kotlin 2.2, Compose (Material 3), Hilt, Room, Retrofit +
kotlinx.serialization, Coil 3.

## Knowledge (read before changing the related area)

| Doc | Read it when |
|---|---|
| [docs/knowledge/architecture.md](docs/knowledge/architecture.md) | Adding a screen, repository, table or ViewModel |
| [docs/knowledge/data-sources.md](docs/knowledge/data-sources.md) | Touching any API: endpoints, rate limits, quirks, caching |
| [docs/knowledge/stats.md](docs/knowledge/stats.md) | Changing any derived number (base rates, form, track profile, strategy) |
| [docs/knowledge/device.md](docs/knowledge/device.md) | Installing, debugging or inspecting the app on the phone |

## Project agents (`.claude/agents/`)

- `f1-api-researcher`: checks an endpoint against live data before code depends on it.
- `stats-auditor`: reviews `domain/stats` changes for correctness and sample-size honesty.
- `android-device-tester`: builds, installs, drives the app over adb, and reports crashes and screenshots.

## Working conventions

- **Plan before building features:** research first, present a plan with the key decisions, and
  implement after approval. Small, agreed fixes don't need a plan.
- **Every rate shows its sample** ("7 of 12 races"). No blended or "model" probabilities unless
  asked.
- **Sync errors never crash:** wrap syncs in `syncCatching` (`ui/common/SyncCatching.kt`), which
  catches malformed responses too and rethrows cancellation.
- **Respect rate limits:** each API has its own OkHttp client with a `RequestSpacer`. Don't bypass it.
- **Dates come from the injected `Clock`** in new code, never `LocalDate.now()` or `Year.now()`
  directly.
- **Commits:** conventional style (`feat:`, `fix:`, …), one per feature. Install on the phone after
  each feature.
- **Tests:** keep the existing suite green; new tests are currently optional (the owner's choice, to
  save time).
