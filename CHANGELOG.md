# Changelog

For the full notes and community acknowledgements, see [Waypoint release notes](docs/releases/current.md). Published releases are available on [GitHub Releases](https://github.com/the-geeky-couldron/waypoint/releases).

## 2.6.0

- Added a speed-limit warning for car guidance on the phone. The HUD shows the posted limit from OpenStreetMap `maxspeed` data, turns your speed red when you pass it by more than 3 km/h or mph, and says the limit once through the guidance voice with a one-minute gap between alerts. It can be switched off in Settings.
- Added a first version of bus and transit directions ([#51](https://github.com/the-geeky-couldron/waypoint/issues/51)). A destination's **Bus & transit** button lists the next trips with the line, direction, boarding stop, stops, walking time and live times where the operator supplies them, using Transitous open timetable data.
- Added the Waypoint Discord invite to Settings and to the What's New window.
- The website's day and night section now compares real map captures of the same place with a slider and a world switcher, and the site gained a Discord section and the new Upstar Riser of the Week badge.
- Documented the Overpass and Transitous requests in the privacy and services tables.

## 2.5.4

- Reviewed all 38 worlds' day and night place icon atlases and repaired damaged cells, including Home Console's split icons. Recorded reproducible crop mappings and added contact-sheet review and CI validation with Pillow 13.0.0.
- Restored direct-match Cyberpunk 2077 symbols in Night City and a consistent player marker across travel modes on phone and Android Auto.
- Restored Machine Atlas's cream palette, charcoal text, rust accent, and monochrome symbols at every hour; aligned its phone controls with the other worlds.
- Added a camera-aware phone scale bar, persisted Compact/Standard/Large floating-control sizes and Left/Right placement, and scrolling for truncated world names.
- Improved compact landscape route previews, guidance scale-bar clearance, and map-control and map-key spacing across all worlds.

## 2.5.3

- Removed the fixed 150 MiB client-side APK download cap for users who install this version. Downloads still require enough free space and must match the release's declared size, SHA-256 digest, package, version, and signing certificate.
- Retained the 150 MiB signed-release publishing check so users of earlier Waypoint versions can update directly.
- Updated MapLibre to 13.7.1 using its OpenGL ES build for broad device compatibility, along with Gradle 9.8.2, TypeScript 6.0.3, and the JSON test dependency.

## 2.5.2

- Updated the Android Gradle plugin, Gradle wrapper, Kotlin and KSP tooling, Compose, Google Play services, OkHttp, MockWebServer, and other compatible Android dependencies from the reviewed Dependabot PRs.
- Updated pinned GitHub Actions and the motion source toolchain; adapted Android builds and Compose code to the new tooling.
- Kept the release APK within the 150 MiB download limit enforced by older installed updaters.

## 2.5.1

- Reduced the APK download size so older installed versions can update through Waypoint again, including versions that showed "The update has an invalid download size."
- Kept all 38 worlds and the features introduced in 2.5.0.
- Added a release build check against the 150 MiB limit built into earlier updaters.

## 2.5.0

- Reworked the map artwork across Waypoint’s 38 worlds with generated, Waypoint-specific materials, symbols and markers. The terrestrial map still uses real streets, places and routes; MapLibre remains the renderer, while its stock preset styles are not used as the source of Waypoint’s artwork.
- Improved road-following camera behavior, recentering and Android Auto route choices so the vehicle follows the selected road route through turns and alternatives.
- Refreshed the release screenshots and reorganized the map-asset and development documentation.
- Added a merge-to-main release flow that verifies the successful signed Android build, then publishes the APK, checksum, changelog and GitHub release page behind the existing release approval gate.

Earlier releases: [2.0.1](docs/releases/2.0.1.md), [2.0.0](docs/releases/2.0.0.md), [1.5.0](docs/releases/1.5.0.md).
