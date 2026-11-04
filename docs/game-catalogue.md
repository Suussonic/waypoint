# Game map catalogue

38 game map themes. Game names indicate visual references, not official assets. Each uses real GPS streets and a dedicated native composition, cartographic profile, marker family, type and material rules. See [visual-sourcebook.md](visual-sourcebook.md) for evidence and limits.

## Expanded games

- **MACHINE ATLAS** — NieR:Automata. Warm beige panels, charcoal map details, real building relief and original machine markers; Noto Sans adaptation. Requested in [#9](https://github.com/the-geeky-couldron/waypoint/issues/9).

- **EXCLUSION ZONE** — S.T.A.L.K.E.R.: Shadow of Chernobyl. A rugged PDA frame, muted terrain, green routes and a personal field journal. Requested in [#8](https://github.com/the-geeky-couldron/waypoint/issues/8).

- **DARK ZONE** — The Division. Orange tactical details and a teal city atlas.
- **SEA CHART** — Sea of Thieves. Nautical paper, ink outlines and a compass-led layout.
- **DOS ATLAS** — DOS / ASCII interfaces. Amber roads, monospaced type and simple line framing.
- **AINCRAD** — Sword Art Online. Light quest panels and a warm atlas with gold hierarchy.
- **TACTICAL RADAR** — Counter-Strike. Flat buildings, compact radar and clear directional markers.
- **VECTOR COMMAND** — DEFCON. Sparse vector geography and thin command-panel rules.
- **FIELD OPS** — Call of Duty. Restrained tactical map and briefing-style readouts.
- **16-BIT QUEST** — Retro console adventures. A colourful adventure atlas with original pixel lettering.
- **HOME CONSOLE** — Wii U / console menus. Bright map, rounded panels and approachable controls.

- **ANIMUS** — Assassin’s Creed Origins. Desert cartography, gold guidance and restrained atlas panels.
- **HOPE COUNTY** — Far Cry 5. Green terrain relief, pale roads and a clear exploration HUD.
- **TSUSHIMA** — Ghost of Tsushima. Washed ink terrain, paper-white land and scarlet guidance.
- **BRIDGES** — Death Stranding. Cold terrain, cyan data marks and the blue-gray route interface.
- **THE EMBRACE** — Horizon Zero Dawn. Dark forest terrain, relief and fine ivory atlas controls.
- **MANHATTAN** — Marvel’s Spider-Man. Teal 3D city blocks, blue street lines and red HUD accents.
- **HIGHLANDS** — Hogwarts Legacy. Muted green relief, gilded borders and a midnight-blue atlas HUD.
- **SURFACE MAP** — Starfield. Deep-blue cartography, bright location marks and precise readouts.
- **BOHEMIA** — Kingdom Come: Deliverance II. Warm medieval atlas colors, woodland relief and golden routes.
- **STEELPORT** — Saints Row: The Third. Dark city blocks, purple selections and bright GPS routes.
- **MEDICI** — Just Cause 3. Navy terrain, turquoise coastlines and clean tactical-map controls.
- **LOST HEAVEN** — Mafia: Definitive Edition. 1930s street-atlas colors, golden roads and quiet cream typography.

All 38 styles are listed in README.md. Search the Worlds library using a theme title or game name. To add a style, extend World.kt, the explicit atlas dispatcher and renderer, Cartography.kt, the HUD tokens and scripts/cartography_profiles.py; generate the style assets and verify native rendering.

The Witcher 3 appears as **THE CONTINENT**, with **The Witcher 3** below it. Every world uses the same large-style/small-game hierarchy. The ordinary Classic style has been removed, including car day/night defaults.
