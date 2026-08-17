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

The Release workflow is defined in `.github/workflows/release.yml`. It performs the following steps:

1.  **Sets up the environment:** Provisions Ubuntu with Java 21 and the Android SDK.
2.  **Caches dependencies:** Caches Gradle packages.
3.  **Builds signed release APK:** Builds release APK using `./gradlew assembleRelease` signed with repository secrets.
4.  **Uploads & Releases:** Attaches the APK as a release artifact and creates a GitHub Release for tags.

## Validating the Workflows

To validate the workflows:

*   **CI Workflow:** Create or update a pull request to `master`.
*   **Debug Workflow:** Trigger manually from the "Actions" tab.
*   **Release Workflow:** Push a version tag (e.g., `git tag v3.4.0 && git push origin v3.4.0`) or trigger via `workflow_dispatch`.

