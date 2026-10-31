# Build and safe-upgrade baseline

Last reviewed: 2026-10-31

## 1. How the app builds

Health Monitor is an Android application with Kotlin/Jetpack Compose UI and an embedded Python 3.11 engine provided by Chaquopy.

### Required build environment

| Item | Baseline |
|---|---|
| JDK | 17 |
| Gradle | 8.13 |
| Android compile SDK | 36 |
| Android build tools | 36.0.0 |
| Minimum Android API | 26 |
| Target Android API | 36 |
| Python runtime | 3.11 (Chaquopy) |
| Native ABIs | arm64-v8a, x86_64 |
| SQLCipher Android | 4.19.1 |
| AndroidX SQLite | 2.7.0 |
| App version baseline | 1.0.0 / versionCode 1 |

The project currently uses a system Gradle installation and does not include a Gradle wrapper.

### Build commands

From the repository root:

    gradle :app:assembleDebug :app:bundleDebug --no-daemon

Expected outputs:

- app/build/outputs/apk/debug/app-debug.apk
- app/build/outputs/bundle/debug/app-debug.aab

The APK can be installed on a compatible Android device. An AAB is a distribution bundle and is not directly installable. Google Colab setup is documented in docs/GOOGLE_COLAB_BUILD.md.

### Single workflow

.github/workflows/test.yml is the only CI workflow. It builds the debug APK and AAB and uploads them as a seven-day artifact. It does not run unit tests, instrumentation tests, lint, static analysis, or physical-device tests.

## 2. Baseline rules

Treat the versions above as a known project baseline, not as a statement that every dependency is the newest available. Upgrade only for a concrete reason: a security advisory, compatibility requirement, Android platform change, reproducible defect, or planned feature.

Do not upgrade several high-risk layers at once. Keep these changes isolated where practical:

- Android Gradle Plugin / Gradle / Kotlin / Compose compiler.
- compileSdk / targetSdk / minSdk.
- SQLCipher and AndroidX SQLite.
- Chaquopy and Python runtime.
- Native inference libraries and ABI filters.
- Authentication, Keystore aliases, encryption envelope formats.
- Database schema and migration logic.

## 3. Safe upgrade sequence

1. Record the starting point: main-branch commit, app version, database schema version, SDK levels, Gradle/JDK versions, dependency versions, and current workflow result.
2. Read the issue register and check whether the planned change overlaps an unresolved issue or failed attempt.
3. Create a branch. Never experiment directly on main.
4. Change one layer at a time. Keep dependency and schema changes small and explain why each is needed.
5. Preserve database compatibility. Use forward-only, transactional migrations. Never lower the database version or silently replace an unreadable encrypted database with an empty one.
6. Preserve key compatibility. Do not change the SQLCipher passphrase, key derivation, Keystore alias, AES-GCM envelope, or authentication path without a migration and recovery plan.
7. Build APK and AAB using the single workflow. A successful build is necessary but not sufficient.
8. Install on a real device. Verify fresh install, upgrade over the previous app, password unlock, biometric unlock and fallback, process death/restart, database read/write, export, transfer in both directions, notifications, and relaunch.
9. Protect user data. Test upgrades with a disposable copy or synthetic profile. Never use the only copy of real health data as a migration test.
10. Update this baseline, the issue register, and README. State exactly what was and was not verified.
11. Merge only after review. Keep the PR and build artifacts available for rollback analysis.

## 4. Database migration requirements

- Preserve all existing records and identifiers unless a documented transformation is required.
- Run schema changes in a transaction where supported.
- Make migrations idempotent where possible.
- Keep the old schema version and migration path documented.
- On wrong key, incompatible file, missing native library, or migration error, stop and show a recovery message. Never delete or recreate the database automatically.
- Keep the database key available for the lifetime of the SQLCipher helper. Clear owned key bytes only after the helper has closed.
- Do not assume opening a database handle proves that the key is valid; SQLCipher may detect a wrong key on the first page read.

## 5. Release baseline

Current workflow artifacts are debug outputs, not release-signed packages. A production release requires protected signing configuration, release build verification, versionCode/versionName update, privacy review, device validation, and a documented recovery strategy. Never commit signing keys or passwords.

## 6. Evidence standard

Use these labels in PRs and the issue register:

- Build verified: APK/AAB build completed in CI.
- Device verified: exact behavior was exercised on a named physical device and Android version.
- Not verified: no evidence yet.
- Open: a known risk or incomplete implementation remains.

Do not convert build-verified into fixed for a runtime or security issue.
