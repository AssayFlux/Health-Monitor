# Release safety gate

Health Monitor is not approved for real patient records until every gate below is evidenced on the release commit.

## Current known status (2026-10-30)

- SQLCipher is present in the Android dependency graph and the application calls `System.loadLibrary("sqlcipher")`. This confirms integration intent, not a verified encrypted-database release.
- The plaintext migration currently validates integrity and row counts for a fixed list of tables. It does not yet prove full schema/index/trigger/foreign-key preservation, WAL/SHM cleanup under interruption, or recovery across every crash point.
- The Android Keystore envelope protects the database passphrase from direct preference-file disclosure, but its wrapping key is created with `setUserAuthenticationRequired(false)`. It is therefore not biometric-gated.
- The login composable is rendered after the database has already been opened. Password entry currently does not gate release of the SQLCipher key.
- SecureTransfer contains encryption/decryption primitives, but there is no complete selective export/import workflow, preview, member mapping, or duplicate-safe transactional merge.
- Reminder UI accepts a free-form identifier and interval. It is not persisted against a medicine/appointment record and is not a dose schedule.
- Analytics currently plots vital-sign trends only. Laboratory and anthropometric rows are counted but not analyzed as unit-/analyte-specific series.
- CI run for commit `d229dc7b15d50fc85ac31d9f3f606bfc5c7fd871` failed in both build-and-unit-test and instrumentation jobs. Unit tests were skipped after APK assembly failed.

## Mandatory release gates

1. **Build:** `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:connectedDebugAndroidTest` pass on the exact release commit.
2. **SQLCipher:** prove a newly created database is unreadable by stock SQLite, readable with the correct SQLCipher key, and rejects a wrong key. Confirm supported SQLCipher version and ABI coverage on minSdk and target devices.
3. **Migration:** test copied v1/v2/v3 databases with empty, small, large, null-containing, and multi-member data. Compare every table's schema, indexes, triggers, foreign keys, row counts and canonicalized row contents. Inject failures before/after checkpoint, export, validation, rename, and cleanup. The original must remain recoverable; unknown/corrupt files must fail closed and must never be re-created as an empty database.
4. **Key lifecycle:** authenticate the owner before opening the database. A biometric CryptoObject must unwrap the database key from Android Keystore; password recovery must remain possible using the local account policy. Handle biometric enrollment/key invalidation without silently replacing keys or losing data. Clear transient key material when practical.
5. **Transfer:** export only selected members/categories/date windows; encrypt the whole archive with authenticated encryption and a separately shared high-entropy code. Import must preview, validate schema/version, map members, detect duplicates, and merge in one transaction with rollback. Test wrong codes, tampering, truncation, oversized archives, duplicate paths, and malicious ZIP metadata.
6. **Reminders:** store reminder definitions in the encrypted database and reference actual medicine/appointment IDs. Persist local date/time/time-zone, recurrence, enabled state and member. Reschedule after reboot/time-zone changes; handle permission denial and battery restrictions. Do not describe WorkManager periodic jobs as exact dose alarms.
7. **Scientific analytics:** preserve analyte identity and units; never average unlike analytes or units. Show per-member filters, dates, sample counts, missingness, distribution summaries and source/reference metadata. Plot lab series only when values are numeric and unit-consistent. Anthropometry must retain age/sex/reference-standard context and avoid invented pediatric percentiles.
8. **Physical devices:** test install/upgrade, app process death, reboot, lock/unlock, biometric enrollment changes, Android 13+ notification permission, battery optimization, export/import across two devices, storage-full behavior, and interrupted migration. Record device model, Android build, test result and logs with health data redacted.

## Privacy and clinical use

- No cloud database, server-side sync, email, phone, or OTP is intended.
- Do not put patient names, medicine names, diagnoses, or measurements in CI logs, crash reports, notification text, or test fixtures.
- A green CI build is necessary but not sufficient for clinical reliability. This app is a personal recordkeeping tool, not a diagnostic or emergency-care system.
- Do not use the app for real patient records until the gates above are completed and independently reviewed.
