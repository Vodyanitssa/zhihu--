# AGENTS.md

Android-only fork of [zly2006/zhihu-plus-plus](https://github.com/zly2006/zhihu-plus-plus): a third-party Zhihu client (Kotlin, Jetpack Compose via JetBrains Compose artifacts). Two Gradle modules: `:app` (the whole app) and `:shared-local-db` (Room DB for content filtering/blocklists).

## Commands

- JDK: nothing to install — Gradle daemon auto-downloads JetBrains Runtime 25 (`gradle/gradle-daemon-jvm.properties` + foojay); compile target is Java 17.
- Fast compile check: `./gradlew :app:compileDebugKotlin`
- Full check (ktlint + unit tests): `./gradlew check`
- Lint only: `./gradlew ktlintCheck`; autofix: `./gradlew ktlintFormat`
- Tests (only `:app` has any): `./gradlew :app:testDebugUnitTest --tests "com.zhihuminus.navigation.router.AppRouterRoundTripTest"`
- Build: `./gradlew assembleDebug`

## Gotchas

- **Package ≠ directory.** App code lives in `app/src/main/kotlin/zhihuminus/...` but declares package `com.zhihuminus.*`. Shared-local-db files live under `src/main/kotlin/com/github/zly2006/zhihuminus/...` but declare `com.zhihuminus.data` / `com.zhihuminus.viewmodel.filter`. Never infer imports or new-file paths from directories.
- **material3 is force-pinned** to `1.10.0-alpha05` via `resolutionStrategy` in `app/build.gradle.kts` (strict constraint from material-kolor; mismatched resolution breaks internal APIs at runtime). Don't bump it alone.
- Core UI comes from JetBrains Compose (`org.jetbrains.compose.*`), not AndroidX; AndroidX Compose BOM is used only for icons/tooling.
- Repositories are centralized (`FAIL_ON_PROJECT_REPOS`) — never add `repositories {}` in module build scripts.
- App version lives in root `gradle.properties` (`app.versionName`, `app.versionCode`), not in module config.
- Release builds are unsigned unless env vars `signingKey` (base64 keystore), `keyStorePassword`, `keyAlias`, `keyPassword` are set — see `.secret/signing_env.fish`. Keystores are gitignored.
- Every Kotlin file carries the AGPL-3.0 header from `.copyright` — include it in new files.
- Configuration cache and parallel execution are on; local build cache lives in `.gradle/build-cache`.

## Architecture notes

- Entry point is `MainActivity.kt`; UI is Compose throughout. In-app routing for `zhminus://` deep links is `navigation/router/AppRouter.kt` (+ `AppRouteTable.kt`), round-trip tested by `AppRouterRoundTripTest`.
- Content rendering is a self-built AST pipeline (`core/content/Ast.kt`, `AstParser.kt`) rendered by `core/content/renderer/` (Compose/Html/Picture) — no WebView for articles. Real Zhihu HTML/HAR fixtures for parser work live in `samples/` and `tools/zhihu-formula-corpus/` (both gitignored, local-only).
- LaTeX formula work: baseline AST oracle in `tools/katex-ast-oracle` (Node; `npm ci && npm run generate`). Per its README, after regenerating you must run the `ZhihuFormulaCorpusTest` suite, not just commit the oracle.
- `misc/` holds dev-only reference material, not build tooling: the obfuscated Zhihu `__zse_ck` v4 signing JS + a Tampermonkey cookie hook (for reverse-engineering request signing; the app implements zse96 v2/v3 itself in `ZhihuFetchSignature.kt`), `install-avd-system-cert.py` for installing a MITM CA on an AVD, and an archived `chrome-zhihu-ad-filter` extension whose ad rules are stale vs. the app's current filter logic. Nothing in it is referenced by the build.

## Conventions

- Conventional commits (`feat:`, `fix:`, `refactor:`, `chore:`).
- ktlint android style with experimental rules on; exceptions configured in `.editorconfig` (max line 150, some rules disabled).
- UI copy is Chinese-first and hardcoded in Compose code (no `stringResource` usage); the only string resource is `app_name` (`res/values` + `values-zh`).
- Don't add AGPL copyright header for new files
