# AGENTS.md

Android-only fork of [zly2006/zhihu-plus-plus](https://github.com/zly2006/zhihu-plus-plus): a third-party Zhihu client for Android. Single Gradle module `:app`; namespace/applicationId `com.zhihuminus`; minSdk 28, compileSdk 37, targetSdk 35.

## Commands

- JDK: nothing to install — Gradle auto-downloads JetBrains Runtime 25 (`gradle/gradle-daemon-jvm.properties` + foojay); compile target is Java 17 (Gradle 9.8.0, AGP 9.4.1).
- Fast compile signal: `./gradlew :app:compileDebugKotlin`
- Full pre-finish check: `./gradlew check` — ktlint + Android lint (`lintDebug`) + JVM unit tests. Slower than the individual tasks.
- Lint only: `./gradlew ktlintCheck`; autofix: `./gradlew ktlintFormat`; Android lint only: `./gradlew :app:lintDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`; one class: `./gradlew :app:testDebugUnitTest --tests "com.zhihuminus.navigation.router.AppRouterRoundTripTest"`
- Build APK: `./gradlew assembleDebug`
- Tests are plain JVM `kotlin.test`; there is no `androidTest`/Compose UI test setup. Android stubs return defaults (`unitTests.isReturnDefaultValues = true`), so test business logic, not real platform behavior. Test sources live in `app/src/test/kotlin/zhihuminus/...` but declare `com.zhihuminus.*` packages.

## Gotchas

- **Package ≠ directory.** App code lives in `app/src/main/kotlin/zhihuminus/...` but declares package `com.zhihuminus.*`. Never infer imports or new-file paths from directories.
- **material3 is force-pinned** to `1.10.0-alpha05` via `resolutionStrategy` in `app/build.gradle.kts` (strict constraint from material-kolor; mismatched resolution breaks internal APIs at runtime). Don't bump it alone.
- Core UI comes from JetBrains Compose (`org.jetbrains.compose.*`), not AndroidX; the AndroidX Compose BOM is used only for icons/tooling.
- **No `buildSrc`.** The git hash is resolved dynamically in `app/build.gradle.kts` via `providers.exec`. Temporary signing keystores go to `build/signing/zhihu.jks`, not the source tree.
- Repositories are centralized (`FAIL_ON_PROJECT_REPOS`) — never add `repositories {}` in module build scripts.
- App version lives in root `gradle.properties` (`app.versionName`, `app.versionCode`), not in module config.
- Release builds are unsigned unless env vars `signingKey` (base64 keystore), `keyStorePassword`, `keyAlias`, `keyPassword` are set; local helper is gitignored `.secret/signing_env.fish`. Keystores are gitignored.
- Configuration cache, build cache, and parallel execution are on; local build cache lives in `.gradle/build-cache`.
- HTTP is Ktor + kotlinx.serialization; there is no Retrofit, Hilt, or Koin. Repositories are wired through `ZhihuRepositoryFactory` and created in Compose with `viewModel {}` / `remember`.
- Release builds enable R8 shrinking but intentionally keep `-dontobfuscate` + source/line attributes (`app/proguard-rules.pro`) so crash traces stay readable. Don't remove those without reason.
- `samples/`, `.secret/`, `PLAN.md`, `.memory/`, and `local.properties` are gitignored local-only. `samples/` holds real Zhihu HTML/HAR fixtures for AST/parser work; never make the build depend on any of these.

## Architecture notes

- Entry point is `MainActivity.kt`; UI is Compose throughout. Other activities: login, video player, QR/capture.
- Navigation is typed Compose Navigation in `navigation/` (`NavDestination`, `TopLevelDestination`, etc.). URL/deep-link resolution is `navigation/router/AppRouter.kt` + `AppRouteTable.kt` + `ZhihuWebUrlResolver.kt`, round-trip tested by `AppRouterRoundTripTest`. For content-link clicks use `rememberInAppLinkOpener()` (`navigation/link/`) instead of custom navigation logic.
- Detail routes are declared in `ui/ZhihuMainAndroidContent.kt` (`AndroidZhihuMain`); tab content in `ui/ZhihuMain.kt`. New screens generally need wiring in one of these.
- **Feature architecture (`feature/<name>/`)**: All 15 domain modules live under `app/src/main/kotlin/zhihuminus/feature/`: `collection`, `column`, `comment`, `daily`, `follow`, `history`, `home`, `imageview`, `notification`, `people`, `post`, `question`, `search`, `topic`, `watchlater`. They follow a clean MVI structure:
  - `<Name>.kt` (and sometimes `<Name>Repository.kt`): domain models + repository interface.
  - `<Name>Signal.kt`: `Event` (user intents) and `Effect` (one-shot navigation/snackbars) sealed interfaces.
  - `<Name>ViewModel.kt`: MVI ViewModel with Compose-observable state (`mutableStateOf`), `onEvent(event)`, and `Channel<Effect>`.
  - `<Name>Route.kt`: wiring layer that creates the ViewModel, collects effects, and passes pure state + callbacks to Screen.
  - `<Name>Screen.kt`: pure stateless UI; subcomponents in `feature/<name>/components/`.
  - Exceptions: `imageview` is just domain/manager types (no Screen/ViewModel). `collection` and `notification` contain multiple flows, each with its own Route/Screen/Signal/ViewModel.
  - Before migrating a legacy screen or adding a feature module, load `.opencode/skills/feature-migration/SKILL.md` (skill `feature-migration`) for the full contract, anti-patterns, and checklist.
- **Data layer (`data/zhihu/`)**: Remote DataSources in `api/` (`Zhihu<Name>Api.kt`), `@Serializable` DTOs in `dto/`, DTO↔domain mapping in `mapper/` (recently consolidated — don't reintroduce mappers inside repositories), concrete repositories in `repository/` (`Zhihu<Name>Repository.kt`), and wiring in `ZhihuRepositoryFactory.kt`.
  - DTO fields are camelCase: `data/common/ZhihuJson.kt` recursively converts snake_case API keys to camelCase (but preserves dynamic `cookie`/`cookies` subkeys). Use `ZhihuJson` rather than a fresh `Json` instance.
  - Request signing lives in `data/zhihu/crypto/` (`ZhihuFetchSignature.kt`, `ZseSigner.kt`); call `signFetchRequest()` instead of hand-writing `x-zse-*` headers.
- **Core layer (`core/`)**:
  - `environment/`: decoupled runtime environments (`PaginationEnvironment`, `AndroidPaginationEnvironment`, `ZhihuApiEnvironment`) that break data/UI ↔ ViewModel cycles.
  - `platform/`: Android capabilities (`Clipboard`, `FileExporter`, `PlatformDialogs`, `PlatformAppLauncher`, `UserMessage`, `WebView`).
  - `content/`: self-built AST pipeline (`Ast.kt`, `AstParser.kt`) rendered by `core/content/renderer/` (Compose/Html/Picture) — no WebView for articles. Inline AST rendering is also used for private-message bubbles (`AstParser.parseInline`).
  - `settings/`: JSON-backed app settings; `state/`: app-wide unread state; `telemetry/`: `za` protobuf encoding; `util/`: formatters (`Format.kt`) and error mapping (`FriendlyError.kt`).
- **UI patterns**: standard Material 3 `ModalBottomSheet` (no legacy custom sheets); single-sheet navigation with overlay input (e.g. `CommentInputOverlay`) to avoid Dialog/window and nested-sheet gesture conflicts; reusable components in `ui/components/`.
- `misc/` is dev-only reference material, not build tooling: obfuscated Zhihu `__zse_ck` v4 signing JS + a Tampermonkey cookie hook (the app implements zse96 v2/v3 itself), `install-avd-system-cert.py` for installing a MITM CA on an AVD, and an archived `chrome-zhihu-ad-filter` extension whose rules are stale — current feed ad filtering is the `feed_advert` blocklist in `data/zhihu/api/ZhihuFeedApi.kt`.

## Conventions

- Conventional commits (`feat:`, `fix:`, `refactor:`, `chore:`).
- ktlint android style with experimental rules on; exceptions in `.editorconfig` (`max_line_length = 150`, but standard max-line-length and several wrapping rules are disabled). Run `ktlintFormat` before committing.
- UI copy is Chinese-first and hardcoded in Compose code — do not introduce `stringResource`; the only string resource is `app_name` (`res/values` + `values-zh`).
- Don't add an AGPL copyright header to new files (existing files keep theirs).
