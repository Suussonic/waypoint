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
a destination or hold the map to drop a pin. Choose Car, Motorcycle or Walk. Once a route is ready, tap
**Start navigation**. The app provides live position, next turns, ETA, remaining
distance, speed, spoken instructions, off-route recalculation, and arrival.

- **Worlds:** 27 game-inspired styles, searchable by game name. All included.
- **Saved:** bookmark a destination; navigate to it again later.
- **Settings:** voice, metric/imperial, 2D/3D, updates, permissions,
  and clearing saved places/history.
- The map follows live GPS while browsing until you pan or select a destination. During navigation, a pan pauses following for eight seconds, then it resumes automatically. Tap Locate to recenter immediately. The location pointer stays centred during position animation.
- Navigation continues through a location foreground service when the screen is
  off. Stop it in the app or from its ongoing Android notification.
- Standard `geo:` and `google.navigation:` destination links are supported. Waypoint asks before using your location to look up a shared place or preview its route.

## Android Auto

Connect your phone to Android Auto, then open **Waypoint** from the car launcher. The car map
follows the world selected on the phone. Search for a destination, choose a saved or recent
place, preview the route, then start guidance. The map supports touch or rotary panning,
recenter and zoom controls. Settings offers worlds, voice, units and map licences.
The car map uses a live native MapLibre view capped at 60 frames per second. Position and
camera movement interpolate between location fixes, while route and destination layers
update directly as navigation progresses.
Google Maps stays available separately in the launcher.

Android Auto uses the last navigation app the driver opened as its default for Assistant and
other navigation handoffs. Open Waypoint in the car to make it the current default; installing
it alone cannot override the driver's choice. Waypoint accepts `geo:` navigation intents
through a confirmation screen. It does not replace an active destination without consent.
The host's Auto Drive test command simulates movement along a selected route.

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

## Architecture

- `MainActivity.kt`: native Material 3 screens, permissions and preferences.
- `GameAtlasHud.kt`, `FantasyAtlases.kt`, `ModernAtlases.kt`: 27 distinct native atlas compositions.
- `GameHud.kt`: panel shapes, typography, library and shared route guidance.
- `Cartography.kt`: per-world camera rules, player pointers and original POI icons.
- `HeadingSensor.kt`: sensor projection, display remapping and true-north correction.
- `HeadingArbiter.kt`: confidence, freshness, hysteresis and compass/course selection.
- `WorldMap.kt`: MapLibre lifecycle, themed vector map, markers and camera following.
- `NavigationController.kt`: GPS, async search/routing, persisted places, voice and state.
- `FerrostarGuidance.kt`: Ferrostar navigation session and Android Auto test-drive simulation.
- `NavigationEngine.kt`: retained pure route projection utilities and historical test fixtures.
- `NavigationService.kt`: visible foreground service and background guidance lifecycle.
- `WaypointCarService.kt`, `WaypointCarMenus.kt`: projected Android Auto templates and controls.
- `WaypointCarLiveMap.kt`, `WaypointCarOverlay.kt`: live themed car map, route, markers and HUD.
- `MapRepository.kt`: HTTPS search, reverse geocoding and OSRM routing.
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
art, logos, textures, voices, or proprietary fonts are included.

## Transport modes

Choose **Car**, **Motorcycle**, or **Walk** in search or route preview. Switching mode
recalculates the route and ETA before starting. Car uses OSRM; motorcycle and walking
use the FOSSGIS Valhalla public server with dedicated `motorcycle` and `pedestrian`
costing profiles and GeoJSON/OSRM output. There is no silent car-route substitution.
The mode is saved for the next launch. `TravelMode.kt` is the extension point for
additional supported transport profiles. Valhalla requests are spaced by at least
1.1 seconds and use an identifying client header; use remains subject to the public server’s capacity and usage policy.

## Expanded library

27 themes: Los Santos, Old West, Night City, Pixel World, Green Terminal, San Andreas,
Midnight Run, The Continent, Vice Coast, Liberty City, Nudle Maps, Sheikah Slate,
Skyrim, Lands Between, Horizon, Animus, Hope County, Tsushima, Bridges, The Embrace,
Manhattan, Highlands, Surface Map, Bohemia, Steelport, Medici and Lost Heaven. Search the Worlds library by game name.
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
