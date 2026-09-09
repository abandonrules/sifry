---
name: code-review
description: Review PRs for Android/Java deprecation issues, security, and correctness in the Sifry puzzlehunt cipher app.
---

# Sifry Code Review Skill

Review pull requests for the Sifry (Puzzlehunt Assistant) Android app.

## Project context

- **Language**: Java (Android), with native C (PCRE regex via JNI)
- **Min SDK**: 21, **Target SDK**: 35
- **Build**: Gradle 9.7.1, AGP 9.4, JDK 17 (temurin in CI, JDK 17 for dev + device)
- **Architecture**: Activity + Fragment (traditional Android, no Compose)
- **Modules**: 12 cipher tools (Morse, Braille, Substitution, Transposition, etc.)
- **Tests**: 3-tier suite — Tier 1+2 JVM/Robolectric unit tests (in CI, `testDebugUnitTest`, 231 tests), Tier 3 on-device instrumented tests (`connectedDebugAndroidTest`, local Pixel) — flag any untested logic paths

## Review checklist

### Deprecated API hotspots (active migration)

These are the known deprecation categories in priority order. Flag any of these:

1. **`android.preference.*`** — migrating to `androidx.preference.*`
   - `PreferenceActivity` → `AppCompatActivity` + `PreferenceFragmentCompat`
   - `PreferenceFragment` → `PreferenceFragmentCompat`
   - `DialogPreference` → AndroidX `DialogPreference`
   - `ListPreference`, `Preference` imports

2. **`android.text.ClipboardManager`** → `android.content.ClipboardManager` + `ClipData`

3. **`Html.fromHtml(String)`** → `HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY)`

4. **`Handler()` no-arg** → `Handler(Looper.getMainLooper())`

5. **`getFragmentManager()`** → `getParentFragmentManager()`

6. **`Bundle.getSerializable(String)`** → type-safe `BundleCompat.getSerializable()`

7. **`setRetainInstance(true)`** → `ViewModel` pattern

8. **`startActivityForResult`/`onActivityResult`** → `ActivityResultLauncher`

9. **`conf.locale` / `updateConfiguration()`** → `createConfigurationContext()` or `AppCompatDelegate.setApplicationLocales()`

### Security checks

- No hardcoded API keys, tokens, or secrets (the app is fully offline — no INTERNET permission)
- No `exported=true` on activities without intent filters (debug manifest overlay is fine)
- Validate any `Intent` extras before use
- Flag any new permission requests (the app intentionally avoids runtime permissions)

### Native code (C/JNI)

- If C files are changed: check for buffer overflows, use-after-free, null pointer dereference
- PCRE 8.45 is vendored in `spa2/src/main/cpp/pcre-mini/` (interpreter-only, JIT disabled) — flag any new PCRE API usage or version changes
- JNI boundary: verify `JNIEnv*` usage, prevent memory leaks across JNI calls

### Code style

- All activities are `final` — flag if someone removes `final`
- Czech/English dual localization: flag any hardcoded user-facing strings
- Each cipher module follows Activity → DFragment/CFragment/RFragment pattern
- No comments unless asked (project convention)
