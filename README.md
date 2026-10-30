# Health Monitor — Native Android

A local-first Android family health record app. The complete user interface is Kotlin + Jetpack Compose. Python 3.11 is embedded with Chaquopy and is used only for calculations and clinical reference helpers.

## Open
Open this repository root in Android Studio, allow Gradle sync, then run the `:app` configuration on an Android device or emulator.

## Architecture
- `app/src/main/java`: native Kotlin UI and on-device SQLite persistence.
- `app/src/main/python`: Python calculation engine, ranges, diagnostic catalogue and units.
- No Flask server, HTML templates, CSS, JavaScript website, WebView, or website runtime.

## Data
Family profiles, vitals, laboratory reports and medicines are stored in the app's private SQLite database on the device. Android app data is removed when the app is uninstalled unless the user has separately exported it. This version does not yet include encrypted export/import, cloud sync, record editing/deletion, or a validated migration from a previous website database.

## Clinical safety
Derived values and reference classifications are informational, not diagnoses. Use the reference interval printed by the reporting laboratory and consult a qualified clinician. Verify all calculations and workflows before clinical use.

## Build status
The repository has not been built or instrumented on an Android device in this environment. Run Gradle sync/build and test the Python engine before production use.
