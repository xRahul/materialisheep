# Material HN — Technical Design

Package `in.rahulja.materialhn`. Android Hacker News client, MVVM, Java + Kotlin,
minSdk 31, targetSdk/compileSdk 36.

## 1. Layers

- **UI** — Activities and Fragments (`ListFragment`, `ItemFragment`, `WebFragment`,
  `SearchActivity`, `ThreadPreviewActivity`), RecyclerView adapters in `widget/`.
- **View models** — Kotlin `ViewModel` + `StateFlow`/`LiveData` (`StoryListViewModel`,
  `FullscreenViewModel`).
- **Domain/data** — `ItemManager` interface, `HackerNewsClient` (official Firebase API),
  `AlgoliaClient` (search), `FavoriteManager`, `SessionManager`, `ReadabilityManager`,
  `LocalCache` (Room-backed).
- **Platform** — sync adapter stack, JobScheduler refresh, home-screen widget, WebView.

## 2. Dependency injection

Dagger 2 with KSP. `ApplicationComponent` (singleton) composed from
`ApplicationModule`, `DataModule`, `NetworkModule`, `UiModule`, `ActivityModule`.
Schedulers are injected by name (`DataModule.IO_THREAD`, `MAIN_THREAD`).

## 3. Flows

### Story list

`ListFragment` → `StoryListViewModel` (`viewModelScope`, `Dispatchers.IO`) →
`ItemManager.getStories()` → `HackerNewsClient` → batched `getItems()` → `StateFlow`
→ adapter. Blocking Retrofit `.execute()` inside the IO dispatcher.

### Networking and caching

`NetworkModule` builds one OkHttp client: HTTPS only, `followRedirects(false)`,
`CacheOverrideNetworkInterceptor` (1 min feeds, 5 min users, 30 min items, with
error shielding), logging interceptor at `NONE` for release.

### Web content and reader

`WebFragment` + `MaterialWebView`. Settings live in one policy
(`WebViewSettingsPolicy`): remote content gets JavaScript, DOM storage and pinch
zoom; local content gets neither JavaScript nor DOM storage; file and content
access stay off in both. In dark themes remote pages are algorithmically
darkened through `androidx.webkit`. Loads are direct (`loadUrl`, no
`about:blank` round-trip) and visibility is driven by `onPageCommitVisible` /
`onPageStarted`; history is cleared once per fresh load. Main-frame network, SSL
and renderer failures show the in-tab error panel (`include_web_error.xml`,
Retry / Open in browser); `onRenderProcessGone` replaces the dead WebView.
`MaterialWebView` owns its scrolling, claims horizontal gestures only when the
page can pan (`GestureIntent`), and `AppBarScrollSync` drives the activity app
bar. Cleartext stays banned; a main-frame `http://` URL is upgraded to `https://`
(`ArticleUrlPolicy`). `AdBlockWebViewClient` never blocks the main frame.
`ReadabilityClient` waits for the document to settle (`SettleTracker`, 400 ms
poll, 4 s cap, 20 s timeout) before injecting Readability.js, and its hidden
WebView uses DOM storage and the ad blocker. `UrlSanitizer` strips tracking
parameters before a link leaves the app.

### Credentials

`LoginActivity` → `AccountSecurity` (AES-256-GCM under an `AndroidKeyStore` key,
IV prefixed to the ciphertext, stored in `auth_secure_store` preferences). The
`AccountManager` entry carries the username only. Legacy plaintext passwords are
migrated on first read and then cleared.

### Sync and background work

`ItemSyncAdapter` + `SyncDelegate` (RxJava, DB-deferred) with `SyncQueueDao` for
offline vote/save actions; `ItemSyncJobService` schedules periodic refresh;
`ItemSyncNetworkCallback` triggers on connectivity change. Widget refresh uses
`WidgetRefreshJobService`.

### Persistence

Room database `MaterialisticDatabase` v6 (`saved`, `read`, `readable`, `syncqueue`),
schema exported to `app/schemas/`. Writes publish a `SharedFlow`, exposed to the UI
as `LiveData`.

## 4. Security controls

| Area | Control |
|---|---|
| Transport | HTTPS only; cleartext disabled in `network_security_config.xml`; no redirects |
| Credentials | Keystore AES-GCM; ciphertext excluded from backup; `AccountManager` holds username only |
| WebView | JS and DOM storage gated by remote/local; file and content access disabled; SSL errors always cancelled; algorithmic darkening only; cache reads restricted to the app cache directory |
| Links | Tracking parameters stripped; external opens go through Custom Tabs with a validated URL |
| Ads/trackers | Bundled domain blocklist, request interception |
| Backup | `data_extraction_rules.xml` excludes `auth_secure_store` and `app_webview/` |
| Release build | R8 shrinking; signing keys from CI secrets only |
| Telemetry | None. No analytics or crash-upload SDK |

Component exposure (exported components and why) is recorded in the table below;
anything not listed is `android:exported="false"`.

| Component | Exported for | Input handling |
|---|---|---|
| `LauncherActivity` | launcher | none |
| `ItemActivity` | Hacker News item links, app scheme | `AppUtils.getDataUriId` accepts only `https`/`http` on `news.ycombinator.com` or this app's scheme; otherwise finishes |
| `UserActivity` | Hacker News user links | same validation; finishes without a username |
| `NewActivity`, `ShowActivity`, `AskActivity`, `JobsActivity`, `BestActivity`, `PopularActivity` | Hacker News feed links | intent data is not read; the feed is fixed per activity |
| `FavoriteActivity`, `SearchActivity` | `ACTION_SEARCH` from system search | query string only |
| `SettingsActivity` | `ACTION_APPLICATION_PREFERENCES` | none |
| `SubmitActivity` | `ACTION_SEND` share target | subject and text pre-fill the submit form, user confirms |
| `WidgetConfigActivity`, `WidgetProvider` | widget host | refresh broadcast accepted only for widget ids this provider owns |
| `accounts.AuthenticatorService` | `AccountManager` | `signature` permission `in.rahulja.materialhn.permission.AUTHENTICATE_ACCOUNTS` |
| `data.ItemSyncService` | sync framework | `signature` permission `in.rahulja.materialhn.permission.SYNC_ITEM` |
| `data.ItemSyncJobService`, `appwidget.WidgetRefreshJobService` | job scheduler | `android.permission.BIND_JOB_SERVICE` |


## 5. Build

- Gradle 9.7.1 wrapper, AGP 9.2.1, JDK 21 toolchain, Kotlin 2.3.21 / KSP 2.3.8.
- Versions in `version.properties`; overridable with `-PversionCode` and
  `-PversionName` (used by F-Droid).
- Release signing from `KEY_STORE_BASE64`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`; unsigned when absent.
- `resConfigs en, zh-rCN, es`.

## 6. Testing

Robolectric unit tests in `app/src/test` (`@Config(sdk = S)` or newer). The debug
build installs an uncaught-exception handler that starts `CrashActivity`; it is
skipped when `Build.FINGERPRINT == "robolectric"`. Instrumented tests live in
`app/src/androidTest`.

## 7. Known technical debt

- `HackerNewsClient` async APIs return no `Disposable`, so callers cannot cancel
  work after the UI dies.
- `StoryListViewModel` logs fetch failures without surfacing an error state.
- RxJava 3 and coroutines coexist.
- `Preferences.java` is a monolith over `SharedPreferences`.
- Google Gson reflects over `HackerNewsItem` fields; renaming a field without
  `@SerializedName` breaks parsing.
