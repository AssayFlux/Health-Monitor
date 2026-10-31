# Build Health Monitor in Google Colab

This project currently uses a **system Gradle installation**; it does not include `gradlew`. Use Gradle 8.13 and Java 17. The app compiles against Android SDK 36.

The following cells build a **debug APK and debug AAB**. A GPU runtime is not required: the GGUF model runs on the phone, not in Colab.

## 1. Install Java, Gradle and Android SDK command-line tools

```bash
!apt-get update -qq
!apt-get install -y -qq openjdk-17-jdk unzip wget
!wget -q https://services.gradle.org/distributions/gradle-8.13-bin.zip -O /tmp/gradle-8.13.zip
!unzip -q /tmp/gradle-8.13.zip -d /opt
!mkdir -p /content/android-sdk/cmdline-tools
!wget -q https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip -O /tmp/android-tools.zip
!unzip -q /tmp/android-tools.zip -d /tmp/android-tools
!mv /tmp/android-tools/cmdline-tools /content/android-sdk/cmdline-tools/latest
```

## 2. Set the build environment and install SDK packages

```python
import os
os.environ["JAVA_HOME"] = "/usr/lib/jvm/java-17-openjdk-amd64"
os.environ["ANDROID_HOME"] = "/content/android-sdk"
os.environ["ANDROID_SDK_ROOT"] = "/content/android-sdk"
os.environ["PATH"] = (
    "/opt/gradle-8.13/bin:"
    "/content/android-sdk/cmdline-tools/latest/bin:"
    "/content/android-sdk/platform-tools:"
    + os.environ["PATH"]
)
```

```bash
!yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses
!sdkmanager --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-36" "build-tools;36.0.0"
!gradle --version
```

## 3. Clone the repository

```bash
!git clone https://github.com/AssayFlux/Health-Monitor.git
%cd /content/Health-Monitor
```

If you already cloned it in this runtime, use `%cd /content/Health-Monitor && !git pull origin main` before building.

## 4. Build the debug APK and AAB

```bash
!gradle :app:assembleDebug :app:bundleDebug --no-daemon
```

Expected outputs:

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- AAB: `app/build/outputs/bundle/debug/app-debug.aab`

The debug APK is installable on a compatible Android phone. The AAB is for Google Play / bundle workflows and is not directly installable.

## 5. Copy artifacts to Colab's download area

```python
from google.colab import files
files.download("app/build/outputs/apk/debug/app-debug.apk")
```

For the AAB, use:

```python
from google.colab import files
files.download("app/build/outputs/bundle/debug/app-debug.aab")
```

## Important checks before installing

- The local AI engine's published free AAR contains **ARM64** native libraries. The AI inference feature is intended for ARM64 Android phones; an x86_64 emulator can run other app screens but cannot load this backend.
- The GGUF model is **not inside the APK**. Import a compatible GGUF file from the Assistant screen after installing the app. The suggested Qwen 3.5 4B Q4 model is about 2.78 GB and needs substantial free RAM; an 8 GB phone is a starting point, not a guarantee.
- Offline speech recognition requires Android 12+ and an installed on-device speech-recognition language pack. Offline spoken replies require an installed TTS voice that Android reports as not network-dependent.
- Colab only compiles the application. It does not verify model loading, speech, SQLCipher, biometrics, transfer, notifications or the UI on a physical phone.
- This command builds a debug APK/AAB. A Play Store release needs a properly configured signing key and a separate release build. Do not put signing passwords or keystore files in a public notebook or repository.
- Gradle and Android SDK packages must be downloaded during the Colab build. The installed app's AI inference and the app's selected health-record context are designed to run locally.
