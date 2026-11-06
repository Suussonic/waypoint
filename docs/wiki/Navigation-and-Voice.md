# Navigation and Voice

## Routing and guidance

Waypoint requests route geometry from public routing endpoints and hands the route to Ferrostar for guidance. Ferrostar tracks progress, selects spoken maneuver instructions and supports off-route recalculation. The app shows the route, next turn, remaining distance, speed when available and an estimated arrival time. Routing quality depends on provider coverage and connectivity.

Current profiles are OSRM for driving and Valhalla for motorcycle, bicycle and walking. No live-traffic provider is configured. Ferrostar owns route progress, rerouting and remaining ETA using the selected routing response. Lane-level guidance and offline route data are not included. See [[Privacy and Providers]].

The theme changes map presentation only. Switching worlds during a route keeps the destination and guidance session. Navigation remains available without quests or rewards.

## Voice guidance

The default spoken voice comes from the Android text-to-speech engine and voice selected in device settings. On the four recorded-pack worlds, exact recorded prompts can play while TTS starts or is unavailable; uncovered instructions remain visible and use TTS when it is ready.

**World voice style** gives each of the 38 worlds its own bounded pitch/rate profile and original introductory phrases. Skyrim, Forza Horizon, the DOS Atlas and Liberty City also have optional recorded Waze-style prompt sets for maneuvers with exact clip matches. GLaDOS is used as a computer-like voice for the DOS Atlas theme, not as a DOS game character. The other worlds keep their distinct Android TTS profiles unless a compatible, licensed recorded pack is added.

Recorded packs contain only common maneuver and distance prompts. Waypoint uses a recording only when it can preserve the complete route instruction; named roads, unsupported wording or distances fall back to Android TTS with the original instruction intact. The recording player does not own route selection, timing, rerouting or ETA; Ferrostar remains in charge of those. The packs currently provide English clips. See [[Sources and Licensing]] for their sources and license notes.

## Route safety

Use the spoken turn, route line and real-world signs together. Estimated arrival, location accuracy and place coverage can vary. Public demo routing servers can be throttled or unavailable, and no live-traffic data is included.
