# Majon 🧶

A row counter for knitting projects — native Android, Jetpack Compose.

Track multiple projects, split them into parts (front, back, sleeves…),
count rows with one big button, and get pattern instructions ("decrease by
two") shown automatically right before the row they apply to. Progress
percentages, English/Italian with in-app switching, light/dark craft theme,
JSON export/import. No permissions, no network: data stays on the device.

| | |
|---|---|
| Stack | Kotlin 2.3, Compose + Material 3, Room, Hilt, DataStore |
| Build | Gradle 9.5, AGP 9.2, minSdk 24, targetSdk 37 |
| Tests | 71 unit tests (TDD), Robolectric + Turbine |

## Build

```bash
./gradlew :app:assembleDebug        # APK
./gradlew :app:testDebugUnitTest    # tests
./gradlew :app:lintDebug            # lint
```

## Docs

- [Development documentation](docs/DEVELOPMENT.md) (Italian)
- User guide: [English](docs/USER_GUIDE.en.md) · [Italiano](docs/USER_GUIDE.it.md)
- Original specs and audio brief: `starting-docs/`
