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
- **WorkManager/Room need explicit keep rules.** `androidx.work:work-runtime-ktx` pulls in Room; `proguard-rules.pro` keeps `RoomDatabase`/`WorkDatabase_Impl` constructors and `ListenableWorker(Context, WorkerParameters)`. Removing them makes release builds crash during `InitializationProvider` startup.
- **System notifications need runtime permission.** `POST_NOTIFICATIONS` is declared in the manifest and requested on API 33+; without it (or with every notification type switched off) `NotificationSyncWorker` silently returns success.
- **The bundled code font is large.** `res/font/maple_mono_nl_nf_cn_regular.ttf` (Maple Mono CN, built-in CJK monospace) is ~20 MB; check APK size before swapping or adding another font.
- `samples/`, `.secret/`, `PLAN.md`, `.memory/`, `local.properties`, `.omx/`, `.worktrees/`, and `.codex/autoreview-state.json` are gitignored local-only. `samples/` holds real Zhihu HTML/HAR fixtures for AST/parser work; never make the build depend on any of these.

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
  - Exceptions: `imageview` is a manager + gesture layer rather than a screen flow (`ImageViewManager` collects URLs via `submitImages()` and opens previews via `show()`; `DragToDismissContainer` owns the swipe-down-to-dismiss gesture). `collection` and `notification` contain multiple flows, each with its own Route/Screen/Signal/ViewModel.
  - `notification` also owns system notifications: `worker/NotificationScheduler` enqueues a 15-minute periodic `NotificationSyncWorker` (network constraint, unique work), `NotificationType` classifies inbox content, and the settings screen turns the schedule on/off. Presentation lives in `core/platform/SystemNotificationPresenter`; tap targets are built from app URLs (`APP_URL_SCHEME` + `toAppUrl`) so notification deep links reuse the normal router.
  - Before migrating a legacy screen or adding a feature module, load `.opencode/skills/feature-migration/SKILL.md` (skill `feature-migration`) for the full contract, anti-patterns, and checklist.
- **Data layer (`data/zhihu/`)**: Remote DataSources in `api/` (`Zhihu<Name>Api.kt`), `@Serializable` DTOs in `dto/`, DTO↔domain mapping in `mapper/` (recently consolidated — don't reintroduce mappers inside repositories), concrete repositories in `repository/` (`Zhihu<Name>Repository.kt`), and wiring in `ZhihuRepositoryFactory.kt`.
  - DTO fields are camelCase: `data/common/ZhihuJson.kt` recursively converts snake_case API keys to camelCase (but preserves dynamic `cookie`/`cookies` subkeys). Use `ZhihuJson` rather than a fresh `Json` instance.
  - Request signing lives in `data/zhihu/crypto/` (`ZhihuFetchSignature.kt`, `ZseSigner.kt`); call `signFetchRequest()` instead of hand-writing `x-zse-*` headers.
  - `data/cache/PostContentCache.kt` is a typed in-memory cache of already-mapped `Post` objects (no JSON round-trip). Detail refreshes must merge through `Post.mergeFallbackFrom`: `author.isFollowing` is nullable and an unknown value must never overwrite a known one (Zhihu's single-content endpoints ignore `is_following`), and a cached non-null `coverUrl` wins over the detail value (detail endpoints provide no stable cover).
- **Core layer (`core/`)**:
  - `environment/`: decoupled runtime environments (`PaginationEnvironment`, `AndroidPaginationEnvironment`, `ZhihuApiEnvironment`) that break data/UI ↔ ViewModel cycles.
  - `error/`: the single home for error handling. `AppError` is the sealed domain model (`Network`/`Timeout`/`Auth`/`Business`/`Http`/`Parse`/`Unknown`, each carrying `userMessage`, `retryable`, `cause`); `Throwable.toAppError()` is the only mapping point and gives server `error.message` priority. `reportFailure(tag, logAction, error, userAction)` rethrows `CancellationException`, logs `Failed to $logAction`, and returns an `AppError` prefixed with `$userAction失败: ` when requested. `HttpStatusException` and `LoginExpiredException` live here too; `raiseForStatus` turns 401 + `ERR_TICKET_NOT_EXIST` into the latter, `toAppError` maps it to `AppError.Auth`, and `reportFailure` emits `LoginExpiredEvents` so the main shell shows one shared relogin dialog. `friendlyErrorMessage()` stays as a thin compat wrapper. Error-handling convention: first-load failure → `UiState.errorMessage: AppError?` + retry UI; incremental failure → effect with `isError = true` (snackbar) while successes stay on toast; ignorable → `reportFailure` for logging only. Data-layer repositories must throw instead of returning `Result`.
  - `platform/`: Android capabilities (`Clipboard`, `FileExporter`, `PlatformDialogs`, `PlatformAppLauncher`, `UserMessage`, `WebView`) plus `SystemNotificationPresenter` (channels, permission check, local notification presentation). `UserMessageSink.showError` prefers the main-shell `SnackbarHost` via `LocalSnackbarHostState` and falls back to toast outside Compose.
  - `content/`: self-built AST pipeline (`Ast.kt`, `AstParser.kt`) rendered by `core/content/renderer/` (Compose/Html/Picture) — no WebView for articles. Inline AST rendering is also used for private-message bubbles (`AstParser.parseInline`).
    - Typography is centralized in `ContentTypographyConfig` (`LocalContentTypographyConfig`): font-size/line-height scaling and `paragraphSpacing()`, which derives block gaps from the current line height. Use it instead of hardcoded `Spacer` sizes so all renderers scale together.
    - Tables (`ContentNode.Table`, nullable `header` from `<thead>`) render with adaptive content-weighted columns, horizontal scroll, Material 3 card styling and zebra rows; paragraph spacing comes from `ContentTypographyConfig`.
    - Inline code backgrounds are drawn manually in `ComposeRenderer` because `SpanStyle.background` cannot be rounded; keep the existing `drawBehind` path instead of reverting to span backgrounds.
  - `settings/`: JSON-backed app settings; `state/`: app-wide unread state; `telemetry/`: `za` protobuf encoding; `util/`: formatters (`Format.kt`, including humanized counters/timestamps), HTTP status checking (`HttpUtils.kt`) and logging (`Log.kt`).
- **UI patterns**: standard Material 3 `ModalBottomSheet` (no legacy custom sheets); single-sheet navigation with overlay input (e.g. `CommentInputOverlay`) to avoid Dialog/window and nested-sheet gesture conflicts; reusable components in `ui/components/`.
- **Pin detail galleries**: multiple images render as a horizontally scrollable strip of fixed 4:3 `ContentScale.Crop` thumbnails (`PinImageGallery`); a single image keeps its natural aspect ratio and caption. Image taps go through `LocalImageViewManager.current?.show(url)` so the viewer opens at the right index.
- `misc/` is dev-only reference material, not build tooling: obfuscated Zhihu `__zse_ck` v4 signing JS + a Tampermonkey cookie hook (the app implements zse96 v2/v3 itself), `install-avd-system-cert.py` for installing a MITM CA on an AVD, and an archived `chrome-zhihu-ad-filter` extension whose rules are stale — current feed ad filtering is the `feed_advert` blocklist in `data/zhihu/api/ZhihuFeedApi.kt`.

## Conventions

- ktlint android style with experimental rules on; exceptions in `.editorconfig` (`max_line_length = 150`, but standard max-line-length and several wrapping rules are disabled). Run `ktlintFormat` before committing.
- UI copy is Chinese-first and hardcoded in Compose code — do not introduce `stringResource`; the only string resource is `app_name` (`res/values` + `values-zh`).
- Don't add an AGPL copyright header to new files (existing files keep theirs).

## Commit messages

- First line: `<type>(<scope>): <English summary>` — imperative mood, ≤ 72 characters, no trailing period.
- Then a blank line, then the detail as a `- ` bullet list (English): one concrete point per bullet, covering behavior changes, public API/data-contract changes, platform/permission/proguard consequences, and tests. Don't just restate the diff.
- Types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `chore`. Scope is the module/area touched (`post`, `content`, `notification`, `imageview`, `build`, ...).
- Keep each commit to one logical change and one scope; split unrelated work instead of piling bullets into one message.

Example:

```
feat(notification): add background system notifications

- Add work-runtime-ktx and the POST_NOTIFICATIONS runtime permission
- Sync unread counts every 15 minutes with a network constraint
- Route notification taps through app URLs so the in-app router handles them
- Keep WorkManager/Room constructors in proguard rules for release builds
```
