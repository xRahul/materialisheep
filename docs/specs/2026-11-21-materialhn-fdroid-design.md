# Material HN — Store, Security & Identity Design

**Status:** draft for review
**Date:** 2026-11-21
**Base:** `master` @ `0a93fa32` (v4.5.0), fork of `sheepdestroyer/materialisheep`
**Goal:** ship one maintained app, `in.rahulja.materialhn`, on F-Droid and IzzyOnDroid, with the security and repository hygiene a published app needs.

---

## 1. What success looks like

1. `./gradlew assembleRelease` produces `in.rahulja.materialhn` APKs that install cleanly on Android 12+.
2. `./gradlew testDebugUnitTest lintDebug` is green in CI on every push.
3. An F-Droid recipe exists in-repo and builds the tag without network access at build time, no prebuilt binaries, no non-free dependencies.
4. IzzyOnDroid can ingest releases automatically from GitHub release assets.
5. No plaintext credential path, no silent crypto downgrade, no invalid backup rule, no false privacy claim.
6. One product doc set, one roadmap; no AI-agent leftovers.

## 2. Decisions

Each decision is reversible; the cost of flipping is noted.

| # | Decision | Rationale | Flip cost |
|---|---|---|---|
| D1 | **License: GPL-3.0-or-later** | Bundled `assets/hagezi-pro-mini.txt` is GPL-3.0. Whole work under GPLv3 is legal (Apache-2.0 is GPLv3-compatible) and keeps offline ad-blocking. | Medium: must drop the list or move to runtime download. |
| D2 | **Full rename** to `in.rahulja.materialhn` — `applicationId`, `namespace`, Java/Kotlin package tree, custom permissions, provider authorities, account type, proguard keeps, tests, docs. | Leaves no upstream identity in a published app; avoids "which app is this" in crash reports and store pages. | Low while unpushed; git revert of a mechanical commit. |
| D3 | **Version continuity** — keep `VERSION_CODE=101`, `VERSION_NAME=4.5.0` and continue. | F-Droid `CurrentVersionCode` and GitHub release history stay monotonic and explainable. | Trivial. |
| D4 | **Scope of first pass: store readiness + security.** Reliability and upstream cherry-picks are a second plan; product features come after the app is listed. | Smallest diff that produces a shippable, auditable app. | Scope can grow later without rework. |
| D5 | **Store submission artifacts prepared in-repo; owner submits** the fdroiddata MR and the IzzyOnDroid inclusion issue. | Both need the owner's GitLab/GitHub identity. | n/a |
| D6 | **Display name "Material HN"**, application id `in.rahulja.materialhn`. | User requirement; readable on launchers and store listings. | Trivial. |
| D7 | **Support via GitHub issues**, no Play Store link, no analytics, no crash upload. | Store policy and privacy accuracy. | n/a |
| D8 | **Keep Apache-2.0 attribution** for the upstream Materialistic/Materialisheep code and keep `THIRD-PARTY-NOTICES.txt` accurate. | GPLv3 requires preserving copyright notices. | n/a |

## 3. Non-goals for this pass

- Kotlin/Compose migration, DataStore, Hilt, multi-module split, WorkManager sync rewrite.
- New product features (search facets, predictive back, baseline profiles).
- Upstream rebase: the two build systems diverged (`libs.versions.toml` vs `ext`); only test suites and dependency versions are cherry-picked, as a separate plan.
- Certificate pinning: HN rotates certificates; pinning buys little for a public read-only API.

## 4. Identity and build changes

### 4.1 Rename surface (must all move together)

| Surface | Current | New |
|---|---|---|
| `applicationId` | `in.rahulja.materialhn` | `in.rahulja.materialhn` |
| `namespace` | same | same |
| Source path | `app/src/main/java/io/github/sheepdestroyer/materialisheep` | `app/src/main/java/in/rahulja/materialhn` |
| Test path | `app/src/test/java/io/github/sheepdestroyer/materialisheep` | `app/src/test/java/in/rahulja/materialhn` |
| Custom permissions | `<old>.permission.SYNC_ITEM`, `.AUTHENTICATE_ACCOUNTS` | `<new>.permission.*` |
| Providers | `<old>.syncprovider`, `.recentprovider`, `.fileprovider` | `<new>.*` |
| Account type | `<old>` (`authenticator.xml`, `BuildConfig.APPLICATION_ID`) | `<new>` |
| Proguard keeps | `-keep class in.rahulja.materialhn.**` | `<new>.**` |
| Intent scheme | `${applicationId}` (auto) | auto |
| Class refs in `res/xml/*` | `<old>.preference.*` | `<new>.preference.*` |

Data loss is accepted: a new `applicationId` is a new app. Existing GitHub-release users (old id, v4.5.0) cannot upgrade in place and must reinstall. Document this in the release notes.

### 4.2 Version overrides for F-Droid

`app/build.gradle` currently reads only `version.properties`. Add precedence:

```
versionCode = providers.gradleProperty("versionCode").orNull?.toInteger()
            ?: versionProps['VERSION_CODE']
versionName = providers.gradleProperty("versionName").orNull
            ?: "${major}.${minor}.${patch}"
```

so `./gradlew -PversionCode=102 -PversionName=4.6.0 assembleRelease` works, matching what fdroiddata expects.

`GIT_HASH` must come from a gradle property/env first, `git rev-parse` second, `"unknown"` last — a tarball build must stay reproducible.

### 4.3 Release build

- Keep R8 enabled (`minifyEnabled true`, `shrinkResources true`) but remove `-dontobfuscate` and `-dontoptimize` from `app/proguard-rules.pro` **only after** Gson model fields are protected (see plan Task 14).
- Publish `mapping.txt` as a release asset so obfuscated stack traces stay readable.

## 5. Store compliance

### 5.1 F-Droid requirements this repo violates today

| Requirement | Evidence | Fix |
|---|---|---|
| License declared and consistent | GPL-3.0 list inside Apache-2.0 app | D1 |
| No prebuilt binaries | `pdfjs-legacy.zip`, 6.5 MB, tracked | Delete; assets already unpacked |
| No invalid privacy claims | `PRIVACY-POLICY.txt` claims analytics/crash reporting that do not exist | Rewrite |
| Store metadata | no `fastlane/` tree, no recipe | Add |
| Build from source offline | assets are in git; no download step | Verified; keep it that way |
| Reproducible-ish | `GIT_HASH` from git at configure time | 4.2 |

### 5.2 Recipe

`metadata/in.rahulja.materialhn.yml`:

```yaml
Categories: [Internet]
License: GPL-3.0-or-later
AuthorName: Rahul Jain
SourceCode: https://github.com/xRahul/materialisheep
IssueTracker: https://github.com/xRahul/materialisheep/issues
AutoName: Material HN
Summary: Hacker News reader with offline sync, ad-blocking and reader mode
RepoType: git
Repo: https://github.com/xRahul/materialisheep.git
Builds:
  - versionName: 4.6.0
    versionCode: 102
    commit: v4.6.0
    gradle:
      - yes
      - -PversionCode=102
      - -PversionName=4.6.0
AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: 4.6.0
CurrentVersionCode: 102
```

Risks to record in the MR:
- AGP 9.2.1 / compileSdk 36 / JDK 21 must exist on the buildserver. If not, pin `sdk: 36` and `jdk: 21` in the build recipe, or ship with the highest SDK the server has.
- No `subdir` — gradle root is the repo root.

### 5.3 IzzyOnDroid

- Asset naming `in.rahulja.materialhn-v<tag>.apk` and `sha256sum.txt`, both attached to the GitHub release.
- `fastlane/metadata/android/en-US/{title,short_description,full_description}.txt` + `images/icon.png` (512×512) + `images/featureGraphic.png` (1024×500) so the crawler picks up listing data.
- Tracker scan: expect zero hits (no GMS, no analytics). The `AdBlocker` asset is a domain list, not a tracker SDK.

## 6. Security model

### 6.1 Credentials

- HN password: AES-256-GCM under an `AndroidKeyStore` key, stored in `auth_secure_store` SharedPreferences; password never in `AccountManager`.
- **Change:** `AccountSecurity.getOrCreateSecretKey()` must fail closed in release. The ephemeral in-memory key is a test-only path (`BuildConfig.DEBUG` or Robolectric fingerprint). A production Keystore failure surfaces as "re-login required", not silent encryption with a key that dies with the process.
- **Change:** key invalidation (`KeyPermanentlyInvalidatedException`, metadata mismatch) clears the stored ciphertext and prompts re-login once; it must not throw into the login flow.
- **Change:** both `auth_secure_store` and session cookies are excluded from Auto Backup. Keystore keys never leave the device, so backing up ciphertext only manufactures undecryptable garbage on restore.

### 6.2 Web content

- JS stays `isRemote`-gated (already true).
- **Change:** `shouldOverrideUrlLoading` accepts only `http`, `https`, `mailto`, `tel`, `market`, `${applicationId}`. `javascript:`, `file:`, `content:`, `intent:` from page content are dropped.
- **Change:** `PdfAndroidJavascriptBridge` only serves files whose canonical path is inside the app cache dir, and is removed as soon as the document finishes or navigation leaves `PDF_LOADER_URL`.
- File access stays disabled (`allowFileAccess=false`, `allowContentAccess=false`); the existing canonical-path check in `CacheableWebView.interceptRequest` stays.

### 6.3 Component exposure

18 exported components. Audit each for: who can launch it, what data it accepts, whether the data is validated. Rules:
- Activities that accept `Intent` data validate host/scheme before use.
- `WidgetProvider` and `WidgetConfigActivity` remain exported (required).
- The sync `Service` keeps `android:permission="<appid>.permission.SYNC_ITEM"` (signature).
- Anything that does not need export becomes `false`.

### 6.4 Supply chain

- Pin every GitHub Action to a full commit SHA; Dependabot updates `github-actions` and `gradle` ecosystems weekly.
- `permissions:` least privilege on every workflow (`contents: read` unless releasing).
- Drop `gemini-review.yml` and `.github/commands/gemini-*` — third-party AI automation with repo write access on PRs.
- CodeQL: `security-extended` queries, keep the weekly schedule.
- Add `osv-scanner` on push + weekly; fail on `HIGH`/`CRITICAL` in runtime dependencies.

### 6.5 Not doing

- No telemetry, no crash upload, no network logging in release (already `Level.NONE`).
- No certificate pinning.
- No root/tamper detection: it punishes legitimate power users and is bypassable.

## 7. Repository cleanup

Delete: `.Jules/`, `.jules/`, `.deepsource.toml`, `.vscode/settings.json`, `.github/commands/gemini-*`, `.github/workflows/gemini-review.yml`, `.github/workflows/gradle.yml` (duplicate of `ci.yml`), `pdfjs-legacy.zip`.

Consolidate docs:

| Keep | Absorb from | Delete |
|---|---|---|
| `docs/PRD.md` | product sections of `PRODUCT_REVIEW.md` | `ANALYSIS_REPORT.md` |
| `docs/TD.md` | architecture facts of `PROJECT_KNOWLEDGE.md`, security table | `PROJECT_KNOWLEDGE.md` |
| `docs/ROADMAP.md` | open items of `TODO.md`, `docs/issues.md`, `QOL_POLISH_PLAN.md` | `docs/SUMMARY_REPORT.md` |
| `README.md`, `CONTRIBUTING.md`, `CI.md`, `Changelog.md` | — | `docs/PRODUCT_REVIEW.md`, `docs/issues.md`, `TODO.md`, `QOL_POLISH_PLAN.md` |

`docs/licenses.html` / `docs/privacy.html` are upstream web pages: regenerate privacy text or drop the pair.

## 8. CI/CD model

```
push / PR  -> ci.yml        : testDebugUnitTest, lintDebug, assembleDebug, reports on failure
weekly     -> codeql.yml    : security-extended (java-kotlin, manual build)
weekly     -> osv.yml       : dependency vulnerabilities
tag v*     -> release.yml   : verify version.properties == tag, build signed, apksigner verify,
                              sha256, release assets in.rahulja.materialhn-v<tag>.apk
release    -> store.yml     : validate fastlane metadata + recipe, print submission checklist
```

All workflows: `permissions:` explicit, `concurrency:` set, actions pinned by SHA, Gradle cache keyed on `**/*.gradle*` + `gradle/libs.versions.toml`.

## 9. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| F-Droid buildserver lacks SDK 36 / JDK 21 / AGP 9 | Recipe build fails | Check before MR; add `sdk:`/`jdk:`; fall back to compileSdk 35 if needed |
| R8 obfuscation breaks Gson models | Release crashes on first API call | Annotate or keep `data` model classes; smoke-test release APK in CI artifact |
| Package rename orphans existing fork users | They must reinstall and lose data | Release-note warning; keep old APK available |
| GPL relicensing objection | n/a | Drop the list instead; decision is cheap to flip now |
| 18 exported components hide an issue | Local attack surface | Task 13 audits each one and records the result in `docs/TD.md` |

## 10. Verification gates

1. `grep -r "io.github.sheepdestroyer" app/src` returns nothing.
2. `./gradlew testDebugUnitTest lintDebug assembleRelease` green.
3. `aapt2 dump badging` shows `package: name='in.rahulja.materialhn'`.
4. `apksigner verify` passes on the release APK.
5. `git ls-files` contains no `*.zip`, `*.jar` other than `gradle/wrapper/gradle-wrapper.jar`.
6. Backup rules exclude credentials (unit test asserts the XML contents; lint validates schema).
7. `osv-scanner` reports no HIGH/CRITICAL.
