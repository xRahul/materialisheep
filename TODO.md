

# Materialistic TODO

- [x] Review PRs for code quality and memory leaks <!-- id: 10 -->
- [x] Fix Gradle Deprecation Warnings (Issue #46) <!-- id: 21 -->
- [x] Modernize Room schema export and AGP built-in Kotlin configuration
- [x] Implement comment indentation clamping for mobile readability
- [x] Implement Material You Dynamic Theming support

## Deprecation Refactoring (Long-term)
- [x] **Phase 1: Fragment API Modernization** <!-- id: 17 -->
    - [x] Replace `setHasOptionsMenu`/`onOptionsItemSelected` with `MenuProvider`
    - [x] Replace `onActivityCreated` with `onViewCreated`
    - [x] Replace `setRetainInstance` with `ViewModel`
    - [x] Migrate `FragmentStatePagerAdapter` to `ViewPager2`
- [x] **Phase 2: System & Device API Migration** <!-- id: 18 -->
    - [x] Migrate `NetworkInfo` to `ConnectivityManager.NetworkCallback`
    - [x] Update `Vibrator` usage to `VibrationEffect`
    - [x] Adopt `WindowMetrics` and `WindowInsetsController`
    - [x] Implement Edge-to-Edge and Predictive Back support
- [x] **Phase 3: Widget & View Cleanup** <!-- id: 19 -->
    - [x] Update `RemoteViews` adapter API (`RemoteCollectionItems` + mutable template)
    - [x] Fix `setLayoutFrozen` (RecyclerView) and `BottomSheetCallback`
    - [x] Update Preferences to AndroidX Preferences
- [x] **Phase 4: Architecture Components** <!-- id: 20 -->
    - [x] Remove unused `LocalBroadcastManager`
    - [x] Replace regex HTML scraping with Jsoup DOM parser
    - [x] Fix static mutable thread-safety risks

## Future Work
- [ ] Implement Jetpack Compose UI migration roadmap <!-- id: 14 -->
- [ ] Implement Algolia ETag persistence with LruCache <!-- id: 15 -->
- [ ] Adjust NetworkModule caching strategy <!-- id: 16 -->
