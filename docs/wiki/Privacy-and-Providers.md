# Privacy and Providers

## On-device data

Waypoint has no app-owned account, analytics backend or trip-history service. Saved places, quests, notes, reminders, pictures, progress and settings stay in private device storage and are excluded from cloud backup/device transfer. Location history is not recorded. You can clear saved places and history in Settings. Uninstalling the app removes its private data.

Precise location is requested when you use the GPS control. Foreground location stops when the app is not visible unless active navigation is running; Android shows the foreground service notification. Waypoint does not request background-location permission.

## Network services

| Use | Provider |
| --- | --- |
| Vector tiles and map features | OpenFreeMap using OpenStreetMap-derived data |
| Search and reverse geocoding | Photon; Android Geocoder may be used when available |
| Car route geometry | OSRM public demo |
| Live traffic and traffic-adjusted ETA | Not configured; no traffic-service requests are made |
| Supported Google Maps link import / destination handoff | Google Maps, only when you choose either action |
| Motorcycle, bicycle and walking geometry | Valhalla public demo |
| Guidance and rerouting | Ferrostar consuming the selected route |
| Elevation / terrain data | Mapzen public service |
| Release check and optional APK download | GitHub Releases |

Tile providers receive requested map tiles. Search providers receive the search text and may receive location bias. OSRM or Valhalla receives route origin and destination coordinates with the selected profile. No traffic provider is configured, so Waypoint makes no live-traffic requests and sends no in-trip position to Google for traffic. User-initiated Google Maps link import and destination handoff remain available. GitHub release checks do not include location or saved-place data. An APK download starts only after you choose an update.

## Limits

Public map, search and routing endpoints may throttle requests or go offline. Live traffic is not available; the remaining ETA comes from Ferrostar using the selected OSRM or Valhalla route. Waypoint does not offer offline routing, lane guidance or guaranteed business coverage. Do not rely on ETA as a promise. For details, read the [service and privacy documentation](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/guide.md#services-and-limits).
