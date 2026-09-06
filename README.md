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

## Testing

The migration relies on a 3-tier test suite (the "migration contract",
issue #7). Tiers 1 and 2 run on any JVM anywhere; Tier 3 needs a device.

| Tier | Where it runs | Command |
|---|---|---|
| 1. Pure JVM unit tests | CI + local | `./gradlew :spa2:testDebugUnitTest` |
| 2. Robolectric (resources/prefs/XML) | CI + local | `./gradlew :spa2:testDebugUnitTest` |
| 3. Instrumented (native `libregrep`/PCRE, app smoke) | **physical Pixel, local only** | `./gradlew :spa2:connectedDebugAndroidTest` |

Tier 3 — the NDK/PCRE safety net (`RegExpNativeTest` + `AppSmokeTest`) —
intentionally stays on the attached Pixel (`adb -s 52141FDAS001QF`):
GitHub Actions runners cannot host physical devices, and Managed/Firebase
Test Lab is deferred (issue #8). When a step touches native code or JNI,
run Tier 3 locally before closing the issue.

## CI

`.github/workflows/build.yml` runs Tier 1+2 and `assembleDebug` on every
push/PR (JDK 17, platform android-35, build-tools 36.1.0, CMake 3.22.1,
NDK 27.0.12077973, Gradle 9.7.1 from the wrapper). Robolectric is pinned to
4.17-beta-4 — an AGP/JDK-sensitive choice that must advance with the
toolchain (unit tests were validated on JDK 17/26).

## Versioning

`versionCode`/`versionName` live in `spa2/src/main/AndroidManifest.xml`.
Codes use the scheme `major*10000 + minor*1000 + patch*10 + sub`, so
4.0.0 = 40000. Codes only ever increase (Android requires it for sideloaded
upgrades).