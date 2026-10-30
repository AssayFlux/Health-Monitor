# Health Monitor — Native Android

A local-first Android family health record app. The complete user interface is Kotlin + Jetpack Compose. Python 3.11 is embedded with Chaquopy and is used only for calculations and clinical reference helpers.

## Open
Open this repository root in Android Studio, allow Gradle sync, then run the `:app` configuration on an Android device or emulator.

## Architecture
- `app/src/main/java`: native Kotlin UI and on-device SQLite persistence.
- `app/src/main/python`: Python calculation engine, ranges, diagnostic catalogue and units.
- No Flask server, HTML templates, CSS, JavaScript website, WebView, or website runtime.
- No email, phone-number, cloud account, or online authentication dependency.
- The SQLite database is created by Android's SQLiteOpenHelper in the app's private internal data directory. It is not placed in shared storage and is not uploaded by this app.

## Local data privacy
- Android app sandbox permissions restrict the database and app-private files to this app's Linux UID on a normal, non-rooted device.
- Android cloud backup and device-to-device data extraction are disabled in the manifest and explicit extraction rules. Moving records between phones must use the app's intended export/import workflow.
- App screens set Android `FLAG_SECURE` to block screenshots, screen recording and recent-app task previews.
- Cleartext network traffic is disabled. The app does not request internet access as part of its current manifest.
- These protections do **not** encrypt the SQLite database file itself. A rooted/compromised device, forensic access, or a future app vulnerability can expose plaintext data. SQLCipher/Keystore-backed database encryption and an authenticated app lock are still required before storing highly sensitive real patient records.

## Offline security and backup implementation status

`SecureTransfer.kt` adds the first cryptographic foundation for offline backups:
- A fresh 160-bit random import code can be generated for each export.
- PBKDF2-HMAC-SHA-256 derives a key from the separate import code.
- AES-256-GCM encrypts and authenticates the ZIP payload with a fresh salt and nonce.
- The ZIP manifest is versioned and checked on import; unsafe paths, duplicate entries and oversized expanded archives are rejected.
- `LocalCredentialStore` provides salted PBKDF2 password-verifier primitives for a device-local user ID/password.

**These primitives are not yet wired into the app's login screens, Android biometric prompt, database, or Import/Export UI.** The existing SQLite database is not yet encrypted at rest. Do not use this build for sensitive real-world patient records until SQLCipher/database migration, Keystore-backed key protection, biometric activation, login lockout/session policy, and the complete backup/import UI have been implemented and security-tested.

The intended user flow is: local user ID + password; optional Android strong biometric unlock; select family members and record categories; export a `.hmbackup` file and share its generated code separately; import only inside Health Monitor, verify/decrypt, preview, then explicitly confirm merge. Import codes must never be the local account password.

## Security and reminder foundations added in this branch\n\n- `LocalKeyVault` wraps a random 256-bit database passphrase using an Android Keystore AES-GCM key. It refuses to silently generate a replacement if initialization was interrupted. It is a key-management primitive; SQLCipher is added as a dependency but the current `HealthDb` still uses Android plaintext SQLite until a validated migration and full DB API conversion are completed.\n- `LocalAuthPolicy` adds persistent progressive throttling and a two-minute session-expiry policy helper. The current Compose UI is not yet gated by login, and biometric unlock is not yet wired to the UI.\n- `ReminderScheduler` schedules offline one-shot WorkManager reminders and emits generic notification text. The app still needs reminder-entry screens, runtime notification-permission onboarding, recurring-dose logic, and integration with stored medicine schedules.\n\nThese additions are foundations, not a declaration that database encryption, login, biometrics, or reminder workflows are complete. Do not store real patient records until the SQLCipher migration and app-level authentication gate are integrated and device-tested.\n\n## Clinical safety
Derived values and reference classifications are informational, not diagnoses. Use the reference interval printed by the reporting laboratory and consult a qualified clinician. Verify all calculations and workflows before clinical use.

## Build status
The repository has not been built and instrumented on an Android device in this environment. Run Gradle sync/build and test the Python engine before production use.

## Completion work in progress (2026-10-30)

- HealthDb uses the SQLCipher Android helper and a random database passphrase wrapped by Android Keystore.
- A one-time migration exports the existing plaintext database to a temporary encrypted database, checks integrity and per-table row counts, then performs a recoverable file replacement. The old database is retained until validation succeeds.
- A local user-ID/password account-creation and login gate now protects entry to the family workspace. Password-attempt throttling is applied.
- A scientific analytics destination plots all saved date-grouped vital observations (blood pressure, pulse, weight, SpO2, temperature) and reports sample counts, means, standard deviations, and ranges. Missing values are excluded rather than imputed.
- Offline one-time and periodic WorkManager reminders have a management screen and generic lock-screen notification content.
- Android build/unit-test/emulator CI and instrumentation tests for encrypted transfer and database migration have been added.

### Still incomplete / not yet verified

- Native biometric unlock is not yet wired to SQLCipher key release.
- Selective encrypted backup export/import screens, import preview, member mapping, duplicate-safe merge, and selective PDF options still need UI integration.
- Reminder metadata is not yet linked to medicine records/member schedules; WorkManager intervals are approximate and not suitable as exact medication alarms.
- Structured numeric lab trends need per-analyte/unit visualizations; unstructured lab JSON is not silently interpreted.
- No CI run or physical Android-device test has yet been confirmed for this branch. Do not treat this branch as a production release until CI passes and migration/authentication/restore flows are device-tested.

The analytics dashboard is descriptive, not diagnostic. It preserves native units and does not invent clinical thresholds.