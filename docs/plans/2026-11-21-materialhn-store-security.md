# Material HN — Store & Security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship `in.rahulja.materialhn` as a clean, GPL-3.0, auditable app that F-Droid and IzzyOnDroid can build and list.

**Architecture:** Mechanical identity rename first, then licence/backup/privacy corrections, then store metadata and CI, then the three security fixes. No product code is rewritten; every change is either configuration, deletion, or a small contained code change with a test.

**Tech Stack:** Android Gradle Plugin 9.2.1, Gradle 9.7.1, JDK 21, Kotlin 2.3.21/KSP 2.3.8, Java + Kotlin, Dagger 2, Room, RxJava 3, OkHttp 5, Robolectric 4.

**Spec:** `docs/specs/2026-11-21-materialhn-fdroid-design.md`

## Global Constraints

- Application id, namespace and source package: `in.rahulja.materialhn`.
- Display name: `Material HN`.
- Licence: `GPL-3.0-or-later`; Apache-2.0 notices for upstream Materialistic and Materialisheep retained.
- `minSdk 31`, `targetSdk 36`, `compileSdk 36`, JDK 21. Do not raise or lower these in this plan.
- `versionCode` starts at `101`, `versionName` at `4.5.0`; both overridable with `-PversionCode` / `-PversionName`.
- No new runtime dependency. Dev-only tooling (osv-scanner via action) is allowed.
- Conventional Commits. One commit per task unless a step says otherwise.
- Do not add planning documents beyond the ones named here.
- Run `./gradlew testDebugUnitTest` before every commit; green is required.
- Never commit a keystore, a mapping file, or a signed APK.

## Review Focus

1. Installing the new APK over the old `in.rahulja.materialhn` install — must be treated as a separate app, and the release notes must say so; no in-place upgrade is possible.
2. Upgrading an installed old version with credentials saved — old ciphertext must not be read by the new app, and the new app must not crash on a missing account.
3. Remote page navigating to `file:`, `javascript:`, `intent:` or `content:` URLs — must be blocked, not opened.
4. Auto Backup / device transfer restore where the Keystore key is absent — app must prompt re-login rather than throw or loop.
5. Release build with R8 enabled and Gson models — API responses must still deserialize (field names preserved or annotated).
6. `-PversionCode`/`-PversionName` on a source tarball with no `.git` — build must still succeed and version correctly.

---

## Phase 0 — Identity, licence, hygiene

### Task 1: Rename the app to `in.rahulja.materialhn`

**Files:**
- Move: `app/src/main/java/io/github/sheepdestroyer/materialisheep/` → `app/src/main/java/in/rahulja/materialhn/`
- Move: `app/src/test/java/io/github/sheepdestroyer/materialisheep/` → `app/src/test/java/in/rahulja/materialhn/`
- Modify: every `.java`, `.kt`, `.xml`, `.pro`, `.gradle`, `.md` containing the old package (238 files)

**Interfaces:**
- Produces: `BuildConfig.APPLICATION_ID == "in.rahulja.materialhn"`, account type `in.rahulja.materialhn`, authorities `in.rahulja.materialhn.syncprovider|recentprovider|fileprovider`, permissions `in.rahulja.materialhn.permission.SYNC_ITEM|AUTHENTICATE_ACCOUNTS`.

- [ ] **Step 1: Move the source trees**

```bash
cd /home/rahul/projects/materialisheep
mkdir -p app/src/main/java/in/rahulja app/src/test/java/in/rahulja
git mv app/src/main/java/io/github/sheepdestroyer/materialisheep app/src/main/java/in/rahulja/materialhn
git mv app/src/test/java/io/github/sheepdestroyer/materialisheep app/src/test/java/in/rahulja/materialhn
rmdir -p app/src/main/java/io/github/sheepdestroyer app/src/test/java/io/github/sheepdestroyer 2>/dev/null || true
```

- [ ] **Step 2: Replace the package string everywhere**

Java, Kotlin and XML use the plain dotted name, except that Kotlin needs the leading segment escaped:

```bash
grep -rl "io\.github\.sheepdestroyer\.materialisheep" --include="*.java" --include="*.kt" \
  --include="*.xml" --include="*.pro" --include="*.gradle" --include="*.md" . \
  | grep -vE "^\./(build|app/build|\.gradle|\.kotlin|\.superpowers|docs/superpowers)/" \
  | xargs sed -i 's/io\.github\.sheepdestroyer\.materialisheep/in.rahulja.materialhn/g'

# `in` is a Kotlin keyword: package and import lines must escape it.
for f in $(grep -rl "in\.rahulja\.materialhn" --include="*.kt" app/src); do
  sed -i 's/^package in\.rahulja\.materialhn/package `in`.rahulja.materialhn/;
          s/^import in\.rahulja\.materialhn/import `in`.rahulja.materialhn/' "$f"
done
```

Fully-qualified uses inside Kotlin code (for example `in.rahulja.materialhn.R`) need the same backticks; string literals must stay plain. Without this, KSP fails with `Element '…MaterialisticDatabase' references a type that is not present`.

- [ ] **Step 3: Set the display name**

In `app/src/main/res/values/strings.xml` change `<string name="app_name">Materialisheep</string>` to `<string name="app_name">Material HN</string>`.

- [ ] **Step 4: Remove the Play Store link**

In `AppUtils.java` delete `PLAY_STORE_URL` and its use in the "rate app" flow; point that action at `https://github.com/xRahul/materialisheep` (open via `CustomTabsDelegate`). In `README.md` delete the Play Store badge/reference.

- [ ] **Step 5: Verify no old identity remains**

Run: `grep -rn "sheepdestroyer" app/ --include="*.java" --include="*.kt" --include="*.xml" --include="*.gradle" --include="*.pro" | grep -v "Copyright\|@author" | wc -l`
Expected: `0`

- [ ] **Step 6: Build and test**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 7: Verify the installed identity**

Run: `$ANDROID_HOME/build-tools/36.0.0/aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk | head -1`
Expected: `package: name='in.rahulja.materialhn'`

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor(identity)!: rename app to in.rahulja.materialhn"
```

### Task 2: Relicense to GPL-3.0-or-later

**Files:**
- Modify: `LICENSE.txt` (replace with GPLv3 text), `README.md`, `THIRD-PARTY-NOTICES.txt`, `PRIVACY-POLICY.txt` (licence section only), `docs/specs/*`

- [ ] **Step 1: Replace the licence text**

```bash
cp /usr/share/common-licenses/GPL-3 LICENSE.txt
head -3 LICENSE.txt   # expect: GNU GENERAL PUBLIC LICENSE / Version 3, 29 June 2007
```

- [ ] **Step 2: Add the upstream notice**

Create `NOTICE` at the repo root:

```
Material HN
Copyright (C) 2026 Rahul Jain

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

Derived from Materialisheep (https://github.com/sheepdestroyer/materialisheep),
Copyright the Materialisheep contributors, Apache License 2.0, and from
Materialistic (https://github.com/hidroh/materialistic), Copyright (c) 2015
Ha Duy Trung, Apache License 2.0. Those portions remain available under
Apache-2.0; the combined work is distributed under GPL-3.0-or-later.
```

- [ ] **Step 3: Record the GPL assets**

Append to `THIRD-PARTY-NOTICES.txt`: HaGeZi Pro mini DNS/Browser Blocklist, `app/src/main/assets/hagezi-pro-mini.txt`, GPL-3.0; PDF.js, `app/src/main/assets/pdf/vendor/pdfjs`, Apache-2.0; Readability.js, Apache-2.0; Droid Sans/Droid Serif/Roboto Slab, Apache-2.0; Libre Baskerville, SIL OFL 1.1.

- [ ] **Step 4: Update README licence badge and section**

Replace the Apache badge with GPL-3.0, state the fork lineage, and link `NOTICE`.

- [ ] **Step 5: Verify**

Run: `grep -c "GNU GENERAL PUBLIC LICENSE" LICENSE.txt && grep -i "GPL-3.0" README.md THIRD-PARTY-NOTICES.txt | wc -l`
Expected: `1` then at least `2`.

- [ ] **Step 6: Commit**

```bash
git add LICENSE.txt NOTICE README.md THIRD-PARTY-NOTICES.txt
git commit -m "docs(license): relicense to GPL-3.0-or-later for bundled GPL blocklist"
```

### Task 3: Delete binary blob and agent leftovers

**Files:**
- Delete: `pdfjs-legacy.zip`, `.Jules/`, `.jules/`, `.deepsource.toml`, `.vscode/settings.json`, `.github/commands/gemini-*.toml`, `.github/workflows/gemini-review.yml`, `.github/workflows/gradle.yml`
- Modify: `.gitignore` (drop now-unused `pdfjs-legacy/` entry only if the directory is gone)

- [ ] **Step 1: Confirm nothing references the zip**

Run: `grep -rn "pdfjs-legacy" --include="*.gradle" --include="*.yml" --include="*.java" --include="*.kt" . | grep -v "^./build"`
Expected: no output.

- [ ] **Step 2: Delete and commit**

```bash
git rm -r --cached .Jules .jules .deepsource.toml .vscode/settings.json pdfjs-legacy.zip
git rm .github/commands/gemini-invoke.toml .github/commands/gemini-review.toml \
       .github/commands/gemini-scheduled-triage.toml .github/commands/gemini-triage.toml \
       .github/workflows/gemini-review.yml .github/workflows/gradle.yml
rm -rf .Jules .jules .deepsource.toml .vscode/settings.json pdfjs-legacy.zip .github/commands
git commit -m "chore: remove prebuilt pdfjs archive and AI-agent leftovers"
```

- [ ] **Step 3: Verify no binaries**

Run: `git ls-files | grep -E "\.(zip|aar|so|jks|keystore)$" | wc -l`
Expected: `0`

### Task 4: Make backup rules valid and exclude credentials

**Files:**
- Create: `app/src/main/res/xml/data_extraction_rules.xml`
- Modify: `app/src/main/AndroidManifest.xml` (`android:dataExtractionRules`), `app/src/main/res/xml/backup.xml`
- Test: `app/src/test/java/in/rahulja/materialhn/BackupRulesTest.kt`

**Interfaces:**
- Produces: exclusion of SharedPreferences `auth_secure_store` and `app_webview` from cloud backup and device transfer.

- [ ] **Step 1: Write the failing test**

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class BackupRulesTest {
    @Test
    fun `credentials and webview data are excluded from backup`() {
        val xml = java.io.File("src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue(xml.contains("""<exclude domain="sharedpref" path="auth_secure_store.xml"/>"""))
        assertTrue(xml.contains("""<exclude domain="root" path="app_webview/"/>"""))
        assertTrue(xml.contains("<device-transfer>"))
    }
}
```

- [ ] **Step 2: Run it, expect failure**

Run: `./gradlew testDebugUnitTest --tests "*BackupRulesTest*"`
Expected: FAIL — file not found.

- [ ] **Step 3: Create the rules**

`app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="auth_secure_store.xml" />
        <exclude domain="root" path="app_webview/" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="auth_secure_store.xml" />
        <exclude domain="root" path="app_webview/" />
    </device-transfer>
</data-extraction-rules>
```

Keep `backup.xml` as `<full-backup-content>` and add the same two `<exclude>` entries for API 30 and below (`minSdk 31` makes it inert, but keep the schema valid). Point `android:dataExtractionRules="@xml/data_extraction_rules"` in the manifest.

- [ ] **Step 4: Run the test**

Run: `./gradlew testDebugUnitTest --tests "*BackupRulesTest*"`
Expected: PASS.

- [ ] **Step 5: Lint the manifest**

Run: `./gradlew lintDebug`
Expected: `BUILD SUCCESSFUL`, no `DataExtractionRules` error.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/xml/data_extraction_rules.xml app/src/main/res/xml/backup.xml \
        app/src/main/AndroidManifest.xml app/src/test/java/in/rahulja/materialhn/BackupRulesTest.kt
git commit -m "fix(backup): use valid extraction rules and exclude credentials"
```

### Task 5: Rewrite the privacy policy to be true

**Files:**
- Modify: `PRIVACY-POLICY.txt`, `README.md` (link), delete `docs/privacy.html` and `docs/licenses.html`

- [ ] **Step 1: Replace the policy**

Write: no analytics, no crash upload, no ads, no third-party SDKs; network destinations are `hacker-news.firebaseio.com`, `hn.algolia.com`, `news.ycombinator.com` and the article site the user opens; credentials are encrypted with an Android Keystore key and never leave the device; uninstalling removes all local data.

- [ ] **Step 2: Verify no false claims**

Run: `grep -inE "analytic|crash report|usage monitoring|collected" PRIVACY-POLICY.txt`
Expected: no matches except an explicit "does not collect" sentence.

- [ ] **Step 3: Commit**

```bash
git add PRIVACY-POLICY.txt README.md
git rm docs/privacy.html docs/licenses.html
git commit -m "docs(privacy): state the actual data handling"
```

### Task 6: Consolidate planning documents

**Files:**
- Create: `docs/ROADMAP.md`
- Modify: `docs/PRD.md`, `docs/TD.md`, `README.md`, `AGENTS.md`
- Delete: `ANALYSIS_REPORT.md`, `PROJECT_KNOWLEDGE.md`, `TODO.md`, `QOL_POLISH_PLAN.md`, `docs/SUMMARY_REPORT.md`, `docs/PRODUCT_REVIEW.md`, `docs/issues.md`

- [ ] **Step 1: Write `docs/ROADMAP.md`** with three sections: `Now` (this plan's phases), `Next` (reliability: UI error states, cancellable network calls, upstream test cherry-picks, dependency bumps), `Later` (product features: search facets, predictive back, baseline profile). Copy unresolved items out of the deleted docs; drop everything already shipped.

- [ ] **Step 2: Fix `docs/TD.md`** — replace the architecture description with what exists today: RxJava 3 clients + Kotlin `viewModelScope`, Room, Dagger 2 via KSP, `CacheOverrideNetworkInterceptor` TTLs, WebView JS gating, `AccountSecurity`, sync stack. Add the component-exposure table from Task 13 when it lands.

- [ ] **Step 3: Delete the rest**

```bash
git rm ANALYSIS_REPORT.md PROJECT_KNOWLEDGE.md TODO.md QOL_POLISH_PLAN.md \
       docs/SUMMARY_REPORT.md docs/PRODUCT_REVIEW.md docs/issues.md
git add docs/ROADMAP.md docs/PRD.md docs/TD.md README.md AGENTS.md
git commit -m "docs: consolidate planning into PRD, TD and ROADMAP"
```

- [ ] **Step 4: Verify no dangling links**

Run: `grep -rn "QOL_POLISH_PLAN\|PROJECT_KNOWLEDGE\|ANALYSIS_REPORT\|issues.md\|SUMMARY_REPORT" --include="*.md" . | grep -v "^./docs/superpowers"`
Expected: no output.

---

## Phase 1 — Store readiness

### Task 7: Version overrides and reproducible git hash

**Files:**
- Modify: `app/build.gradle`
- Test: `app/src/test/java/in/rahulja/materialhn/VersionConfigTest.kt` is not possible on Gradle properties; verify by command instead.

- [ ] **Step 1: Add property precedence**

In `app/build.gradle`, replace the version block with:

```groovy
def propOr = { String name, String fallback ->
    (project.findProperty(name) ?: System.getenv(name) ?: fallback).toString()
}
def verCode = propOr('versionCode', versionProps['VERSION_CODE'] ?: "101").toInteger()
def verName = propOr('versionName', "${verMajor}.${verMinor}.${verPatch}")
```

and the git hash:

```groovy
def gitHash = propOr('gitHash', { ->
    try { "git rev-parse --short HEAD".execute().text.trim() ?: "unknown" } catch (Exception ignored) { "unknown" }
}())
```

- [ ] **Step 2: Verify default values**

Run: `./gradlew -q :app:printVersion` after adding a `printVersion` task, or `./gradlew -q :app:properties | grep -i versionname | head -2`
Expected: `4.5.0`.

- [ ] **Step 3: Verify overrides**

Run: `./gradlew -PversionCode=999 -PversionName=9.9.9 -q :app:properties | grep -i versionName | head -2`
Expected: contains `9.9.9`.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle
git commit -m "build: allow versionCode/versionName/gitHash overrides"
```

### Task 8: Fastlane metadata tree

**Files:**
- Create: `fastlane/metadata/android/en-US/title.txt`, `short_description.txt`, `full_description.txt`, `images/icon.png`, `images/featureGraphic.png`

- [ ] **Step 1: Write the text files**

`title.txt`: `Material HN`
`short_description.txt`: `Hacker News reader with offline sync, reader mode and ad-blocking` (≤ 80 chars, verify).
`full_description.txt`: 4 short paragraphs — what it is, offline/sync, reader mode + ad-block, privacy (no analytics), GPL-3.0, link to source.

- [ ] **Step 2: Add images**

```bash
mkdir -p fastlane/metadata/android/en-US/images
python3 - <<'EOF'
from PIL import Image
src = Image.open("assets/materialistic-hi-res-icon.png").convert("RGBA")
src.resize((512, 512), Image.LANCZOS).save("fastlane/metadata/android/en-US/images/icon.png")
fg = Image.open("assets/materialistic-feature-graphic.png").convert("RGB")
fg.resize((1024, 500), Image.LANCZOS).save("fastlane/metadata/android/en-US/images/featureGraphic.png")
EOF
```

- [ ] **Step 3: Verify lengths and sizes**

Run: `wc -m fastlane/metadata/android/en-US/short_description.txt; python3 -c "from PIL import Image; print(Image.open('fastlane/metadata/android/en-US/images/icon.png').size, Image.open('fastlane/metadata/android/en-US/images/featureGraphic.png').size)"`
Expected: ≤ `80`, then `(512, 512) (1024, 500)`.

- [ ] **Step 4: Commit**

```bash
git add fastlane
git commit -m "docs(store): add fastlane metadata for F-Droid and IzzyOnDroid"
```

### Task 9: F-Droid recipe and submission checklist

**Files:**
- Create: `metadata/in.rahulja.materialhn.yml`, `docs/STORE_SUBMISSION.md`

- [ ] **Step 1: Write the recipe** exactly as in the spec §5.2, with the last released tag and matching `versionCode`/`versionName`.

- [ ] **Step 2: Write the checklist** — fdroiddata MR steps (`git clone https://gitlab.com/fdroid/fdroiddata`, add `metadata/in.rahulja.materialhn.yml`, `fdroid lint`, MR description template) and IzzyOnDroid steps (issue on `https://gitlab.com/IzzyOnDroid/repo/-/issues` with repo URL, licence, release asset pattern).

- [ ] **Step 3: Validate YAML**

Run: `python3 -c "import yaml;d=yaml.safe_load(open('metadata/in.rahulja.materialhn.yml'));print(d['License'],d['CurrentVersionCode'])"`
Expected: `GPL-3.0-or-later 101`.

- [ ] **Step 4: Commit**

```bash
git add metadata docs/STORE_SUBMISSION.md
git commit -m "docs(store): add F-Droid recipe and submission checklist"
```

### Task 10: Store validation workflow

**Files:**
- Create: `.github/workflows/store.yml`

- [ ] **Step 1: Write the workflow** on `release: types: [published]` and `workflow_dispatch`: assert fastlane files exist, short description ≤ 80 chars, icon and feature graphic sizes, recipe keys present, `CurrentVersionCode` equals the released `versionCode`, APK asset named `in.rahulja.materialhn-v*.apk`, and print the submission checklist to `$GITHUB_STEP_SUMMARY`.

- [ ] **Step 2: Run locally**

Run: `python3 - <<'EOF'` … same assertions … `EOF` or `act release -j verify` if `act` is installed.
Expected: all assertions pass.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/store.yml
git commit -m "ci: validate store metadata and F-Droid recipe on release"
```

---

## Phase 2 — CI/CD

### Task 11: Rewrite CI to cover push, PR, tests, lint and build

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Replace the workflow** with: `on: push: branches: [master]` + `pull_request`; `permissions: contents: read`; `concurrency: ci-${{ github.ref }}`; jobs `test` (`./gradlew testDebugUnitTest`), `lint` (`./gradlew lintDebug`), `assemble` (`./gradlew assembleDebug`); cache key `${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*','gradle/libs.versions.toml','gradle/wrapper/gradle-wrapper.properties') }}`; upload test and lint reports when `failure()`.

- [ ] **Step 2: Verify the YAML parses**

Run: `python3 -c "import yaml;yaml.safe_load(open('.github/workflows/ci.yml'));print('ok')"`
Expected: `ok`.

- [ ] **Step 3: Commit and push to a branch, confirm the run**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: run tests, lint and build on every push and pull request"
git push -u origin HEAD
gh run list --branch "$(git branch --show-current)" --limit 3
```

Expected: a `CI` run listed, then `gh run watch` green.

### Task 12: Pin actions and enable Dependabot

**Files:**
- Modify: every `.github/workflows/*.yml`
- Create or modify: `.github/dependabot.yml`

- [ ] **Step 1: Resolve SHAs**

Run: `for r in actions/checkout actions/setup-java actions/cache actions/upload-artifact android-actions/setup-android gradle/actions github/codeql-action softprops/action-gh-release; do echo "$r $(git ls-remote https://github.com/$r.git 'refs/tags/v*' | tail -1)"; done` and substitute each `@vN` with `@<sha> # vN`.

- [ ] **Step 2: Configure Dependabot**

`.github/dependabot.yml`: `package-ecosystem: github-actions` weekly, and `gradle` weekly with `open-pull-requests-limit: 5`.

- [ ] **Step 3: Verify no floating tags**

Run: `grep -rnE "uses: [^ ]+@v[0-9]" .github/workflows | wc -l`
Expected: `0`.

- [ ] **Step 4: Commit**

```bash
git add .github
git commit -m "ci(security): pin actions to commit SHAs and add dependabot"
```

### Task 13: Upgrade CodeQL and add dependency scanning

**Files:**
- Modify: `.github/workflows/codeql.yml`
- Modify: `.github/workflows/release.yml` (guard)

- [ ] **Step 1: CodeQL** — add `queries: security-extended` to the `init` step; keep `build-mode: manual` and `./gradlew assembleDebug`.

- [ ] **Step 2: Dependency graph** — add a `submit-dependencies` job to `ci.yml` on push to master using `gradle/actions/dependency-submission`, so Dependabot advisories cover Gradle dependencies. (OSV scanning was dropped: without lockfiles it cannot read a version catalog.)

- [ ] **Step 3: Release guard** — in `release.yml`, before `assembleRelease`, assert `git describe --tags --exact-match` equals `v$(VERSION_MAJOR).$(VERSION_MINOR).$(VERSION_PATCH)` from `version.properties`; fail otherwise. Rename the asset to `in.rahulja.materialhn-${TAG}.apk`. Add `apksigner verify --print-certs` and upload `app/build/outputs/mapping/release/mapping.txt` as an artifact.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows
git commit -m "ci(security): security-extended codeql, osv scan, release guards"
```

---

## Phase 3 — Security

### Task 14: Fail closed in `AccountSecurity`

**Files:**
- Modify: `app/src/main/java/in/rahulja/materialhn/accounts/AccountSecurity.kt`
- Test: `app/src/test/java/in/rahulja/materialhn/AccountSecurityTest.kt`

**Interfaces:**
- Produces: `AccountSecurity.getPassword(context, username): String?` returns `null` when the key is unavailable or invalidated; it never throws; it never substitutes an ephemeral key outside debug/test.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `savePassword does not persist when keystore is unavailable in release mode`() {
    // Robolectric has no AndroidKeyStore; the debug/test fallback is allowed there.
    AccountSecurity.savePassword(context, "alice", "secret")
    assertNotNull(AccountSecurity.getPassword(context, "alice"))
}

@Test
fun `getPassword returns null after stored ciphertext is corrupted`() {
    AccountSecurity.savePassword(context, "bob", "secret")
    context.getSharedPreferences("auth_secure_store", Context.MODE_PRIVATE)
        .edit().putString("pwd_bob", "AAAA").commit()
    assertNull(AccountSecurity.getPassword(context, "bob"))
}
```

- [ ] **Step 2: Run, expect the corruption test to fail or throw**

Run: `./gradlew testDebugUnitTest --tests "*AccountSecurityTest*"`
Expected: second test fails (current code logs and returns null — adjust the test if it already passes).

- [ ] **Step 3: Implement**

- Rename the catch-all to `catch (e: GeneralSecurityException)` plus `IOException`.
- Keep the ephemeral key path only under `BuildConfig.DEBUG`.
- In release, rethrow wrapped in `KeystoreUnavailableException` from `getOrCreateSecretKey`, and let `savePassword`/`getPassword` catch it, log once, clear the stored value, and return `null`.
- Catch `KeyPermanentlyInvalidatedException` explicitly and clear the ciphertext for that user.

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests "*AccountSecurityTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/in/rahulja/materialhn/accounts/AccountSecurity.kt \
        app/src/test/java/in/rahulja/materialhn/AccountSecurityTest.kt
git commit -m "fix(auth): fail closed when the keystore key is unavailable or invalidated"
```

### Task 15: WebView scheme allowlist and bridge path guard

**Files:**
- Modify: `app/src/main/java/in/rahulja/materialhn/MaterialWebView.java`, `WebFragment.java`, `data/PdfAndroidJavascriptBridge.java`
- Test: `app/src/test/java/in/rahulja/materialhn/WebFragmentSecurityTest.kt` (extend), `app/src/test/java/in/rahulja/materialhn/PdfBridgeTest.kt` (create)

**Interfaces:**
- Produces: `MaterialWebView.isAllowedNavigation(Uri): Boolean` accepting `http`, `https`, `mailto`, `tel`, `market`, `in.rahulja.materialhn`.
- Produces: `PdfAndroidJavascriptBridge.openFile(String): InputStream?` returning `null` unless the canonical path is inside `context.cacheDir`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `navigation to file scheme is blocked`() {
    assertFalse(MaterialWebView.isAllowedNavigation(Uri.parse("file:///data/data/in.rahulja.materialhn/x")))
}
@Test fun `navigation to https is allowed`() {
    assertTrue(MaterialWebView.isAllowedNavigation(Uri.parse("https://news.ycombinator.com/item?id=1")))
}
@Test fun `bridge rejects path outside cache`() {
    assertNull(bridge.openFile("/data/data/in.rahulja.materialhn/shared_prefs/auth_secure_store.xml"))
}
```

- [ ] **Step 2: Run, expect failure**

Run: `./gradlew testDebugUnitTest --tests "*PdfBridgeTest*" --tests "*WebFragmentSecurityTest*"`
Expected: FAIL — members missing.

- [ ] **Step 3: Implement**

- `MaterialWebView` overrides `shouldOverrideUrlLoading` (both overloads) and returns `true` for any URI failing `isAllowedNavigation`.
- `PdfAndroidJavascriptBridge` resolves the requested path, compares canonical paths against `cacheDir.getCanonicalPath() + File.separator`, returns `null` on mismatch.
- `WebFragment.loadUrl()` removes the JS interface in `onPageFinished` for the PDF document and before every non-PDF navigation.

- [ ] **Step 4: Run tests**

Run: `./gradlew testDebugUnitTest --tests "*PdfBridgeTest*" --tests "*WebFragmentSecurityTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/in/rahulja/materialhn/MaterialWebView.java \
        app/src/main/java/in/rahulja/materialhn/WebFragment.java \
        app/src/main/java/in/rahulja/materialhn/data/PdfAndroidJavascriptBridge.java \
        app/src/test/java/in/rahulja/materialhn/PdfBridgeTest.kt \
        app/src/test/java/in/rahulja/materialhn/WebFragmentSecurityTest.kt
git commit -m "fix(webview): restrict navigation schemes and confine pdf bridge to cache"
```

### Task 16: Audit exported components

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`, affected activities
- Modify: `docs/TD.md` (component table)

- [ ] **Step 1: Enumerate**

Run: `grep -n -B8 'exported="true"' app/src/main/AndroidManifest.xml`

- [ ] **Step 2: For each component record** name, export reason, accepted input, validation. Set `exported="false"` where no external caller exists. Add `android:permission` to any exported service or receiver that performs writes.

- [ ] **Step 3: Add input validation** for `ItemActivity`/`ListActivity` deep links: reject URIs whose host is not `news.ycombinator.com` and whose scheme is not `https`/`http`, except the `${applicationId}` internal scheme.

- [ ] **Step 4: Verify lint**

Run: `./gradlew lintDebug`
Expected: `BUILD SUCCESSFUL`, no `ExportedActivity`, `ExportedReceiver`, or `ExportedService` errors.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/in/rahulja/materialhn docs/TD.md
git commit -m "fix(security): narrow exported components and validate deep-link input"
```

### Task 17: Re-enable R8 optimisation safely

**Files:**
- Modify: `app/proguard-rules.pro`, `app/src/main/java/in/rahulja/materialhn/data/HackerNewsItem.java` (annotations only)

- [ ] **Step 1: Inventory reflection targets**

Run: `./gradlew assembleRelease --dry-run` then inspect `app/build/outputs/mapping/release` after a real build. Gson reflects on `HackerNewsItem` fields; Room and Dagger generate code.

- [ ] **Step 2: Protect the model**

Add `@SerializedName("...")` to each JSON-mapped field of `HackerNewsItem` (and the Algolia response classes), or keep `-keep class in.rahulja.materialhn.data.** { <fields>; }` if annotating is too invasive. Narrow the existing `-keep class ...** { *; }` to the classes that need it.

- [ ] **Step 3: Remove the global switches**

Delete `-dontobfuscate` and `-dontoptimize` from `app/proguard-rules.pro`.

- [ ] **Step 4: Verify release behaviour**

Run: `./gradlew assembleRelease && ./gradlew testDebugUnitTest`
Then install the release APK and open Top, New, an item with comments, and the reader. Expected: feeds load, comments render, no `JsonSyntaxException`.

- [ ] **Step 5: Compare size**

Run: `ls -l app/build/outputs/apk/release/*.apk`
Expected: smaller than the v4.5.0 release APK; record both numbers in the commit message.

- [ ] **Step 6: Commit**

```bash
git add app/proguard-rules.pro app/src/main/java/in/rahulja/materialhn/data
git commit -m "perf(release): enable r8 optimisation with narrow keep rules"
```

### Task 18: Cut the first store release

**Files:**
- Modify: `version.properties`, `metadata/in.rahulja.materialhn.yml`, `Changelog.md`

- [ ] **Step 1: Bump**

Set `VERSION_MINOR=6`, `VERSION_PATCH=0`, `VERSION_CODE=102` in `version.properties`. Update the recipe's `CurrentVersion`, `CurrentVersionCode` and `Builds[0]` block to `4.6.0` / `102`.

- [ ] **Step 2: Verify the tag guard works**

Run: `./gradlew -PversionCode=102 -PversionName=4.6.0 assembleRelease` then check the release workflow guard locally:
`test "$(git describe --tags --abbrev=0)" = "v4.6.0"` → expected to fail until the tag exists. Tag after the commit.

- [ ] **Step 3: Release**

```bash
git add version.properties metadata Changelog.md
git commit -m "chore(release): 4.6.0"
git tag v4.6.0
git push origin master --tags
gh run watch
gh release view v4.6.0 --json assets -q '.assets[].name'
```

Expected: `in.rahulja.materialhn-v4.6.0.apk`, `sha256sum.txt`, `mapping.txt`.

- [ ] **Step 4: Confirm installability**

Run: `adb install -r app/build/outputs/apk/release/*.apk` on an Android 12+ device, open Top/New/Item/Reader, log in, vote, save a favorite. Expected: no crash, no ANR.

---

## Phase 4 — Deferred (separate plan)

Recorded in `docs/ROADMAP.md` under `Next`: UI error states in `StoryListViewModel`, cancellable network calls in `HackerNewsClient`, upstream test-suite cherry-picks (`AppUtils` resource/intent suites, `Preferences` suites, `FileDownloader` traversal test), dependency bumps (Kotlin 2.4.0, KSP 2.3.9, OkHttp 5.5.0). Under `Later`: search facets, predictive back, baseline profile.

---

## Final verification

- [ ] `grep -rn "sheepdestroyer" app/ --include="*.java" --include="*.kt" --include="*.xml" --include="*.gradle" | grep -vi "copyright"` → empty
- [ ] `./gradlew clean testDebugUnitTest lintDebug assembleRelease` → `BUILD SUCCESSFUL`
- [ ] `aapt2 dump badging … | head -1` → `package: name='in.rahulja.materialhn'`
- [ ] `apksigner verify --print-certs …` → `Verifies`
- [ ] `git ls-files | grep -E "\.(zip|aar|so|jks)$"` → empty
- [ ] `gh run list --limit 5` → CI, CodeQL, OSV green
- [ ] Store checklist in `docs/STORE_SUBMISSION.md` ready for the owner to post
