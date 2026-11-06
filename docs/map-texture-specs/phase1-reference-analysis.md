# Map texture specs, phase 1 (reference analysis)

Written 6 November 2026 from a visual pass over the reference screenshots supplied for the project. Reference images are kept out of the repository (`.local/map-references/`, gitignored). This file holds written observations only. Textures are drawn from scratch; nothing is traced or sampled from the originals.

Colours below are described, not measured. Swatches get sampled when each world's texture kit is built.

## What every world shares today

Every style in `app/src/main/assets/styles/` uses the same OpenMapTiles layer recipe (about 35 layers, no sprite sheet, no pattern fills except three in Pixel World), so differences are colour and line width only. The gap list under each world is measured against that baseline.

## Not yet analysed

| Reference | Reason |
|---|---|
| (none outstanding) | The five reddit images (HUD example, NFS Heat, Vice City, Elden Ring, GTA San Andreas) were sent in chat on 6 Nov and are analysed below |
| Minecraft | No image supplied by design: keep the blocky look and add 3D block textures |
| Spider-Man | Tiny thumbnail only (about 385 px wide); palette and mood noted, texture detail is not readable |
| Thumbnails for NFS Heat, Witcher 3, Skyrim, Far Cry 5, Ghost of Tsushima | Seen, but too small to add detail beyond the full-size images |

## Per-world specs

### Los Santos (GTA V pause map)
- **Ground:** near-black charcoal base with a dim brown-grey terrain tint at the edges; water is pure black.
- **Roads:** thin light-grey strokes on dark with a faint darker casing; major roads only slightly wider; no centre line; no building footprints at all.
- **Buildings:** none drawn. Density is read from the road mesh.
- **Labels and icons:** small white sans labels; round white-on-dark blip discs; a right-hand list panel with highlighted row.
- **Gap and approach:** the app already uses a dark base; add road casing, drop footprints to near-invisible, a faint terrain tint layer and blip-disc POI sprites.

### Old West (Red Dead Redemption 2 map)
- **Ground:** warm cream parchment, flat, very slight paper tone.
- **Roads:** thin dark-brown ink lines, hairline weight, little width variation; rivers are hand-drawn looking, slightly heavier with soft meander.
- **Buildings:** none shown at this scale; towns appear as small ink clusters.
- **Terrain:** short hachure strokes for slopes; regions washed in a soft pink-red (a flat tint, not a gradient).
- **Labels:** spaced capitals for regions, small caps for towns, dark brown, no halo.
- **Gap and approach:** add hachure `fill-pattern` for slopes, uneven ink-line sprite for roads and rivers, paper-fibre overlay, a region wash tint, tight spaced-caps labels.

### Night City (Cyberpunk 2077 map)
- **Ground:** deep navy/black.
- **Buildings:** solid red extruded masses with a darker red side face and flat top; no outline; very dense.
- **Roads:** thin glowing cyan lines tracing between blocks, lighter at junctions.
- **Water:** near-black navy with faint cyan grid.
- **HUD tie-in:** thin red rule lines and cyan highlights framing the map; icons small, white or yellow with glow.
- **Gap and approach:** extrusion with two-tone red (top and side), cyan road glow via a wide low-opacity casing under a thin bright line, dark water with scan grid pattern.

### Pixel World (Minecraft, no reference by design)
- **Direction:** keep the blocky look and add 3D texture: bevelled block faces, lit top and shaded side, extruded buildings with block-face pattern, stepped (stair-cased) road edges and tiled grass, stone and water.
- **Approach:** extend the three existing pattern layers to full block tiles at 16 px, add side-shading via `fill-extrusion` colour stops, and a stair-step `line-pattern` for roads.

### Green Terminal (Fallout Pip-Boy)
- **Ground:** near-black with faint scanlines.
- **Everything:** monochrome phosphor green; walls drawn as double outlines with slightly lighter inner fill; labels small caps green.
- **Icon sheet:** bold single-weight green glyphs, thick strokes, simple silhouettes; roughly 170 glyphs in a regular grid.
- **Gap and approach:** double-stroke casing for roads and building edges, scanline overlay, glow on the primary stroke, bold glyph POI set redrawn in the same weight.

### Midnight Run (NFS Heat)
- **Ground:** one ultraviolet scheme: deep violet land with fine contour lines on the hills, slightly lighter lavender where the ground is built up.
- **Water:** electric blue, brightest and teal at the shoreline, fading to navy offshore; no outline.
- **Roads:** thin pale grey-white lines, curvy on the hills, a little heavier on motorways; racetracks and lots show as lighter lavender shapes.
- **Buildings:** urban areas as light-lavender blocks, dense downtown and sparse elsewhere; no outlines.
- **Labels:** small white condensed capitals, tracked wide, no halo. A thin pale outline marks the playable area.
- **Gap and approach:** violet duotone ground with a contour-line layer, a shoreline gradient (line-gradient or stacked blurred casings), pale hairline roads, lavender block footprints, white condensed labels, playable-area border.

### Vice Coast (GTA Vice City)
- **Ground:** flat poster colours. Sea is a light sky blue, land is white and light-grey city blocks, parks and shoreline edges are mid green, the beach is pale cream.
- **Roads:** thick solid black strokes, same weight for most roads, with no centre line or casing; bridges are the same black bars across the water.
- **Depth:** a soft grey drop shadow sits offset under each landmass.
- **Radar (from the collage):** round, with a pink and white ring and a white north marker.
- **Gap and approach:** flat fills with no gradients, thick black road stroke, shadow layer under land (offset blurred fill), cream beach strip, pink-ringed round minimap frame.

### Elden Ring (Lands Between)
- **Sea:** weathered blue-grey green with fine engraved wave and current lines.
- **Fog:** cream cloud banks with swirled line texture surround the unexplored edges, irregular and layered.
- **Land:** painted relief in muted brown, ochre and olive; snow-capped mountains in white with pale blue lakes; one region in rusty red.
- **Roads:** golden glowing thread lines between sites, soft light along the edges.
- **Icons and labels:** round gold markers with a white building glyph; region names on ribbon banners; compass rose in the top right corner; dark circular dials in the bottom corners.
- **Gap and approach:** engraved wave `fill-pattern` for water, cloud-bank sprite layer or edge mask, painted relief tile, gold thread road with soft glow casing, ribbon banner label background, compass rose frame ornament.

### Los Santos radar and the other radar styles (HUD example collage)
The collage shows how the in-game radar evolved, which suits the "HUD expanded to a map" idea:
- **GTA III:** oval radar, thick black outline, orange land with grey roads.
- **Vice City:** round, pink and white ring, white blocks, black roads, light-blue water, green parks.
- **San Andreas:** round, thick black outline with a green inner ring, white blocks, grey roads, green land.
- **Bully:** round, light-blue blocks with dark outlines, yellow X markers.
- **GTA IV:** round, ring that fades green to teal, pale grey ground, white roads, blue-grey water.
- **RDR1 and RDR2:** thin ring only, wire-light linework on a transparent or dark teal ground, little colour.
- **GTA V:** square, grey ground with darker grey roads, white icons, a colour status bar underneath.
Use each ring and frame as the minimap shape for its world, so the map and HUD read as one object.

### Liberty City (GTA IV)
- **Ground:** pure black.
- **Roads:** white and grey double-line streets, thin, with the main avenues slightly brighter; no buildings or terrain.
- **Icons:** white solid discs with black pictograms.
- **Gap and approach:** thin double-line road sprite, black land, white disc POIs, near-zero building opacity.

### Nudle Maps (Watch Dogs, London map used as the reference)
- **Ground and water:** deep blue holographic base; river as a flat lighter blue band.
- **Buildings:** lighter blue footprints with darker blue outline, slightly glowing.
- **Parks:** muted teal green blocks.
- **Icons:** teardrop pins in white, red, dark grey and cyan, with white glyphs.
- **Gap and approach:** blue-on-blue two-tone footprint style with outline, teardrop pin sprite family, faint glow layer.

### Sheikah Slate (Zelda BotW and TotK minimap)
- **BotW map:** sepia height bands from cream peaks to olive-brown valleys, blue-grey water, very thin yellow roads, dark vignette at the edges.
- **TotK minimap:** round with a pointed north tab; same sepia relief, small yellow arrow.
- **Gap and approach:** terrain-driven banding (hillshade or colour-ramped fill by elevation), fine yellow road line, rounded minimap frame with north tab.

### Skyrim (Elder Scrolls V)
- **Ground:** aged parchment with stains and darkened edges; compass rose lower left.
- **Terrain:** hand-drawn mountain illustrations (small peaked glyphs), tree glyphs, sea in flat blue with a hand-inked coast; roads thin dark red-brown ink.
- **Labels:** serif, mixed case, dark brown; hold emblems as small shields.
- **Gap and approach:** mountain glyph `fill-pattern` driven by elevation, stain and edge-burn overlay, inked coastline stroke, serif labels.

### San Andreas (GTA San Andreas)
- **Ground:** flat poster colours with no gradient: slate-blue sea, dark green land, lighter green meadows, tan desert, pale-grey rock, pale-yellow beaches.
- **Roads:** thick solid black strokes; motorways slightly heavier; thin dark-red lines for rail; thin grey for dirt tracks.
- **Buildings:** city areas are solid white blocks with a thin black gap between them, dense downtown, sparse in towns; parks are small green blocks inside; airports are light grey shapes with white runway marks.
- **Gap and approach:** poster-colour land-use fills (no gradients), thick black road strokes without casing, white block footprints, red rail line, pale beach strip. The round radar from the collage is the minimap frame.

### Forza Horizon
- **Ground:** satellite-style terrain photo tint: greens, tans, grey rock.
- **Roads:** white lines with a soft grey casing; motorways wider and brighter; thin dashed tracks for dirt roads.
- **Water:** blue with a pale shoreline stroke.
- **Icons:** coloured rounded pins with white borders and a small label tag ("NEW").
- **Gap and approach:** photographic ground tint (a tiled terrain texture), white road with casing, shoreline stroke, coloured pin family.

### Assassin's Creed (Mirage)
- **Ground:** warm sepia relief, grey-brown with stippled texture; buildings as dense cream-white blocks with fine dark outlines; circular city wall as a strong ring.
- **Labels:** wide-spaced serif capitals in dark brown.
- **Icons:** orange, blue and grey teardrop pins with white glyphs.
- **Gap and approach:** sepia stipple texture on land, dense cream block footprints with fine outline, wide-spaced serif labels, teardrop pin set.

### Hope County (Far Cry 5)
- **Ground:** dark satellite imagery look, tan and olive terrain; water deep navy-aqua.
- **Roads:** thin pale tan and grey lines, winding.
- **Icons:** dark red and teal pins with white glyphs.
- **Gap and approach:** satellite-style tiled ground texture, desaturated roads, red/teal pin family, dark water.

### Tsushima (Ghost of Tsushima)
- **Ground:** off-white paper with ink-wash land masses: black to grey brush texture, ragged edges, no hard outlines; rivers as white gaps; red diamond markers; small torii glyphs; vertical Japanese-style labels in black.
- **Gap and approach:** brush-edge land mask via soft noise pattern, white river gaps, red diamond pins, paper background, no road casing.

### Bridges (Death Stranding)
- **Ground:** photoreal terrain tinted blue, with fine scan lines and a holographic overlay; UI panels with thin cyan frames and bright blue highlight rows.
- **Gap and approach:** blue duotone terrain, scanline overlay, thin cyan frame ornament, glowing route.

### The Embrace (Horizon Zero Dawn)
- **Ground:** black void; land is white frost-textured with irregular crystalline edges, no colour; orange route and yellow player marker; white and green glyphs.
- **Gap and approach:** inverted scheme: black ground with frosted white land texture, orange route, glyph POIs.

### Manhattan (Spider-Man): only a thumbnail
- Dark navy base, light-blue footprints for the active district, red HUD highlights; too small to specify more.

### Highlands (Hogwarts Legacy)
- **Ground:** soft grey-brown parchment with a faint vignette and corner compass roses; land in muted green with painted texture; water in desaturated blue-grey.
- **Roads:** none visible at the world scale.
- **Icons:** small round white-on-dark emblems.
- **Gap and approach:** painted-wash land fill, vignette and compass overlay, round emblem POIs, muted palette.

### The Continent (Witcher 3)
- **Ground:** warm ochre and olive painted terrain, aged; water in blue-grey with soft edges.
- **Roads:** thin pale tracks; places marked with small white diamonds and question marks.
- **HUD tie-in:** dark top bar with gold highlight on the active tab.
- **Gap and approach:** painted ochre terrain tile, soft-edged water, small white diamond pins, gold-on-dark HUD frame.

## Next step

Build the texture kit for the three pilots (Old West, Los Santos, Pixel World), capture on the phone beside the reference, then roll out in batches of five.
