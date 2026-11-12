# Waypoint 2.6.5

This release steadies navigation on the phone and in Android Auto, brings offline place names into downloaded regions, and makes place discovery and map rendering more complete.

## Navigation that holds its line

- The camera now settles when the vehicle is stationary, filters noisy heading changes, and stops the world from jittering as you move the phone or end guidance.
- Turn guidance uses a steadier course and road-position estimate so the vehicle marker stays with the selected route through bends and roundabouts.
- Navigation camera framing is tuned for closer road context, smoother turns and a clearer forward view. The start-navigation action transitions into guidance with less visual interruption.
- The map scale indicator has moved away from the center of the route.

## Places that work offline

- Downloaded map regions can include nearby open-place records. Their place names remain available for local search and nearby discovery without a network connection, including when you are elsewhere.
- Search can offer optional Google Maps web suggestions while you type. Opening a nearby place can load Google business details and photos, with open-data results and Commons imagery available as alternatives.
- Google web lookups are experimental and off by default. They use undocumented Maps endpoints rather than the supported Places API, have no Places API key or Places API billing, and may change or stop working. Google's published terms restrict automated scraping. Offline place snapshots contain open-data records, not Google suggestions, details or photos.

## More readable worlds

- Liberty City regains its landmark blips and place references, with repaired icon assets.
- Full zoom-out rendering now preserves map content across the worlds instead of leaving large areas incomplete.
- In Night City, 3D buildings and the highlighted route now use a clearer draw order, so routes remain legible around raised geometry.
- World styles, map symbols, and day/night artwork receive another consistency pass.

## Android Auto and the updater

- Android Auto now holds a more useful forward camera direction, tilt and zoom while stationary and while moving.
- The car map adapts its viewport and guidance layout when Android Auto shares screen space with another app.
- The updater keeps its download progress visible while the release details scroll, and its loading and download states have clearer feedback.

## A longer route through CI

The Android Actions workflow now runs on pull requests and pushes to both `dev` and `main`. Separate gates validate workflow security, dependency changes, the release page, map styles and assets, motion source, debug and release builds, JVM tests, lint and emulator coverage. Main builds sign the APK and attach GitHub build-provenance attestation before the release workflow can publish it.

## Credits for the route

Thank you to the people who reported these issues and helped shape the fixes.

**GitHub**

- **[@TheNoob1703](https://github.com/TheNoob1703)** — map rotation and compass behavior in [#54](https://github.com/the-geeky-couldron/waypoint/issues/54).
- **[@harrydbarnes](https://github.com/harrydbarnes)** — the updater's download progress in [#63](https://github.com/the-geeky-couldron/waypoint/issues/63).
- **[@Balzamus](https://github.com/Balzamus)** — the Cyberpunk map's 3D presentation in [#55](https://github.com/the-geeky-couldron/waypoint/issues/55).
- **[@lakshvin](https://github.com/lakshvin)** — richer Google place discovery in [#21](https://github.com/the-geeky-couldron/waypoint/issues/21).

**Discord**

- **@ForgotMyИame_124** — Liberty City landmarks and blips in ticket-0002.
- **@Aleksandyr** — compass behavior in ticket-0003.
- **@shaurya124** — Android Auto split-screen sizing in ticket-0003.
- **@Nomad** — map rotation through turns and slowdowns in ticket-0004.

Discord ticket channels are private, so their names are credited here without exposing ticket links.

Downloaded regions keep their map data and open-place snapshots available offline. Route calculation and live Google suggestions, details and photos still need an internet connection. Compatible updates preserve saved places, quests and settings. Android Auto availability in a real vehicle still depends on Google's trusted installation requirements.
