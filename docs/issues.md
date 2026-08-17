# Materialisheep Comprehensive Issues & Audit Report

This document records the exhaustive, multi-role review and adversarial **Gauntlet-Loop** audit of the `materialisheep` repository (codebase, architecture, UI/UX, security, build systems, CI/CD pipelines, test coverage, and documentation), along with completed remediations and newly verified findings.

---

## Executive Summary & Taxonomy

### Issues by Category & Severity
| Category | Historical Resolved | Newly Audited & Verified | Total Issues | Key Focus Areas |
|---|---|---|---|---|
| **Critical** | 8 | 0 | 8 | Options menu, Security permissions, Disposables, Widget click navigation, Sync queue crash, CodeQL SARIF upload |
| **High** | 8 | 0 | 8 | Singleton safety, Insets, Predictive back, Jsoup parsing, Back button dispatch, OkHttp response leaks |
| **Medium** | 7 | 0 | 7 | Dead dependencies, Room schema, Background web cache, Comment squeeze, Cursor leak, PDF stream read safety |
| **Low / Tech Debt** | 3 | 0 | 3 | Dynamic theming, Obsolete SDK version checks (API 30 on minSdk 31), Deprecated Bundle Parcelable APIs |
| **Doc Sync Errors** | 7 | 0 | 7 | Min/target SDK sync, `ItemManager` `Disposable` status, `TODO.md` pruning, Clone repo URLs |

---

## 1. Functional Bugs & Runtime Logic Issues

### [CRIT-01] Fragment `MenuProvider` Migration Incomplete: Options Menu Clicks Dropped
- **Severity:** `Critical`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`BaseFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/BaseFragment.java#L63-L85)
  - [`BaseListFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/BaseListFragment.java#L129-L138)
  - [`ItemFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/ItemFragment.java#L199-L208)
  - [`FavoriteFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/FavoriteFragment.java#L161-L172)
- **Problem Statement:**
  [`BaseFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/BaseFragment.java) registers a modern `MenuProvider` which delegates menu clicks to `BaseFragment.this.onMenuItemSelected(MenuItem)`. Child fragments previously overrode deprecated `onOptionsItemSelected(MenuItem)`.
- **Resolution:**
  Changed all child fragment methods to `@Override public boolean onMenuItemSelected(MenuItem item)` and removed deprecated `setHasOptionsMenu(true)`.

---

### [CRIT-02] `SettingsActivity` Declares System Permission `WRITE_SECURE_SETTINGS`
- **Severity:** `Critical`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`AndroidManifest.xml`](file:///home/rahul/projects/materialisheep/app/src/main/AndroidManifest.xml)
- **Problem Statement:**
  `android.permission.WRITE_SECURE_SETTINGS` was declared on `SettingsActivity`, causing third-party launcher apps and preference intents to crash with `SecurityException`.
- **Resolution:**
  Removed signature-level `android:permission="android.permission.WRITE_SECURE_SETTINGS"` from `SettingsActivity` in `AndroidManifest.xml`.

---

### [CRIT-03] Uncancellable RxJava Subscriptions & Missing `Disposable` Lifecycle Management
- **Severity:** `Critical`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`HackerNewsClient.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/data/HackerNewsClient.java)
  - [`AlgoliaClient.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/data/AlgoliaClient.java)
  - [`UserServicesClient.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/accounts/UserServicesClient.java)
  - [`AdBlocker.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/AdBlocker.java)
- **Problem Statement:**
  RxJava subscriptions were made without capturing disposables, allowing network and background threads to run indefinitely.
- **Resolution:**
  Updated `ItemManager` methods (`getStories`, `getItem`, `getItems`), `UserServicesClient` (`reply`, `voteUp`), and `AdBlocker.init` to return and manage active `Disposable` references.

---

### [CRIT-06] Home Screen Widget Uses `FLAG_IMMUTABLE` with `setPendingIntentTemplate`, Breaking Item Clicks (Android 12+)
- **Severity:** `Critical`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`WidgetHelper.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/appwidget/WidgetHelper.java#L268-L273)
  - [`WidgetHelperTest.java`](file:///home/rahul/projects/materialisheep/app/src/test/java/io/github/sheepdestroyer/materialisheep/appwidget/WidgetHelperTest.java)
- **Problem Statement:**
  In [`WidgetHelper.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/appwidget/WidgetHelper.java), `remoteViews.setPendingIntentTemplate` was initialized with `PendingIntent.FLAG_IMMUTABLE`. On Android 12+ (`minSdk = 31`), immutable PendingIntents prevent the Android framework from applying fill-in data (`setOnClickFillInIntent`), breaking story click navigation from the home screen widget.
- **Resolution:**
  Updated the template PendingIntent flag to `PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT`, removed unused `mAlarmManager` field, and added Robolectric multi-SDK unit tests verifying mutable template flag behavior.

---

### [CRIT-07] `SyncDelegate.stopSync()` Throws `NumberFormatException` on Null `job.id` During Batch Sync
- **Severity:** `Critical`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`SyncDelegate.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/data/SyncDelegate.java#L420-L435)
  - [`SyncDelegateFinishTest.java`](file:///home/rahul/projects/materialisheep/app/src/test/java/io/github/sheepdestroyer/materialisheep/data/SyncDelegateFinishTest.java)
- **Problem Statement:**
  When `SyncDelegate` executes a batch queue sync via `mSyncQueueDao.getAll()`, `mJob.id` is null. When all items finished, `finish()` called `stopSync()`, which unconditionally executed `int id = Integer.valueOf(mJob.id);`, crashing the background sync with `NumberFormatException: null`.
- **Resolution:**
  Guarded the parsing logic with `if (mJob != null && !TextUtils.isEmpty(mJob.id))` and `try-catch (NumberFormatException)`, and added automated unit tests verifying null-ID batch sync completion.

---

### [HIGH-01] Non-Thread-Safe Global State & Singletons
- **Severity:** `High`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`FontCache.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/FontCache.java)
  - [`AlgoliaClient.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/data/AlgoliaClient.java)
  - [`Preferences.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/Preferences.java)
- **Problem Statement:**
  Unsynchronized singletons and unsynchronized collections accessed across UI and worker threads.
- **Resolution:**
  Added thread-safe `ConcurrentHashMap` and double-checked locking singletons in `FontCache`, `volatile` modifier on `AlgoliaClient.sSortByTime`, and synchronized static sets in `Preferences.Observable`.

---

### [HIGH-02] Brittle HTML Screen-Scraping for Authentication & Voting
- **Severity:** `High`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`UserServicesClient.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/accounts/UserServicesClient.java)
- **Problem Statement:**
  Regex pattern matchers were used to parse HTML forms from HN login and submit pages.
- **Resolution:**
  Replaced regex scraping with robust DOM parsing via `org.jsoup.Jsoup` (`selectFirst`, `body().ownText()`).

---

### [HIGH-07] `WebFragment` `OnBackPressedCallback` Intercepts Back Press on Comments Tab in `ItemActivity`
- **Severity:** `High`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`WebFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/WebFragment.java#L177-L184)
  - [`ItemActivity.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/ItemActivity.java#L642-L647)
  - [`WebFragmentTest.java`](file:///home/rahul/projects/materialisheep/app/src/test/java/io/github/sheepdestroyer/materialisheep/WebFragmentTest.java)
- **Problem Statement:**
  [`WebFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/WebFragment.java) only checked `isCurrentPage` when hosted in `BaseListActivity`. In `ItemActivity`, pressing Back while viewing comments on position 0 hijacked navigation into the hidden WebView history.
- **Resolution:**
  Added `isCurrentPage(Fragment)` to `ItemActivity.java`, added `isCurrentPage` validation inside `WebFragment.java`'s back callback, and added automated unit tests verifying Back behavior when active vs off-screen.

---

### [HIGH-08] Unclosed OkHttp `Response` Instances Leak Sockets & Connections in `UserServicesClient`
- **Severity:** `High`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`UserServicesClient.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/accounts/UserServicesClient.java#L103-L160)
- **Problem Statement:**
  In `UserServicesClient.login`, `voteUp`, and `reply`, OkHttp `Response` objects returned from `execute(request)` were mapped or redirected without calling `response.close()` in `try-finally` blocks, leaking socket descriptors and connection pool slots.
- **Resolution:**
  Wrapped response handling across `login()`, `voteUp()`, `reply()`, and `submit()` with `try-finally { response.close(); }`.

---

## 2. Modern Android Development (MAD) & Platform Compliance

### [CRIT-04] Outdated Embedded JavaScript Assets (`pdf.js` & `Readability.js`)
- **Severity:** `Critical`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`app/src/main/assets/Readability.js`](file:///home/rahul/projects/materialisheep/app/src/main/assets/Readability.js)
  - [`app/src/main/assets/pdf/`](file:///home/rahul/projects/materialisheep/app/src/main/assets/pdf/)
- **Problem Statement:**
  Arc90 2010 readability script and outdated pdf.js 1.9.658 failed on modern HTML5/PDFs.
- **Resolution:**
  Upgraded to `@mozilla/readability` v0.5.0 and `pdf.js` v4.10.38 with lazy-allocated page canvases.

---

### [HIGH-03] Missing Android 13–16 Predictive Back Support
- **Severity:** `High`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`AndroidManifest.xml`](file:///home/rahul/projects/materialisheep/app/src/main/AndroidManifest.xml)
  - [`WebFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/WebFragment.java)
- **Problem Statement:**
  Lacked `android:enableOnBackInvokedCallback="true"` and modern back dispatcher routing.
- **Resolution:**
  Enabled `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml` and wired WebView back history through `OnBackPressedDispatcher`.

---

### [HIGH-04] Android 15 Edge-to-Edge System Bar Inset Clashing
- **Severity:** `High`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`ThemedActivity.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/ThemedActivity.java)
  - [`ListActivity.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/ListActivity.java)
- **Problem Statement:**
  Bottom navigation bars overlapped FABs and RecyclerView items under Android 15 edge-to-edge enforcement.
- **Resolution:**
  Configured `ViewCompat.setOnApplyWindowInsetsListener` to consume system bar insets and apply padding dynamically.

---

### [MED-01] Background Sync & Background Safety
- **Severity:** `Medium`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`SyncDelegate.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/data/SyncDelegate.java)
- **Problem Statement:**
  Background service startup (`Context.startService(WebCacheService)`) risked `IllegalStateException` on Android 8.0+ when triggered from background sync jobs.
- **Resolution:**
  Routed web caching through main `Handler.post` dispatch with exception guarding, and added explicit `mWebView.destroy()` cleanup on completion and `stopSync()`.

---

### [MED-02] Room Database Schema Export
- **Severity:** `Medium`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`MaterialisticDatabase.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/data/MaterialisticDatabase.java)
  - [`app/build.gradle`](file:///home/rahul/projects/materialisheep/app/build.gradle)
- **Problem Statement:**
  `exportSchema = false` disabled Room schema exports.
- **Resolution:**
  Set `exportSchema = true` and configured `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`, successfully generating versioned schema JSON artifacts (`app/schemas/`).

---

### [MED-06] `FavoriteManager.FavoriteRoomLoader` Leaks `SQLiteCursor` on Multiple Loads
- **Severity:** `Medium`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`FavoriteManager.kt`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/data/FavoriteManager.kt#L123-L126,L394-L398)
  - [`FavoriteManagerTest.kt`](file:///home/rahul/projects/materialisheep/app/src/test/java/io/github/sheepdestroyer/materialisheep/data/FavoriteManagerTest.kt)
- **Problem Statement:**
  Inside `FavoriteRoomLoader.load()`, `cursor = Cursor(it)` reassigned the `cursor` field on every reload without invoking `cursor?.close()` on the previous instance, leaking SQLite native cursors.
- **Resolution:**
  Cached and closed previous cursor on reloads (`oldCursor?.close()`), added explicit `cursor?.close()` in `detach()`, and hardened column lookups against null/missing values.

---

### [MED-07] `PdfAndroidJavascriptBridge.getChunk` Susceptible to Incomplete Buffer Reads
- **Severity:** `Medium`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`WebFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/WebFragment.java#L709-L733)
- **Problem Statement:**
  `mRandomAccessFile.read(data)` did not guarantee reading all requested bytes into the array in a single call, causing corrupted Base64 chunks sent to PDF.js.
- **Resolution:**
  Added sanity checks for chunk bounds and size limits, and switched to `mRandomAccessFile.readFully(data)` with `EOFException` guarding.

---

## 3. Build System & CI/CD Pipeline Deficiencies

### [CRIT-05] GitHub Actions Release Tag Overwrite in `release.yml`
- **Severity:** `Critical`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`.github/workflows/release.yml`](file:///home/rahul/projects/materialisheep/.github/workflows/release.yml)
- **Problem Statement:**
  Release workflow hardcoded `v3.4.<run_number>` tag, ignoring developer-pushed tags.
- **Resolution:**
  Configured `tag_name: ${{ github.ref_name }}` and `name: Release ${{ github.ref_name }}`.

---

### [CRIT-08] CodeQL Analysis Disables SARIF Upload via `upload: false`
- **Severity:** `Critical`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`.github/workflows/codeql.yml`](file:///home/rahul/projects/materialisheep/.github/workflows/codeql.yml#L114)
- **Problem Statement:**
  `codeql.yml` explicitly set `upload: false` in `github/codeql-action/analyze@v4`, silently suppressing SARIF analysis upload to GitHub Security.
- **Resolution:**
  Removed `upload: false` from `.github/workflows/codeql.yml`.

---

### [MED-03] Deprecated AGP Flags & Built-in Kotlin Migration
- **Severity:** `Medium`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`gradle.properties`](file:///home/rahul/projects/materialisheep/gradle.properties)
  - [`app/build.gradle`](file:///home/rahul/projects/materialisheep/app/build.gradle)
- **Problem Statement:**
  `android.newDsl=false`, `android.builtInKotlin=false`, and `kotlin-android` plugin caused deprecation warnings and blocked AGP 10.
- **Resolution:**
  Removed deprecated properties from `gradle.properties`, removed `kotlin-android` plugin in favor of AGP 9.2+ built-in Kotlin support, and modernized DSL options (`minSdk = 31`, `targetSdk = 36`, `lint { ... }`).

---

### [MED-04] Dead Dependency: `androidx.localbroadcastmanager`
- **Severity:** `Medium`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`app/build.gradle`](file:///home/rahul/projects/materialisheep/app/build.gradle)
- **Problem Statement:**
  Unused `localbroadcastmanager` dependency included in APK.
- **Resolution:**
  Removed `androidx.localbroadcastmanager` from `app/build.gradle`.

---

### [LOW-02] Obsolete SDK Checks in `AppUtils.java`
- **Severity:** `Low`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`AppUtils.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/AppUtils.java#L733)
- **Problem Statement:**
  `if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)` was redundant because the application's `minSdk` is 31 (Android S, API 31 > API 30).
- **Resolution:**
  Removed dead legacy branch and simplified `getDisplayWidth` to direct `WindowMetrics` calculation.

---

### [LOW-03] Deprecated `Bundle.getParcelable` in `ItemFragment.java`
- **Severity:** `Low`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`ItemFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/ItemFragment.java#L117,L119,L122)
- **Problem Statement:**
  [`ItemFragment.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/ItemFragment.java) used deprecated `Bundle.getParcelable(String)` without class parameters, producing compiler deprecation warnings.
- **Resolution:**
  Migrated to `BundleCompat.getParcelable(bundle, key, Item.class)` / `BundleCompat.getParcelable(bundle, key, WebItem.class)` consistent with `WebFragment` and `ItemActivity`.

---

## 4. Test Strategy & Quality Assurance Gaps

### [HIGH-05] Robolectric Multi-SDK Target Matrix
- **Severity:** `High`
- **Status:** `Fixed`
- **Impacted Files:**
  - Unit tests in `app/src/test/`
- **Problem Statement:**
  Robolectric tests were only running on API 31.
- **Resolution:**
  Configured supported SDK test matrix across API 31, 34, and 35.

---

### [HIGH-06] Baseline Android Instrumentation Tests (`androidTest`)
- **Severity:** `High`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`app/build.gradle`](file:///home/rahul/projects/materialisheep/app/build.gradle)
  - [`app/src/androidTest/java/io/github/sheepdestroyer/materialisheep/ListActivityTest.kt`](file:///home/rahul/projects/materialisheep/app/src/androidTest/java/io/github/sheepdestroyer/materialisheep/ListActivityTest.kt)
- **Problem Statement:**
  Zero UI / Espresso instrumentation tests existed in repository.
- **Resolution:**
  Configured `AndroidJUnitRunner` and added `ListActivityTest.kt` with Espresso assertions.

---

## 5. UI/UX Design & Usability Issues

### [MED-05] Deep Comment Squeeze on Mobile Screens
- **Severity:** `Medium`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`SinglePageItemRecyclerViewAdapter.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/widget/SinglePageItemRecyclerViewAdapter.java)
  - [`ThreadPreviewAdapter.kt`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/widget/ThreadPreviewAdapter.kt)
  - [`CommentItemDecoration.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/widget/CommentItemDecoration.java)
  - [`CommentIndentationTest.java`](file:///home/rahul/projects/materialisheep/app/src/test/java/io/github/sheepdestroyer/materialisheep/widget/CommentIndentationTest.java)
- **Problem Statement:**
  Uncapped indentation margins (`mLevelIndicatorWidth * level`) crushed nested comments into narrow vertical strips.
- **Resolution:**
  Clamped effective indentation margin and guide line count to `MAX_INDENT_LEVEL = 4` across all comment adapters and decorations.

---

### [LOW-01] Material You Dynamic Theming (Monet)
- **Severity:** `Low`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`MaterialisticApplication.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/MaterialisticApplication.java)
  - [`Preferences.java`](file:///home/rahul/projects/materialisheep/app/src/main/java/io/github/sheepdestroyer/materialisheep/Preferences.java)
  - [`preference_keys.xml`](file:///home/rahul/projects/materialisheep/app/src/main/res/values/preference_keys.xml)
  - [`preferences_display.xml`](file:///home/rahul/projects/materialisheep/app/src/main/res/xml/preferences_display.xml)
- **Problem Statement:**
  Lacked support for Android 12+ wallpaper-based dynamic color palettes.
- **Resolution:**
  Integrated `DynamicColors.applyToActivitiesIfAvailable(this, DynamicColorsOptions)` with user preference toggle.

---

## 6. Documentation Synchronization Discrepancies

### [DOC-01..04] Baseline Documentation Synchronization
- **Severity:** `Doc Sync`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`PROJECT_KNOWLEDGE.md`](file:///home/rahul/projects/materialisheep/PROJECT_KNOWLEDGE.md)
  - [`CI.md`](file:///home/rahul/projects/materialisheep/CI.md)
  - [`AGENTS.md`](file:///home/rahul/projects/materialisheep/AGENTS.md)
  - [`TODO.md`](file:///home/rahul/projects/materialisheep/TODO.md)
- **Resolution:**
  Synchronized Min SDK (31), Target SDK (36), Gradle (9.5.1), AGP (9.2.1), Retrofit (3.0.0), `debug.yml` trigger (`workflow_dispatch`), and task completion status across all documentation.

---

### [DOC-05] `docs/SUMMARY_REPORT.md` & `docs/TD.md` State `ItemManager` Lacks `Disposable`
- **Severity:** `Doc Sync`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`docs/SUMMARY_REPORT.md`](file:///home/rahul/projects/materialisheep/docs/SUMMARY_REPORT.md#L41)
  - [`docs/TD.md`](file:///home/rahul/projects/materialisheep/docs/TD.md#L79)
- **Problem Statement:**
  Both documents stated `ItemManager` methods do not return `Disposable` and requests are fire-and-forget. In reality, `ItemManager` methods (`getStories`, `getItem`, `getItems`) were refactored to return `Disposable`.
- **Resolution:**
  Synchronized `SUMMARY_REPORT.md` and `TD.md` to reflect active `Disposable` subscription support.

---

### [DOC-06] `TODO.md` Contains Obsolete `minSdk` 28 Upgrade Goal
- **Severity:** `Doc Sync`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`TODO.md`](file:///home/rahul/projects/materialisheep/TODO.md#L32-L34)
- **Problem Statement:**
  `TODO.md` listed "Consider upgrading minSdk to 28 for architectural benefits". The project `minSdk` is already 31.
- **Resolution:**
  Removed obsolete item from `TODO.md`.

---

### [DOC-07] Clone Repository URL and CI Trigger Inaccuracies in `README.md` & `CI.md`
- **Severity:** `Doc Sync`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`README.md`](file:///home/rahul/projects/materialisheep/README.md#L52)
  - [`CI.md`](file:///home/rahul/projects/materialisheep/CI.md#L9)
- **Problem Statement:**
  `README.md` instructed users to clone upstream `sheepdestroyer/materialisheep.git` rather than `xRahul/materialisheep.git`. `CI.md` omitted references to `debug.yml`, `codeql.yml`, and `gemini-review.yml`.
- **Resolution:**
  Synchronized repository URLs and documented full CI/CD workflow coverage.
