# SPA2

SPA2 (riddle-hunters' "Puzzlehunt Assistant") is a self-hosted collection of
cipher and code tools for Android (substitution ciphers, Morse code, Braille,
and more), plus a checkpoint logbook for field puzzle hunts. Native UI in Java
with a C/C++ core (PCRE-based search). The UI is localized in English and
Czech.

## Screens

The main menu (plus the app icon) opens one tool per screen; each cipher has a
bottom tab bar switching between Encode, Decode and a Reference. The sample
message **"Secret Message"** is used below so the encoded output is visible.
All shots are from the v4.0.0 build on a Pixel 9 Pro XL (SDK 37); they are
maintained as on-device captures attached to
[issue #17](https://github.com/abandonrules/sifry/issues/17).

| Screen | What it does |
|---|---|
| <img src="https://github.com/user-attachments/assets/6bb8a8d2-ca12-40be-b420-299487b846a1" width="280"> | **01 · Splash screen** — app icon on cold start |
| <img src="https://github.com/user-attachments/assets/038516e0-4672-48fa-9fe8-e9e2440e3b02" width="280"> | **02 · Main menu** — every tool, tap to open |
| <img src="https://github.com/user-attachments/assets/154e3261-fa85-4e8e-9222-d1367a1dc19f" width="280"> | **03 · Settings** — locale (English / čeština), logbook auto-numbering and friends |
| <img src="https://github.com/user-attachments/assets/35f6c837-e672-4f31-8dd9-1dccedadab96" width="280"> | **04 · About / Licence** — version (Puzzlehunt Assistant v4.0.0), licensing and component credit (PCRE 8.45) |
| <img src="https://github.com/user-attachments/assets/fee84eee-0f20-4970-80ee-da34bdf9d634" width="280"> | **05 · Release notes** — in-app changelog (v4.0 + older versions) |
| <img src="https://github.com/user-attachments/assets/6349826b-c275-4ccc-8ee8-288cb08199b6" width="280"> | **06 · Morse code** — Encode tab: letters → dots/dashes, binary signal, and timing numbers (1 = dot, 2 = dash, 0 = separator) |
| <img src="https://github.com/user-attachments/assets/e80654b0-70ac-4282-bb51-58bbe5221706" width="280"> | **07 · Braille** — Encode tab: letters → raised-dot pattern, numeric and binary (123456) representation |
| <img src="https://github.com/user-attachments/assets/78e7da63-ebfb-49e7-9afd-709dd0eb8f90" width="280"> | **08 · Letter numbers** — Encode tab: letters → 1-based/0-based numbers and ASCII codes |
| <img src="https://github.com/user-attachments/assets/3f8ec1a7-ade9-4859-96c4-d313398cab8d" width="280"> | **09 · Semaphore** — Encode tab: letters → flag pairs and the numeric mapping used on paper |
| <img src="https://github.com/user-attachments/assets/19a0cc53-894f-4d46-b79d-53161409eaa3" width="280"> | **10 · Cipher grid** — Encode tab: letters into the 3×3+2×2 (or other) grid layout |
| <img src="https://github.com/user-attachments/assets/c0c0af4d-9855-48f6-98e4-9dae20c971da" width="280"> | **11 · Signal flags** — Encode tab: letters → International Code of Signals flag icons |
| <img src="https://github.com/user-attachments/assets/1b436bf5-a88f-4428-887d-969712703c9e" width="280"> | **12 · Frequency analysis** — letter/char counts, alphabetical order and word lengths, grouped per input |
| <img src="https://github.com/user-attachments/assets/cc7f7f3f-a473-41af-a859-11fc0ec52587" width="280"> | **13 · Substitution ciphers** — brute-force all twenty-six Caesar/Atbash shifts at once |
| <img src="https://github.com/user-attachments/assets/2c856d96-670c-4dc7-98ef-634f6edddf6e" width="280"> | **14 · Transposition** — multi-touch shape probing to recover transposed plaintext |
| <img src="https://github.com/user-attachments/assets/70a3718d-5381-4f88-bf19-c62b32802238" width="280"> | **15 · Calendar** — month/year reference grid (e.g. for Caesar-keyed dates) |
| <img src="https://github.com/user-attachments/assets/576fca76-c853-482d-b431-29e137fa2734" width="280"> | **16 · Checkpoint logbook** — add a checkpoint: title, code, check-in time, remark |
| <img src="https://github.com/user-attachments/assets/cd6c92a3-9c63-448c-8ac0-e7a97794a4f5" width="280"> | **17 · Checkpoint logbook** — the field list of captured checkpoints |
| <img src="https://github.com/user-attachments/assets/8dc1fca0-251c-4cb1-81dd-a93f447a3f3d" width="280"> | **18 · Dictionary search** — PCRE/regular-expression filter over the bundled dictionary ("sifr" → words) |

## Dictionary data sources

Dictionary Search searches exactly one dictionary at a time, chosen under
Settings → Dictionary Search. Settings → Data sources has an on/off tickbox
for every bundled dictionary; unticked sources are skipped even when still
selected, and an unselected dictionary that becomes disabled falls back to
the first ticked source.

| Source | Asset | Origin / licence |
|---|---|---|
| Czech | `cs.canon` | aspell-cs v0.60 word list |
| English | `en.canon` | aspell-en v7.1 / SCOWL 7.0 |
| Periodic table | `periodic.canon` | GoodmanSciences gist, 118 elements by name and symbol, e.g. `^actinium:` or `^he:` |
| Pokémon | `pokemon.canon` | cristobalmitchell/pokedex (MIT): names/types, e.g. `^pikachu:`, `^mrmime:`, `^flabebe:` |
| Wordle | `wordle.canon` | steve-kasica/wordle-words: valid answers/guesses, e.g. `^crane:` |

Attribution and licence texts live in app → About → Licence, and in
`spa2/src/main/res/raw/lic_sources.txt`. The sources are reprocessed into
canon form (lowercase `[a-z]` key, `:Display`) and gzipped; the build
unpacks the `.gz` assets (see `tools/canon/gen_canon.py`).

## Building

Requirements:
- JDK 26 (primary; build also validated on JDK 17 and 24)
- Android SDK with: platform android-36, build-tools 36.1.0, CMake 3.22.1,
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
push/PR (JDK 26, platform android-36, build-tools 36.1.0, CMake 3.22.1,
NDK 27.0.12077973, Gradle 9.7.1 from the wrapper). Robolectric is pinned to
4.17-beta-4 — an AGP/JDK-sensitive choice that must advance with the
toolchain (unit tests were validated on JDK 17 and 26).

## Versioning

`versionCode`/`versionName` live in `spa2/src/main/AndroidManifest.xml`.
Codes use the scheme `major*10000 + minor*1000 + patch*10 + sub`, so
4.0.0 = 40000. Codes only ever increase (Android requires it for sideloaded
upgrades).