# CI/CD Workflows

This document explains the CI/CD workflows for this project, which are managed using GitHub Actions.

## Workflows

The repository includes the following automated workflows:

1.  **CI (Continuous Integration) (`ci.yml`):** Runs unit tests (`./gradlew test`) on pull requests targeting `master`.
2.  **Release (`release.yml`):** Builds signed release APKs and publishes GitHub Releases on tag pushes (`v*`) and manual triggers.
3.  **Debug Build (`debug.yml`):** Compiles and uploads debug APK artifacts on manual trigger (`workflow_dispatch`).
4.  **CodeQL Advanced (`codeql.yml`):** Runs automated static security code analysis on push, PR, and weekly schedule.
5.  **Gemini Review (`gemini-review.yml`):** Provides AI-assisted PR reviews.
6.  **Java CI with Gradle (`gradle.yml`):** Validates Gradle builds and D8 packaging.

### CI Workflow

The CI workflow is defined in `.github/workflows/ci.yml`. It performs the following steps:

1.  **Sets up the environment:** Provisions Ubuntu with Java 21 and the Android SDK (API level 36).
2.  **Caches dependencies:** Caches Gradle dependencies to speed up future runs.
3.  **Runs tests:** Executes Robolectric unit tests via `./gradlew test`.

### Release Workflow

The Release workflow is defined in `.github/workflows/release.yml`. It supports one-click releases:

1. **Triggering:**
   - **Manual (`workflow_dispatch`):** Choose the version increment segment (`patch`, `minor`, `major`) or enter an optional `custom_version`.
   - **Tag Push:** Triggered on pushes matching `v*`.
2. **Automated Steps:**
   - Increments `version.properties` and commits the bump with `[skip ci]`.
   - Generates annotated git tag (e.g. `v3.4.14`) and pushes to the repository.
   - Compiles and signs the release APK via `./gradlew assembleRelease`.
   - Generates categorized changelog with Conventional Commits, PR references, commit hashes, authors, and SHA-256 checksums.
   - Publishes the GitHub Release with attached `materialisheep-vX.Y.Z.apk` and `sha256sum.txt`.

## Validating the Workflows

To validate the workflows:

* **CI Workflow:** Create or update a pull request to `master`.
* **Debug Workflow:** Trigger manually from the "Actions" tab.
* **Release Workflow:** Trigger manually from the Actions tab (select Release workflow -> Run workflow -> select increment type) or push a version tag (e.g., `git tag v3.4.14 && git push origin v3.4.14`).


