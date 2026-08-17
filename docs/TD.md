# Materialisheep Technical Documentation

## 1. Architecture Overview

Materialisheep follows a **Model-View-ViewModel (MVVM)** architecture, transitioning from a legacy MVP/Clean-ish structure. The app is a hybrid of Java and Kotlin.

### Layers

1.  **Presentation Layer (UI):**
    -   **Activities/Fragments:** Handle UI rendering and user input. (e.g., `ListActivity`, `ItemActivity`, `ListFragment`).
    -   **ViewModels:** Manage UI state and business logic integration. (e.g., `StoryListViewModel`).
    -   **Pattern:** MVVM. ViewModels expose state via `StateFlow` (Kotlin) or `LiveData` (Java bridge).
    -   **Language:** Mixed. New components are Kotlin.

2.  **Domain/Data Layer:**
    -   **Repositories/Managers:** Abstract data sources. `ItemManager` is the core interface.
    -   **Clients:** Implementations of `ItemManager`.
        -   `HackerNewsClient`: Retrofit/RxJava client for Firebase API.
        -   `AlgoliaClient`: Client for Search API.
    -   **Database:** Room (`MaterialisticDatabase`) for caching and user preferences.
    -   **Concurrency:**
        -   **RxJava 3:** Used heavily in `HackerNewsClient` for chaining API calls (e.g., fetching story + user session + favorite status).
        -   **Coroutines:** Used in ViewModels (`viewModelScope`) to call blocking Data Layer APIs on IO threads.

## 2. Dependency Injection

The project uses **Dagger 2** for dependency injection.

-   **Components:** `ApplicationComponent` (Singleton).
-   **Modules:**
    -   `ApplicationModule`: Context, SharedPreferences.
    -   `DataModule`: Network clients, Database, Gson.
    -   `NetworkModule`: OkHttp, Retrofit, Interceptors.
    -   `UiModule`: UI helpers (Toast, Dialogs).
    -   `ActivityModule`: Activity-scoped bindings.

## 3. Key Components & Flows

### Story Loading Flow
1.  `ListActivity` initializes `ListFragment`.
2.  `ListFragment` obtains `StoryListViewModel` (via `Factory`).
3.  `StoryListViewModel.getStories()` is called.
    -   Checks `isLoading` state.
    -   Updates state to `isLoading = true`.
    -   Launches Coroutine on `IO` dispatcher.
    -   Calls `ItemManager.getStories()` (Blocking).
4.  `HackerNewsClient.getStories()` (Blocking):
    -   Executes Retrofit call synchronously (`.execute()`).
    -   Returns list of `Item` (IDs only).
5.  `StoryListViewModel` updates `StateFlow` with new items.
6.  `ListFragment` observes state change and updates `StoryRecyclerViewAdapter`.
7.  `StoryRecyclerViewAdapter` batches item IDs and calls `ItemManager.getItems()` (Async) to fetch details.

### Web Content Rendering
-   `WebFragment` hosts a `MaterialWebView` (extends `WebView`; package `widget/`) for article rendering.
-   **Security hardening (fork):** `setJavaScriptEnabled(isRemote)` — JavaScript is disabled for local/offline content and only enabled for remote pages and the bundled PDF.js viewer. A `WebFragmentSecurityTest` enforces this contract.
-   **Ad blocking:** `AdBlockWebViewClient` + lock-free `AdBlocker` intercept and drop known ad/tracker requests.
-   **Fullscreen:** `FullscreenViewModel` exposes a `LiveData<Boolean>` `fullscreenEvent()`; `WebFragment`/`ItemActivity`/`BaseListActivity` observe it to expand/collapse the reading view.

### Widgets
-   `WidgetProvider` (app widget) delegates to `WidgetHelper` (`@Inject` HN + Algolia `ItemManager`s).
-   `WidgetHelper` builds `RemoteViews.RemoteCollectionItems`, fetches stories via `getStories(...)`, and refreshes stale items individually via `getItem(...)`.
-   `WidgetRefreshJobService` (JobScheduler) drives periodic refreshes based on the configured widget frequency.
-   App widgets use `appwidget` (default), `appwidget_dark`, or `appwidget_light` layouts.

### Error Handling & Observability
-   **Error Handling:**
    -   `HackerNewsClient` and `AlgoliaClient` catch network exceptions in async callbacks and pass them to `ResponseListener.onError`.
    -   Sync methods log errors to `Log.e`.
    -   `StoryListViewModel` catches exceptions, logs them, and updates `UiState.error`.
-   **Logging:**
    -   **Debug Builds:** Full `HttpLoggingInterceptor` (Headers/Body).
    -   **Application Logs:** Critical flows (ViewModel fetch, Adapter updates) are logged via `android.util.Log` in Debug builds.
-   **Crash Reporting (fork):** Debug builds install a default uncaught-exception handler that launches `CrashActivity`; the handler is bypassed under Robolectric (`Build.FINGERPRINT == "robolectric"`) so unit tests do not self-terminate.

## 4. Known Technical Debt & Risks

### Fragility
-   **RxJava Subscriptions:** `ItemManager` methods do not return `Disposable`. Async requests are "fire and forget". `WeakReference` in listeners prevents UI leaks, but background work cannot be cancelled, leading to potential resource waste.
-   **Blocking Calls:** `StoryListViewModel` relies on blocking calls (`execute()`) which bypasses some RxJava composition benefits.

### Refactoring Status
-   `StoryListViewModel` was hardened to handle errors and state explicitly.
-   `HackerNewsClient` was patched to prevent swallowed exceptions.
-   Upstream deprecation migrations (Parcelable, NotificationCompat, LocalBroadcastManager, Vibrator/Display, Preference APIs, etc.) are merged in; deprecated `SystemUiHelper` was replaced with a `WindowInsetsController`-based implementation.

## 5. Build System
-   **Gradle:** 9.5.1
-   **JDK:** 21 Required.
-   **Android Gradle Plugin:** 9.2.1
-   **Kotlin:** 2.3.21 (KSP 2.3.8)
-   **Min SDK:** 31.
-   **Dependencies:** Dagger 2.59, OkHttp 5.3.2, Retrofit 3.0.0, Room 2.8.4, Material 1.14.0, Browser 1.10.0, Mockito 5.23.0.
