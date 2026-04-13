# Memos Android App - Build Guide

## 1. Local Build Setup

### Prerequisites
- **Java 17 Development Kit (JDK 17)**
- **Android SDK** (usually installed via Android Studio)
- **Environment Variables**: Make sure `ANDROID_HOME` is correctly set to your Android SDK path (e.g., `C:\Users\<user>\AppData\Local\Android\Sdk`). 

### Verify Device Connection
Before building, ensure your phone or emulator is connected, with Developer Options and USB Debugging enabled. 
To verify:
```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices
```
---

## 2. Building the App

This project uses the Gradle Wrapper, meaning you do not need to install Gradle manually. Running the wrapper commands below will auto-download Gradle, fetch all dependencies from `build.gradle.kts`, and compile the application.

### Regenerate and Install (Debug)
To compile the app and install it directly to your connected device in one step, run:

```powershell
.\gradlew.bat installDebug
```
*Note: If you are on Mac/Linux, use `./gradlew installDebug` instead.*

### Build APK only
If you simply want to generate the APK file to drag-and-drop or inspect later:
```powershell
.\gradlew.bat assembleDebug
```
The resulting APK will be found at: `app/build/outputs/apk/debug/app-debug.apk`

To copy this debug APK to your computer's `Downloads` folder, run:
**Windows (PowerShell):**
```powershell
Copy-Item ".\app\build\outputs\apk\debug\app-debug.apk" -Destination "$env:USERPROFILE\Downloads\memos-debug.apk"
```
**Mac / Linux:**
```bash
cp app/build/outputs/apk/debug/app-debug.apk ~/Downloads/memos-debug.apk
```

---

## 3. Opening the App

Once installed, there are two ways to open the app on your phone:

**Option A: Open manually on the device**
Simply look for the `UseMemos` icon in your phone's app drawer and tap to open it.

**Option B: Open via Command Line (ADB)**
If you prefer to launch the app remotely via powershell:
```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n com.usememos.android/.MainActivity
```
---

## 4. Building a Release Build

To generate an optimized release version of the app suitable for production:

```powershell
.\gradlew.bat assembleRelease
```

The resulting release APK will be generated at: `app/build/outputs/apk/release/app-release.apk`

*Note: For actual distribution, you must configure a signing keystore in `app/build.gradle.kts`.*

**Installing the Release APK manually:**
If you want to push the built release APK via ADB to test it, use the following command:
```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r ".\app\build\outputs\apk\release\app-release.apk"
```

---

## 5. Configuring Secure Signing (For App Store / Production Files)

If you plan to distribute the APK, or install it manually via the Android Files app instead of ADB, it **must** be cryptographically signed.

### Step 1: Generate a Keystore
Run this one-time command to generate a release key. Keep it safe and **never** commit it to version control:
```powershell
keytool -genkey -v -keystore release.keystore -alias release_key -keyalg RSA -keysize 2048 -validity 10000
```
This generates a file named `release.keystore`.

### Step 2: Configure Signing config
Ensure your `app/build.gradle.kts` points to this keystore in the `signingConfigs` block. For security, define the keystore variables in your `local.properties` file:

```properties
# Append this to your root local.properties
KEYSTORE_FILE=../release.keystore
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=release_key
KEY_PASSWORD=your_password
```

### Step 3: Extracting to your Downloads Folder
If you're not using ADB, generating your release build (`.\gradlew.bat assembleRelease`) will leave the APK deep inside the `app/build` directory. 

To copy it to your computer's `Downloads` folder so you can transfer it to your phone, run:
**Windows (PowerShell):**
```powershell
Copy-Item ".\app\build\outputs\apk\release\app-release.apk" -Destination "$env:USERPROFILE\Downloads\memos-release.apk"
```
**Mac / Linux:**
```bash
cp app/build/outputs/apk/release/app-release.apk ~/Downloads/memos-release.apk
```
