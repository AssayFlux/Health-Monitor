# Health Monitor

**A native, offline-first Android application for managing family health records.**

Health Monitor brings personal health observations, laboratory reports, medicines, appointments, reminders, and descriptive analytics into one on-device workspace. It is built with **Kotlin and Jetpack Compose**, with a Python calculation layer embedded through Chaquopy.

> **Project status:** Active development. Core Android screens and encrypted database foundations are present, but several security and data-management workflows remain incomplete. This repository is not yet a clinically validated or production-ready medical-record system. See [Implementation status](#implementation-status).

---

## Contents

- [Highlights](#highlights)
- [Technology stack](#technology-stack)
- [Repository structure](#repository-structure)
- [Getting started](#getting-started)
- [Data model](#data-model)
- [Privacy and security](#privacy-and-security)
- [Analytics and clinical interpretation](#analytics-and-clinical-interpretation)
- [Reminders](#reminders)
- [Build and test](#build-and-test)
- [Implementation status](#implementation-status)
- [Limitations and responsible use](#limitations-and-responsible-use)
- [Contributing](#contributing)

## Highlights

The application is designed around a local-first family health record:

- **People and profiles** — maintain separate records for family members.
- **Medical history** — capture member-linked conditions, allergies, procedures, immunisations, family history, and encounter notes with dates, status, optional codes, and source notes.
- **Vitals** — record blood pressure (including left/right arm readings), pulse, weight, height, oxygen saturation, temperature, dates, and notes.
- **Laboratory reports** — store named reports and structured clinical test observations, including units and reference intervals where supplied.
- **Medicines** — maintain medicine names, dose details, dates, and notes.
- **Appointments and reminders** — manage appointment records and local scheduled notifications.
- **Analytics** — explore saved measurements using date-grouped charts and descriptive statistics.
- **PDF reports** — generate reports on-device using the native PDF implementation.
- **Vintage visual themes** — choose from four warm, restrained palettes in Settings.
- **Clinical interoperability** — versioned clinical projection, FHIR R4 Bundle exchange with a human review queue, terminology identifiers and UCUM unit handling.
- **Health Connect** — permission-based read import for heart rate, steps and body weight.
- **Document OCR** — on-device extraction from report images, saved as unverified text for manual review.
- **Authentication and privacy** — password-gated SQLCipher opening, optional strong-biometric key unlock, local consent controls, encrypted application audit events, and screenshot/backup safeguards.

The interface is native Android UI. The project does not use a website runtime or WebView.

## Technology stack

| Area | Implementation |
|---|---|
| Platform | Android, minimum SDK 26 |
| Language | Kotlin |
| UI | Jetpack Compose and Material 3 |
| Database | SQLCipher for Android |
| Key protection | Android Keystore-backed AES-GCM key wrapping |
| Background work | AndroidX WorkManager |
| Clinical exchange | FHIR R4 JSON mapping and normalized local projection |
| Device health data | AndroidX Health Connect |
| Document text extraction | Google ML Kit Text Recognition |
| Authentication | PBKDF2 local verifier, Android Keystore AES-GCM, BiometricPrompt |
| Privacy controls | Local consent and encrypted application audit history |
| Biometric foundation | AndroidX Biometric and a Keystore-backed key helper |
| Calculation layer | Embedded Python 3.11 via Chaquopy |
| Build language level | Java 17 / Kotlin JVM target 17 |
| Package | `com.assayflux.healthmonitor` |

## Repository structure

```text
.
├── app/
│   └── src/main/
│       ├── java/com/assayflux/healthmonitor/
│       │   ├── MainActivity.kt
│       │   ├── EncryptedDatabaseMigration.kt
│       │   ├── LocalKeyVault.kt
│       │   ├── LocalAuthGate.kt
│       │   ├── BiometricDatabaseKey.kt
│       │   ├── ClinicalInteroperability.kt
│       │   ├── HealthConnectBridge.kt
│       │   ├── InteroperabilityScreen.kt
│       │   ├── ReminderScheduler.kt
│       │   └── NativePdfExport.kt
│       └── python/
└── docs/
    ├── STAGE_2_INTEROPERABILITY.md
    └── UI_DESIGN_AND_DELIVERY.md
```

File names and responsibilities may evolve as implementation is completed.

## Getting started

### Prerequisites

- Android Studio with Android SDK support for **API 36**
- JDK 17
- Android SDK Platform 36 and the required build tools
- An Android device or emulator running **Android 8.0 (API 26) or later**
- Git

Chaquopy downloads and configures its embedded Python runtime during the Gradle build. Allow Gradle to download dependencies on the first sync.

### Open and run

1. Clone the repository:
   ```bash
   git clone https://github.com/AssayFlux/Health-Monitor.git
   cd Health-Monitor
   ```
2. Open the project root in Android Studio.
3. Allow Gradle sync to finish and install any requested SDK components.
4. Select the `app` run configuration.
5. Run on an Android 8.0+ device or emulator.

From a shell, the standard Gradle tasks are:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

The connected test task requires a running emulator or attached Android device. A successful build alone does not establish device-level security or clinical correctness.

## Data model

The local database currently contains these principal tables:

| Table | Purpose |
|---|---|
| `members` | Family-member profiles |
| `vitals` | Dated vital-sign observations |
| `labs` | Laboratory report records and stored report values |
| `medicines` | Medicine details |
| `clinical_tests` | Structured clinical test observations |
| `anthropometry` | Dated anthropometric measurements |
| `appointments` | Scheduled appointment records |
| `reminders` | Reminder definitions and scheduling metadata |
| `clinical_history` | Member-linked conditions, allergies, procedures, immunisations, family history, and encounters |

Records are stored locally in the app's private database. The schema is versioned and the legacy plaintext-to-SQLCipher migration includes integrity and per-table row-count checks before replacing the original database. The migration retains a recoverable copy of the original database until validation succeeds.

## Privacy and security

Health information is sensitive. The project is being developed with an offline-first and data-minimization approach.

### Implemented foundations

- **SQLCipher database:** the app's database helper uses SQLCipher rather than Android's plaintext SQLite helper.
- **Keystore-wrapped database key:** a randomly generated database passphrase is protected by an Android Keystore AES-GCM key.
- **Legacy migration:** an existing plaintext database is exported to a temporary encrypted database, validated, and replaced through a recoverable file operation. The migration verifies database integrity and row counts for the known tables, including appointments and reminders.
- **Local credential primitives:** password-verifier support and progressive authentication throttling are present.
- **Android app sandbox:** database files are kept in app-private storage.
- **Reduced data exposure:** cleartext traffic is disabled; Android backup/data-extraction rules and secure-window handling are configured in the app.

### Important work still required

A security primitive is not the same as a complete user-facing security workflow. In particular:

- Native biometric authentication is **not yet fully connected** to release of the SQLCipher database key.
- Backup, recovery, and sharing are intentionally not available in this build. The previous encrypted transfer helper has been removed and will be redesigned separately.
- Account recovery, reinstall/device-loss recovery, and a complete security review need to be addressed before real-world deployment.
- Security behavior must be tested on supported physical Android devices, not inferred from compilation or emulator tests.

The app is intended to operate locally; the current project does not provide a cloud account, server-side identity, email/phone verification, or cross-device synchronization.

## Analytics and clinical interpretation

The analytics area is intended to present observations transparently rather than diagnose. Current dashboard work includes time-based vital-sign visualizations and descriptive summaries such as sample counts, means, standard deviations, and observed ranges.

Scientific presentation principles:

- Preserve the units and dates associated with source measurements.
- Do not silently impute missing values.
- Keep measurements with different units or analytes in separate series.
- Show sample counts and the time period represented.
- Distinguish descriptive statistics from clinical reference intervals.
- Do not present charts or automated classifications as a diagnosis.

Structured numeric laboratory trends and complete analyte/unit-specific visualizations remain an area of ongoing development. Stored report JSON should not be interpreted as a numeric result unless its structure and units are explicitly understood.

## Reminders

The project uses WorkManager for local background scheduling and notification delivery. WorkManager periodic jobs are inexact: execution can be delayed by Android battery, background-work, and system scheduling policies. They must **not** be relied on as exact-time medication alarms or as a substitute for prescribed care.

Medicine-linked reminder workflows, recurring-dose behavior, notification-permission onboarding, and complete appointment integration still require further implementation and device testing.

## Build and test

The repository includes Android unit/instrumentation test infrastructure and CI workflows. Check the **Actions** tab for the status of the latest run on the branch you intend to use.

Before treating a build as release-ready, verify at minimum:

1. Gradle build and unit tests.
2. Migration from a populated legacy database, including rollback/failure scenarios.
3. SQLCipher key lifecycle and authentication behavior.
4. Confirm that no backup, import, export, or sharing entry point remains in this build; validate that local database encryption and local authentication still work.
5. Notifications and file import/export on supported Android versions.
6. Biometric enrollment, cancellation, lockout, and password fallback on physical devices.

Do not infer that a workflow is complete simply because its helper class or dependency exists.

## Implementation status

**Implemented or present in the current codebase**

- Native Kotlin/Jetpack Compose application and local health-record screens.
- Structured family medical-history capture and browsing (conditions, allergies, procedures, immunisations, family history, encounters).
- Versioned normalized clinical projection with source provenance and verification states.
- FHIR R4 collection Bundle export and staged import review for supported clinical resource types.
- Health Connect read integration for heart rate, steps and weight, subject to provider availability and user permission.
- ML Kit image OCR with manual review before clinical data entry.
- SQLCipher-backed database helper and Keystore-wrapped database passphrase.
- Recoverable legacy database migration with integrity and table row-count validation.
- Local user-ID/password gate and authentication-throttling foundations.
- WorkManager reminder foundations and a reminder-management screen.
- Native PDF generation.
- Descriptive vital-sign analytics and four selectable vintage palettes.
- Android build, unit-test, and instrumentation-test workflow infrastructure.

**Still incomplete or requiring verification**

- BiometricPrompt-to-SQLCipher-key integration using an authenticated cryptographic object.
- Backup, recovery, and sharing (deferred for a separate threat-modelled redesign).
- Editing/deleting history records, comprehensive profile lifecycle, and selective report filters remain in Stage 1.
- Import preview, incoming-member mapping, duplicate-resolution choices, and transactional record merge.
- Selective profile/category/date filtering for PDF export.
- Full medicine/appointment-linked recurring reminder behavior and notification onboarding.
- Complete structured laboratory and anthropometric trend analysis with validated units.
- Broader migration equivalence and failure-injection tests.
- Confirmed green CI for the current commit and physical-device validation of security-sensitive flows.

For interoperability capabilities and limitations, see [Stage 2 interoperability notes](docs/STAGE_2_INTEROPERABILITY.md). For security, privacy, backup threat modelling and release gates, see [Stage 3 security and release readiness](docs/STAGE_3_SECURITY_ACCESSIBILITY_RELEASE.md). For the detailed visual system, screen map, acceptance criteria, and delivery notes, see [UI design and delivery specification](docs/UI_DESIGN_AND_DELIVERY.md). The global-readiness work is organised in the [three-stage delivery plan](docs/GLOBAL_READINESS_THREE_STAGE_PLAN.md).

## Limitations and responsible use

Health Monitor is a personal record-keeping and informational tool under active development. It is not a medical device, electronic health record certified for clinical practice, emergency service, or replacement for a qualified healthcare professional.

- Confirm entries against original reports and clinical records.
- Use the reference interval and units supplied by the reporting laboratory.
- Seek professional medical advice for interpretation or treatment decisions.
- Do not rely on reminders as the sole method for time-critical medication administration.
- Do not use pre-release builds to store sensitive real patient information until the remaining security workflows have been completed and independently tested.

## Contributing

Contributions are welcome. For changes involving database schema, encryption, migration, authentication, clinical calculations, or reminders:

- Include tests for normal, invalid, and failure/rollback paths.
- Preserve source units and provenance for clinical measurements.
- Avoid logging passwords, import codes, passphrases, or health-record contents.
- Document security assumptions and any migration implications.
- Keep clinical language descriptive and avoid unsupported diagnostic claims.

Please open an issue to discuss substantial changes before starting a large implementation.
