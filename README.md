# Health Monitor — Android Native

The product UI is a native Android application built with Kotlin and Jetpack Compose. The calculation layer is Python, embedded on-device with Chaquopy. There is no Flask server or WebView in the Android app.

## Open in Android Studio
Open this repository root, allow Gradle sync, then run the `:app` configuration on an Android device or emulator.

## Architecture
- `app/src/main/java`: Kotlin + Jetpack Compose UI.
- `app/src/main/python/health_engine.py`: Python calculation bridge.
- `app/src/main/python/ranges.py`, `diagnostics.py`, `units.py`: existing clinical range, test catalogue and unit definitions.
- UI and calculations are deliberately separated. Kotlin owns navigation, forms and presentation; Python owns calculations.

## Current implementation status
Native shell and Python bridge are scaffolded. Persistent patient/record storage, complete medical/lab forms, editing, reports, and regression tests are not yet migrated. Do not use this starter as a clinical system until those flows and calculations have been validated.
