---
name: feature-module-migration
description: Migrate legacy UI/ViewModel architecture to the feature-module architecture used by zhihuminus. Use when migrating a screen or feature from the legacy flat ui/viewmodel structure, including extracting repository contracts, MVI ViewModels, pure Screens, Routes, data-layer implementations, DTOs, navigation wiring, and removing legacy files.
---

# Feature-Module Migration Skill

Guide for migrating legacy screens (flat `ui/` + `viewmodel/`) to the feature-module architecture used by column, question, comment, post, and daily.

## Target Architecture Overview

```
feature/<name>/
  <Name>.kt              # Domain models + repository interface
  <Name>Signal.kt        # Event (sealed interface) + Effect (sealed interface)
  <Name>ViewModel.kt     # MVI ViewModel: UiState + onEvent() + Channel<Effect>
  <Name>Route.kt         # Composable wiring: creates VM, collects effects, passes state to Screen
  <Name>Screen.kt        # Pure Compose UI: receives state + callbacks, no ViewModel knowledge
  components/            # Sub-composables extracted from Screen

data/zhihu/
  api/Zhihu<Name>Api.kt    # Remote DataSource: endpoint calls and response parsing
  dto/<Name>Dto.kt         # @Serializable wire-format models
  repository/Zhihu<Name>Repository.kt  # Implements feature's repository interface
  ZhihuRepositoryFactory.kt # Unified instantiation and DI factory
```

## Architectural Principles

### 1. Feature layer owns the contract; data layer owns the implementation

**What**: The `feature/` directory defines repository **interfaces** and domain models. The `data/zhihu/` directory provides the concrete implementations.

**Why**: Prevents feature code from depending on HTTP clients, serialization libraries, or API internals. Enables testing with fake repositories.

**Apply when**: Every new feature. The feature's ViewModel and Screen never import from `data/zhihu/`.

### 2. Screen is a pure composable

**What**: `<Name>Screen` takes `(state, onEvent, ...)` callbacks. It never creates ViewModels, repositories, or makes API calls.

**Why**: Separates presentation from business logic. Enables preview, testing, and reuse.

**Boundary**: Screen can read `MaterialTheme`, `LocalNavigator`, `LocalUriHandler`, platform remember* helpers. It cannot access `ViewModel`, `Repository`, `HttpClient`, or coroutine scope for data fetching.

### 3. Route is the only place that knows about dependencies

**What**: `<Name>Route` creates the ViewModel, collects effects, and wires everything. It's the only composable that imports `ZhihuApiImpl`, `ZhihuDailyRepository`, etc.

**Why**: Centralizes dependency creation. Other composables stay decoupled.

**For tab screens**: Route is self-contained — creates its own repository from `rememberPaginationEnvironment()`.

**For detail screens**: Route receives repository as a parameter (wired from `AndroidZhihuMain`).

### 4. ViewModel holds Compose-observable state, not LiveData

**What**: Use `mutableStateOf` for UI state, `Channel<Effect>` for one-shot side effects.

**Why**: Direct Compose integration, no lifecycle ceremony.

**Pattern**:
```kotlin
class FooViewModel(private val repository: FooRepository) : ViewModel() {
    var uiState by mutableStateOf(FooUiState())
        private set

    private val _effect = Channel<FooEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: FooEvent) { ... }
}
```

### 5. Navigation goes through unified URL handling

**What**: Story/link clicks pass URLs to `rememberInAppLinkOpener()` (from `navigation/link/`). Don't build custom navigation logic in ViewModel effects for URL-based navigation.

**Why**: `InAppLinkOpener` handles in-app route resolution, external browser fallback, and URL normalization in one place. Duplicating this logic leads to inconsistency.

**Exception**: ViewModel effects handle navigation for **programmatic** destinations (e.g., "navigate to question after recording read history") — these emit `FooEffect.Navigate(destination)` collected by Route.

### 6. DTOs live in `data/zhihu/dto/`, not in feature layer

**What**: `@Serializable` API response models go in `data/zhihu/dto/<Name>Dto.kt`. Domain models (if different) go in `feature/<name>/<Name>.kt`.

**Why**: Keeps serialization concerns in the data layer. Feature layer works with clean domain types.

**When they're the same**: If the DTO maps 1:1 to the domain model with no transformation needed, the feature can reference the DTO directly (as daily does with `DailyStoryDto`). But the repository interface should still return domain types when there's any mapping.

### 7. Components are extracted when they exceed ~50 lines or are reused

**What**: Sub-composables in `feature/<name>/components/` are extracted from Screen when they're complex enough to warrant their own file, or when they might be reused.

**Naming**: `DailyDateHeader`, `DailyStoryCard` — prefixed with the feature name.

## Migration Workflow

### Phase 1: Analyze legacy code

Before writing any new code, identify:

1. **Data flow**: Where does data come from? (HTTP client, repository, ViewModel direct calls)
2. **State ownership**: What state does the ViewModel hold? What's transient UI state vs. persistent?
3. **Side effects**: What happens besides updating UI? (navigation, toasts, dialogs, API calls on click)
4. **Dependencies**: What does the ViewModel/Screen import? (HttpClient, specific API, platform APIs)
5. **Navigation triggers**: Which user actions cause navigation? Are they URL-based or programmatic?
6. **Reference files**: List all files that import from legacy files (grep for old class names)

### Phase 2: Define the contract

Create the feature directory structure:

```
feature/<name>/
  <Name>.kt          — data classes + repository interface
  <Name>Signal.kt    — events + effects
```

**Repository interface** design:
- Return domain types, not DTOs
- Accept simple parameters (IDs, strings), not HTTP clients
- Name methods by intent (`getLatestDaily`, not `fetchDailyStories`)

**Events** design:
- One event per user action
- No compound events (don't `data class Click(val id, val type)`)
- `Refresh`, `LoadMore`, `SelectDate(id)` are common patterns

**Effects** design:
- One effect per one-shot side effect
- `ShowMessage(text)` for toasts/snackbars
- `Navigate(destination)` for programmatic navigation
- Don't put URL-based navigation in effects — use `InAppLinkOpener` directly in Screen/Route

### Phase 3: Implement ViewModel

```kotlin
class FooViewModel(private val repository: FooRepository) : ViewModel() {
    var uiState by mutableStateOf(FooUiState())  // Compose-observable
        private set

    private val _effect = Channel<FooEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: FooEvent) {
        when (event) {
            is FooEvent.Refresh -> loadData(reset = true)
            is FooEvent.LoadMore -> loadData(reset = false)
        }
    }
}
```

**Key decisions**:
- ViewModel takes repository as constructor parameter (not HttpClient)
- `viewModelScope.launch` for async work, not `CoroutineScope` fields
- Handle `CancellationException` re-throw in catch blocks
- Error state goes in `uiState`, not effects (user should see it)

### Phase 4: Create Screen and components

Move existing UI code into the feature. Key changes:
- Replace `viewModel.xxx` state reads with `state.xxx` parameter
- Replace `viewModel.onEvent(...)` with `onEvent(...)` callback
- Extract reusable/repeatable composables into `components/`
- Remove any API/HTTP imports from Screen

### Phase 5: Create Route

Route is the wiring layer:
```kotlin
@Composable
fun FooRoute(scrollToTopTrigger: Int = 0) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) { ZhihuRepositoryFactory(environment.apiEnvironment).fooRepository }
    val viewModel: FooViewModel = viewModel { FooViewModel(repository) }
    val inAppLinkOpener = rememberInAppLinkOpener()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FooEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
            }
        }
    }

    FooScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onLinkClick = { url -> inAppLinkOpener(url) },
        scrollToTopTrigger = scrollToTopTrigger,
    )
}
```

### Phase 6: Wire into navigation

Update call sites in `ui/ZhihuMain.kt`:
- Replace `FooScreen()` with `FooRoute(scrollToTopTrigger = scrollToTopTrigger)`
- Remove old imports

### Phase 7: Add data layer

1. Add Remote DataSource in `data/zhihu/api/Zhihu<Name>Api.kt`
2. Create `Zhihu<Name>Repository` in `data/zhihu/repository/` implementing feature's interface
3. Wire repository into `ZhihuRepositoryFactory.kt`
4. Move DTOs to `data/zhihu/dto/`

### Phase 8: Delete old files

Remove legacy files and grep to confirm no dangling imports:
```
grep -r "import com.zhihuminus.ui.FooScreen" --include="*.kt"
grep -r "import com.zhihuminus.viewmodel.FooViewModel" --include="*.kt"
```

## Naming Conventions

| Item | Convention | Example |
|------|-----------|---------|
| Feature directory | lowercase singular | `feature/daily/` |
| Repository interface | `<Name>Repository` | `DailyRepository` |
| Repository impl | `Zhihu<Name>Repository` | `ZhihuDailyRepository` |
| DTO file | `<Name>Dto.kt` | `DailyDto.kt` |
| DTO classes | `<Name>Dto` suffix | `DailyStoryDto` |
| Domain models | No suffix | `DailySection` |
| UI State | `<Name>UiState` | `DailyUiState` |
| Events | `<Name>Event` | `DailyEvent` |
| Effects | `<Name>Effect` | `DailyEffect` |
| Composables | `<Name>Screen/Route` | `DailyScreen`, `DailyRoute` |
| Components | `<Name><Component>` | `DailyStoryCard` |

## Responsibility Boundaries

| Layer | CAN | CANNOT |
|-------|-----|--------|
| **Screen** | Read theme, use platform helpers, call `onEvent`, open URLs via `InAppLinkOpener` | Create ViewModels, call repositories, import from `data/`, hold coroutine scope for data fetching |
| **ViewModel** | Hold UI state, call repository, emit effects | Import HTTP client, know about API URLs, hold Compose state directly (use `mutableStateOf`) |
| **Route** | Create ViewModel/Repository, collect effects, wire dependencies | Import Screen internals, hold business logic |
| **Repository (interface)** | Define data contracts | Import HTTP client, Ktor, serialization |
| **Repository (impl)** | Call API, map DTOs, parse responses | Hold UI state, emit effects |
| **Zhihu<Name>Api** | HTTP calls, endpoint response parsing | Hold business logic, know about UI |
| **ZhihuRepositoryFactory** | Instantiate and wire repositories and APIs | Hold business state |
| **Domain models** | Represent business concepts | Have serialization annotations, HTTP dependencies |
| **DTOs** | `@Serializable` data classes for API responses | Contain business logic |

## Anti-Patterns

### 1. Passing `HttpClient` to ViewModel
```kotlin
// BAD
val viewModel = viewModel { DailyViewModel(httpClient) }

// GOOD
val viewModel = viewModel { DailyViewModel(repository) }
```
**Problem**: Couples ViewModel to HTTP layer, prevents testing, leaks network concerns into UI.

### 2. Navigation in ViewModel effects for URL-based links
```kotlin
// BAD
sealed interface FooEffect {
    data class NavigateToStory(val url: String) : FooEffect
}

// GOOD — use InAppLinkOpener in Route/Screen directly
onStoryClick = { url -> inAppLinkOpener(url) }
```
**Problem**: Duplicates URL resolution logic that `InAppLinkOpener` already handles.

### 3. Screen making API calls directly
```kotlin
// BAD
onClick = {
    scope.launch {
        val data = httpClient.get("...").body()
        // parse and navigate
    }
}

// GOOD
onClick = { onEvent(FooEvent.StoryClicked(story.id)) }
// or for simple URL passthrough:
onClick = { onStoryClick(story.url) }
```
**Problem**: Bypasses ViewModel, makes Screen untestable, duplicates business logic.

### 4. Putting all state in effects instead of UiState
```kotlin
// BAD
sealed interface FooEffect {
    data class ShowData(val items: List<Item>) : FooEffect  // one-shot, lost on config change
}

// GOOD
data class FooUiState(val items: List<Item> = emptyList())
```
**Problem**: Effects are one-shot; configuration changes lose the data.

### 5. Feature layer importing data layer internals
```kotlin
// BAD — feature/daily/FooViewModel.kt
import com.zhihuminus.data.fetchDailyStories  // extension function on HttpClient

// GOOD — feature imports only its own interface
import com.zhihuminus.feature.daily.FooRepository
```
**Problem**: Creates circular or inappropriate dependencies, breaks abstraction.

### 6. Creating repository inside Screen instead of Route
```kotlin
// BAD — FooScreen.kt
val repository = remember { ZhihuFooRepository(api) }

// GOOD — FooRoute.kt
val repository = remember(environment) { ZhihuFooRepository(ZhihuApiImpl(environment)) }
```
**Problem**: Screen takes on dependency creation responsibility, coupling it to data layer.

### 7. Skipping the Route layer
```kotlin
// BAD — calling FooScreen directly from ZhihuMain
composable<Foo> { FooScreen() }

// GOOD
composable<Foo> { FooRoute() }
```
**Problem**: Bypasses ViewModel creation, effect collection, and dependency wiring.

## Migration Checklist

Use this checklist for migrating any legacy screen:

- [ ] **Analyze**: grep for all imports of old Screen/ViewModel, list every dependency
- [ ] **Contract**: create `feature/<name>/` with repository interface + domain models + signals
- [ ] **ViewModel**: create MVI ViewModel with `UiState`, `onEvent()`, `Channel<Effect>`
- [ ] **Screen**: move UI code, replace ViewModel state with parameters, extract components
- [ ] **Route**: create wiring composable, self-contained for tabs / parameterized for detail screens
- [ ] **Data**: add API methods to `data/zhihu/api/Zhihu<Name>Api`, create `Zhihu<Name>Repository` in `data/zhihu/repository/`, wire into `ZhihuRepositoryFactory`
- [ ] **DTOs**: move `@Serializable` models to `data/zhihu/dto/`
- [ ] **Wire**: update `ZhihuMain.kt` or `AndroidZhihuMain.kt` call sites
- [ ] **Delete**: remove old files
- [ ] **Verify**: `grep -r` for dangling imports of old classes
- [ ] **Build**: `./gradlew :app:compileDebugKotlin`
- [ ] **Lint**: `./gradlew ktlintCheck`
- [ ] **Test**: `./gradlew :app:testDebugUnitTest` if applicable
