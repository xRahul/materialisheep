# CodeQL Setup Instructions

This repository uses a custom **Advanced Setup** for CodeQL (`.github/workflows/codeql.yml`) to ensure the correct build environment (JDK 21, Android SDK) is provisioned.

## Important: Disable Default Setup

GitHub's **Default Setup** for CodeQL conflicts with the custom workflow defined in this repository. If both are enabled, the CodeQL analysis will fail during the upload step with the following error:

> "Code Scanning could not process the submitted SARIF file: CodeQL analyses from advanced configurations cannot be processed when the default setup is enabled"

### How to Fix

To resolve this error and ensure the CodeQL workflow runs successfully:

1.  Go to the repository **Settings**.
2.  Navigate to **Code Security and Analysis** (under the "Security" section).
3.  Find **CodeQL analysis** under "Code scanning".
4.  Click the **...** (three dots) menu or the configuration button.
5.  Select **Disable CodeQL** (or disable "Default Setup").

Once "Default Setup" is disabled, the `.github/workflows/codeql.yml` workflow will handle the analysis correctly.
