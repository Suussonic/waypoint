# Map Worlds

Every world has a Waypoint-authored palette, typography, road hierarchy, map material, POI icon atlas, destination marker and legend. The symbols sit over live geographic features and names. Map styles do not alter route geometry or navigation rules.

## The 38 worlds

| World | Inspiration |
| --- | --- |
| Los Santos | Grand Theft Auto V |
| Old West | Red Dead Redemption 2 |
| Night City | Cyberpunk 2077 |
| Pixel World | Minecraft |
| Green Terminal | Fallout |
| San Andreas | Grand Theft Auto: San Andreas |
| Midnight Run | Need for Speed Heat |
| The Continent | The Witcher 3 |
| Vice Coast | Grand Theft Auto: Vice City |
| Liberty City | Grand Theft Auto IV |
| Nudle Maps | Watch Dogs 2 |
| Sheikah Slate | The Legend of Zelda: Breath of the Wild |
| Skyrim | The Elder Scrolls V: Skyrim |
| Lands Between | Elden Ring |
| Horizon | Forza Horizon |
| Animus | Assassin's Creed Origins |
| Hope County | Far Cry 5 |
| Tsushima | Ghost of Tsushima |
| Bridges | Death Stranding |
| The Embrace | Horizon Zero Dawn |
| Manhattan | Marvel's Spider-Man |
| Highlands | Hogwarts Legacy |
| Surface Map | Starfield |
| Bohemia | Kingdom Come: Deliverance II |
| Steelport | Saints Row: The Third |
| Medici | Just Cause 3 |
| Lost Heaven | Mafia: Definitive Edition |
| Dark Zone | The Division |
| Sea Chart | Sea of Thieves |
| DOS Atlas | DOS / ASCII interfaces |
| Aincrad | Sword Art Online |
| Tactical Radar | Counter-Strike |
| Vector Command | DEFCON |
| Field Ops | Call of Duty |
| 16-bit Quest | Retro console adventures |
| Home Console | Wii U / console menus |
| Exclusion Zone | S.T.A.L.K.E.R.: Shadow of Chernobyl |
| Machine Atlas | NieR:Automata |

Names identify the inspiration for each original Waypoint treatment. They do not indicate endorsement, affiliation or included game assets.

## Symbols and legend

Each world has its own original 13-category POI atlas and destination marker, with separate day and night variants. The Map info control explains the symbols for the current world. The key includes mapped places, food, cafes, pubs, lodging, fuel, parking, shops, sights, health, nature, cash and schools. Which points appear depends on the live map data and zoom level.

## Day and night

The local device time switches between authored day and night map styles at 06:00 and 19:00. The map, symbols and HUD use the same phase. The check repeats every minute; no network location or sunrise service is needed.

## Dark Zone

Dark Zone keeps its tactical 2D map and uses its separate holographic style when the existing tilt control selects 3D. It shares the same real geography and route overlay as the other worlds.

## How the map is built

MapLibre Native renders the live vector geography and Waypoint-authored style layers. Waypoint controls the colors, materials, relief, roads, labels, markers, POI sprites and HUD. It does not load a pre-made MapLibre theme. Routing engines continue to supply route geometry; the theme does not rewrite it. See [the visual sourcebook](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/visual-sourcebook.md), [map icon notes](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/poi-icon-sources.md) and [style source](https://github.com/the-geeky-couldron/waypoint/tree/dev/app/src/main/assets/styles).
