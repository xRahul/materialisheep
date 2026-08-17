# Materialisheep Comprehensive Issues & Audit Report

This document records the exhaustive, multi-role review and adversarial **Gauntlet-Loop** audit of the `materialisheep` repository (codebase, architecture, UI/UX, security, build systems, CI/CD pipelines, test coverage, and documentation), along with completed remediations and resolutions.

---

## Executive Summary & Taxonomy

### Issues by Category & Severity (All Remediated)
| Category | Total Issues | Status | Key Remediations |
|---|---|---|---|
| **Critical** | 5 | **Resolved** | Options menu dispatch restored, `WRITE_SECURE_SETTINGS` removed, Readability 0.5.0 + pdf.js 4.10.38 upgraded, RxJava subscriptions managed via Disposables, GitHub Release tag fixed |
| **High** | 7 | **Resolved** | Thread-safe singletons (`FontCache`, `Preferences`, `AlgoliaClient`), Edge-to-Edge insets consumed, Android 14+ Predictive Back enabled, Jsoup HTML DOM parser adopted, Robolectric multi-SDK matrix restored, Android UI instrumentation tests added |
| **Medium** | 5 | **Resolved** | Removed dead `localbroadcastmanager`, AGP 9.2 built-in Kotlin migrated, Room `exportSchema = true` configured via KSP, Background web cache service call replaced with safe main Handler dispatch and WebView cleanup, Comment indentation clamped |
| **Low / Tech Debt** | 2 | **Resolved** | Material You Dynamic Theming added via `DynamicColors` on Android 12+, Comment indentation squeeze eliminated |
| **Doc Sync Errors** | 4 | **Resolved** | `PROJECT_KNOWLEDGE.md`, `CI.md`, `AGENTS.md`, and `TODO.md` fully synchronized with current stack |

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

### [DOC-01..04] Documentation Synchronization
- **Severity:** `Doc Sync`
- **Status:** `Fixed`
- **Impacted Files:**
  - [`PROJECT_KNOWLEDGE.md`](file:///home/rahul/projects/materialisheep/PROJECT_KNOWLEDGE.md)
  - [`CI.md`](file:///home/rahul/projects/materialisheep/CI.md)
  - [`AGENTS.md`](file:///home/rahul/projects/materialisheep/AGENTS.md)
  - [`TODO.md`](file:///home/rahul/projects/materialisheep/TODO.md)
- **Resolution:**
  Synchronized Min SDK (31), Target SDK (36), Gradle (9.5.1), AGP (9.2.1), Retrofit (3.0.0), `debug.yml` trigger (`workflow_dispatch`), and task completion status across all documentation.
