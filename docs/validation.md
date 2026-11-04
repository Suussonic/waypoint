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
- A current-tree text scan found no local account paths, personal email addresses or key-shaped strings. One six-decimal, device-like location in a camera test was replaced with an approximate public Kolkata centre; CI now flags overly precise coordinate literals in device tests. A scan of 162 tracked images found no GPS or personal-path metadata markers. One onboarding JPEG has EXIF limited to dimensions, resolution and colour space; the three runtime videos contain only container, encoder and Remotion tags. These checks do not establish that every visible place name or future screenshot is safe to publish; contributors still review public examples manually.

## Android Auto driving view (3 November 2026)

- Compared Google Maps and Waypoint on the same Android Auto Desktop Head Unit and shared destination. Google Maps showed a flat route overview before Start, then a tilted, heading-up driving camera with the car low on screen and a closer view near turns. Waypoint now uses that camera behavior while keeping the phone's selected world palette and its own map design. Google Maps and Waypoint use different route providers, so their roads, duration, and ETA can differ.
- On the connected Samsung phone and Desktop Head Unit, imported a `geo:` destination, verified its resolved place name and full route line in the car preview, then started guidance on the phone and watched it appear in the car. A simulated drive showed the route line, moving car marker, changing heading, speed, remaining distance, ETA, and left/right turn cues. Near-turn look-ahead is limited so the marker remains visible as the map zooms in. Switching the phone from Los Santos to Old West and back during guidance changed the car's map and route palette without ending the trip.
- The car map now renders through a live MapLibre surface rather than a repeated snapshot. Position and camera updates interpolate between guidance fixes, with native rendering capped at 60 fps. A SurfaceFlinger sample during an active simulated drive recorded 62 map frames, 61 consecutive frame intervals with a 16.79 ms median, 60 intervals between 15 and 18 ms, and no gap above 25 ms. This is a short Desktop Head Unit observation, not a guarantee for every car display or route.
- Android Auto's navigation template still receives route and maneuver data through the Car App Library, including native turn, fork, merge, ramp and roundabout types; the Desktop Head Unit did not display its standard instruction card in this setup, so Waypoint also draws a compact driving cue on the map. Route, destination, car marker, and cue were checked together in the head-unit view.
- Final debug and release assembly, Android test APK assembly, debug and release unit tests, debug and release lint, and both packaged-manifest checks passed. The installed final debug APK passed eight connected-device tests across car compatibility, navigation journey and security configuration. A physical vehicle, long-duration drive, and measured performance on production car hardware remain untested.

## Phone and dashboard regression pass (3 November 2026)

- On the connected Samsung, the complete final-build global scenario class passed `OK (5 tests)` in 261 seconds. Its 50-place fixtures covered 25 countries: all 50 map positions showed road features and a position marker, all 50 nearby searches returned a result, and all 50 driving routes progressed to arrival. Walking and motorcycle routing succeeded at all 16 additional urban/rural fixtures for each mode. Six actual phone navigation screens were captured and reviewed across Kolkata, Ziro, Tokyo, London, São Paulo and Dubai in six different world styles. The route line, maneuver, and trip controls remained visible.
- The phone's 2D/3D camera and Locate loops each passed in all 27 worlds on the revised build (`OK (2 tests)`). A stationary tilt toggle had revealed that the camera update condition ignored a tilt-only change; that condition now includes tilt. The synthetic Locate tests were isolated from live GPS to make their results deterministic. The route, onboarding, large-text, landscape, catalogue scroll, car-template and journey suites passed in later targeted runs, including `OK (10 tests)` and `OK (9 tests)` on the final installed debug build.
- The current onboarding was exercised from the opening globe through referral, favorite game, map selection and location choice, including 1.3× and 2× text with reduced motion. The navigation UI run verified a nearby route, world changes before and during guidance, 1.3× text, landscape, ending the trip and map information. Test fixtures now set up their own saved destination and welcome state instead of depending on a fresh install.
- The updater settings UI passed its final device check. The live GitHub endpoint returned a rate-limit response during this pass; Waypoint showed the specific error and a usable **Try again** action. This validates error presentation, not a successful download or installer handoff in this run.
- A jump between distant GPS locations exposed a stale preview route when the replacement request failed. The preview now clears its old line while requesting a route and leaves it clear after an error; active guidance keeps its current route during rerouting. A connected-device regression test covers the failure and recovery behavior. The crossing of the International Date Line is covered by car overview and look-ahead tests. Another test checks car camera overview bounds across seven northern/southern/date-line journeys and four display sizes.
- Android Auto Desktop Head Unit runs used a 60 fps configuration at 800 × 480, 1280 × 720, 1920 × 1080, and 1280 × 720 with top/bottom margins for a wide effective viewport. The checked-in `scripts/dhu-60fps.ini` reproduces the 720p configuration; change its resolution or marginheight for the other cases. Screenshots were inspected for route visibility, destination title, marker, cue, map controls and clipped/overlapping content. Day/night and driving restrictions were toggled on the 480p layout. A shared `geo:` destination was accepted on the car, reverse-resolved to **Hooghly Ghat**, routed, and started on the phone; the head unit showed its live route and maneuver changes during Android Auto's `AUTO_DRIVE` simulation. Car template tests also verified preview and active controls for all 27 worlds, an untrusted host rejection, shared-destination consent, GPS loss and roundabout exit cues.
- A SurfaceFlinger sample of the car map during an active 1080p simulated drive contained 54 frames: median interval **19.09 ms**, 95th percentile **20.88 ms**, one interval above 25 ms, and none above 33 ms. The MapLibre surface is capped at 60 fps, but this 1080p observation did **not** sustain 60 fps. The earlier short lower-resolution sample measured a 16.79 ms median. Neither result predicts every dashboard or certifies a constant frame rate.
- A navigation-only continuous-render trial at 1080p measured 18.90 ms median and 20.44 ms at the 95th percentile in a different world style. It still did not sustain 60 fps, and it rendered unchanged frames continuously, so the trial was reverted. The retained map uses render-on-change with interpolated position and camera updates.
- Final debug and release unit suites passed **124 tests each**, with zero failures; debug/release assembly, Android test APK assembly, lint, packaged-manifest security checks, repository hygiene and whitespace checks passed. Lint reported zero errors (57 debug and 51 release warnings). Device screenshots and latency samples are kept outside the repository.

This pass was stationary and used simulated trips and public coordinates. The 50 international journeys were exercised through production search/routing and the phone map, with synthetic position updates; the actual Desktop Head Unit drive was local. A temporary system GPS test provider did not reliably displace Google Play Services' fused fix in the car session, so it was removed after the failed international projection attempt. No physical vehicle, offline map/routing test, long-duration endurance run, production head-unit benchmark, or claim of exact Google Maps parity is included.

## 1.1.0 release preparation and README captures (3 November 2026)

- The updated source builds debug and optimized release APKs, builds the Android test APK, passes **126 debug and 126 release unit tests**, and passes debug/release Android lint. Packaged debug and release manifest boundary checks, version validation for `1.1.0` / `1001000`, repository hygiene, all **28** generated map styles, and `git diff --check` pass. The local release APK is intentionally unsigned; GitHub Actions owns distribution signing.
- The connected Samsung passed **11 targeted Android tests** across car compatibility, navigation journeys and security configuration on the installed debug navigation code. The final version metadata and updated release-prompt copy were compiled in the 1.1.0 build; the device test APK used the same navigation source before that version bump.
- The opt-in `CarReadmeCaptureTest` passed on the phone while connected to the Android Auto Desktop Head Unit. It requested a route between fixed public London coordinates, supplied a simulated guidance state, and projected Old West and Night City. The DHU's own `screenshot` command captured both 1280 × 720 displays. The opt-in `ReadmeScreenshotsTest` also passed and supplied six fixed-London phone map states. Status/navigation bars and image metadata were removed before the public posters were generated; source captures remain outside Git.
- The README now shows both dashboard captures in responsive posters and refreshed phone images. The SDK 36 workflow install now matches the app's compile SDK. Publication and the installed-app update prompt require the signed `v1.1.0` GitHub release; a push to `main` alone does not create an update.

## Published 1.1.0 and update discovery (3 November 2026)

- The [main verification run](https://github.com/the-geeky-couldron/waypoint/actions/runs/41531016389) and [tagged release run](https://github.com/the-geeky-couldron/waypoint/actions/runs/41531832476) both passed. The latter published the stable [v1.1.0 release](https://github.com/the-geeky-couldron/waypoint/releases/tag/v1.1.0) with `Waypoint.apk` and `SHA256SUMS`.
- A fresh download of the 1.1.0 APK passed `apksigner verify`; its SHA-256 matched both the checksum file and GitHub's asset digest (`ff3532806f9be490bdfb328f871d6035c10e1524d1516daed937ee55cc388e96`). The APK declares `com.waypoint.app`, version `1.1.0` / `1001000`, and the same signing certificate as the published 1.0.0 APK.
- The connected phone retained its installed 1.0.0 debug build and saved data. After the stable release appeared, reopening Waypoint showed the **Waypoint 1.1.0 is ready** popup with the published release notes and **Download update** action. No download or installer handoff was attempted on that debug-signed installation because its signer differs from the published release. The matching published release certificates and higher version code establish the signed in-place update prerequisites; final installation confirmation remains with Android and the user.

## Local issue fixes — 4 November 2026

Changes were implemented in issue creation order. The README includes the Repolode badge. The next-release draft, including reporter
credits, is in [releases/next.md](releases/next.md); the published 1.1.0 notes and version are unchanged.

| Issue | Local result | Evidence and remaining limit |
| --- | --- | --- |
| #1 — voice guidance | Fixed missing explicit `AndroidTtsObserver.start()`; added initialization/error states, English voice selection, retry and pending-cue cleanup | Six voice lifecycle unit tests pass. On a physical Android 17 phone, the installed engine initialized, accepted a synthetic navigation utterance and the tester confirmed hearing it. No real route was used. |
| #2 — Android Auto missing from launcher | Launcher setup and distribution requirements documented; manifest checks expanded; issue remains open | Packaged service discovery, descriptor, car API metadata and permissions pass. Installed Android Auto host signature validation and four simulated car-template tests pass. Both the installed release and isolated local test build appear in the Desktop Head Unit launcher and open their maps. Real-car launcher eligibility is **not resolved or verified**. |
| #3 — launcher visibility and silent guidance | Voice fix shared with #1; launcher finding shared with #2 | Same evidence and outstanding real-car distribution check. |
| #4 — selecting multiple favourite games | Toggle events read the latest saved selection, with checkmarks, count and announced states | Device regression selects/deselects several games, navigates Back, recreates the activity and completes onboarding; selected preferences and initial world are verified. Normal and 1.3× text layouts inspected. |
| #5 — UK spelling | Android default/UK resources for onboarding, world descriptions, recenter controls and car credits/units | Device resource tests verify UK and US variants. The physical onboarding screen displays “favourite” under the device's UK locale. Provider place names and game names are preserved. |
| #6 — missing Twitter option | Added Twitter / X and a scrolling referral list | Device onboarding test selects Twitter / X and continues. Layout inspected on the physical phone. |
| #7 — feature wishlist | Deferred at the maintainer's request | Separate task. |

Verification: 133 local JVM tests, debug APK/instrumentation compilation, Android lint,
version checks, packaged-manifest checks, repository hygiene and whitespace checks passed.
Seven focused device tests passed across `GuidanceVoiceDeviceTest`, `OnboardingIssuesTest`
and `CarCompatibilityTest`. The car tests require precise-location permission; without it
the app correctly returns a permission message instead of a navigation template. The
onboarding accessibility assertion refreshes cached nodes before reading their announced state.

Device testing used a separately installed `Waypoint Local Test` package. The existing
Waypoint installation and its stored data were not replaced. Captures and test logs remain
in ignored local directories; they are not public release artwork. Synthetic speech acceptance
and simulated car templates do not establish real-road timing, rerouting audio reliability,
Bluetooth audio behavior or real-head-unit launcher availability.

### Desktop Head Unit launcher check

Google's Desktop Head Unit 2.0 (macOS ARM64) connected to the physical Android 17 phone
through Android Auto's developer head-unit server and ADB port 5277. USB accessory mode
failed to attach, so the documented ADB connection was used. Both the installed release
(`com.waypoint.app`) and isolated local build (`com.waypoint.app.localtest`) appeared as
separate entries in the projected launcher. Opening the release rendered Liberty City;
opening the local build rendered Los Santos, and its map settings opened successfully.
Android Auto's projection process was bound to the respective car service. No installer
identity or host-validation policy was changed for this check.

This verifies launcher discovery and map presentation in the DHU on this phone. It does
not reproduce the reporter's real-car failure or establish real-car distribution eligibility.
Issue #2 remains open. The simulator was stopped and the ADB forwarding removed afterward.
