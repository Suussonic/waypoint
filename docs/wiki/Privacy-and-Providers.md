# Privacy and Providers

## On-device data

Waypoint has no app-owned account, analytics backend or trip-history service. Saved places, quests, notes, reminders, pictures, progress and settings stay in private device storage and are excluded from cloud backup/device transfer. Location history is not recorded. Open place names and basic details are stored locally only when you include them in a saved offline area; removing that area deletes its place snapshot. You can clear saved places and history in Settings. Uninstalling the app removes its private data.

Precise location is requested when you use the GPS control. Foreground location stops when the app is not visible unless active navigation is running; Android shows the foreground service notification. Waypoint does not request background-location permission.

## Network services

| Use | Provider |
| --- | --- |
| Vector tiles and map features | OpenFreeMap using OpenStreetMap-derived data |
| Search and reverse geocoding | Photon; Android Geocoder may be used when available |
| Vela open-place archive | Vela's public regional archive, containing Overture, AllThePlaces and OpenStreetMap records; fetched by HTTP byte range for map pins, Nearby, open-data place search, and offline-area downloads |
| Optional place photos | Wikimedia Commons geosearch, off by default; when enabled, it receives the displayed place's coordinates and the network address, and each shown image includes its source and license |
| Optional name suggestions and Google place details/photos | Google's undocumented Maps web endpoints, off by default; see below |
| Car route geometry | OSRM public demo |
| Live traffic and traffic-adjusted ETA | Not configured; no traffic-service requests are made |
| Supported Google Maps link import / destination handoff | Google Maps, only when you choose either action |
| Motorcycle, bicycle and walking geometry | Valhalla public demo |
| Guidance and rerouting | Ferrostar consuming the selected route |
| Posted speed limits (car guidance, optional) | Overpass API reading OpenStreetMap `maxspeed` tags |
| Bus and transit trips (only when opened) | Transitous (MOTIS) |
| Elevation / terrain data | Mapzen public service |
| Release check and optional APK download | GitHub Releases |

Tile providers receive requested map tiles. Search providers receive the search text and may receive location bias. OSRM or Valhalla receives route origin and destination coordinates with the selected profile. No traffic provider is configured, so Waypoint makes no live-traffic requests and sends no in-trip position to Google for traffic. User-initiated Google Maps link import and destination handoff remain available. GitHub release checks do not include location or saved-place data. An APK download starts only after you choose an update.

Open-place pins read the public Vela archive for the visible map center once the map is zoomed in. Nearby discovery and open-data search read the same archive around their selected center; search terms are matched on-device and are not sent to Vela. Place-name search checks all downloaded open-place snapshots locally first, even when the current location is outside those regions or unavailable. Exact matches in the user's saved and recent destinations also resolve locally. With Google suggestions off, the search field starts a normal place search after a 700 ms typing pause. On a local miss, Photon receives the search text and location bias while Vela searches archive names on-device and requests nearby tile ranges; GitHub does not receive the query text. Android Geocoder may also receive the query and region bias if matches are weak. Each archive operation has a 64 MiB aggregate PMTiles read limit; saving an area downloads only ranges for the selected rectangle and creates a bounded, on-device place snapshot. Archive requests go to GitHub, which receives the network address and byte ranges; those ranges can reveal which tiles were viewed, searched or saved, but the archive host does not receive a latitude/longitude or search-text query. Pins can be disabled in Settings; this hides pins but leaves open-data search available. Nearby lookups covered by a saved snapshot run locally. Removing the offline area deletes that snapshot. Google live suggestions require an internet connection and are not copied into downloaded offline place packs. **Wikimedia Commons photo searches are off by default.** When enabled, photo searches for visible places send each place's coordinates to Commons; Commons also receives the network address. Waypoint loads a matching thumbnail only when it can verify a close coordinate and name match. The Commons item's author and license are shown with the image and link to its source page. Offline snapshots do not include photos or Google business details.

**Google web search, details and photos are off by default.** When enabled, Waypoint sends partial search text and the current device location as a suggestion bias to Google's Maps web autocomplete after a 320 ms typing pause. If Google returns no suggestions, the normal search provider may also receive the query as described above. Opening a nearby place can send that place's name and coordinates for listing details and photos. Google also receives the network address used for each request. Detail and photo requests are limited to a place the user opened; autocomplete runs after typing pauses. Waypoint uses undocumented web endpoints and a hidden WebView, not Google's supported Places API. There is no Places API key or Places API SKU billing, but this is not a supported free Places API: Google may change or block it, and Google's published terms restrict automated scraping. The OpenStreetMap-backed search remains available with Google web lookups off.

While Speed limit warning is on and you guide by car, Overpass receives your current position about once a minute; turn the setting off to stop it. Transitous receives your position and the destination only when you open **Bus & transit**. The Discord invite opens in your browser or the Discord app and sends nothing from Waypoint.

## Limits

Public map, search, image and routing endpoints may throttle requests or go offline. The Google web integration is undocumented, can stop working without notice, and is subject to Google's terms. Live traffic is not available; the remaining ETA comes from Ferrostar using the selected OSRM or Valhalla route. Waypoint does not offer offline routing, lane guidance or guaranteed business coverage. Do not rely on ETA as a promise. For details, read the [service and privacy documentation](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/guide.md#services-and-limits).
