<div align="center">

# Health Monitor

**A private, offline-first family health-record workspace for Android.**

Kotlin · Jetpack Compose · SQLCipher · Python 3.11 · Chaquopy

[![Build](https://github.com/AssayFlux/Health-Monitor/actions/workflows/test.yml/badge.svg?branch=main)](https://github.com/AssayFlux/Health-Monitor/actions/workflows/test.yml)
![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?logo=python&logoColor=white)
![Status](https://img.shields.io/badge/Status-Active%20development-orange)

</div>

---

Health Monitor brings family profiles, medical history, vital signs, laboratory observations, medicines, appointments and descriptive analytics into one on-device Android workspace.

> **Development status:** This project is actively evolving. It is not a clinically validated medical-record system, medical device, diagnostic service, or substitute for professional care. Do not rely on pre-release builds for emergency or time-critical clinical decisions.

## What it includes

| Area | Current capabilities |
|---|---|
| Family records | Separate member profiles, medical history, allergies, procedures and encounter notes |
| Measurements | Vitals, laboratory observations, anthropometry and descriptive summaries |
| Private storage | SQLCipher database with an Android Keystore-wrapped random database key |
| Local intelligence | Python calculation modules embedded with Chaquopy |
| Interoperability | FHIR R4 exchange foundations, UCUM unit handling and Health Connect integration |
| Documents | On-device OCR review flow and native PDF export |
| Device transfer | QR-paired local transfer with AES-256-GCM encrypted payloads |
| Reminders | Android WorkManager-based local reminders; delivery is inexact |

### Offline assistant

An optional on-device assistant uses a GGUF model through a llama.cpp Android runtime. Model weights are not bundled with the application; users import a compatible model into app-private storage.

- Text inference is designed to run locally.
- Health-record context is off by default and requires explicit user selection.
- Voice input depends on an installed on-device Android speech-recognition service.
- The assistant is informational and must not diagnose, prescribe, or change treatment.

Model compatibility and clinical safety are not guaranteed by the presence of this feature.

## Technology baseline

| Component | Baseline |
|---|---|
| Android minimum / target / compile SDK | 26 / 36 / 36 |
| Java | 17 |
| Gradle | 8.13 |
| Database | SQLCipher for Android 4.19.1 |
| AndroidX SQLite | 2.7.0 |
| Embedded Python | 3.11 via Chaquopy |
| Native ABIs | arm64-v8a, x86_64 |
| Build outputs | Debug APK and debug AAB |

The complete baseline and safe-upgrade rules are maintained in [maintenance/BUILD_AND_UPGRADE_BASELINE.md](maintenance/BUILD_AND_UPGRADE_BASELINE.md).

## Build

### Requirements

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Gradle 8.13
- Android Studio or a compatible command-line environment

From the repository root:

    git clone https://github.com/AssayFlux/Health-Monitor.git
    cd Health-Monitor
    gradle :app:assembleDebug :app:bundleDebug --no-daemon

Expected outputs:

- app/build/outputs/apk/debug/app-debug.apk
- app/build/outputs/bundle/debug/app-debug.aab

The APK can be installed on a compatible Android device. An AAB is a distribution bundle and is not directly installable.

### Google Colab

This repository is designed to be buildable without a desktop computer. Follow the step-by-step [Google Colab build guide](docs/GOOGLE_COLAB_BUILD.md).

### One simple CI workflow

The repository intentionally has **one workflow**, named **Test**. It checks out the source, configures Java, Android SDK and Gradle, builds the debug APK and AAB, and stores both as short-lived workflow artifacts.

There are no unit-test suites, instrumentation-test suites, lint suites or security-test suites in this workflow. The workflow is a build check, not proof of runtime correctness or clinical safety. Physical-device verification remains necessary.

See the [latest workflow runs](https://github.com/AssayFlux/Health-Monitor/actions/workflows/test.yml).

## Project layout

    app/
      src/main/java/com/assayflux/healthmonitor/  Android application
      src/main/python/                            Embedded calculation engine
      src/main/res/                               Android resources
    docs/                                         Product and implementation notes
    maintenance/
      README.md                                   Maintenance directions
      BUILD_AND_UPGRADE_BASELINE.md               Build baseline and upgrade policy
      KNOWN_ISSUES.md                              Findings and unresolved items
    .github/workflows/test.yml                    Single build workflow

## Privacy and security

Health Monitor is designed around local storage. The current source includes SQLCipher-backed storage, Android Keystore key wrapping, screenshot protection, disabled Android backup, generic reminder notifications and local audit records.

QR transfer encrypts the record payload with AES-256-GCM before transmission. The encryption secret is shared through the scanned QR, not in the HTTP request. The local transfer transport still uses HTTP on the private network, so network metadata remains visible and the app currently permits cleartext traffic for compatibility. Use a trusted private Wi-Fi network or hotspot. This transport limitation is tracked in [maintenance/KNOWN_ISSUES.md](maintenance/KNOWN_ISSUES.md).

Encrypted cloud backup and a complete recovery workflow are not available. **Do not uninstall the app or clear its data as a troubleshooting step**; doing so may permanently remove local health records.

## Safe maintenance

Before changing app code, dependencies, database schema, native libraries, authentication or transfer behavior, read the files in [maintenance/](maintenance/README.md). Preserve the baseline unless a deliberate upgrade is documented. Never silently reset, recreate or delete a user's health database to recover from an error.

The maintenance issue register records what was found, what changed, what remains open, and what evidence is still required. It is updated alongside repository maintenance.

## Responsible use

Health Monitor is an informational record-keeping tool. Verify imported measurements against their original reports. Consult qualified healthcare professionals for diagnosis and treatment. WorkManager reminders are inexact and must not be treated as time-critical medication alarms.

---

<div align="center">

**AssayFlux · Open source · Active development**

</div>
