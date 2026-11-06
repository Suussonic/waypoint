# Building and releasing Waypoint

The public update channel is `the-geeky-couldron/waypoint` on GitHub Releases. Android installation remains user-confirmed. This APK distribution is intended for GitHub sideloading, not Google Play in-app updates.

## What runs automatically

- Pull requests targeting `main` run source checks: [actionlint](https://github.com/rhysd/actionlint) and [zizmor](https://github.com/zizmorcore/zizmor) for workflow safety; repository hygiene and version checks; all 39 authored map styles and their night variants; ESLint and TypeScript for the editable motion source; and the [Dependency Review Action](https://github.com/actions/dependency-review-action) to reject new dependencies with moderate or more severe known vulnerabilities. Pull requests do not build the Android app or receive signing secrets.
- Only a push to `main` builds the debug APK, runs unit tests and Android lint, then runs focused emulator tests for packaged security, quest storage and car sessions. After those checks pass, that same `main` push signs the optimized release APK. The debug APK and test reports are saved for 14 days. Pushing `dev` does not start the Android workflow.
- The `Android` workflow graph starts with repository integrity, fans out into workflow, dependency, map-style and motion-source checks, then joins again. On a `main` push, it also builds the Android app, runs the emulator and signs the release artifact. The final `CI tree` job writes an indented result tree to the run summary and fails if any check required for that event failed or was skipped. Require **CI tree** in the `main` branch rules if you want a single branch-protection check; its pull-request result covers source checks, while Android builds and tests run after a `main` push.
- Every push to `dev` checks the public site and deploys the `docs/` site through pinned GitHub Pages Actions. Pages has no manual branch dispatch; a deployment always checks out the pushed `dev` commit. The `github-pages` environment accepts only `dev` deployments. Keep `dev` branch write access limited to reviewed maintainers.
- Every successful push to `main` builds and signs the optimized APK after the checks pass, then saves `Waypoint.apk` and `SHA256SUMS` as a 14-day Actions artifact. No manual signing dispatch is required. Main builds are never canceled by a newer push, so a tag can still find its exact commit's artifact.
- The separate `Release` workflow handles pushed `v*` tags, so tag publishing does not appear as a disconnected skipped node in the `Android` graph. It does not rebuild or re-sign the APK. It finds the successful `main` run for the exact tagged commit, downloads that run's signed artifact, verifies its checksum, then publishes the APK and checksum file. If the main run is still running, the tag job waits up to 30 minutes; it fails sooner if that run finishes unsuccessfully. A tag without a successful matching main artifact fails.
- The signing job disables Gradle caches. Other Android jobs use the basic GitHub cache provider, with pull requests and emulator jobs read-only. npm dependencies use lockfiles and the setup-node cache.
- The motion-source check also runs `npm audit` at moderate severity, including the optional local tooling dependencies.
- [Dependabot](../.github/dependabot.yml) checks Gradle, both npm toolchains and pinned GitHub Actions weekly, and proposes updates for review. Repository security updates are enabled; Dependabot opens security pull requests against `main`. This configuration becomes active when it reaches the default branch. Dependency changes still have to pass the Android check tree before merging.
- Actions are pinned to commit hashes; the actionlint binary is pinned to a release and SHA-256 digest. Signing jobs are read-only; only the tag-publish job receives repository-content write permission and read access to Actions artifacts. The release job checks for a successful exact-commit `main` build before checking out repository code. zizmor runs without paid GitHub Advanced Security.

## Protect release authority

The hosted `release` environment accepts only `v*` tags and requires approval from the repository owner before publishing. The owner must verify the effective GitHub rules for `main` and `v*` tags before relying on the signed update channel: require review and passing checks before changes reach `main`, limit who can create release tags, and prevent tag replacement or deletion. GitHub evaluates a tag-push workflow from the tagged revision, so protect `main` and release-tag authority even though the publisher only accepts artifacts from a successful `main` run for the exact same commit. Record who can change workflow files and who can access release signing secrets. Main and tag rules are hosted controls and are not established by this repository's source.

The published artifact must come from a completed successful `main` run and match the tagged commit exactly. Release tags should be pushed while that signed artifact is still available; it expires after 14 days.

## Signing

The automatic `main` signing job requires four repository Actions secrets:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 contents of the signing keystore |
| `ANDROID_STORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Signing key alias |
| `ANDROID_KEY_PASSWORD` | Private-key password |

Keep an encrypted, offline backup of the keystore. Never commit it, paste it into issues, or upload it as a workflow artifact. The workflow restores it only in the runner's temporary directory and removes it after the build. Release builds fail if the signing secret is missing.

The initial project installation used a local development signing identity. Its existing identity is retained for this personal release channel so installed copies can update without losing saved places. Forks should create and retain their own release key before distributing their first APK; a fork's APK cannot replace the official release. Do not generate a fresh key on each workflow run.

Local release builds accept `WAYPOINT_KEYSTORE`, `WAYPOINT_STORE_PASSWORD`, `WAYPOINT_KEY_ALIAS` and `WAYPOINT_KEY_PASSWORD` environment variables. Without them, the release APK is unsigned and cannot be installed. GitHub's release workflow always requires the explicit signing secrets.

## Publish an update

1. Change `version.properties`. Use a stable `major.minor.patch` version, with each part from 0–999. Set `VERSION_CODE = major × 1,000,000 + minor × 1,000 + patch`. This matches the updater in the published app, which checks the APK code against the release tag. Always increase the code; `1.5.0` uses `1005000`.
2. Update `docs/releases/current.md` and copy it to `app/src/main/assets/release-notes.md`. The bundled copy powers the post-update What’s New screen; GitHub’s release body powers the updater. `scripts/check_version.py` requires both copies to match. Update tests/screenshots when behavior or appearance changes.
3. Run verification, commit and push to `main`. Wait for its signed artifact job to pass, then tag that exact commit. The tag workflow publishes the existing signed artifact without rebuilding it:

```sh
python3 scripts/check_version.py
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
# Example only: first change version.properties and both release-note files to match.
git add version.properties docs/releases/current.md app/src/main/assets/release-notes.md
git commit -m "Release 2.0.0"
git push origin main
git tag -a v2.0.0 -m "Waypoint 2.0.0"
git push origin v2.0.0
```

Watch the `Android` workflow for the signed artifact, then the `Release` workflow after pushing the tag. The release job verifies that the tag and version file agree, then checks the downloaded APK against its SHA-256 file before publishing. After publication, verify the APK and checksum on the release page. Keep pushed tags immutable, including tags whose release workflow failed. Use a new patch version and an annotated tag for corrections; never overwrite an already-published APK.

## Updater behavior

Waypoint checks GitHub's latest stable release API when the app enters the foreground, at most once every 30 minutes. A manual Settings check remains available at any time. Drafts, prereleases and older/equal versions are ignored. An available update is offered over the idle map and returns on each launch until installed; choosing Later dismisses it only for the current foreground entry. The update prompt and Settings show GitHub's release notes before download. After installation, a one-time What’s New dialog shows the matching notes bundled in the APK. Downloads remain explicit, with no GitHub credentials or location information involved.

Version 2.0.0 also schedules background checks by default, about twice a day, using Android's persistent JobScheduler with network and battery constraints. Android chooses the actual execution time and may delay checks while the phone sleeps. A contextual explanation precedes the notification permission request. Battery settings are offered with an explanation of delayed checks; declining either permission path leaves normal use and manual checks available. There is no direct battery-exemption permission or always-running update service.

An allowed App updates notification opens update details and is deduplicated per release version. It never starts an APK download or installation. The notification channel and Android notification settings remain under the user's control. Checks defer notifications during driving navigation or Android Auto use. No paid push provider, account or new backend is involved.

On a later foreground entry, Waypoint refreshes an earlier available offer so a newer release can supersede it. A temporary check failure keeps the earlier offer; an active download or verified APK remains available until the user acts on it.

Pushing a commit to `main` automatically creates a signed APK artifact for testing; it does not publish or notify users. Installed users see the update prompt after the newer signed `v*` GitHub release is published and they next open Waypoint on the phone. Version 2.0.0 and later can also notify while closed after a scheduled check, if Android allows notifications. Older installed versions gain that background behaviour only after updating.

Before showing Install update, Waypoint checks:

- Stable semantic version and a higher Android version code.
- Exactly one uploaded `Waypoint.apk` asset at this repository's expected release URL.
- HTTPS downloads restricted to GitHub's release hosts.
- Declared size (maximum 150 MiB), complete download and GitHub's SHA-256 asset digest.
- Correct Android package, version name/code and the same signing identity as the installed app.

Partial files are discarded when a download fails or is cancelled. Downloads survive screen rotation while the app process lives; if Android ends the process, check and download again. Only the dedicated cache/updates directory is shared with Android's installer through a non-exported FileProvider. Android may require the user to allow installation from Waypoint. The app never toggles that setting or installs silently. Installation is disabled during active navigation.

Private GitHub repositories need authenticated release access and are not supported by this public, token-free update channel. A fork must change `ReleaseUpdate.REPOSITORY`, README links and the signing identity to its own values.

## Android Auto distribution and testing

The GitHub APK channel alone does not guarantee visibility in a real car. Google's
[testing documentation](https://developer.android.com/training/cars/testing#real-vehicles)
requires a trusted source for Car App Library apps; **Unknown sources** does not cover this
category. Do not describe a missing launcher entry as fixed by a manifest change or DHU test.

1. Build locally and run `scripts/check_packaged_manifest.py debug` (or `release` for the
   release variant). This checks service discovery, the template descriptor, car API metadata,
   and navigation/surface permissions in the packaged manifest.
2. Exercise the build using the Android Auto Desktop Head Unit. This is local development
   validation, not a distribution eligibility check.
3. For real-vehicle validation, the maintainer must establish a Google Play Internal App
   Sharing or Internal Test Track channel and install through that channel. Follow Play's
   setup and signing requirements; keep its signing identity compatible with the intended
   update channel. No Play channel is created or published by this repository's GitHub workflow.
4. Confirm that Waypoint appears in **Customize launcher**, opens on a real head unit,
   and provides map, preview, guidance and voice. Record the installation channel and test
   outcome without publishing personal routes or device identifiers.

Keep the host certificate allowlist intact. Do not fake an installer identity or relax host
validation to work around launcher eligibility.
