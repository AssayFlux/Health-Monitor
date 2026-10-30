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

## Clinical safety
Derived values and reference classifications are informational, not diagnoses. Use the reference interval printed by the reporting laboratory and consult a qualified clinician. Verify all calculations and workflows before clinical use.

## Build status
The repository has not been built and instrumented on an Android device in this environment. Run Gradle sync/build and test the Python engine before production use.
