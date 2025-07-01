# ScreenshotApp

This repository contains a minimal Android application written in Kotlin that demonstrates how to capture the device screen using the MediaProjection API.

The app requests screen capture permission on launch. After permission is granted, a foreground service captures a screenshot every 10 seconds and saves it under the app-specific external storage directory:

```
/storage/emulated/0/Android/data/com.example.screenshotapp/files/screenshots
```

Use the Start and Stop buttons on the main screen to control capturing. The capture state is preserved across configuration changes.

## Build

Run the following command to build the debug APK:

```
./gradlew assembleDebug
```
If the Gradle wrapper JAR is missing, regenerate it with:

```
gradle wrapper --gradle-version 8.0
```
