# Materialisheep Actionable Engineering Backlog & Execution Roadmap

Strict chronological execution roadmap (Ref: [`docs/issues.md`](file:///home/rahul/projects/materialisheep/docs/issues.md)).
Separates **Existing System Improvements** from **New Product Features**.

---

## Phase 1: Critical Reliability, Memory Health & Tooling (DO FIRST - P0)
*Target: Eliminate native memory leaks, thread crashes, and centralize build dependencies.*

### Existing System Improvements & Fixes
- [x] **Task 1.1:** Fix `WebFragment` / `MaterialWebView` native memory leak in `onDestroyView()` (`[ARCH-09]`)
- [x] **Task 1.2:** Replace lossy & thread-unsafe `MaterialisticDatabase` LiveData event bus with `SharedFlow` (`[ARCH-10]`)
- [x] **Task 1.3:** Guard `FontCache.java` asset loading against uncaught exceptions and migrate to `res/font/` (`[ARCH-15]`)
- [x] **Task 1.4:** Eliminate unscoped fire-and-forget RxJava subscriptions in `SessionManager.kt` (`[ARCH-16]`)
- [ ] **Task 1.5:** Centralize all dependencies into Gradle Version Catalog `gradle/libs.versions.toml` (`[DEP-01]`)

---

## Phase 2: Data Architecture, Concurrency & Security Modernization (DO SECOND - P1)
*Target: Establish non-blocking DataStore, Coroutines repository, in-memory caching, and encrypted authentication.*

### Existing System Improvements & Hardening
- [ ] **Task 2.1:** Replace plaintext `AccountManager` password storage with `EncryptedSharedPreferences` Keystore cookie storage (`[ARCH-11]`)
- [ ] **Task 2.2:** Split coarse 30-minute OkHttp `Cache-Control` overrides into granular feed vs item TTLs (`[ARCH-12]`)
- [ ] **Task 2.3:** Implement multi-tier in-memory `LruCache` for story and comment items (`[ARCH-13]`)
- [ ] **Task 2.4:** Migrate monolithic `Preferences.java` to Jetpack Preferences DataStore (`Flow<T>`) (`[ARCH-04]`)
- [ ] **Task 2.5:** Unify data layer from RxJava 3 to Kotlin Coroutines `suspend fun` and `Flow<T>` (`[ARCH-03]`)
- [ ] **Task 2.6:** Precompute text layouts (`PrecomputedTextCompat`) and adopt `ListAdapter` with `DiffUtil` in story/comment feeds (`[ARCH-06]`)
- [ ] **Task 2.7:** Deprecate `SyncAdapter` / `JobScheduler` in favor of AndroidX `WorkManager` `CoroutineWorker` (`[ARCH-05]`)
- [ ] **Task 2.8:** Modernize deprecated Custom Tabs APIs to `CustomTabColorSchemeParams` (`[ARCH-14]`)

---

## Phase 3: Core Product Features & UX Upgrades (DO THIRD - P1 / P2)
*Target: Deliver high-demand user-facing features on top of the modernized Phase 2 foundation.*

### New Product Features
- [ ] **Task 3.1:** Implement Pure Black (AMOLED/OLED `#000000`) theme toggle in Preferences & Activity theme engine (`[FEAT-07]`)
- [ ] **Task 3.2:** Implement Keyword, Domain, and Author Muting & Filter Engine (`[FEAT-01]`)
- [ ] **Task 3.3:** Add interactive Material 3 Filter Chips to Algolia search (`[FEAT-03]`)
- [ ] **Task 3.4:** Build Background Reply & Karma Notification Engine via `WorkManager` (`[FEAT-05]`)
- [ ] **Task 3.5:** Add comment swipe gestures, thread collapse shortcuts, and OP badges (`[FEAT-04]`)
- [ ] **Task 3.6:** Add OpenGraph link previews, domain favicons, and comment code syntax highlighting (`[FEAT-06]`)
- [ ] **Task 3.7:** Add Offline Storage & Archive Manager UI (`[FEAT-08]`)

---

## Phase 4: Advanced Modernization, Jetpack Compose & AI (DO LAST - P2 / P3)
*Target: Large-scale architectural shifts, declarative UI, and AI integrations.*

### Architecture Modernization
- [ ] **Task 4.1:** Complete Java-to-Kotlin conversion across remaining Activities, Fragments, and Adapters (`[ARCH-01]`)
- [ ] **Task 4.2:** Execute Jetpack Compose phased UI migration (Phase 1: Design System → Phase 2: Dialogs → Phase 3: Components → Phase 4: Feed/Reader) (`[ARCH-02]`)
- [ ] **Task 4.3:** Migrate Retrofit JSON parsing from reflection-heavy `Gson` to `kotlinx.serialization` (`[DEP-02]`)
- [ ] **Task 4.4:** Migrate manual Dagger 2 boilerplate to Google Hilt (`[DEP-03]`)
- [ ] **Task 4.5:** Implement Roborazzi automated screenshot regression testing in CI (`[ARCH-08]`)
- [ ] **Task 4.6:** Split monolith into multi-module architecture (`:core:...`, `:feature:...`) (`[ARCH-07]`)
- [ ] **Task 4.7:** Configure AndroidX Baseline Profiles for sub-300ms cold start (`[DEP-04]`)

### Advanced Features & AI
- [ ] **Task 4.8:** Implement AI Article & Discussion Summarization (On-device Gemini Nano / Cloud BYOK API) (`[FEAT-02]`)
