# Map and HUD sourcebook

Research for the user's private Waypoint Android app, 30 October 2026. These sources inform separately authored Compose controls and real-world map styles. Reference screenshots stay in ignored local research folders; they are not packaged as runtime map artwork. Sources show different versions and states, so a pause map, a gameplay radar, a treasure map and a cinematic hologram are not interchangeable.

## Archives checked

| Source | Useful material | Research decision |
|---|---|---|
| [Game UI Database](https://www.gameuidatabase.com/) | Categorized original game map and HUD captures | User-pinned authority. Inspected through the browser; Direct map/HUD pairs checked for GTA V, GTA IV, RDR2, Cyberpunk, Fallout 4, Skyrim, Elden Ring and Saints Row III. |
| [Interface In Game](https://interfaceingame.com/) | Game-specific map, HUD and interaction screens | User-pinned authority. Retrieved and visually inspected map/HUD samples for Cyberpunk, RDR2, NFS Heat, Witcher 3, Watch Dogs 2, BOTW, AC Origins, Ghost, Death Stranding, Horizon Zero Dawn, Spider-Man and Starfield. |
| [HUDS+GUIS](https://www.hudsandguis.com/home/category/Games) | Interface breakdowns, motion, credits and artist work | Useful secondary geometry/motion reference. Its [2019 Cyberpunk article](https://www.hudsandguis.com/home/2019/cyberpunk-2077) is pre-release material, not authority for the retail map. |
| [GameGUI](https://www.gamegui.net/) | Older game interface archive; GTA 3/4, Oblivion, Assassin's Creed | Supplement for era-specific framing and menu hierarchy. |
| [PlayCraft Gallery](https://playcraft.space/gallery/) | Publisher screenshot gallery with map/HUD/component tags | Additional official-release imagery; inspected categories and coverage. |
| [MobyGames San Andreas screenshots](https://www.mobygames.com/game/15393/grand-theft-auto-san-andreas/screenshots/) | Original platform captures and captions | Supplement for older GTA interface versions. |
| [Game UI Wiki](https://gameuiwiki.com/index.php/Game_UI_Wiki) | Community interface archive | Found through search; direct fetch failed. Not used as visual evidence. |
| [Forza world map artist portfolio](https://www.behance.net/gallery/131465877/Forza-Horizon-5-World-Map-UI) | Map design by the UI designer | Earlier retained reference; direct fetch on this pass returned 403. |

## Authoritative screen pairs and implementation decisions

| Game | Reference | Map/HUD grammar carried into the app |
|---|---|---|
| GTA V | [Database game 115](https://www.gameuidatabase.com/gameData.php?id=115) | Flat grayscale atlas, slim active white tab, right place legend, outlined white blips, purple guidance and compact lower radar readout. |
| RDR2 | [Database game 34](https://www.gameuidatabase.com/gameData.php?id=34) | Flat parchment, thin ink building outlines, dashed paths, scarlet route, location stamp, circular core-style navigation controls. The Interface In Game “Map” sample is a held treasure map; excluded as main UI authority. |
| Cyberpunk 2077 | [Map](https://interfaceingame.com/screenshots/cyberpunk-2077-map/), [driving](https://interfaceingame.com/screenshots/cyberpunk-2077-driving/) | Red extruded building masses, dark-blue terrain, cyan road lines, thin red rails, right-side destination controls, yellow route, technical Rajdhani lettering. |
| Minecraft | [Survival HUD](https://minecraft.fandom.com/wiki/Gameplay) | Coherent pixel scale, bevelled hotbar, discrete sprite controls, original grass/stone/water tiles and Monocraft type. Real building footprints remain geographic polygons. |
| Fallout 4 | [Database game 633](https://www.gameuidatabase.com/gameData.php?id=633) | Connected green tab rules, subdued green roads, isolated glyphs, local-map heading and fixed bottom readout. Uses Roboto Condensed for the Pip-Boy, separate from terminal lettering. |
| GTA San Andreas | [Original screenshots](https://www.mobygames.com/game/15393/grand-theft-auto-san-andreas/screenshots/) | High-contrast flat atlas, broad cream roads, heavier symbols, corner map title and circular orientation. |
| NFS Heat | [Map](https://interfaceingame.com/screenshots/need-for-speed-heat-map/) | Earth-toned road atlas, saturated blue water, compact racing controls and destination action. Corrected earlier violet terrain. |
| The Witcher 3 | [Map](https://interfaceingame.com/screenshots/the-witcher-3-wild-hunt-map/) | Dark header/footer, center world-map label, antique land cover, fine roads, serif labels, lower-left location actions. |
| GTA Vice City | [PS2 reference](https://anotherretroworld.wordpress.com/2016/11/02/gta-vice-city-sur-ps2/) | Pink script map label, graphic pale roads and dark casing, coastal colors, outlined symbols and corner controls. |
| GTA IV | [Map references](https://steamcommunity.com/sharedfiles/filedetails/?id=147215606) | Sparse monochrome atlas, restrained top tabs, amber guidance and lower-right compact controls. |
| Watch Dogs 2 | [Map](https://interfaceingame.com/screenshots/watch-dogs-2-map/) | Daylight green/blue atlas, white Nudle search header, colored place markers, left filter icons, orange route. |
| BOTW | [Map](https://interfaceingame.com/screenshots/the-legend-of-zelda-breath-of-the-wild-map/) | Brown terrain under blue slate, gold geometric edge bands, cyan markers and integrated upper/lower controls. |
| Skyrim | [Database game 287](https://www.gameuidatabase.com/gameData.php?id=287) | Gray relief, sparse silver map glyphs, hidden building masses, thin white typography and low dark toolbar. |
| Elden Ring | [Database game 1371](https://www.gameuidatabase.com/gameData.php?id=1371) | Warm atlas material, circular gold markers, upper-left Map label, restrained edge ornament and bottom actions. |
| Forza Horizon 5 | [World map portfolio](https://www.behance.net/gallery/131465877/Forza-Horizon-5-World-Map-UI) | White controls, magenta selected tab, colored event-like place symbols and blue GPS guidance. |
| AC Origins | [Map](https://interfaceingame.com/screenshots/assassins-creed-origins-map/) | Ochre relief, thin gold active line, dark slim tabs and lower map controls. |
| Far Cry 5 | Local study reference archived outside Git | Green relief with building mass, yellow guidance, sparse right location filters. |
| Ghost of Tsushima | [Map](https://interfaceingame.com/screenshots/ghost-of-tsushima-map-2/) | Washed grayscale paper, dark tab strip, ink road detail, diamond locations and compact guiding-route action. |
| Death Stranding | [Map / information](https://interfaceingame.com/screenshots/death-stranding-map-2/) | Cold relief, thin cyan frame, small left data rail, compact lower-right route controls. Cinematic world-map images excluded. |
| Horizon Zero Dawn | [Map](https://interfaceingame.com/screenshots/horizon-zero-dawn-map/) | Dark forest relief, thin ivory navigation strip, sparse white symbols; corrected earlier copper-dominant terrain. |
| Spider-Man | [Map legend](https://interfaceingame.com/screenshots/marvels-spider-man-map-legend/) and earlier real-map capture | Teal extruded city, cyan diamond markers, red upper selection strip, lower waypoint bar. |
| Hogwarts Legacy | Local study reference archived outside Git | Muted green relief, navy/gold framing, maroon region strip and ornate compass. |
| Starfield | [Surface map](https://interfaceingame.com/screenshots/starfield-map-3/) | Blue survey field, dot treatment, white diamond symbols, upper-left information and bottom controls. Retains streets for navigation. |
| Kingdom Come II | Local study reference archived outside Git | Warm paper, inset ornament, ink buildings, gold guidance and compass cartouche. |
| Saints Row III | [Database game 342](https://www.gameuidatabase.com/gameData.php?id=342) | Dark tablet framing, purple atlas route, left location pane; yellow player arrow and green dotted route in live guidance. |
| Just Cause 3 | Local study reference archived outside Git | Blue tactical relief, yellow selected tab, cyan location glyphs, lower-right waypoint controls. |
| Mafia DE | Local study reference archived outside Git | Printed pale atlas, yellow roads, muted blocks, black map header/footer and compact actions. |

The retained additional-game source URLs follow below; the local files are private review evidence. Only the 27 game inspirations are offered; the plain Classic map was removed at the user’s request.

## Authored assets and limitations

`generate_materials.py` produces original 16-pixel stone/grass/water tiles, nine pixel control/place sprites, and paper fibre. `Cartography.kt` draws semantic POI glyphs, player pointers and destination shapes, selected by each game's map rules. Compose renders game-specific map composition; map icons and geometry are not screenshots pasted over a functional map. Fonts are bundled with licenses.

No screenshot proves navigation accuracy. Real street connectivity, position, routes and elevations are preserved. Game-authored fictional terrain, unique proprietary fonts and hand-painted map illustrations are not present, so this is not a pixel-exact game asset port. The sourcebook identifies observed similarities and adaptations rather than claiming identity.

## Retained reference-image URLs

- [assassin reference image](https://cdn.staticneo.com/ew/e/e6/AC_Origins_Papyri_11a.jpg)
- [farcry reference image](https://romly.com/blog_images/ps4_farcry5/farcry5-03.jpg)
- [ghost reference image](https://images.pushsquare.com/fa434df42c085/ghost-of-tsushima-map-size.original.jpg)
- [bridges reference image](https://cdn.tgdd.vn/GameApp/249064/Death-Stranding-chiral-network-800x450.jpg)
- [horizon_wilds reference image](https://platget.com/wp-content/uploads/2020/09/Horizon-Zero-Dawn%E2%84%A2_20200917064616-1024x576.jpg)
- [spider reference image](https://i0.wp.com/www.downtimebros.com/wp-content/uploads/2022/08/spider-man-landmark-icon.jpg?resize=640%2C387&ssl=1)
- [hogwarts reference image](https://cdn.staticneo.com/ew/3/3e/HL_SQ_Carted-Away-01.jpg)
- [starfield reference image](https://cdn.mos.cms.futurecdn.net/rmnThKaHtVRGTm4KCHzHgF-2560-80.png)
- [kingdom reference image](https://cdn.gracza.pl/galeria/galeria_duze3/909653671.jpg)
- [saints reference image](https://www.saintsrowmods.com/forum/attachments/downloadfile-12-jpg.13347/)
- [justcause reference image](https://media.vandal.net/m/1-2016/201611512418_1.jpg)
- [mafia reference image](https://i0.wp.com/gameranx.com/wp-content/uploads/2020/09/Mafia_-Definitive-Edition_20200924095207-scaled.jpg?fit=800%2C450&ssl=1)

The Saints Row mod-forum URL above is superseded by the verified original retail map/minimap pair below; it is not authority for fidelity.

## Map versus navigation state — direct database evidence

Full-size captures were downloaded and visually inspected, including line endings, route patterns and marker treatment:

| State | Direct image | Applied decision |
|---|---|---|
| Cyberpunk atlas | [City map](https://www.gameuidatabase.com/uploads/Cyberpunk-207712292020-014137-26627.jpg) | Red building masses; cyan road hierarchy; solid yellow atlas/vehicle guidance. |
| Cyberpunk walking | [Walking radar](https://www.gameuidatabase.com/uploads/Cyberpunk-207712292020-014340-64291.jpg) | Yellow dotted route when Walk is selected, distinct cyan player chevron. |
| RDR2 atlas | [Selected saloon map](https://www.gameuidatabase.com/uploads/Red-Dead-Redemption-207112020-090844-25672.jpg) | Ink road hierarchy, fine building outlines, dashed paths, red waypoint. Narrowed excessive road casing after native comparison. |
| Fallout atlas | [Pip-Boy map](https://www.gameuidatabase.com/uploads/Fallout-403042021-084648-42721.jpg) | Connected phosphor tabs and subdued terrain detail; real streets remain visible for navigation. |
| Skyrim atlas | [World map](https://www.gameuidatabase.com/uploads/The-Elder-Scrolls-V-Skyrim07192020-073934-21177.jpg) | Muted cold relief, outlined white symbols and compact lower controls. Geography stays real rather than manufacturing snowy mountains. |
| Elden Ring atlas | [Map](https://www.gameuidatabase.com/uploads/Elden-Ring03022022-114430-89115.jpg) | Warm paper, fine paths, gold circular marks and compass. Painted fictional terrain and sites-of-grace rays are not driving routes and are not fabricated. |
| Saints Row atlas | [Phone map](https://www.gameuidatabase.com/uploads/Saints-Row-The-Third09132020-111621-91368.jpg) | Purple solid route and compact left destination pane. |
| Saints Row navigation | [Driving radar](https://www.gameuidatabase.com/uploads/Saints-Row-The-Third09132020-111621-17408.jpg) | Live guidance switches to green dots and a yellow player arrow. |

The research images are retained locally in `.tooling/gameuidb/`, not shipped inside the APK or open-source archive. Runtime textures and icons are authored in the project; downloaded full-screen pictures cannot become geographically accurate road tiles.

## Native map fonts

See [map-fonts.md](map-fonts.md) for researched font roles, bundled native SDF stacks, and the distinction between identified original families and licensed substitutes.

## Reproducible base-style input

The OpenFreeMap Liberty input used for these themes has SHA-256
`408245464e76791801a64393c403433d09e8110eb9516040f8dc2b62584eee2a`. `scripts/generate_styles.py` verifies this pin before
regenerating. Upstream changes fail closed until reviewed and repinned. The source
is https://tiles.openfreemap.org/styles/liberty; a fresh download may no longer
match. Committed generated styles are Android build inputs; CI does not regenerate
from that endpoint.
