# Building and releasing Waypoint

The public update channel is `the-geeky-couldron/waypoint` on GitHub Releases. Android installation remains user-confirmed. This APK distribution is intended for GitHub sideloading, not Google Play in-app updates.

## What runs automatically

- Pushes to `main`, pull requests and manual runs: version validation, 27 map-style checks, debug APK, unit tests and Android lint. Reports and the debug APK are saved for 14 days.
- A pushed `v*` tag: the same checks, followed by an optimized, signed APK. GitHub publishes `Waypoint.apk` and `SHA256SUMS` only after those jobs succeed.
- The signing job disables Gradle caches. Verification caches are read-only outside pushes to `main`; the basic GitHub cache provider is explicit.
- Pull requests never receive the release signing secrets. Actions are pinned to commit hashes. The release job alone receives repository-content write permission.
- Before restoring the signing key, the release job checks that the tagged commit is already reachable from `main`. This catches accidental tags on unmerged commits.

## Protect release authority

The owner must verify the effective GitHub rules for `main` and `v*` tags before relying on the signed update channel: require review and passing checks before changes reach `main`, limit who can create release tags, prevent tag replacement or deletion, and require an independent release approval where available. A tag-push workflow is read from the tagged revision, so the ancestry check inside this workflow cannot by itself protect against an actor who can tag an arbitrary commit containing a changed workflow. Record who can change workflow files and who can access release signing secrets. These controls are configured on GitHub and are not established by this repository's source.

## Signing

The release job requires four repository Actions secrets:

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
3. Run verification, commit, push, then tag the exact commit:

```sh
python3 scripts/check_version.py
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
# Example only: first change version.properties and both release-note files to match.
git add version.properties docs/releases/current.md app/src/main/assets/release-notes.md
git commit -m "Release 1.5.0"
git push origin main
git tag -a v1.5.0 -m "Waypoint 1.5.0"
git push origin v1.5.0
```

Watch the Android workflow. Its release job verifies that the tag and version file agree. After publication, verify the APK and checksum on the release page. Keep pushed tags immutable, including tags whose release workflow failed. Use a new patch version and an annotated tag for corrections; never overwrite an already-published APK.

## Updater behavior

Waypoint checks GitHub's latest stable release API when the app enters the foreground. Drafts, prereleases and older/equal versions are ignored. An available update is offered over the idle map and returns on each launch until installed; choosing Later dismisses it only for the current foreground entry. The update prompt and Settings show GitHub's release notes before download. After installation, a one-time What’s New dialog shows the matching notes bundled in the APK. Downloads remain explicit, with no background polling, GitHub credentials or location information involved. Settings also offers a manual check.

On a later foreground entry, Waypoint refreshes an earlier available offer so a newer release can supersede it. A temporary check failure keeps the earlier offer; an active download or verified APK remains available until the user acts on it.

Pushing a commit to `main` only runs verification. Installed users see the update prompt after the newer signed `v*` GitHub release is published and they next open Waypoint on the phone. The app does not send a push notification while closed or interrupt navigation in the car.

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
