# AGENTS.md

Android-only fork of [zly2006/zhihu-plus-plus](https://github.com/zly2006/zhihu-plus-plus): a third-party Zhihu client (Kotlin, Jetpack Compose via JetBrains Compose artifacts). Single Gradle module `:app`.

## Commands

- JDK: nothing to install — Gradle daemon auto-downloads JetBrains Runtime 25 (`gradle/gradle-daemon-jvm.properties` + foojay); compile target is Java 17 (Gradle 9.8.0, AGP 9.4.1).
- Fast compile check: `./gradlew :app:compileDebugKotlin`
- Full check (ktlint + unit tests): `./gradlew check`
- Lint only: `./gradlew ktlintCheck`; autofix: `./gradlew ktlintFormat`
- Unit tests: `./gradlew :app:testDebugUnitTest` (or specific tests: `./gradlew :app:testDebugUnitTest --tests "com.zhihuminus.navigation.router.AppRouterRoundTripTest"`)
- Build: `./gradlew assembleDebug`

## Gotchas

- **Package ≠ directory.** App code lives in `app/src/main/kotlin/zhihuminus/...` but declares package `com.zhihuminus.*`. Never infer imports or new-file paths from directories.
- **material3 is force-pinned** to `1.10.0-alpha05` via `resolutionStrategy` in `app/build.gradle.kts` (strict constraint from material-kolor; mismatched resolution breaks internal APIs at runtime). Don't bump it alone.
- Core UI comes from JetBrains Compose (`org.jetbrains.compose.*`), not AndroidX; AndroidX Compose BOM is used only for icons/tooling.
- **No `buildSrc`.** `buildSrc` has been eliminated; git hash is resolved dynamically in `app/build.gradle.kts` via `providers.exec`. Temporary signing keystores are written to `build/signing/zhihu.jks` to avoid in-source writes.
- Repositories are centralized (`FAIL_ON_PROJECT_REPOS`) — never add `repositories {}` in module build scripts.
- App version lives in root `gradle.properties` (`app.versionName`, `app.versionCode`), not in module config.
- Release builds are unsigned unless env vars `signingKey` (base64 keystore), `keyStorePassword`, `keyAlias`, `keyPassword` are set — see `.secret/signing_env.fish`. Keystores are gitignored.
- Configuration cache, build cache, and parallel execution are on; local build cache lives in `.gradle/build-cache`.

## Architecture notes

- Entry point is `MainActivity.kt`; UI is Compose throughout. In-app routing for `zhminus://` deep links is `navigation/router/AppRouter.kt` (+ `AppRouteTable.kt`), round-trip tested by `AppRouterRoundTripTest`.
- **Feature architecture (`feature/<name>/`)**: Features are organized as domain modules under `app/src/main/kotlin/zhihuminus/feature/<name>/` (`collection`, `column`, `comment`, `daily`, `history`, `imageview`, `notification`, `post`, `question`, `topic`) following a clean MVI architecture:
  - Feature layer owns domain models and contracts: `<Name>.kt` defines domain models and `<Name>Repository` interface.
  - `<Name>Signal.kt`: Defines `Event` (user actions/intents) and `Effect` (one-shot navigation/snackbars) sealed interfaces.
  - `<Name>ViewModel.kt`: MVI ViewModel holding Compose-observable state (`mutableStateOf`), handling `onEvent(event)` and emitting `Channel<Effect>`.
  - `<Name>Route.kt`: Composable wiring layer that creates ViewModels, collects side effects, and passes pure state and event lambdas to Screen.
  - `<Name>Screen.kt`: Pure stateless UI composable (no ViewModel/Repository knowledge). Subcomponents live in `feature/<name>/components/`.
  - Data layer implementation in `data/zhihu/`: Implements API (`ZhihuApi`, `ZhihuApiImpl`), `@Serializable` DTOs (`dto/<Name>Dto.kt` using camelCase fields mapped to snake_case JSON via `ZhihuJson`), and concrete repositories (`Zhihu<Name>Repository.kt`).
- **Core layer separation (`core/`)**:
  - `core/environment/`: Decoupled runtime environments (`PaginationEnvironment`, `AndroidPaginationEnvironment`, `ZhihuApiEnvironment`, domain environments) to break circular dependencies between data/UI and ViewModels.
  - `core/platform/`: Android platform capabilities (`Clipboard`, `FileExporter`, `PlatformDialogs`, `PlatformAppLauncher`, `WebView`).
  - `core/content/`: Self-built AST pipeline (`Ast.kt`, `AstParser.kt`) rendered by `core/content/renderer/` (Compose/Html/Picture) — no WebView for articles. AST inline rendering is also used for styled inline text like private message bubbles (`AstParser.parseInline`). Real Zhihu HTML/HAR fixtures for parser work live in `samples/` (gitignored, local-only).
  - `core/util/`: Formatters (`Format.kt`) and error mappers (`FriendlyError.kt`).
- **UI patterns**:
  - Standard Material 3 `ModalBottomSheet` replaces legacy custom bottom sheets.
  - Single sheet navigation with overlay input systems (e.g. `CommentInputOverlay`) avoids Dialog window conflicts and nested sheet gesture issues.
  - Reusable components live in `ui/components/` (e.g. `EmojiPicker`).
- `misc/` holds dev-only reference material, not build tooling: the obfuscated Zhihu `__zse_ck` v4 signing JS + a Tampermonkey cookie hook (for reverse-engineering request signing; the app implements zse96 v2/v3 itself in `ZhihuFetchSignature.kt`), `install-avd-system-cert.py` for installing a MITM CA on an AVD, and an archived `chrome-zhihu-ad-filter` extension whose ad rules are stale vs. the app's current filter logic. Nothing in it is referenced by the build.

## Conventions

- Conventional commits (`feat:`, `fix:`, `refactor:`, `chore:`).
- ktlint android style with experimental rules on; exceptions configured in `.editorconfig` (max line 150, some rules disabled).
- UI copy is Chinese-first and hardcoded in Compose code (no `stringResource` usage); the only string resource is `app_name` (`res/values` + `values-zh`).
- Don't add AGPL copyright header for new files.
