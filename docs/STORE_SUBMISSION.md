# Store submission

Everything F-Droid and IzzyOnDroid need is in this repository. Two manual steps
remain, and both need the owner's account.

Before submitting: cut a release (tag + GitHub release with the APK attached), so
the recipe's `commit`, `versionCode` and `versionName` point at something that
exists.

## 1. F-Droid

1. Fork `https://gitlab.com/fdroid/fdroiddata` and clone your fork.
2. Create the branch: `git checkout -b in.rahulja.materialhn`.
3. Copy `metadata/in.rahulja.materialhn.yml` from this repository into
   `metadata/in.rahulja.materialhn.yml` in fdroiddata.
4. Check the recipe against the build server:

   ```bash
   fdroid lint in.rahulja.materialhn
   fdroid rewritemeta in.rahulja.materialhn
   ```

5. Build it locally if you have the server image, otherwise rely on CI:

   ```bash
   fdroid build -v -l in.rahulja.materialhn:101
   ```

6. Commit, push, open a merge request titled
   `New app: Material HN (in.rahulja.materialhn)`.

MR body checklist:

- Upstream source: `https://github.com/xRahul/materialisheep`, tagged `v4.5.0`.
- Licence: GPL-3.0-or-later, `LICENSE.txt` in the repository root.
- No prebuilt binaries: assets are plain text/JS committed to git; the PDF.js
  archive was removed.
- No non-free dependencies: `./gradlew assembleRelease` resolves only from Maven
  Central, Google Maven and the Gradle plugin portal.
- No `google-services.json`, no Firebase runtime; the app talks to the public
  Hacker News API over HTTPS.
- `versionCode`/`versionName` accept `-P` overrides, which the recipe uses.
- Build server needs JDK 21 and Android SDK 36 (AGP 9.2.1). If the server lags,
  add `sdk: 36` and `jdk: 21` to the `Builds:` entry, or wait for the server
  image to catch up.
- Auto-update: `UpdateCheckMode: Tags` with `AutoUpdateMode: Version`.

## 2. IzzyOnDroid

1. Open an issue at `https://gitlab.com/IzzyOnDroid/repo/-/issues` titled
   `App inclusion request: Material HN (in.rahulja.materialhn)`.
2. Body:

   - Repository: `https://github.com/xRahul/materialisheep`
   - Releases: `https://github.com/xRahul/materialisheep/releases`
   - Release asset: `in.rahulja.materialhn-v<tag>.apk` plus `sha256sum.txt`
   - Licence: GPL-3.0-or-later
   - Category: Internet
   - Fastlane metadata is present under `fastlane/metadata/android/en-US/`.
   - No trackers, no analytics, no proprietary dependencies.

3. The crawler picks up new releases automatically once the app is added.

## 3. Release checklist

For every release:

- [ ] `version.properties` bumped; `metadata/in.rahulja.materialhn.yml` updated to
      match the tag.
- [ ] `Changelog.md` has an entry.
- [ ] Tag `v<versionName>` pushed; `release.yml` produced
      `in.rahulja.materialhn-v<tag>.apk`, `sha256sum.txt` and `mapping.txt`.
- [ ] `store.yml` validation job green.
- [ ] F-Droid `UpdateCheckMode` picks up the tag (no action needed after the first
      merge).
- [ ] Release notes warn that this is a new application id and cannot upgrade an
      existing `io.github.sheepdestroyer.materialisheep` install.
