# Materialisheep Product Requirements Document (PRD)

## 1. Executive Summary
Materialisheep is a robust, feature-rich, and open-source Android client for Hacker News. It is designed to provide the best reading experience for the HN community by combining the principles of Material Design with a suite of power-user features. The app emphasizes performance, offline capability, and deep customization to cater to both casual readers and heavy consumers of tech news.

## 2. Objectives
*   **User Experience:** Provide a clean, distraction-free, and intuitive interface for browsing and interacting with Hacker News.
*   **Performance:** Ensure fast loading times and smooth navigation, even on slower networks or older devices.
*   **Accessibility:** Adhere to Android accessibility standards and provide features like dynamic text sizing and high-contrast themes.
*   **Reliability:** Offer robust offline support to allow users to read content anywhere.
*   **Open Source:** Maintain a transparent and community-driven development process.

## 3. Target Audience
*   **Hacker News Community:** Users who actively read, vote, and comment on HN.
*   **Developers & Tech Enthusiasts:** The primary demographic of HN who value well-engineered tools.
*   **Commuters:** Users who need offline access to content during transit.
*   **Privacy-Conscious Users:** People who prefer open-source apps without tracking.

## 4. Feature Specifications

### 4.1 Browsing & Content Consumption
*   **Multiple Feeds:** Access to all standard HN feeds: Top, New, Best, Show HN, Ask HN, and Jobs.
*   **Catch Up (Popular):** A dedicated section to see the most popular stories over specific time ranges (Last 24h, Past Week, Past Month, Past Year).
*   **Readability Mode:** Integrated `Readability.js` to parse and display article content natively within the app, stripping clutter for a clean reading experience.
*   **Threaded Comments:**
    *   Collapsible comment threads for easy navigation of deep discussions.
    *   Visual indicators for thread depth with interactive parent/root jump: tap color bar to smooth-scroll to parent comment, long-press to jump to root comment.
    *   **Author Badges:** Prominent `[OP]` badge identifying the story submitter and `[YOU]` identifying the logged-in reader across discussion threads.
    *   **Syntax Highlighting:** Code blocks (`<pre><code>`) rendered in monospace with subtle container tinting and keyword syntax highlighting for programming languages.
    *   Navigation helpers: Floating action button, depth indicator tap gestures, and volume key scrolling to jump between comments.
*   **Search:**
    *   Powered by the Algolia Hacker News API.
    *   Filter by date, popularity, and specific tags (story, comment, poll, etc.).
    *   Sort results by relevance or date.

### 4.2 User Account Integration
*   **Authentication:** Zero-trust login architecture. Hacker News credentials are encrypted using hardware-backed `AndroidKeyStore` (AES-256 GCM in `AccountSecurity.kt`) instead of plaintext storage in system `AccountManager`.
*   **Engagement:**
    *   **Upvote/Downvote:** Vote on stories and comments.
    *   **Comment:** Reply to stories or other comments.
    *   **Submit:** Post new stories (URL or Text) directly from the app.
    *   **Drafts:** Save unfinished comments or submissions as drafts.
*   **Profile:** View user profiles, including "karma" score, account creation date, and submission history.

### 4.3 Offline Capabilities
*   **Smart Caching & Cache TTL Hierarchy:**
    *   Feed index endpoints (`*stories.json`, `updates.json`, Algolia search): 1-minute TTL for fast pull-to-refresh without stale IDs.
    *   User profiles: 5-minute TTL.
    *   Story and comment items: 30-minute TTL.
    *   Explicit `no-cache` directives preserved; HTTP error responses are shielded from caching.
*   **Offline Mode:**
    *   Explicitly download content for offline access.
    *   Options to download full articles (web content), comments, and readability views.
    *   Configurable download constraints (e.g., Wi-Fi only).
*   **Sync:** Background service to keep saved content updated.

### 4.4 Customization & Settings
*   **Themes:** Extensive theming engine with multiple presets:
    *   Light, Dark, Pure Black AMOLED (`#000000` true black surfaces and AAA contrast text), Sepia, Solarized, Green, etc.
    *   Auto Day/Night mode based on system settings or time.
*   **Typography:**
    *   Adjustable text size (Extra Small to Extra Large).
    *   Choice of fonts and line height (Compact to Comfortable).
*   **Display Options:**
    *   List View vs. Card View for stories.
    *   Comment display modes (Single page, Multiple pages).
*   **Data Sovereignty & Export:**
    *   Multi-format export of saved favorites: JSON metadata archive, Markdown with backlinks for PKM workflows (Obsidian, Logseq, Readwise), and standard Netscape Bookmark HTML.
*   **Gestures:** Configurable swipe actions on list items (e.g., Swipe Left to Vote, Swipe Right to Save).

### 4.5 Widgets
*   **Home Screen Widgets:** Place widgets on the home screen to view specific feeds (e.g., Top Stories) without opening the app.
*   **Customization:** Configure widget theme, refresh frequency, and feed source.

### 4.6 System Integration
*   **Deep Linking:** Automatically opens `news.ycombinator.com` links within the app.
*   **Sharing:** Share stories and comments to other apps (social media, notes, etc.).
*   **Browser Integration:**
    *   Open links in an internal WebView.
    *   Support for Chrome Custom Tabs for a seamless browsing experience.
    *   Option to always open in an external browser.
*   **Security:**
    *   Hardware-backed AES-256 GCM encryption for user credentials via `AndroidKeyStore`.
    *   **Universal Privacy Shield (`UrlSanitizer.kt`):** Strips marketing trackers and campaign attribution IDs (`utm_*`, `fbclid`, `gclid`, etc.) before launching Custom Tabs or opening WebViews.
    *   JavaScript is disabled for local (offline/readability) content and only enabled for remote pages and the bundled PDF viewer, mitigating XSS via injected content.
    *   Ad and tracker blocking powered by synchronized `AdBlocker` trie.
    *   Backend API endpoints enforce HTTPS.
    *   Automated CodeQL analysis runs on every push/PR (Advanced Setup; GitHub Default Setup must stay disabled — see `docs/CODEQL_SETUP.md`).

### 4.7 Sync & Background
*   Background sync of saved/offline content is scheduled via `JobScheduler`, with low-battery deferred sync.
*   Home-screen widgets update on a configurable schedule via `WidgetRefreshJobService`.

## 5. Technical Requirements
*   **Platform:** Android
*   **Minimum SDK:** API Level 31 (Android 12)
*   **Target/Compile SDK:** API Level 36 (Android 16)
*   **Architecture:**
    *   **Pattern:** MVVM (Model-View-ViewModel) with Clean Architecture principles.
    *   **Language:** Kotlin (primary) and Java.
    *   **Dependency Injection:** Dagger 2.
    *   **Networking:** Retrofit 3 with OkHttp 5.
    *   **Persistence:** Room Database.
    *   **Concurrency:** Hybrid: RxJava 3 (Data Layer) and Kotlin Coroutines (ViewModel).
    *   **Observability:** Comprehensive logging of user flows, errors, and UI states (Debug builds).

## 6. Future Roadmap
*   **Modernization:**
    *   Complete migration of legacy Java code to Kotlin.
    *   Adopt Jetpack Compose for UI development to replace XML layouts.
    *   Full "Edge-to-Edge" UI design.
*   **Features:**
    *   Enhanced tablet support with multi-pane layouts.
    *   Improved Algolia search caching.
    *   Updates to internal ad-blocking definitions.
