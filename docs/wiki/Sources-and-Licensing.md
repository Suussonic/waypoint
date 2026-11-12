# Sources and Licensing

## Waypoint

Waypoint application code is GPL-3.0-only. Fonts, map data, imagery, recordings and other third-party material can have separate licenses; keep each notice with its asset. The repository records font and map-data terms under `docs/licenses/`, icon provenance in `docs/poi-icon-sources.md`, and research references in `docs/visual-sourcebook.md`.

No live-traffic provider is configured; Ferrostar remains authoritative for navigation progress, rerouting and remaining ETA. See [`THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md) for the components and material used by Waypoint.

The map HUD artwork, destination markers, materials and most world-specific POI icons are authored for Waypoint. Night City's POI atlas also includes seven Cyberpunk 2077 game glyphs for directly matching place categories. Their source URLs, hashes and the maintainer's confirmation of written CD PROJEKT RED permission for public app and repository use are in [`docs/poi-icon-sources.md`](../poi-icon-sources.md#night-city); the grant itself is held privately. Game screenshots are not bundled. Waypoint is independent and not affiliated with the named publishers.

## Map data

OpenMapFree/OpenStreetMap-derived map data is subject to its provider terms and OpenStreetMap attribution requirements. See the current [map service guide](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/guide.md#services-and-limits) and bundled notices before redistributing map material.

## Nearby places and photos

Waypoint adapts the PMTiles reader and regional place-archive search from [PimpinPumpkin/Vela](https://github.com/PimpinPumpkin/Vela), whose source is GPL-3.0-or-later. The adapted files are `app/src/main/java/com/waypoint/app/places/PmtilesReader.kt` and `VelaNearbyPlaces.kt`; the Google web request builder and autocomplete decoder in `VelaSearchPb.kt`, `VelaMapSuggestions.kt` and `GooglePlaceLookup.kt` also follow Vela source. Waypoint remains GPL-3.0-only, and the full Vela license is included in its repository.

Vela's regional place archive combines separate datasets: Overture Maps under CDLA-Permissive-2.0, AllThePlaces under CC0, and OpenStreetMap under ODbL. The archive is streamed from Vela's public GitHub release and the displayed place record identifies its dataset origin. Waypoint can show open-place pins while browsing the map, find places through on-device-ranked archive search, and read nearby places from the same archive. When the user includes places in an offline-area download, Waypoint stores only the records in that selected rectangle in private app storage and removes them with the map area. Those snapshots keep the same per-record source and licensing obligations; they do not bundle Commons or Google photos. Dataset terms remain separate from Waypoint's code license. Commons photos are selected individually and display the Commons author/title and license next to the image; Wikimedia Commons content has no single blanket image license.

Google autocomplete, listing details and photos use undocumented Maps web endpoints and an off-screen WebView. They are optional and disabled by default. No Places API key or Places API billing is used; that does not make these endpoints an officially supported free API. Google's [Maps Platform terms](https://cloud.google.com/maps-platform/terms) include restrictions on scraping. The integration is experimental, may stop working, and should not be treated as a durable source or redistributed dataset. See [Privacy and Providers](Privacy-and-Providers.md#network-services) for the data sent and the user control.

## External references

- [Waze Voicepack Links GPL-3.0 license](https://github.com/pipeeeeees/waze-voicepack-links/blob/main/LICENSE)
- [Waze Voicepack Links archive and pack notes](https://github.com/pipeeeeees/waze-voicepack-links)
- [Waypoint Voice Packs issue #24](https://github.com/the-geeky-couldron/waypoint/issues/24)
- [GTA5-Mods Waze Voice Navigation mod and pack list](https://www.gta5-mods.com/scripts/waze-voice-navigation)
- [101 Soundboards search](https://www.101soundboards.com/search)
