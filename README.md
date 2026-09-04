# SPA2

SPA2 is a self-hosted collection of cipher and code tools for Android
(substitution ciphers, Morse code, Braille, and more). Native UI in Java with
a C/C++ core (PCRE-based search).

## Building

Requirements:
- JDK 17+ (build validated on JDK 17, 24 and 26)
- Android SDK with: platform android-35, build-tools 36.1.0, CMake 3.22.1,
  NDK 27.0.12077973

Point Gradle at your SDK one of these ways:

1. Set `ANDROID_HOME` (recommended, no repo changes needed):

   ```sh
   export ANDROID_HOME=$HOME/Android/Sdk
   ./gradlew :spa2:assembleDebug
   ```

2. Or create `local.properties` in the repo root (see
   `local.properties.example`):

   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

`local.properties` is machine-specific and intentionally not tracked.

The output APK is `spa2/build/outputs/apk/debug/spa2-debug.apk`.

## Versioning

`versionCode`/`versionName` live in `spa2/src/main/AndroidManifest.xml`.
Codes use the scheme `major*10000 + minor*1000 + patch*10 + sub`, so
4.0.0 = 40000. Codes only ever increase (Android requires it for sideloaded
upgrades).