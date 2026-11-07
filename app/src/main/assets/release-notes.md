# Waypoint 2.5.4

This update restores the look of Night City and Machine Atlas, repairs damaged place icons across the maps, and gives you more control over the phone map HUD.

### Map artwork and position

- Reviewed all 38 worlds' day and night atlases and repaired damaged place icons, including the split Home Console icons.
- Night City now uses recognizable Cyberpunk 2077 symbols where they match the place type, with simpler Waypoint symbols for the other places. Its Cyberpunk-inspired player marker keeps the same shape across walking, cycling, and driving on the phone and Android Auto; it rotates with a known heading and indicates when heading is unknown.
- Machine Atlas again uses its cream map, charcoal lettering, rust accent, and simple monochrome place symbols at every hour. Its phone controls now align with the other worlds in portrait and landscape.

### Phone map controls

- A scale bar shows the distance represented by the current map view. It updates as the camera moves, uses your selected distance units, and sits at the bottom center in portrait or bottom left in landscape.
- Choose Compact, Standard, or Large floating controls, with an optional Left or Right side placement. Controls keep touch targets at least 48 dp wide and tall.
- Long world names scroll in the top-left selector when they cannot fit, across every world.
- Landscape screens leave clearer space around the map controls in every world. Where there is room, route preview uses a compact side panel; at larger text sizes, route details scroll and guidance actions reflow while the scale bar stays clear.

### Artwork quality

The place icon pipeline now records reviewed crop bounds, checks the finished atlases, and uses Pillow 13.0.0 so regenerated artwork stays consistent.

### Thanks for the reports and ideas

- **[@Balzamus](https://github.com/Balzamus)** spotted the malformed Home Console icons in [#39](https://github.com/the-geeky-couldron/waypoint/issues/39).
- **[@aidonevs](https://github.com/aidonevs)** asked for the game-accurate Night City map symbols in [#48](https://github.com/the-geeky-couldron/waypoint/issues/48).
- **[@FoxlNix](https://github.com/FoxlNix)** asked for a consistent Cyberpunk player marker across travel modes in [#23](https://github.com/the-geeky-couldron/waypoint/issues/23).
- **[@6HentaiMaster9](https://github.com/6HentaiMaster9)** reported the Machine Atlas regression and suggested the scale bar and HUD settings in [#47](https://github.com/the-geeky-couldron/waypoint/issues/47).

Compatible updates preserve your saved places, quests, and settings. Maps, search, and routes still need an internet connection. Android Auto availability in a real vehicle still depends on Google's trusted installation requirements.
