# AGENTS.md

Instructions for agents and automated systems working with this repository.

**Materialisheep** is an open-source Android Hacker News client (MVVM, Java + Kotlin). This fork (`xRahul/materialisheep`) is synced with upstream `sheepdestroyer/materialisheep` and adds its own security, performance, CI, and quality features on top.

## Project Layout

- `app/src/main/java/io/github/sheepdestroyer/materialisheep/` — application code
  - `*Activity.java/.kt`, `*Fragment.java` — UI layer (MVVM)
  - `accounts/` — HN account login + session
  - `annotation/`, `ktx/` — annotations and Kotlin extensions
  - `appwidget/` — home-screen widget (`WidgetProvider`, `WidgetHelper`, `WidgetRefreshJobService`)
  - `data/` — data layer: `ItemManager`, `HackerNewsClient` (Firebase API), `AlgoliaClient`, `MaterialisticDatabase` (Room), `ReadabilityManager`
  - `preference/` — settings
  - `widget/` — custom views: `MaterialWebView`, `NavFloatingActionButton`, `AdBlockWebViewClient`, `LinkTouchListener`
- `app/src/test/` — Robolectric unit tests
- `.github/workflows/` — CI (see [CI/CD](#cicd) below)
- `docs/` — `PRD.md` (product requirements), `TD.md` (technical design), `CODEQL_SETUP.md`

## Development Environment Setup

### 1. Java Development Kit (JDK)

- **Version:** Java 21 is mandatory.
- **Installation:**
  ```bash
  sudo apt-get update && sudo apt-get install -y openjdk-21-jdk
  ```
- **Verification:** Ensure Java 21 is the default version:
  ```bash
  java -version
  ```

### 2. Android SDK

- **`compileSdk`:** 36
- **`targetSdk`:** 36
- **`minSdk`:** 31
- **`buildToolsVersion`:** The Android Gradle Plugin downloads the required version automatically. Ensure `ANDROID_HOME` is set to your SDK location.

### 3. Gradle

- **Gradle Version:** 9.5.1 (wrapper)
- **Android Gradle Plugin (AGP):** 9.2.1
- **Kotlin:** 2.3.21 (KSP 2.3.8; kapt-free — Dagger + Room use KSP)
- The Gradle wrapper (`./gradlew`) is included and should be used for all build commands.

### 4. CodeQL Analysis

This repository uses a custom **Advanced Setup** workflow (`.github/workflows/codeql.yml`) with `build-mode: manual` running `./gradlew assembleDebug` (Kotlin-version independent).

**IMPORTANT:** GitHub's **Default Setup** for CodeQL must be disabled in repository settings, or the custom workflow fails to upload results. See `docs/CODEQL_SETUP.md`.

## Building the Project

- **Clean the project:**
  ```bash
  ./gradlew clean
  ```
- **Build a debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Run unit tests:**
  ```bash
  ./gradlew test
  ```
- **Lint (some rules are advisory):**
  ```bash
  ./gradlew lint
  ```

## Architecture Notes

- **DI:** Dagger 2 via `ApplicationComponent` (singleton) + `ApplicationModule`/`DataModule`/`NetworkModule`/`UiModule`/`ActivityModule`.
- **Data layer:** RxJava 3 (`ItemManager` produces/consumes callbacks), blocking Retrofit `.execute()` calls in Kotlin ViewModels on `Dispatchers.IO`.
- **Story list:** `ListFragment` → Kotlin `StoryListViewModel` (StateFlow) → `HackerNewsClient.getStories()` → batched `getItems()`.
- **Web content:** `WebFragment` + `MaterialWebView`. **Security hardening:** `setJavaScriptEnabled(isRemote)` — JS is off for local content, on for remote + PDF.js viewer (`WebFragmentSecurityTest` enforces). `AdBlockWebViewClient` + `AdBlocker` drop ad/tracker requests. `FullscreenViewModel` (LiveData `fullscreenEvent()`) drives reader fullscreen.
- **Widgets:** `WidgetHelper` (`@Inject` HN + Algolia `ItemManager`s) builds `RemoteCollectionItems`; `WidgetRefreshJobService` (JobScheduler) schedules refreshes.
- **Crash reporting (fork):** debug builds install a default uncaught-exception handler launching `CrashActivity`; bypassed when `Build.FINGERPRINT == "robolectric"` so unit tests never self-terminate (`System.exit`).

## Testing

Unit tests run under Robolectric; describing a background exception on the Robolectric looper triggers the production crash handler, which (post-fingerprint guard) is safe for tests. Keep Robolectric SDK config `@Config(sdk = Build.VERSION_CODES.S)` or newer — the pipeline loads the APK manifest which requires minSdk 31.

Test classes to be aware of: `AppUtilsTest`, `WebFragmentSecurityTest`, `NetworkHttpsTest`, `WidgetHelperTest`, `BaseListActivityTest`, `StoryListViewModelTest`, plus client/perf tests.

## CICD

Workflows in `.github/workflows/`:

- `ci.yml` — build + unit tests on push/PR
- `release.yml` — automated one-click release with semantic version bumping (patch/minor/major), signing via env (`KEY_STORE_BASE64`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), categorized changelog generation, and GitHub Release publish
- `debug.yml` — debug APK artifact build on manual trigger (workflow_dispatch)
- `codeql.yml` — CodeQL Advanced Setup (see above)
- `gemini-review.yml` — AI-assisted PR review (upstream)
- `gradle.yml` — additional Gradle wrapper validation (upstream)

## Fork Sync Workflow

This fork tracks upstream `sheepdestroyer/materialisheep`. Keep it in sync periodically:

```bash
git remote add upstream git@github.com:sheepdestroyer/materialisheep.git  # once
git fetch upstream
git switch -c sync-upstream master
git merge upstream/master
```

**Conflict-resolution rules used for this sync (apply the same next time):**

- **Build config wins from upstream:** AGP, Gradle wrapper, Kotlin/KSP, and `app/build.gradle` dependency versions.
- **Identical deprecation/refactor work on both sides** → take upstream (newer baseline).
- **Fork-unique work → keep ours:**
  - Security: HTTPS enforcement, XSS fix + JS disabled for local content (`WebFragment`), CodeQL workflow
  - Sync stack (`ItemSyncAdapter`/`ItemSyncJobService`/`SyncDelegate`/`SyncQueueDao.kt`) — RxJava + DB-deferred
  - ThreadPreview Kotlin rewrite, `StoryListViewModel` Kotlin, FAB drag fix, optimistic voting, AdBlocker/Readability/N+1 perf, regex precompile
  - Release signing + GitHub Release + debug workflow in CI
- **Junk / stray files** (crash logs, `package.json`, `ListFragment.java.orig`, `read_pr_comments.py`) → delete; don't merge.

**CodeQL caveat:** if Kotlin is ever upgraded past the version compatible with CodeQL analysis, fall back by pinning Kotlin/KSP to the last known-good pair (previously `2.2.10`/`2.2.10-1.0.30`) — but the current `codeql.yml` is version-agnostic, so this should not be needed.

## Troubleshooting

### Build failures due to `Permission denied`

On sandbox/CI environments `.gradle` or `.kotlin` in the project root may be owned by another user.

**Solution** — ensure the current user owns them:

```bash
sudo chown -R $(whoami) .gradle/
sudo chown -R $(whoami) .kotlin/
```

If deleting `app/build` fails with permission errors:

```bash
sudo rm -rf app/build
```

### Build failures due to `R` class not found

"Cannot find symbol" errors pointing at the `R` class indicate the Android resource processor failed — almost always a **symptom** of another error.

**Do not attempt to fix `R` class errors directly.** Look for the real compilation error elsewhere in the build log; fixing it regenerates `R`. If there are no other errors, suspect `app/build.gradle` dependency/config misconfiguration.

### Merge conflicts referencing stale diagnostics

After resolving a conflicted file with an overwrite (`git show :3:... > file`), editor/LSP diagnostics may briefly report the *old* conflicted content. Trust the file itself: `grep -n '<<<<<<<' <file>` (exit 1 = clean), then `git add`.