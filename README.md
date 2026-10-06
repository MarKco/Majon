# Majon 🧶

A row counter for knitting projects — native Android, Jetpack Compose.

Track multiple projects, split them into parts (front, back, sleeves…),
count rows with one big button, and get pattern instructions ("decrease by
two") shown automatically right before the row they apply to. As you count,
a knitted garment shaped like your project fills with color stitch by stitch.
Progress bars that wave like a thread, confetti when a part is finished,
English/Italian with in-app switching, light/dark craft theme (Material 3
Expressive), JSON export/import. No permissions, no network: data stays on
the device.

<p>
  <img src="docs/manual/images/en/01-home.png" width="200" alt="Projects">
  <img src="docs/manual/images/en/03-project.png" width="200" alt="Parts">
  <img src="docs/manual/images/en/05-counter.png" width="200" alt="Counter">
</p>

| | |
|---|---|
| Stack | Kotlin 2.4, Compose + Material 3 (1.5 alpha, for Expressive), Room, Hilt, DataStore |
| Build | Gradle 9.8, AGP 9.4, minSdk 24, targetSdk 37 |
| Tests | 80 unit tests (TDD), Robolectric + Turbine |

## Build

```bash
./gradlew :app:assembleDebug        # APK
./gradlew :app:testDebugUnitTest    # tests
./gradlew :app:lintDebug            # lint
```

## Docs

- User manual, with screenshots: [English](docs/manual/manual.en.md) · [Italiano](docs/manual/manuale.it.md)
- [Development documentation](docs/DEVELOPMENT.md) (Italian)
- Original specs and audio brief: `starting-docs/`
