When performing a code review, apply the checks defined in `.github/skills/code-review/SKILL.md`.

When performing a code review, focus on:
- Deprecated Android API usage (see the migration checklist in the skill)
- Security: no secrets, no unintended permissions, no exported components without protection
- Resource leaks and null safety in Java code
- JNI safety for any native C changes

When performing a code review, note that this project has a 3-tier test suite:
- Tier 1+2: JVM + Robolectric unit tests (run in CI, `testDebugUnitTest`) — 231 tests
- Tier 3: on-device instrumented tests (`connectedDebugAndroidTest`, run locally on a Pixel)
Native code is only exercised by Tier 3, so native changes must not regress the build or unit tests in CI without an on-device check. Flag any complex logic that lacks test coverage as a recommendation.

When performing a code review, respect the project conventions:
- All module activities are `final`
- Czech/English dual localization (no hardcoded strings)
- No comments in code (project convention)
- Cipher modules follow Activity → DFragment/CFragment/RFragment pattern
