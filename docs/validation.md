# Verification record — 30 October 2026

Test device: connected Samsung SM-S948B, 1080 × 2340, density 450, Android 17 development build. Native Kotlin/Compose app, MapLibre Native. Installation and testing used USB debugging.

## Automated checks

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`

- 45 tests pass: 10 route/progress, 4 heading math, 13 heading arbitration/projection/accuracy, 12 repository/service, and 6 catalogue/style regression tests.
- Route tests exercise sparse geometry, turn progression, jitter, off-route movement, crossing roads, looping near the destination, actual arrival, distance units and maneuver instructions.
- Repository tests use a local HTTP test server: query encoding, longitude/latitude ordering, duplicate results, missing names, empty reverse lookup, rate limiting, server errors, valid geometry/maneuvers, missing routes, remote road snaps, malformed JSON, dedicated motorcycle and pedestrian costing.
- Android lint: zero errors. Dependency-version and deprecated-API warnings remain; no claim of a warning-free report.
- All 27 style JSON files pass the official MapLibre style validator (zero errors), have unique layer IDs, valid source references, valid top-level camera expressions, and retain the connected road classes.

## Actual phone interactions

- Install/start and repeated app restarts; GPS opt-in is retained.
- Precise permission granted through Android's system dialog; actual position fixes reported approximately ±3–5 m during stationary tests.
- Verified Android registered Waypoint's hardware rotation-vector listener at a requested 20 ms sample interval. Settings exposes provider, sensors, compass confidence, satellite count and position accuracy.
- Search for a nearby named destination, select a result, render a road route and inspect turns.
- Car route: approximately 3.9 km / 7 min. Motorcycle: approximately 3.7 km / 16 min. Walking: approximately 3.6 km / 45 min. These are one test's estimates, not fixed demonstration data. Motorcycle/walking used Valhalla's actual dedicated profiles, with no car fallback.
- Start navigation, display next turn, ETA, remaining distance and stationary speed; end navigation.
- Navigation service remained present while the Activity was backgrounded. This is not a long-duration battery/endurance test.
- Save the selected destination and find it on Saved.
- The device loop exercised all offered atlas transitions while location and orientation listeners ran. A separate stationary route test confirmed that preview and active-guidance world changes preserve the selected destination and route. Large-text (1.3×) guidance, landscape and credits-dialog interactions were captured. The final optimized build is verified separately below.

## Issues fixed during development

- A per-theme width transformation wrapped zoom interpolation in multiplication. Native MapLibre discarded those road layers. The generator now scales interpolation output stops while preserving top-level camera expressions. Native street rendering was recaptured and confirmed; two regression tests and the official full-style validator cover the failure.
- Theme switching could leave a ValueAnimator pointing at a destroyed native SymbolLayer, causing a native crash. Animators now cancel synchronously before style replacement and disposal, with epoch guards on callbacks.
- A repeated readiness value could skip theme camera updates. Each style completion now uses a monotonic generation so camera/source effects rerun.
- Reverse-geocoding failure no longer leaves the demo city label on a live GPS map. A neutral Current area label and bounded retries replace the one-shot lookup.

- Null GeoJSON geometry caused a MapLibre native startup crash. Empty sources now use an empty FeatureCollection. No recurrence was observed after that fix in the subsequent test sessions.
- Custom POI labels requested unavailable default glyph fonts. They now use named bundled SDF stacks, with provider Noto fallback for other script ranges. MapLibre initializes before the HTTP glyph resolver.
- Excessive POI labels obscured the street network. Relevant categories are filtered and names reveal at closer zoom.
- GPS opt-in and 2D/3D preference now survive restarts.
- Compass heading wraps across north correctly; source/camera work was separated to avoid re-uploading routes for every sensor update.
- Light HUD panels now use their own dark text/secondary colors. Long fantasy titles use a smaller atlas font size. Native radio/tab semantics expose selection.
- Search/route errors provide recovery; changing transport mode invalidates the previous route.

## Limits of this verification

No physical road test, tunnel/urban-canyon test, long drive, audible-speaker verification, battery benchmark, or Android Auto head-unit/DHU test was performed. Reroute/arrival logic is unit tested, not observed during a real journey. An actual tablet was not available. Landscape and text scaling use the connected phone. The integration is a personal prototype, not production navigation certification.

The maps re-create visual design rules from game screenshots. Real-world geography and original assets mean they are not pixel-perfect replicas of proprietary, hand-authored game atlases. Terrain shading uses real elevation data; no fictional relief is invented. Public routing/search services have availability and rate limits; there is no live traffic or offline routing.

Native captures in `.impeccable/review/` contain the user's local geography and are ignored by Git and excluded from the source archive.

## UI and performance changes

- Labelled map tools replace icon-only actions; style/game name hierarchy is uniform in the launcher and Worlds library. Bohemia and Surface Map compasses are positioned in content flow to avoid overlapping lower controls.
- A native welcome screen offers world selection and Open map. Map rendering is deferred until entry; active navigation and external destination links retain their own entry behavior.
- Preview thumbnails batch their geometry into cached paths. The native map renderer and its camera/marker work pause behind lists and sheets. Sensor readings remain current in the arbiter while hidden screens avoid heading-driven state emissions.
- All 27 HUD palette foreground, accent and secondary pairs were checked against their alpha-composited panels over black and white underlays; minimum tested ratio was 6.14:1. This does not measure every label drawn directly over terrain.
- R8/resource shrinking was enabled for the original private release build. The existing signing identity is retained for compatible upgrades. Later releases use the public GitHub distribution described below; no public-store release is claimed.

## Historical private release verification (0.1.x)

The optimized release installed on the phone passed all 45 unit tests and Android lint with zero errors (35 warnings). The 27 styles also pass the official validator. Four connected-device test flows passed across the final targeted runs:

- `AtlasSmokeTest`: all 27 idle worlds with live location/sensor updates, captured individually.
- `WelcomeUiTest`: launch, Witcher search/selection, entry and Back, 1.3× text, and 2× text with animations disabled.
- `NavigationUiTest`: actual GPS route, retained destination/route across world changes in preview and guidance, 1.3× text, landscape, end-trip, idle Western map and map credits.
- `LibraryScrollTest`: ten flings through the catalogue with the native map covered.

The final navigation and scrolling run reports `OK (2 tests)`; the welcome flow passed separately and the 27-world loop passed before the isolated picker/footer corrections. Those corrections do not change idle atlas composition. Early test-harness failures caused by release dependency stripping, rotation timing, and edge gestures were corrected; failed runs are not counted as passes. Test-owned font, animation and orientation settings were restored.

Final scrolling measurement: 920 rendered frames, 36 missed deadlines (3.91%), median 6 ms, 95th percentile 10 ms, 99th percentile 18 ms. The earlier debug capture measured 24.78% and a 27 ms 95th percentile. This compares a debug build with an optimized build and test gestures differ slightly at the screen edge; it is not a controlled benchmark isolating one optimization, nor a promise of zero stutter. No app crash was present in the device crash buffer during the successful release checks from 18:29 onward.

Visual corrections include single-line Locate/Settings captions, non-wrapping End trip/Steps labels at 1.3×, a backed route toolbar, and clipping thumbnail drawing to its preview bounds so descriptions retain their contrast. The installed APK hash was checked against the built release file. Private device captures stay outside the source archive.

The finish review identified one remaining material defect: route endpoints could sit behind preview controls. The correction uses measured overlay bounds and a flat, north-up overview camera, including the actual position and selected destination. A final navigation run passed after recapturing GTA, Witcher and Cyberpunk previews, plus Witcher at 1.3× text in portrait and landscape. The compact panel retains the complete Start navigation action; Save/Saved remains a single line. These changes preserve the reviewed idle atlas compositions.

The independent verdict pass returned **ship** for this scored route-preview fix: all three refreshed previews show the complete route and both endpoints clear of controls; Witcher's 1.3× portrait and landscape previews retain a fully visible Start navigation action. No regression was observed in the refreshed large-text or landscape guidance captures. This bounded verdict does not certify the whole application or replace real-road validation.

## GitHub release extension (0.2.0)

The new updater and release preparation pass 60 unit tests (the existing 45 plus 15 release-parser cases), with zero failures, and Android lint with zero errors (37 warnings). All 27 map styles still validate. Device tests captured six worlds at explicit demonstration coordinates, the welcome/library, and the updater. `UpdateUiTest` exercised Settings, the installed version, a manual live GitHub check, the not-yet-published release error and retry control, then Back. These checks passed; private status bars were cropped out of published README images.

At the time of the 0.2.0 preparation checks, repository creation, signing-secret setup and live release testing were pending. That is a historical checkpoint: the repository is now public, GitHub CLI authorization and encrypted signing-secret setup are complete, and the corrected workflow passes. The current release and device-validation status appears below.

The independent UI review accepted the captured README and native updater idle/error layouts. Its single material fix was stale private-only product context; the verdict pass marked that documentation fix **ship**. This verdict does not certify the still-pending GitHub release or download/installation path. The locally built 0.2.0 APK is installed on the connected phone, with its installed SHA-256 verified against the packaged file.

## Security hardening and release preparation (0.2.1)

Normal development verification after remediation:

- Debug and optimized release builds succeed with Gradle 8.14.6 and AndroidX Car 1.8.0-rc01.
- Both variants pass **82 unit tests**, with zero failures, errors or skips. Added cases cover destination parsing/import generations, strict JSON nesting and value limits, invalid provider fields, redirect rejection, oversized known-length/chunked/gzip responses, geometry limits and request cancellation.
- Android lint reports **0 errors and 35 warnings** for each variant. Warnings remain; this is not a warning-free build claim.
- All **27 map styles** pass the official style validator. Version metadata passes the release-version check.
- Debug instrumentation APK compiles. New read-only tests are ready to check the installed package's disabled/private car component, private update provider, backup and cleartext policy; they have not yet run on a reconnected phone.
- Packaged debug/release manifest checks pass and are now CI gates. The car service is disabled and non-exported; the release is not debuggable. This verifies build output, not cross-UID exploit behavior.
- The historical remediation workflow passed Android SDK setup, the debug build, unit tests, lint, map styles and the packaged-component gate. Its superseded run was retired during the public-history privacy cleanup.
- The README was rendered through GitHub's Markdown renderer and visually inspected in Helium at desktop width and in a 390px iframe. Its hero, paired screenshot gallery and text wrapping were reviewed. The full gallery images remain linked at original size.

The completed standard security audit accounts for 31 coverage units and six independently verified needs-validation records, with no runtime-confirmed exploit. Both audit schema/coverage validators pass. It uses a frozen source snapshot of the earlier baseline; normal remediation builds above are separate development checks. No adversarial Binder/deep-link test was run on a personal phone or deployed service. The audit's required isolated execution controls were unavailable; see [security notes](security.md).

At release preparation, the previously connected phone is absent from ADB. The final in-app download/cancel/installer-handoff smoke test and installation of the GitHub-built APK therefore remain pending until it reconnects. Earlier device checks above remain historical evidence, not new 0.2.1 device results.

## Live navigation, search and launcher icon (0.2.2)

These checks address the reported stale pointer/route and missing search matches. Earlier evidence above is historical.

- **104 unit tests per build variant** cover the existing security/update boundaries plus sustained deviation, opposite-direction travel, noisy and batched fixes, retry backoff, route-line trimming, camera-follow policy, typo/spacing/accent matching, category shortcuts, provider fallback, cache reuse, cancellation and the confirmed search alias.
- **NavigationJourneyTest: three connected-device tests passed.** An isolated controller receives synthetic London fixes through the production location handler, with a controlled routing provider. The tests verify position/progress advancement, replacement geometry after a missed turn, retained motorcycle mode/destination, stale/poor-fix handling, and recovery after a simulated routing outage. They do not replace the device's location provider or change saved places.
- **MapFollowingTest passed on the phone.** Native MapLibre source coordinates and camera target are checked during and after movement. A real touch pan pauses guidance following; it recentres after eight seconds. The test also changes world while the position renderer is active. The remaining line starts at the user, with the travelled line removed.
- **SearchCoverageTest passed against live services on the phone.** Station and alternate-spelling queries returned provider results with their actual coordinates and addresses. Approximate, unrelated results are removed when a strong match exists. This verifies the exercised queries at test time, not all places or all devices. Private example queries and locations are intentionally omitted.
- **LauncherIconTest passed.** The manifest references an Android adaptive icon, its monochrome layer is present, and native renders cover the normal mark plus light/dark wallpaper-derived tints at 112 dp and 48 dp. Samsung may wrap icons returned by PackageManager; the test checks the declared adaptive drawable before native rendering. The two game-independent icon colours are not applied to game-map palettes.
- The final combined device run reports **OK (6 tests)** in 53.47 seconds. Debug and release each pass **104 unit tests**, and the packaged-manifest boundary checks plus all **27 map styles** pass. Android lint reports **0 errors / 35 existing warnings** in each build variant.
- The two read-only `SecurityConfigurationTest` checks also pass on the connected phone: the car service remains disabled/private, the update provider remains private, and backup/cleartext boundaries remain enforced.
- Two initial instrumentation harness problems (a non-void JUnit method and Samsung's wrapped package icon) were corrected and rerun. They are not counted as successful tests.

Native captures and device logs stay under ignored `.tooling/` / `.impeccable/review/`. Only the standalone icon rendered from original vector resources is published. No user trip screenshots or status bars are added to public documentation.

**Not established by these checks:** real moving-road accuracy, difficult GNSS/magnetic environments, distinguishing roads inside the position uncertainty, screen-off endurance/battery behaviour, or Google Maps-equivalent global place coverage. A real-road check is still needed; no claim of perfect navigation is made.


## Public examples and promotional README (0.2.3)

- Debug and release builds each pass **106 unit tests**. The new tests cover empty-by-default, instance-local spelling aliases and rejection of invalid alias entries. Android lint remains **0 errors / 35 warnings** per variant; all **27 map styles** and both packaged-manifest gates pass.
- **SearchAliasStorageTest: three device tests pass.** Synthetic private-preference fixtures verify that a confirmed spelling reaches the provider and that malformed/non-string storage safely preserves the original query.
- **Navigation and icon regressions: five device tests pass.** The three controlled journey tests, native camera-follow test and adaptive-icon test were rerun successfully after the privacy change.
- **SearchCoverageTest passes with fixed public London landmarks** against the live phone providers. Two read-only component/security tests also pass. Private search examples are excluded from committed fixtures and public notes.
- Promotional artwork is composed from existing, sanitised native London demo captures and the original adaptive icon. All public image files were checked for EXIF/GPS metadata; none is present.
- GitHub's Markdown renderer preserves the responsive picture sources. Browser captures at **1280px desktop and 390px mobile** show loaded images, dedicated mobile compositions, and no document-level horizontal overflow. Code blocks retain their normal internal scrolling.

Public release descriptions use neutral feature descriptions. The current source tree is checked for private sample queries, device identifiers and local account paths. The affected earlier commit and release were replaced with cleaned public history. Author, committer and annotated-tag identities use the public GitHub handle and no-reply email. A fresh remote clone verifies the rewritten history and identical file trees. Copies already downloaded or cached elsewhere cannot be recalled.

## Published APK and updater verification (0.2.3)

- The [tagged GitHub Actions workflow](https://github.com/the-geeky-couldron/waypoint/actions/runs/41098264439) passed the debug verification job and signed release build, including unit tests, lint and packaged-component checks. It published the APK and checksum file to [v0.2.3](https://github.com/the-geeky-couldron/waypoint/releases/tag/v0.2.3).
- The downloaded APK's SHA-256 matches both GitHub's asset digest and `SHA256SUMS`. Its package, version, non-debuggable release flag and existing signing identity were independently checked.
- A connected-device test on the previous signed release passed the live update flow: discovery, download cancellation, re-download, signature verification and handoff to Android's installer. The partial download file was absent after verification.
- After installing the verified GitHub APK, a second device test confirmed the installed version and manual update check. The screen reported **Waypoint 0.2.3** and **You're up to date**.
- The installed APK was read back and its hash matched the downloaded GitHub artifact. The test package was removed and normal device sleep restored. These two additional updater checks do not expand the real-road or endurance coverage described above.

## Onboarding and frame-time pass (2 November 2026)

- Connected Samsung phone, 1080 × 2340 at up to 120 Hz, debug build. Device test data/cache were cleared before each run.
- The opening globe's hidden fallback stickers kept animating under the video. After the fallback stops when video is ready and sticker motion uses render translation, a warmed 10-second `dumpsys gfxinfo` sample changed from **447 missed frames / 605 rendered** to **0 / 304**. The video remained visible in a native screenshot.
- The Worlds picker now decodes full preview artwork on a background dispatcher, keeps up to 32 MB of recent images, and preloads nearby cards. The same five-swipe check changed from **30 missed frames / 310 rendered** to **16 / 331** after the final placeholder pass. Original full-size artwork remains in the repository and cards were visually checked after scrolling.
- Map marker images and style JSON are prepared off the UI thread. Native MapLibre image submission remains synchronous: its asynchronous variant caused a rapid world-switch tilt regression and was removed. A single 65–73 ms frame was still observed during a debug theme switch; this pass does not claim zero jank for that transition.
- The separate voice-guidance onboarding page was removed. The horse animation page now explains turn-by-turn voice guidance and leads directly to the referral page; system Back returns to the horse page.
- `HorseOnboardingTest`, `WorldsScrollPositionTest`, `MapTiltWorldsTest` and `MapLocateWorldsTest` passed on the phone. The latter two exercise all 27 worlds. Debug assembly, unit tests, lint and `git diff --check` passed. No changes were pushed.

## Global real-place scenarios (2 November 2026)

- Added `GlobalScenariosTest` with 50 approximate settlement centres across 25 countries, pairing large cities with towns or villages. The set includes Kolkata/Ziro, Tokyo/Shirakawa-go, New York/Marfa, London/Bibury, São Paulo/Paraty, Cairo/Siwa, Sydney/Coober Pedy and Dubai/Hatta. Ziro's initial test point was at its airfield; the fixture was corrected to the town before the final run.
- On the connected Samsung, all **50/50** places loaded the requested world style and real road/building features after MapLibre reported a completed render. The camera and position marker stayed at the requested place. The test captures one screenshot per place; screenshots were reviewed as contact sheets, with individual checks for Tokyo and Ziro.
- The production search flow returned a nearby result in **50/50** places. The production routing repository returned a usable driving route in **50/50** places, and the navigation engine progressed along each route to arrival. Walking and motorcycle profiles each returned usable routes in **16/16** geographically spread urban/rural places.
- Six live routes rendered in the actual navigation screen across different worlds and modes. The Ziro walking case exposed premature “Arrive at your destination” guidance on a long final leg; the banner and navigation notification now say “Continue on NH13” until close to arrival. A unit test covers the distance transition, and the corrected screen was visually checked.
- Existing `NavigationJourneyTest` (3 tests), `MapFollowingTest`, `MapLocateWorldsTest` (27 worlds) and `MapTiltWorldsTest` (27 worlds) passed after this change. Device test data/cache were cleared before each invocation.
- This was a connected, stationary test using public location coordinates and live map/search/routing services; it did not involve travelling to 25 countries or verifying each road on the ground. Sparse rural detail is limited by available map tiles. The phone briefly lost DNS/TCP access to the public routing endpoint during one rerun; later device requests and the full routing suites succeeded.

## Repository hygiene and release packaging (2 November 2026)

- Removed unused Android stills and an obsolete globe-frame generator. All three retained MP4s are referenced by the welcome or onboarding UI. Remotion now stages its temporary images from the committed Android resources instead of tracking duplicate copies.
- Moved source screenshots, render outputs, reference imagery, local APKs, review notes and logs out of the repository. A CI hygiene check rejects those artifacts, unreferenced `drawable-nodpi` images, runtime videos without a source reference, missing README images, and basic private-path, personal-email and secret patterns.
- The cleaned source passed offline debug and release assembly, unit tests, Android lint, both packaged-manifest checks, version validation, 28 style JSON checks, Remotion ESLint/TypeScript checks, `git diff --check` and the new repository hygiene check. The local release APK was intentionally unsigned because no distribution credentials were supplied.
- A current-tree text scan found no local account paths, personal email addresses or key-shaped strings. A scan of 162 current app and README images found no GPS or personal-path metadata markers. These automated scans do not establish that every visible place name or future screenshot is safe to publish; contributors still review public examples manually.
