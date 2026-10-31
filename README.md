# Health Monitor

<p align="center">
  <strong>Private, offline-first family health records for Android.</strong><br/>
  A native Kotlin app with an embedded Python calculation engine.
</p>

<p align="center">
  <a href="https://github.com/AssayFlux/Health-Monitor/actions/workflows/quality-and-integrity.yml"><img src="https://github.com/AssayFlux/Health-Monitor/actions/workflows/quality-and-integrity.yml/badge.svg?branch=main" alt="Quality and integrity checks"/></a>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Min%20SDK-26-blue" alt="Minimum SDK 26"/>
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin and Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Python-Chaquopy-3776AB?logo=python&logoColor=white" alt="Python via Chaquopy"/>
</p>

Health Monitor brings family profiles, medical history, vital signs, laboratory observations, medicines, appointments and descriptive analytics into one on-device workspace. The application is under active development and is **not a clinically validated or production-ready medical-record system**.

---

## At a glance

<table>
  <tr>
    <td width="50%">
      <h3>🗂️ Family health records</h3>
      Member-linked profiles, medical history, allergies, procedures, immunisations and encounter notes.
    </td>
    <td width="50%">
      <h3>📈 Measurements & reports</h3>
      Vitals, laboratory observations, anthropometry, descriptive charts and native PDF reports.
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>🔒 Local-first foundations</h3>
      SQLCipher database, Android Keystore key wrapping and local authentication components.
    </td>
    <td width="50%">
      <h3>🔗 Connected health data</h3>
      Permission-based Health Connect import, FHIR R4 exchange foundations and reviewed OCR text.
    </td>
  </tr>
</table>


## Offline AI assistant and voice

The Android app includes an optional on-device assistant powered by a GGUF model through a llama.cpp Android runtime. Model weights are **not bundled**; users import a compatible GGUF file into app-private storage. The recommended starting model is the plain `Qwen3.5-4B-Q4_K_M.gguf` (about 2.78 GB) for devices with sufficient memory (8 GB RAM or more). Use a smaller 0.8B–2B quantized model on lower-memory devices. The current packaged runtime is CPU/ARM64; x86_64 emulators can build the app but cannot run this inference backend.

- Text inference runs locally; the app does not send prompts or health records to an AI API.
- Health-record context is **off by default**. The user must explicitly enable read-only context and select one family member. Only that member's available vitals, clinical tests, lab reports, medicines, history, appointments, confirmed interoperable records and OCR text marked for review are included. The context is bounded to recent records and is not a complete chart export.
- Conversations are kept in memory for the current screen session and are not saved to the health database.
- Voice input uses Android's on-device speech recognizer (Android 12 / API 31 or later, with an offline recognition service/model installed). The app does not fall back to cloud speech recognition.
- Spoken replies use an installed TTS voice only when Android reports that voice does not require a network connection. If no offline voice is installed, text chat remains available. Replies are generated as complete responses before speech; live token streaming is not included in the free runtime.
- Model import is a one-time local file copy. The model is stored under app-specific external storage and can be removed by uninstalling the app or clearing app data.

The assistant is informational, not diagnostic. It cannot write health records, prescribe treatment or change medicine doses. AI-generated explanations must be checked against original reports and qualified clinical advice. Model quality and clinical safety are not established by the presence of the feature.

Runtime reference: [llama-android AAR](https://github.com/ffmpegkit-maintained/llama-android) · [Qwen 3.5 4B GGUF model files](https://huggingface.co/Boollm/Qwen3.5-4B-GGUF).

## Technology

| Layer | Stack |
|---|---|
| Android | Kotlin, Jetpack Compose, Material 3 |
| Minimum / compile SDK | API 26 / API 36 |
| Database | SQLCipher for Android |
| Key protection | Android Keystore, AES-GCM |
| Background tasks | AndroidX WorkManager |
| Calculations | Python 3.11 embedded with Chaquopy |
| Interoperability | FHIR R4 JSON mapping, UCUM unit handling |
| Device data | AndroidX Health Connect |
| OCR | Google ML Kit Text Recognition |
| Build | Gradle, Java 17 |

## Getting started

### Requirements

- Android Studio with Android SDK Platform 36
- JDK 17
- Android device or emulator running Android 8.0 (API 26) or later

### Run locally

```bash
git clone https://github.com/AssayFlux/Health-Monitor.git
cd Health-Monitor
```

Open the project root in Android Studio, allow Gradle sync to complete, then run the `app` configuration on a supported device or emulator. The repository currently uses a system Gradle installation (8.13 or newer); it does not include a Gradle wrapper.

### Build and test

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
gradle :app:connectedDebugAndroidTest
```

The connected test task requires a running emulator or attached device. For a Google Colab build, follow [the Colab build guide](docs/GOOGLE_COLAB_BUILD.md). Routine CI intentionally does not package APK/AAB files or boot an emulator. It runs Python lint/format/syntax checks, security and contract regression tests, maintenance-script validation, and Android lint/source compilation. Build packaging and device testing are deferred until the app feature set is ready. Check the [Actions tab](https://github.com/AssayFlux/Health-Monitor/actions) for the latest branch status.

## Project structure

```text
app/
├── src/main/java/com/assayflux/healthmonitor/  # Android application
├── src/main/python/                            # Embedded calculation modules
└── src/androidTest/                            # Android instrumentation tests
docs/                                            # Design and implementation notes
scripts/                                         # Build and audit scripts
tests/                                           # Python regression tests
.github/workflows/                               # CI validation
```

## Privacy and responsible use

The project follows an offline-first approach. The current codebase includes SQLCipher-backed storage, Keystore-wrapped database-key foundations and local authentication components. Legacy plaintext-to-encrypted database migration has been removed; QR-paired offline transfer is now implemented over a shared Wi-Fi/hotspot network. Payloads are sent as plain local HTTP with a short-lived one-time pairing token; local SQLCipher database encryption remains enabled. Received records are reviewed and imported as a separate member profile.

These foundations do not yet constitute a complete security or recovery workflow. Biometric-to-database-key integration, backup/recovery design, broader device validation and independent security review remain outstanding. Do not store sensitive real-world patient information in pre-release builds.

Health Monitor is an informational record-keeping tool—not a medical device, emergency service, diagnostic system or substitute for a qualified healthcare professional. Verify records against original reports and seek professional advice for clinical decisions. WorkManager reminders are inexact and must not be relied on as time-critical medication alarms.

## Implementation status

**Present in the codebase**
- Native family health-record screens and member-linked data.
- Vitals, lab observations, anthropometry and descriptive analytics.
- SQLCipher database helper and Android Keystore-protected database-key storage.
- Python calculation engine integrated through Chaquopy.
- FHIR R4 and Health Connect integration foundations.
- On-device OCR review flow, PDF generation and local reminder foundations.
- Core source, Python contract, security and maintainability validation workflow.

**Still in progress**
- Complete biometric authentication integration and recovery design.
- Full import review, duplicate resolution and transactional merge workflows.
- Comprehensive medicine/appointment reminder behavior.
- Broader clinical unit validation and longitudinal laboratory analytics.
- Migration failure-injection tests and physical-device security validation.

See the [Stage 2 interoperability notes](docs/STAGE_2_INTEROPERABILITY.md), [Stage 3 security and release readiness](docs/STAGE_3_SECURITY_ACCESSIBILITY_RELEASE.md), [UI design specification](docs/UI_DESIGN_AND_DELIVERY.md) and [three-stage delivery plan](docs/GLOBAL_READINESS_THREE_STAGE_PLAN.md).

## Contributing

Issues and pull requests are welcome. For changes involving encryption, migrations, clinical calculations or reminders, include tests for normal, invalid and failure paths. Preserve source units and provenance, and never log credentials or health-record contents.

---

<p align="center">
  <sub>Built by <a href="https://github.com/AssayFlux">AssayFlux</a> · Open source · Active development</sub>
</p>
