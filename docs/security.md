# Security review and hardening

## Android 2.0.0 codebase and GitHub audit (5 November 2026)

The full standard audit completed all six phases, including independent candidate and final-record reviews. Both the findings and coverage validators pass. It reviewed the entire Android checkout and read-only GitHub configuration, including quests, photos, reminders, sharing, saved-place names, bicycle routing, all 38 worlds, default release alerts, navigation and release signing. The separate iOS port was outside this Android checkout.

**Outcome: zero confirmed findings, four records needing validation and two rejected historical proposals.** The 167 coverage units comprise 124 covered, 19 candidate-linked and 24 blocked units. Candidate-linked units are review bookkeeping, not confirmed vulnerabilities. There are no deferred or unfinished review assignments. The final 740-path snapshot matched the independent coverage review. Full local reports and structured evidence are retained under `security-audit-skill/waypoint/run-5`.

The four unresolved records concern AndroidX Car App Library transport and lifecycle behavior: requests before a completed handshake, malformed session data, service-wide host metadata and shared Binder authority. Exact installed-package behavior, separate-UID delivery, containment and guidance effects require a disposable isolated Android fixture. No severity or demonstrated exploitation is claimed. The historical alternate-route race is rejected against the current synchronized guidance-session guard; the unused exported Compose preview entry is rejected against the repaired manifests.

The audit's required OS isolation and aggregate resource controls were unavailable, so it did not execute target code, probe the phone or run live attacks. It used source, configuration, hashes and static dependency inspection. The 24 blocked units retain explicit native/framework, hosted-authority and dependency-inventory gaps. Ordinary builds, manifest checks and phone/DHU tests in [validation](validation.md) are separate functional evidence. Completing the audit workflow does not certify that all runtime boundaries are safe.

After that frozen snapshot, the signing workflow was narrowed: successful pushes to `main` sign and save an APK artifact using a read-only repository token. Tag workflows skip the build and signing jobs, require a successful `main` run for the exact same commit, download only that run's signed artifact, verify its checksum, and publish it. Only the tag-publish job receives repository-content write access; it also has Actions read access to retrieve the artifact. Manual signing of arbitrary selected branches was removed. This workflow follow-up is a source change after the audit snapshot, not a new independent security audit.

The current source protects default update jobs with Android's binding permission, cancellation generations and current notification/driving checks. Saved-place renaming changes each copy's name without replacing its coordinates or provenance. Car surface replacement cancels its delayed refresh and closes acquired resources. The unused dependency-provided debug Compose preview activity is removed, and packaged-component checks prevent it returning. Quest rewards remain transactional, pictures stay private and incoming places require confirmation.

Read-only GitHub checks found secret scanning and push protection enabled, a read-only default Actions token and commit-pinned actions in the workflow. The visible collaborator list contained the owner. Main had no protection, effective rulesets and environments were empty, Dependabot alerts were disabled, and code scanning had no analysis. Tag/integration authority and dependency inventory were not fully visible. These are recorded limits and hardening opportunities, not demonstrated unauthorized access. Signing authority includes manual candidate dispatch and editing the selected branch; candidate builds must use reviewed source. The audit changed no repository settings and retrieved no secret values.

## Earlier source audit and safeguards (3 November 2026)

A standard full-source audit of the Android Auto and Ferrostar checkout mapped 44 security coverage units: 28 covered, five candidate units linked to four **needs-validation** leads, and 11 blocked by unavailable AndroidX/native runtime evidence. No exploit was confirmed. The frozen baseline report is outside the repository under `security-audit-skill/waypoint/run-3`. The local Docker daemon was unavailable, so this audit did not run target builds, emulator fixtures, or app tests.

After freezing that baseline, the app began binding notification End actions to random trip IDs, rejecting stale car template/result callbacks, binding Ferrostar alternate routes to the current session and destination, bounding car virtual-display input, and limiting Android Geocoder callback copies. The release job also checks that a tagged commit reaches `main` before restoring signing material. These source changes are preventive safeguards; they do not retroactively confirm or refute the baseline runtime leads. Effective GitHub tag and environment rules, AndroidX Binder behavior, and MapLibre/Ferrostar native decoding still require separate validation.

## Current Android Auto implementation (3 November 2026)

Waypoint's car service is now enabled for signed Android Auto hosts. The service validates
the host package and signing certificate; an on-device test accepts the installed Android
Auto host and rejects a different UID. The projected UI uses AndroidX Car App templates,
keeps map drawing behind the host controls, and requires confirmation before an incoming
navigation destination is selected. Shared links cannot replace active guidance. The
packaged manifest gates check the exported car service, its navigation category, and
phone/car `geo:` handoff filters. These are functional and configuration checks, not a
new adversarial security audit. The 0.2.1 findings below describe the earlier disabled
component and remain historical evidence.

## Current source review (1.0.0 release, 2 November 2026)

A second standard audit reviewed the current dirty checkout, including the new feedback flow, debug capture activity, map-tile input, update path and release workflow. Its 37 coverage units ended with 34 covered, two source-grounded leads needing validation and one blocked native-decoder unit. No vulnerability was confirmed by a bounded runtime test. The host lacked the required isolated execution environment, so this audit did not run app builds, Android fixtures or provider requests. The full report and frozen reviewed-source inventory are outside the repository under `security-audit-skill/waypoint/run-2`.

After that baseline review, the debug preview activity was made private, external destination links were changed to require an in-app confirmation before lookup or route preview for the imported destination, map-tile labels were bounded before selection, and local release builds without distribution credentials were made unsigned. Existing user-initiated provider work may continue while a shared-link confirmation is open. These are source fixes and hardening changes; the two audit leads retain their original **needs validation** verdicts until an isolated Android check establishes the runtime behavior. MapLibre native decode limits remain a separate coverage limit.

## Previous 0.2.1 review

Waypoint 0.2.1 includes a source audit of the Android app and release path, followed by remediation. The baseline reviewed was `2d63dbd57e2622ffa133983dfe04607f9ca43739`. Review covered destination intents, local storage, exported components, location input, provider responses, update downloads and GitHub Actions.

The audit used independent source reviewers and candidate verification. **No exploit was demonstrated on an Android runtime.** The required isolated execution environment was unavailable, so cross-app delivery, process failure and data access hypotheses remain validation leads rather than confirmed vulnerabilities. Normal development builds and regression checks are reported separately in [validation](validation.md).

## Review outcome

The completed review accounted for **31 coverage units** across five hunting waves and a separate final coverage check. Six source-grounded leads passed both candidate and final-record verification: two destination-import leads and four car-service leads. There are **no runtime-confirmed exploits** and no assigned severity. The two app-level paths were fixed; the four car paths are contained by disabling the component.

Two baseline coverage gaps remain explicit: the original CI cache defaults/ref scoping and the complete packaged/runtime class inventory reachable by the car decoder. The release now disables signing-job caches and the car component. Neither change retroactively proves the baseline safe or exploited. Full traces stay in the local audit report; this public note records the fixes and limits.

## Changes in the earlier 0.2.1 release

| Boundary | Source concern | Remediation |
| --- | --- | --- |
| Shared destination → active guidance | An incoming link could select a new destination and stop the current trip. | Active guidance rejects external destination proposals. Only supported VIEW links with bounded, validated data are accepted. |
| Delayed address search → current selection | A stale lookup could overwrite a newer choice or a started trip. | Imports retain a cancellable job and generation. Selection, clearing, searching and guidance transitions invalidate obsolete work; publication checks cancellation and current state. |
| Experimental car service → app state | Host authentication was not consistently bound to later calls; malformed or out-of-order protocol input reached unchecked library paths. | Android Auto is disabled, not exported and no longer advertised. No settings toggle can enable it. Phone navigation stays available. |
| Provider response → navigation | Byte size, parser allocation, numeric fields and geometry complexity needed stronger local bounds. | Decompressed byte limits, strict JSON syntax, nesting/value limits, finite geographic/numeric validation, collection/work limits and cancellable calls. Route preparation runs off the main thread. |
| Sensor fix → heading/progress | Invalid coordinates, accuracy, timing or altitude could contaminate calculations. | Invalid fixes are rejected; speed is sanitized and declination altitude is bounded. This does not establish an unauthorized sensor-injection path. |
| Build input → signed APK | Wrapper verification and cache policy relied on defaults. | Gradle 8.14.6, pinned distribution checksum, URL validation, explicit read-only cache policy outside main pushes and no Gradle cache in signing jobs. |
| Generator input → committed assets | Regeneration could silently consume changed remote input. | Base-style and bundled glyph-generator hashes are verified before regeneration. CI builds committed styles instead of fetching a new base. |

Disabling the Android Auto component contained the reviewed 0.2.1 paths at that time; it did **not** fix the underlying library or prove every possible car-host interaction safe. The current implementation uses AndroidX Car App Library host validation and has connected-device/DHU functional checks; malformed protocol fuzzing and a physical head-unit security assessment remain separate work. Updating the library alone is insufficient evidence.

## Controls already present

- HTTPS-only app network policy; no WebView/native JavaScript bridge.
- Private saved-place storage, with backup and device transfer disabled.
- A non-exported update FileProvider restricted to its update cache.
- Updates restricted to this repository's stable releases and GitHub download hosts.
- Download size and SHA-256 verification, followed by package, version and signer checks.
- Android installation permission and confirmation remain under the user's control.
- No GitHub token, app account, analytics backend or recorded location history.
- Commit-pinned Actions and signing secrets restricted to the `main` signing job.

## What remains unverified

The review does not certify MapLibre's native tile, font, image or terrain decoders; Android framework internals; hosted provider behavior; compromised maintainers; signing-key custody; or every transitive dependency. Updater key rotation intentionally fails closed until a signing-lineage policy is designed. Dependency verification and repository tag/environment protection remain maintenance opportunities, not demonstrated takeover paths.

Android cross-app exploit fixtures were not run. Do not infer a tested attack, severity or guaranteed exploitability from a source trace. Functional unit tests, package configuration checks and ordinary updater UI checks are different evidence. See the current [validation record](validation.md) for completed checks and device limitations.

No single audit makes software “bulletproof.” Report new concerns through the [private reporting process](../SECURITY.md).

## Maintenance references

Gradle's [release checksums](https://gradle.org/release-checksums/) and [wrapper verification guidance](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:verification) support the distribution pin. Gradle 8.14.6 addresses the upstream [repository failover](https://github.com/gradle/gradle/security/advisories/GHSA-j5m7-59rp-24f5), [client/daemon](https://github.com/gradle/gradle/security/advisories/GHSA-xwqc-3h47-hg64) and [worker/daemon](https://github.com/gradle/gradle/security/advisories/GHSA-mvvg-497x-hmj8) advisories; this is dependency maintenance, not proof those issues were exploited here.

The [AndroidX Car release notes](https://developer.android.com/jetpack/androidx/releases/car-app) document upstream security fixes. The current source uses 1.8.0-rc01 with the projected component enabled. Android's [deep-link security guidance](https://developer.android.com/privacy-and-security/risks/unsafe-use-of-deeplinks) describes the external-input boundary.
