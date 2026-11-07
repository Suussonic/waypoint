# Third-party notices

Waypoint's application code is distributed under GPL-3.0-only; the full text is in [LICENSE](LICENSE). This file records components and material that the current project uses.

## Software and services

| Part of Waypoint | Source |
| --- | --- |
| Map rendering | MapLibre Native for Android |
| Navigation progress and rerouting | Ferrostar |
| Android UI and storage | AndroidX, Jetpack Compose and Room |
| Network client | OkHttp |
| Map tiles and labels | OpenFreeMap, using OpenMapTiles data derived from OpenStreetMap |
| Place search | Photon and the Android Geocoder when available |
| Routes | OSRM for car routes; Valhalla for motorcycle, bicycle and walking routes |
| Terrain | Mapzen terrain data |
| Release checks | GitHub Releases |

The build's direct dependency versions are in [app/build.gradle.kts](app/build.gradle.kts). Hosted services are accessed at runtime; their application code is not bundled in Waypoint. OpenStreetMap attribution is displayed in the app's Map info panel.

## Images, fonts and marks

The first onboarding Earth image adapts NASA Earth Observatory's [Blue Marble](https://science.nasa.gov/earth/earth-observatory/the-blue-marble-true-color-global-imagery-at-1km-resolution/) imagery. Service marks used for referral choices come from [Simple Icons](https://simpleicons.org/). Font licenses are collected in [docs/licenses](docs/licenses/) and [third_party/game_fonts](third_party/game_fonts/).

Waypoint's map styles, place-symbol atlases, destination markers and promotional compositions are documented in the [artwork notes](docs/game-artwork-sources.md) and [map icon notes](docs/poi-icon-sources.md). Some retained interface and onboarding images have separate origins.

### Cyberpunk 2077 map glyphs in Night City

Seven Cyberpunk 2077 map glyph PNGs are bundled in [third_party/cyberpunk_map_icons](third_party/cyberpunk_map_icons/) and used in the Night City POI atlas only for directly matching real-world place categories. Cyberpunk 2077 and these glyphs are CD PROJEKT RED artwork. The project maintainer has confirmed written permission covering use in the public Waypoint app and repository; the permission document is held privately. These game glyphs are separate from Waypoint's GPL-3.0-only code license. The [map icon notes](docs/poi-icon-sources.md#night-city) identify each source page, semantic mapping, and SHA-256 hash; [sources.json](third_party/cyberpunk_map_icons/sources.json) records the original download URLs and MediaWiki SHA-1 values. Waypoint is an unofficial fan work and is not approved or endorsed by CD PROJEKT RED.
