# Changelog

## [UNRELEASED]

## [4.0] - 2026-XX-XX
### Added
- Added GitHub Actions workflows for Continuous Integration and Release builds.
- The `CI.md` file documents the new CI/CD pipeline.
- `AGENTS.md` provides instructions for setting up the development environment.
- `TODO.md` tracks the remaining build issues.
- Created a new `MaterialisticApplication` class to initialize Dagger 2.
- Created a new `ApplicationComponent` and `ApplicationModule` for Dagger 2.

### Changed
- **Upgraded build environment to support Java 21.**
  - Gradle: 7.5.1 → 8.12.
  - Android Gradle Plugin: 7.4.2 → 8.9.1.
  - Kotlin: 1.8.20 → 2.3.0.
  - CI/CD workflows: Updated to use Java 21.
- **Migrated the dependency injection framework from Dagger 1 to Dagger 2.51.1.**
  - Replaced all Dagger 1 annotations and modules with their Dagger 2 equivalents.
  - Refactored all activities and fragments to use the new Dagger 2 component for injection.
- Replaced Mercury Web Parser with local Readability.js implementation.
- Fixed `R.attr` resource resolution issues by migrating to `androidx.appcompat.R.attr`.
- Updated various AndroidX and Google Material dependencies to their latest versions.
- Set `compileSdk` and `targetSdk` to 36.
- **Target SDK 36 Compatibility (PR #51):**
  - Suppressed various deprecation warnings (NetworkInfo, Fragment APIs, Parcellable, etc.) to ensure a clean build with `targetSdk` 36 (Issue #46).
  - Added TODOs for future refactoring and migration of suppressed APIs.
- **Modernized Fragment and View Pager Implementations (PR #56):**
  - Migrated from deprecated `ViewPager` to `ViewPager2` using `FragmentStateAdapter`.
  - Replaced `onActivityCreated` with `onViewCreated`.
  - Replaced `setRetainInstance` with `LazyLoadViewModel` for state retention.
  - Implemented `MenuProvider` API for handling menus, replacing `setHasOptionsMenu`.

### Fixed
- Resolved `NetworkOnMainThreadException` crash in `ReadabilityClient` by offloading database caching to background thread.
- Fixed memory leaks and resource management in `ReadabilityClient` and `WebFragment` by implementing proper `destroy()` lifecycle and using `CompositeDisposable`.




### Removed
- Removed the obsolete Dagger 1 dependency (`com.squareup.dagger:dagger:1.2.5`).
- Removed all Dagger 1-related classes and interfaces, including `InjectableActivity` and `Injectable`.
- Removed temporary `CustomCrashHandler` used for debugging.

