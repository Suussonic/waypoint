# Waypoint

Native Android navigation with game-inspired maps. Kotlin + Jetpack Compose +
MapLibre Native. **Twenty-seven game-inspired map styles, no ads, accounts, purchases or feature
locks.** Source code is MIT licensed.

Waypoint is an independent, open-source implementation. Its visual direction and
implementation decisions are recorded in [the sourcebook](visual-sourcebook.md).
It is not affiliated with game publishers.

## Use

Install `Waypoint.apk` from the [latest GitHub release](https://github.com/the-geeky-couldron/waypoint/releases/latest) on Android 8.0 or newer, or build the debug APK below. Local APK copies under `dist/` are ignored and are not part of the source repository.
Open Waypoint, choose a world on the welcome screen, then tap **Open map**. Tap **Locate**, and allow **precise location**. Search for
a destination or hold the map to drop a pin. Choose Car, Motorcycle, Bicycle or Walk. Once a route is ready, tap
**Start navigation**. The app provides live position, next turns, ETA, remaining
distance, speed, spoken instructions, off-route recalculation, and arrival.

- **Worlds:** 38 game-inspired styles, with search, Favourites and New filters. Preview before applying. All included.
- **Saved:** bookmark a destination; navigate to it again later.
- **Settings:** voice, metric/imperial, 2D/3D, updates, permissions,
  and clearing saved places/history.
- The map follows live GPS while browsing until you pan or select a destination. During navigation, a pan pauses following for eight seconds, then it resumes automatically. Tap Locate to recenter immediately. The location pointer stays centred during position animation.
- Navigation continues through a location foreground service when the screen is
  off. Stop it in the app or from its ongoing Android notification.
- Standard `geo:` and `google.navigation:` destination links are supported. Waypoint asks before using your location to look up a shared place or preview its route.

## Android Auto

### Enable Waypoint in the car launcher

1. On your phone, open **Settings** and search for **Android Auto**.
2. Open **Android Auto → Customize launcher**.
3. If **Waypoint** is listed, tick its checkbox. Reconnect the phone to the car,
   open the car's app launcher and select **Waypoint**.
4. Allow precise location in Waypoint on the phone when prompted so it can navigate.

If Waypoint is absent from **Customize launcher**, there is no checkbox to enable.
Read the installation requirement below; location permission and **Unknown sources**
do not make an ineligible installation appear in that list.

### If Waypoint is missing from the launcher

Waypoint uses the Android for Cars App Library. A GitHub-sideloaded APK can work on the
phone yet be absent from both the car launcher and **Customize launcher**. Android's
**Unknown sources** developer option does not apply to Car App Library apps, so enabling
it, reinstalling the same APK, or granting GPS permission does not establish eligibility.

For real-vehicle testing, Google documents installation through a trusted source, including
Google Play Internal App Sharing or an Internal Test Track. Waypoint currently distributes
its public APK through GitHub; a Play testing channel must be set up by the maintainer before
that path is available. See [Android's real-vehicle testing requirements](https://developer.android.com/training/cars/testing#real-vehicles)
and the [maintainer testing checklist](releasing.md#android-auto-distribution-and-testing).
The Desktop Head Unit remains the local development test path. A successful DHU test does
not prove that a sideloaded release will be eligible in a real car. Host signature validation
must remain enabled; changing it cannot make the launcher accept a distribution source.

### Once Waypoint is visible

Connect your phone to Android Auto, then open **Waypoint** from the car launcher. The car map
follows the world selected on the phone. Search for a destination, choose a saved or recent
place, preview the route, then start guidance. The map supports touch or rotary panning,
recenter and zoom controls. Settings offers worlds, voice, units and map licences. Voice and distance units use
native switches and reflect changes made on the phone. The world chooser groups all 38
worlds into six short lists; choosing a world returns to the map without ending guidance.
Saved and recent destinations are separate, host-sized lists. Empty destinations offer search.

If location permission is missing, the car offers **Allow location** while parked and
**Check again** after changing phone permissions. The phone’s settings sheet shows the
current connection and permission state, with an **Android Auto settings** shortcut.
Opening a car menu keeps the car session connected; disconnecting the head unit leaves
phone guidance running. Incoming destinations return from car menus to their confirmation.
The car map uses a live native MapLibre view capped at 60 frames per second. Position and
camera movement interpolate between location fixes, while route and destination layers
update directly as navigation progresses.
Google Maps stays available separately in the launcher.

Android Auto uses the last navigation app the driver opened as its default for Assistant and
other navigation handoffs. Open Waypoint in the car to make it the current default; installing
it alone cannot override the driver's choice. Waypoint accepts `geo:` navigation intents
through a confirmation screen. It does not replace an active destination without consent.
The host's Auto Drive test command simulates movement along a selected route.

## Voice guidance

Waypoint explicitly starts the Android speech engine and waits for initialization. A short
startup delay is not treated as missing voice data. Guidance currently speaks English and
prefers the device's English dialect when available, with another installed English voice
as a fallback. Check the preferred engine and voice in Android's text-to-speech settings
if initialization fails. After changing those settings, turn voice off and on in Waypoint
to retry, and check navigation/media volume if speech is still inaudible. Visual guidance
continues while speech is unavailable.

**Game-style voice** in Settings adds short original phrases and subtle pitch and pace
changes for the selected world. All 38 worlds have three starting, three travelling and
three arrival phrases that rotate during the app session. The distance, turn and road name always come first. Extra
phrases are spaced out and omitted near imminent turns; they never complete quests.
Switch it off for standard instructions and your Android speech settings. This preference
also applies to Android Auto. Voice character depends on the installed speech engine;
Waypoint does not clone game actors or download voice recordings.

## Build

Requires JDK 17, Android SDK Platform 36 and Build Tools 35.0.0. Open the project
in Android Studio or point `local.properties` to your SDK:

```properties
sdk.dir=/absolute/path/to/android-sdk
```

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The downloadable `Waypoint.apk` is the optimized GitHub release build. Rebuild an installable release with
`./gradlew :app:assembleRelease` after setting the four signing variables in [Releasing](releasing.md). Connected UI tests can target that build with
`./gradlew -PtestBuildType=release :app:connectedReleaseAndroidTest` on an unlocked
development phone; the navigation flow expects a saved nearby destination.

The Gradle wrapper is included. API keys are not required. Local debug builds use your Android debug key; local release builds without signing variables are unsigned. GitHub releases use the stable signing identity
from encrypted repository secrets; see [Releasing](releasing.md).
No private signing material is included in the source.

## Services and limits

- Map tiles/glyphs/sprites: [OpenFreeMap](https://openfreemap.org/), derived from
  [OpenStreetMap](https://www.openstreetmap.org/copyright).
- Search and reverse geocoding: [Photon public demo](https://github.com/komoot/photon). Search also uses [Android Geocoder](https://developer.android.com/reference/android/location/Geocoder) when available and Photon lacks a strong match, or when you explicitly submit a search. The device selects that backend; its data is not necessarily open source.
- Driving routes and steps: [OSRM public demo](https://project-osrm.org/).
- Motorcycle and walking routes: Valhalla public demo.
- Guidance progress, maneuvers, rerouting and spoken cue selection: [Ferrostar](https://stadiamaps.github.io/ferrostar/) 0.59.0. Ferrostar consumes routes from the providers above; it does not provide the basemap, search or routing server.
- Voice: Android's installed text-to-speech engine.

The public endpoints are suitable for evaluation and can be throttled or unavailable.
For a public release, self-host or contract services with suitable capacity and
availability; the endpoints are isolated in `MapRepository.kt` and the style JSON.
Routes do not include live traffic, closures, speed-limit enforcement,
lane guidance, or offline routing. GPS accuracy and map coverage vary. ETA is
estimated from route progress. Road testing is still required. The app is a working
prototype, not a claim of complete feature parity or production navigation quality.

## Search matching

Search accepts typos, accent differences and joined place names, and ranks matches from saved/recent places alongside provider results. Press **Search places** or the keyboard Search key for a broader device-geocoder lookup. Search results retain the returned name, full address and actual provider coordinates. Optional confirmed spelling aliases are read from private device preferences; no user-specific place aliases are bundled in the public app. Android sometimes returns an address code instead of a landmark label; these results explicitly say **Address result for …** and retain the complete provider address. Confirm the address before starting.

No provider indexes every business, informal place name or entrance. Google Maps maintains a different places database. Neither an open-source wrapper nor this fallback guarantees identical coverage. Nominatim and Overpass were investigated but not added as autocomplete backends: the former forbids client-side autocomplete on its public endpoint, and the latter advises apps to operate their own backend.

## Sensor-assisted navigation

High-accuracy fused location is preferred, with native Android GPS/network fallback.
The phone’s rotation vector provides compass heading at rest; usable GPS course takes
over in motion. Sensor calibration, accuracy and satellite status appear in Settings.
See [sensor behavior and limits](sensors.md).

## Location and privacy

Location permission is requested when you tap the GPS control. Precise location is
needed for navigation. Foreground GPS stops when the app is not visible unless
navigation is active; the navigation service is visible in Android notifications.
No background-location permission, account, analytics or app-owned server exists.
The optional fused-location dependency uses Google Play services and its device
location settings. Native GNSS/network fallback supports devices without it.

Map providers receive the tiles requested. Photon and, when used, the Android geocoder backend receive search terms and location
bias; OSRM or Valhalla receives route origin/destination coordinates and the selected routing profile. Saved/recent places and
settings remain in device preferences, excluded from cloud backup/device transfer.
You can clear those places in Settings. Location history is not recorded. The small search-result cache lives only in memory and is bounded to 40 queries. Confirmed spelling aliases change the search text; they never supply invented destination coordinates.

Quests, notes, reminders, progress and resized character photos stay in private device storage, excluded from cloud backup and device transfer. The system photo picker grants access only to the selected image. GitHub receives ordinary release-check requests, including scheduled background checks; these requests contain no quest content, saved places or location coordinates. APK downloads happen only when you choose to update.

## Architecture

- `MainActivity.kt`: native Material 3 screens, permissions and preferences.
- `GameAtlasHud.kt`, the fantasy/modern/issue atlases, `StalkerAtlas.kt` and `NierAtlas.kt`: 38 distinct native atlas compositions.
- `GameHud.kt`: panel shapes, typography, library and shared route guidance.
- `Cartography.kt`: per-world camera rules, player pointers and original POI icons.
- `HeadingSensor.kt`: sensor projection, display remapping and true-north correction.
- `HeadingArbiter.kt`: confidence, freshness, hysteresis and compass/course selection.
- `WorldMap.kt`: MapLibre lifecycle, themed vector map, markers and camera following.
- `NavigationController.kt`: GPS, async search/routing, persisted places, voice and state.
- `SavedPlaces.kt`: personal saved-place names and their native editor.
- `AdaptiveRoutePreview.kt`: scrollable route details for large text and short windows.
- `QuestRepository.kt` and `QuestDatabase.kt`: local tasks, focus sessions and transactional rewards.
- `QuestReminders.kt` and `QuestImages.kt`: Android alerts and private resized portraits.
- `NavigationSharing.kt`: confirmed incoming places and outgoing map-app handoff.
- `ReleaseCheck.kt`, `UpdateAlerts.kt` and `UpdateCheckJob.kt`: bounded GitHub release checks and default scheduled notifications.
- `FerrostarGuidance.kt`: Ferrostar navigation session and Android Auto test-drive simulation.
- `NavigationEngine.kt`: retained pure route projection utilities and historical test fixtures.
- `NavigationService.kt`: visible foreground service and background guidance lifecycle.
- `WaypointCarService.kt`, `WaypointCarMenus.kt`: projected Android Auto templates and controls.
- `WaypointCarLiveMap.kt`, `WaypointCarOverlay.kt`: live themed car map, route, markers and HUD.
- `MapRepository.kt`: HTTPS search, reverse geocoding, OSRM car routes and Valhalla motorcycle/bicycle/walking routes.
- `assets/styles/`: generated vector styles, with per-world road hierarchy, building treatment and relief.
- `scripts/cartography_profiles.py`: explicit road, casing, label and building rules.

Tests cover heading wraparound/smoothing, segment projection, route-distance maneuver sequencing, GPS jitter,
off-route fixes, route crossings, destination loops, arrival, units and instructions.
See `docs/validation.md` for actual verification and outstanding hardware checks.

## Licenses

Waypoint code: MIT (`LICENSE`). Google Play services location: Google SDK terms
(the library is a dependency; it is not relicensed as MIT). MapLibre: BSD-2-Clause. OpenStreetMap data: ODbL.
OpenFreeMap Liberty derives from OSM Liberty (BSD-3-Clause) and OpenMapTiles
(CC-BY 4.0 style/schema attribution). Barlow Condensed, Rye and VT323 fonts: SIL OFL,
Cinzel, Monocraft, Rajdhani, Share Tech Mono, Roboto Condensed, Libre Baskerville, Noto Sans and Orbitron are SIL OFL; Yellowtail is Apache-2.0,
with license files in `docs/licenses`. Theme names identify inspiration; no game
art, logos, textures or voices are included. Licensed game-font families and their source notices are documented in `docs/map-fonts.md` and `third_party/game_fonts/`.

## Transport modes

Choose **Car**, **Motorcycle**, **Bicycle** or **Walk** in search or route preview. Switching mode
recalculates the route and ETA before starting. Car uses OSRM; motorcycle, bicycle and walking
use the FOSSGIS Valhalla public server with dedicated `motorcycle`, `bicycle` and `pedestrian`
costing profiles and OSRM-compatible output. There is no silent car-route substitution.
The mode is saved for the next launch. `TravelMode.kt` is the extension point for
additional supported transport profiles. Valhalla requests are spaced by at least
1.1 seconds and use an identifying client header; use remains subject to the public server’s capacity and usage policy.

## Expanded library

38 themes: Los Santos, Old West, Night City, Pixel World, Green Terminal, San Andreas,
Midnight Run, The Continent, Vice Coast, Liberty City, Nudle Maps, Sheikah Slate,
Skyrim, Lands Between, Horizon, Animus, Hope County, Tsushima, Bridges, The Embrace,
Manhattan, Highlands, Surface Map, Bohemia, Steelport, Medici, Lost Heaven, Dark Zone,
Sea Chart, DOS Atlas, Aincrad, Tactical Radar, Vector Command, Field Ops, 16-bit Quest,
Home Console, Exclusion Zone and Machine Atlas. Search by game name, use Favourites or New, then preview before applying.
Fantasy/terrain themes use Mapzen Terrain Tiles for actual geographic relief,
with [provider credits](https://github.com/tilezen/joerd/blob/master/docs/attribution.md).
These recreate map/HUD design language; they are not pixel-perfect game asset ports.

## Regenerating and validating map assets

```sh
python3 scripts/generate_styles.py
npm ci --prefix tools/style-validation
node scripts/validate_styles.cjs
node scripts/generate_glyphs.cjs
# Optional: regenerate original textures/sprites (requires Pillow)
python3 scripts/generate_materials.py
```

Road camera expressions must stay top-level in MapLibre. The generator scales their
output stops; wrapping the entire zoom expression in arithmetic makes native roads
disappear. A theme load also has a monotonic generation so its camera and sources
are reapplied, while pointer animation is cancelled before its native layers are replaced.

The reference screenshots are research evidence only and are excluded from both the
APK and source archive. Original runtime textures, fonts with licenses, authored map
symbols and generated styles are included. No generated binary is needed to build from source.

The Worlds library is reachable from every atlas, route preview and active navigation. Changing worlds preserves the current destination, transport and route. Native road labels use bundled glyphs; see `docs/map-fonts.md` for font provenance and fidelity limits. The small map-info control opens provider credits without a long footer over the map.

## Interface performance

The welcome screen uses a short native reveal and a world-preview crossfade. Lists use stable item keys and cached, batched map thumbnail paths. MapLibre rendering, pointer animations and native camera/source work pause while search, the world library, settings or route details cover the map. Navigation tracking continues independently. The system animation scale is honored by Compose and Android animators.

## Quests and personal progress

Quests is a main destination beside Map, Worlds and Saved. Start with a title, then add details only when useful. See the [quest guide](quests.md) for tasks, timers, reminders, rewards, photos and map sharing. Quests and progression are local, with no account or cloud synchronization.

## Name your saved places

Open **Saved**, tap the three-dot options beside a place, then **Rename**. Enter a name such as Work and tap **Save name**. This changes the label, keeping the address and location. The name is stored on this phone and remains after closing the app or installing a compatible update.

## Ride with Bicycle mode

Find or select a destination, choose **Bicycle**, then review the route and start navigation. Bicycle uses Valhalla’s cycling profile and estimate. Roads and access depend on available map data; no live traffic is included. **Open in another map app** passes cycling mode through a supported Google Maps URL; the other app calculates its own route.

## New-version notifications

Waypoint checks GitHub releases in the background by default. When asked, allow notifications to receive an alert. The battery explanation describes why restricted background access may delay checks and opens Android’s battery settings for your decision. Neither permission is required to keep using Waypoint. The app schedules checks about twice daily, but Android can postpone them. Alerts open update details; downloads and installation always require your action. You can also use **Settings → App updates**.
