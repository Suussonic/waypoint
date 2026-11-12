# Map icon assets

Waypoint ships 38 world-specific POI atlases. Each 640×1280 PNG is a 5×10 grid
of 128-pixel cells: 49 live OpenStreetMap categories and one empty cell. The
cell order is [`poi_categories.json`](../app/src/main/assets/poi_categories.json).
Every world also has a separate 128×128 destination marker and day/night
variants. The map key uses the same atlas as the map.

## Night City

Night City's atlas uses seven Cyberpunk 2077 map glyphs for **direct semantic
matches only**. The original 64×64 PNGs were retrieved through the Cyberpunk
Wiki's MediaWiki image API. Their file pages, original download URLs, MediaWiki
SHA-1 values, SHA-256 hashes, and byte counts are recorded in
[`sources.json`](../third_party/cyberpunk_map_icons/sources.json); the exact
source files are retained alongside it. The generator verifies each SHA-256
before producing the shipped atlas. Run
`python3 scripts/generate_night_city_icons.py --check` to compare the checked-in
runtime PNGs with that source manifest and deterministic generator.

| OSM categories | Cyberpunk source file | Source SHA-256 |
| --- | --- | --- |
| `place` | [PlaceOfInterest](https://cyberpunk.fandom.com/wiki/File:Mappin_PlaceOfInterest_CP2077.png) | `ccabe04326fdbf210953f2d0389a02866e0ddd16ac6f22e65098849577869af2` |
| `restaurant`, `fast_food` | [Food](https://cyberpunk.fandom.com/wiki/File:Mappin_Food_CP2077.png) | `8f8df820452514b00111a6a14494a0353ed10b00ac467a1f40049ebc8ea185d5` |
| `pub`, `bar` | [Bar](https://cyberpunk.fandom.com/wiki/File:Mappin_Bar_CP2077.png) | `519a4e1f13188e52d23a2e7c443fe1d90656e3181bbb82135d311d24b26a2b69` |
| `clothing` | [Clothing](https://cyberpunk.fandom.com/wiki/File:Mappin_Clothing_CP2077.png) | `873137b57bdab33711793744aa7aed439bea45ce9e250391e3934d1fb83bf749` |
| `hospital`, `clinic` | [Medpoint](https://cyberpunk.fandom.com/wiki/File:Mappin_Medpoint_CP2077.png) | `e813ba0054c0c1b0c08e9ff120e7e39931a7bc8a1fc03cc0207d40fbf6942ce4` |
| `monument` | [Landmark](https://cyberpunk.fandom.com/wiki/File:Mappin_Landmark_CP2077.png) | `302866e2978adea23dbe00cc21999c7f5a520f126f44e703668d94a6d57a224e` |
| `police` | [NCPD](https://cyberpunk.fandom.com/wiki/File:Mappin_NCPD_CP2077.png) | `1c13d60743ab63f8a035014e462b4799900703233ee9256c509a3af78c5aab05` |

The other 39 categories use simple original Waypoint glyphs. No game icon is
assigned to an unrelated real-world category. The player caret and destination
target are also original Waypoint artwork. The same upward-pointing caret is
used for Night City in Walk, Car, Motorcycle, and Bicycle modes; the map rotates
it with the known heading, and shows the uncertain-position marker when no
heading is known. [`generate_night_city_icons.py`](../scripts/generate_night_city_icons.py)
builds the day/night atlases, destination targets, player caret, and
[day](assets/poi-audit/night-city-icons.webp) and
[night](assets/poi-audit/night-city-icons-night.webp) icon previews from the retained
sources.

The original caret's open cyan shape was checked against this
[walking HUD capture](https://www.gameuidatabase.com/uploads/Cyberpunk-207712292020-014340-64291.jpg);
the screenshot is a reference and is not bundled.

The project maintainer has confirmed written CD PROJEKT RED permission for
using these game glyphs in the public Waypoint app and repository. The grant is
held privately and is not published here. Cyberpunk 2077 and its game artwork
remain the property of their rights holder; the public source pages document
where these exact files came from, not a general grant to reuse other artwork.

## Liberty City (GTA IV)

The 31 64×64 radar PNGs supplied with Waypoint Discord ticket `ticket-0002` are
preserved under
[`app/src/main/assets/poi/liberty_blips`](../app/src/main/assets/poi/liberty_blips).
The Liberty City map and key use the supplied marks for the closest matching
live OSM categories; categories without an equivalent use the nearest available
mark.
[`Cartography.kt`](../app/src/main/java/com/waypoint/app/map/Cartography.kt)
maps each OSM category, and the map key calls the same lookup so its symbols
match the map in day and night styles.

## Machine Atlas and other worlds

Machine Atlas (NieR) uses deterministic original ink diamonds and monochrome
category glyphs drawn by [`generate_map_assets.py`](../scripts/generate_map_assets.py).
Its day/night icons and destination targets are procedural rather than
ImageGen imports.

The remaining 35 daytime atlases and destination markers are original Waypoint
artwork generated from [dedicated world prompts](../tools/imagegen/poi-atlases-all.jsonl)
and imported by
[`import_imagegen_poi_atlases.py`](../scripts/import_imagegen_poi_atlases.py).
The reviewed source-sheet SHA-256 hashes and nonuniform row/column crop bounds
are recorded in [`poi-crop-manifest.json`](../tools/imagegen/poi-crop-manifest.json).
The importer reads that manifest and explicitly excludes Machine Atlas and
Night City, which have their own generators.
[`generate_night_icon_variants.py`](../scripts/generate_night_icon_variants.py)
derives separate high-contrast night palettes one cell at a time, preventing
glow from crossing into a neighbor. A day/night review of all 38 atlases
identified shifted crop lines, reordered or omitted late categories, and
detached neighbor fragments. The manifest documents 92 cell repairs. Their
128-pixel PNGs are checked in under [`poi-overrides`](../tools/imagegen/poi-overrides),
so [`import_imagegen_poi_cell_repairs.py`](../scripts/import_imagegen_poi_cell_repairs.py)
can apply them without the ignored ImageGen source files. Forty-three of those
repairs came from separately generated original icon artwork; the prompts are
retained in [`poi-cell-repairs.jsonl`](../tools/imagegen/poi-cell-repairs.jsonl).
The [paired contact sheets](poi-atlas-audit.md) show the final day/night review.
The independent [`generate_map_assets.py`](../scripts/generate_map_assets.py)
renderer supplies original vector fallback assets and cartographic materials.

Map geometry, place names, and category semantics still come from live
OpenStreetMap/OpenMapTiles data. The themed map keys preserve the real OSM class
beneath their world-specific terminology. Run
`scripts/validate_map_assets.py` to check coverage, dimensions, transparency,
split or contaminated badges, tracked repair parity, and distinct day/night
assets before packaging.
