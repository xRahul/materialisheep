# TODO

This document outlines the remaining work required to get the project building successfully.

## Status
All compilation blockers have been resolved. The project builds successfully.

### Completed
- Fixed `R` class generation by using `androidx.appcompat.R.attr`.
- Fixed Kotlin and Java compilation errors.
- Provisioned JDK 21.
- **Migrate to RxJava 3**: Successfully migrated from EOL RxJava 1.3.8 to RxJava 3.1.8.
- **Android SDK 36 Upgrade**: Upgraded `compileSdk` and `targetSdk` to API 36 (Jan 2026).
- **Dependabot Analysis**: Verified and rebased all active dependency updates.

## Future Work

### Tech Debt & Modernization
- **Implement Algolia ETag Support**:
  - **Reason**: `TODO` found in codebase. Improves network efficiency.
  - **Context**: Started in `feature/algolia-etag`.
- **Consider increasing `minSdk` to 28**:
  - **Pros**: Cleaner codebase, native modern APIs, better security.
  - **Cons**: Drops support for ~5% of legacy devices (API 21-27).
