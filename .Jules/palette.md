## 2025-02-18 - Pluralized Content Descriptions
**Learning:** When adding accessibility labels to buttons that display numbers (like comment counts), checking existing `plurals` resources often yields a perfect localized string (e.g., "5 Comments") without needing new strings.
**Action:** Always search `strings.xml` for `plurals` before creating new accessibility labels for counts.

## 2026-01-26 - Reusing Preference Titles for Accessibility
**Learning:** Preference titles (e.g., `pref_navigation_title` "On-screen navigation") can serve as effective content descriptions for the UI elements that represent those features, ensuring consistent terminology.
**Action:** When naming a control for a specific feature, check `strings.xml` preference sections for existing user-facing names.
