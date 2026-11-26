# Roadmap

Three buckets, no dates. `Now` is the work in flight; `Next` is agreed and
unblocked; `Later` is wanted but not scheduled. Items leave this file when they
ship.

## Now

Store readiness and security hardening — see
`docs/plans/2026-11-21-materialhn-store-security.md`:

- Rename to `in.rahulja.materialhn`, GPL-3.0 relicensing, attribute the bundled
  blocklist.
- Repository cleanup: remove the prebuilt PDF.js archive, agent leftovers,
  duplicate planning documents.
- Backup rules that exclude credentials; privacy policy that matches the code.
- Fastlane metadata, F-Droid recipe, IzzyOnDroid listing, store validation CI.
- CI: tests, lint, build on every push; pinned actions; Dependabot; CodeQL
  `security-extended`; OSV dependency scan; release guards.
- Security fixes: fail-closed Keystore handling, WebView scheme allowlist,
  PDF bridge confinement, exported-component audit.
- Article tab loading rework — see
  `docs/plans/2026-11-26-article-loading-rework.md`: WebView-owned scrolling,
  central settings policy, commit-driven visibility, in-tab error panel and
  renderer-crash recovery, https upgrade, Readability settle polling, dark web
  content. Device smoke pass outstanding.

## Next

Reliability and maintenance, in rough order:

1. Surface fetch failures in the UI instead of logging them; add an error state to
   `StoryListViewModel` and to `HackerNewsClient` listeners.
2. Return cancellable handles from `HackerNewsClient` async methods and dispose
   them with the view lifecycle.
3. Cherry-pick upstream test suites that this fork lacks: `AppUtils` resource and
   intent tests, `Preferences` parsing tests, `FileDownloader` path-traversal test,
   `HackerNewsClient` error tests.
4. Dependency bumps from upstream: Kotlin 2.4.0, KSP 2.3.9, OkHttp 5.5.0,
   AppCompat 1.8.0, Dagger 2.60.1. Verify CodeQL and R8 after each bump.
5. Split `Preferences.java` into per-concern objects over `SharedPreferences`
   (no DataStore migration: it buys nothing here and costs a rewrite).
6. Room: replace the `SharedFlow` event bus with per-query `Flow`, removing the
   lossy broadcast.

## Later

Product work, only after the app is listed:

- Content filtering: keyword, domain and author blocklists (FEAT-01).
- Search filter chips backed by Algolia facets (FEAT-03).
- Predictive back navigation on all activities.
- Baseline profile + startup trace.
- Widget feed selector and quick actions.

Explicitly rejected: AI summarisation, reply/karma push notifications, and any
feature that needs a server the maintainer operates. They add cost, privacy risk
or a backend that nobody maintains.
