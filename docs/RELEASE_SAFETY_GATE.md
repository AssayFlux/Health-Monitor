# Release safety gate

Health Monitor is not approved for real patient records until every gate below is evidenced on the release commit.

## Current known status (2026-10-31)

- SQLCipher is present in the Android dependency graph and the application calls `System.loadLibrary("sqlcipher")`. This confirms integration intent, not a verified encrypted-database release.
- The plaintext migration currently validates integrity and row counts for a fixed list of tables. It does not yet prove full schema/index/trigger/foreign-key preservation, WAL/SHM cleanup under interruption, or recovery across every crash point.
- The database passphrase is now released to the database host only after local password verification or a successful strong-biometric `BiometricPrompt` CryptoObject operation. The biometric envelope is protected by a per-use Android Keystore key; password-wrapped key recovery remains available.
- The authenticated UI is removed and the database helper closed when the activity leaves the foreground. This lifecycle behaviour still requires real-device verification, especially around system permission dialogs and biometric prompts.
- The earlier SecureTransfer helper and its tests have been removed. Backup, recovery, and sharing are intentionally deferred for a separate redesign.
- Reminder definitions are linked to medicine/appointment records in the encrypted database. WorkManager timing remains approximate and is not an exact dose schedule.
- Analytics currently plots vital-sign trends only. Laboratory and anthropometric rows are counted but not analyzed as unit-/analyte-specific series.
- CI run for commit `d229dc7b15d50fc85ac31d9f3f606bfc5c7fd871` failed in both build-and-unit-test and instrumentation jobs. Unit tests were skipped after APK assembly failed.

- Privacy settings now persist local consent decisions for Health Connect, OCR, FHIR exchange and local analytics; record create/update/delete events are captured by encrypted SQLite audit triggers, alongside consent and selected security events. This is not a tamper-proof forensic audit log.
- The app exposes a privacy/security screen, persists the selected vintage palette, applies `FLAG_SECURE`, and continues to exclude app data from cloud backup/device transfer.

## Mandatory release gates

1. **Build:** `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:connectedDebugAndroidTest` pass on the exact release commit.
2. **SQLCipher:** prove a newly created database is unreadable by stock SQLite, readable with the correct SQLCipher key, and rejects a wrong key. Confirm supported SQLCipher version and ABI coverage on minSdk and target devices.
3. **Migration:** test copied v1/v2/v3 databases with empty, small, large, null-containing, and multi-member data. Compare every table's schema, indexes, triggers, foreign keys, row counts and canonicalized row contents. Inject failures before/after checkpoint, export, validation, rename, and cleanup. The original must remain recoverable; unknown/corrupt files must fail closed and must never be re-created as an empty database.
4. **Key lifecycle:** authenticate the owner before opening the database. A biometric CryptoObject must unwrap the database key from Android Keystore; password recovery must remain possible using the local account policy. Handle biometric enrollment/key invalidation without silently replacing keys or losing data. Clear transient key material when practical.
5. **Backup/sharing (deferred):** no backup, recovery/restore, or person-to-person sharing workflow is included. FHIR clinical-data import/export is a separate interoperability workflow and must not be represented as backup or device migration. Do not restore the removed transfer helper. Before reintroduction, produce a separate threat model and UX specification covering key exchange, archive format/versioning, preview, member mapping, duplicate/conflict resolution, transactional restore, recovery, and failure testing.
6. **Reminders:** store reminder definitions in the encrypted database and reference actual medicine/appointment IDs. Persist local date/time/time-zone, recurrence, enabled state and member. Reschedule after reboot/time-zone changes; handle permission denial and battery restrictions. Do not describe WorkManager periodic jobs as exact dose alarms.
7. **Scientific analytics:** preserve analyte identity and units; never average unlike analytes or units. Show per-member filters, dates, sample counts, missingness, distribution summaries and source/reference metadata. Plot lab series only when values are numeric and unit-consistent. Anthropometry must retain age/sex/reference-standard context and avoid invented pediatric percentiles.
8. **Physical devices:** test install/upgrade, app process death, reboot, lock/unlock, biometric enrollment changes, Android 13+ notification permission, battery optimization, export/import across two devices, storage-full behavior, and interrupted migration. Record device model, Android build, test result and logs with health data redacted.

## Privacy and clinical use

- No cloud database, server-side sync, email, phone, or OTP is intended.
- Do not put patient names, medicine names, diagnoses, or measurements in CI logs, crash reports, notification text, or test fixtures.
- A green CI build is necessary but not sufficient for clinical reliability. This app is a personal recordkeeping tool, not a diagnostic or emergency-care system.
- Do not use the app for real patient records until the gates above are completed and independently reviewed.


## Stage 3 implementation notes

See [Stage 3 security, accessibility and release readiness](STAGE_3_SECURITY_ACCESSIBILITY_RELEASE.md) for the biometric key-release design, consent/audit controls, future backup threat model, MASVS review map, and remaining release evidence. Stage 3 implementation is not release-verified; no build, test suite, device review, independent security audit or legal review has been performed for this change set.
