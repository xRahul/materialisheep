# Project Knowledge Base

This document serves as a detailed technical reference for the `materialisheep` project, a Hacker News client for Android.

## 1. Project Overview

**Materialisheep** is an Android application for browsing Hacker News.
- **Origin:** Fork of `materialisheep` (by sheepdestroyer), which is a fork of `materialistic` (by hidroh).
- **Purpose:** Provide a clean, material-design focused experience for reading HN, with offline support, themes, and widgets.

## 2. Technical Stack & Environment

- **Language:** Kotlin (primary), Java (legacy/mixed).
- **Minimum SDK:** 30
- **Compile/Target SDK:** 36
- **JDK:** Java 21 is required for building.
- **Build System:** Gradle 9.2.1, Android Gradle Plugin 8.9.1.

### Key Libraries
- **Dependency Injection:** Dagger 2
- **Networking:** Retrofit 2, OkHttp 5, Gson.
- **Async/Concurrency:** RxJava 3, RxAndroid.
- **Database:** Android Room (SQLite abstraction).
- **Search:** Algolia API.
- **UI:** Android Views (XML layouts), Material Components, RecyclerView, SwipeRefreshLayout.
- **Architecture Components:** ViewModel, LiveData, Lifecycle.

## 3. Architecture

The application follows a mix of **MVVM (Model-View-ViewModel)** and **Clean Architecture** principles.

- **Presentation Layer:**
    - **Activities/Fragments:** Handle UI rendering and user events.
    - **ViewModels:** Manage UI state and interact with the Data Layer.
- **Domain/Data Layer:**
    - **ItemManager:** Interface for fetching stories/items (implemented by `HackerNewsClient`, `AlgoliaClient`).
    - **Repositories/Managers:** `UserManager`, `FavoriteManager`, `SessionManager`.
    - **Database:** Room database (`MaterialisticDatabase`) caches stories and stores user preferences (saved stories, read status).

## 4. Directory Structure & Key Files

The main source code is located in `app/src/main/java/io/github/sheepdestroyer/materialisheep`.

### Root Package (`io.github.sheepdestroyer.materialisheep`)
- `MaterialisticApplication.java`: App entry point. Initializes Dagger graph (`ApplicationComponent`).
- `ApplicationModule.java`, `DataModule.java`, `NetworkModule.java`, `UiModule.java`, `ActivityModule.java`: Dagger modules defining dependency provision.
- `LauncherActivity.java`: Invisible activity that routes to `ListActivity`.

### `data` Package
Handles all data retrieval and persistence.
- **Interfaces:**
    - `ItemManager.java`: Core interface for fetching lists of stories and individual items.
- **Implementations:**
    - `HackerNewsClient.java`: Retrofit client for the official HN API (Firebase).
    - `AlgoliaClient.java`: Client for Algolia search API.
    - `AlgoliaPopularClient.java`: Client for "Popular" stories.
- **Database:**
    - `MaterialisticDatabase.java`: Room database definition.
    - `SavedStoriesDao.kt`: DAO for saved stories.
    - `LocalCache.kt`: Caching logic.
- **Services:**
    - `ItemSyncService.java`: Background service for syncing data.
    - `WebCacheService.java`: Service for caching web content.

### `ui` / Activities
- `ListActivity.java`: The main screen displaying lists of stories (Top, New, etc.).
- `ItemActivity.java`: Displays a single story/item and its comments. Handles deep links (`news.ycombinator.com/item`).
- `WebFragment.java`: Displays the article content (WebView).
- `SearchActivity.java`: Handles search functionality.
- `SettingsActivity.java`: App configuration.
- `WidgetConfigActivity.java`: Configuration screen for adding a new widget.

### `appwidget` Package (Widget System)
Implements the Android Home Screen Widget.
- `WidgetProvider.java`: `AppWidgetProvider` implementation. Receives broadcasts (`ACTION_REFRESH_WIDGET`, `APPWIDGET_UPDATE`) and orchestrates updates.
- `WidgetService.java`: `RemoteViewsService`. Generates the views for the widget collection (list).
    - Uses `ListRemoteViewsFactory` to bind data to `R.layout.item_widget`.
    - Injects `ItemManager` to fetch data synchronously on a background thread.
- `WidgetHelper.java`: Helper class for widget configuration and refreshing logic.
- `WidgetRefreshJobService.java`: JobService for periodic widget updates.

### `accounts` Package
- `UserServicesClient.java`: Handles user login, voting, and commenting.
- `AuthenticatorService.java`: Android Account Manager integration.

## 5. Data Flow

### Story Listing
1.  `ListActivity` observes a `ViewModel`.
2.  `ViewModel` requests data from `ItemManager`.
3.  `ItemManager` (e.g., `HackerNewsClient`) fetches data via Retrofit.
4.  Data is cached in `MaterialisticDatabase` (if applicable).
5.  Data flows back to UI via RxJava Observables or LiveData.

### Widget Update
1.  System/User triggers update -> `WidgetProvider.onReceive`.
2.  `WidgetProvider` starts/updates `WidgetService`.
3.  `WidgetService.ListRemoteViewsFactory.onDataSetChanged()` is called.
4.  It calls `ItemManager.getStories()` (blocking call).
5.  RemoteViews are built and sent to the HomeScreen.

## 6. Build & Setup
- Ensure `JAVA_HOME` points to JDK 21.
- Run `./gradlew assembleDebug` to build.
- Run `./gradlew test` for unit tests.
- API Keys: The app uses public APIs, but some keys might be defined in `build.gradle` (e.g., Algolia, though often public for HN).
