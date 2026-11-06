# Changelog

For the full notes and community acknowledgements, see [Waypoint release notes](docs/releases/current.md). Published releases are available on [GitHub Releases](https://github.com/the-geeky-couldron/waypoint/releases).

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
