---
name: Waypoint
description: Native Android navigation with distinct game-inspired atlases.
colors:
  launcher-map-ink: "#131915"
  launcher-route-lime: "#D5F16C"
  launcher-arrow-ivory: "#F3F5E8"
  launcher-compass-muted: "#77866F"
  library-ink: "#151815"
  library-cream: "#F1F2E8"
  library-muted: "#AFB5A7"
  default-panel: "#191B1BF2"
  default-foreground: "#F5F5F0"
  default-accent: "#B6DB91"
  western-panel: "#1D1A16F5"
  western-cream: "#F5EAD4"
  western-route: "#B52E31"
  night-panel: "#0D1720F2"
  night-cyan: "#8FF6ED"
  night-route: "#F6E75C"
  terminal-panel: "#07180DF5"
  terminal-phosphor: "#A4F780"
  slate-blue: "#0B1C29F5"
  slate-gold: "#F0D18B"
  daylight-panel: "#F8F9F3F5"
  daylight-ink: "#18272D"
typography:
  library-display:
    fontFamily: "Barlow Condensed"
    fontSize: "32sp"
    fontWeight: 700
    lineHeight: "38sp"
  welcome-display:
    fontFamily: "Barlow Condensed"
    fontSize: "48sp"
    fontWeight: 700
    lineHeight: "49sp"
  world-style-name:
    fontSize: "22sp"
    lineHeight: "28sp"
  world-game-name:
    fontSize: "12sp"
    lineHeight: "17sp"
  control-caption:
    fontFamily: "Roboto Condensed"
    fontSize: "12sp"
    lineHeight: "16sp"
    letterSpacing: "0sp"
  atlas-default:
    fontFamily: "Barlow Condensed"
    fontSize: "16sp"
    fontWeight: 700
    lineHeight: "20sp"
  atlas-action:
    fontFamily: "Barlow Condensed"
    fontSize: "15sp"
    fontWeight: 700
    lineHeight: "19sp"
  western-title:
    fontFamily: "Rye"
    fontSize: "20sp"
    lineHeight: "24sp"
  fantasy-title:
    fontFamily: "Cinzel"
    fontSize: "24sp"
    lineHeight: "28sp"
  technical-label:
    fontFamily: "Rajdhani"
    fontSize: "17sp"
    lineHeight: "21sp"
  terminal-title:
    fontFamily: "Roboto Condensed"
    fontSize: "18sp"
    lineHeight: "22sp"
  pixel-label:
    fontFamily: "Monocraft"
    fontSize: "14sp"
    lineHeight: "18sp"
  vice-title:
    fontFamily: "Yellowtail"
    fontSize: "44sp"
    lineHeight: "48sp"
rounded:
  soft-panel: "3dp"
  library-input: "4dp"
spacing:
  control-gutter: "4dp"
  small: "8dp"
  medium: "12dp"
  panel: "16dp"
  atlas-edge: "18dp"
  generous: "24dp"
components:
  adaptive-launcher-icon:
    backgroundColor: "{colors.launcher-map-ink}"
    size: "108dp"
  atlas-tool:
    backgroundColor: "{colors.default-panel}"
    textColor: "{colors.default-foreground}"
    width: "64dp"
    height: "70dp"
  atlas-action:
    backgroundColor: "{colors.default-panel}"
    textColor: "{colors.default-foreground}"
    typography: "{typography.atlas-action}"
    padding: "10dp 12dp"
  atlas-action-active:
    backgroundColor: "{colors.default-foreground}"
    textColor: "{colors.default-panel}"
    typography: "{typography.atlas-action}"
    padding: "10dp 12dp"
  atlas-find:
    backgroundColor: "{colors.default-panel}"
    textColor: "{colors.default-foreground}"
    padding: "12dp 14dp"
  route-footer-action:
    backgroundColor: "{colors.default-panel}"
    textColor: "{colors.default-foreground}"
    typography: "{typography.control-caption}"
    width: "64dp"
    height: "64dp"
    padding: "2dp"
  destination-card:
    backgroundColor: "{colors.default-panel}"
    textColor: "{colors.default-foreground}"
    padding: "16dp"
  welcome-open-map:
    backgroundColor: "{colors.default-accent}"
    textColor: "{colors.library-ink}"
    height: "56dp"
  world-card:
    backgroundColor: "{colors.default-panel}"
    textColor: "{colors.default-foreground}"
    typography: "{typography.world-style-name}"
  library-field:
    backgroundColor: "{colors.library-ink}"
    textColor: "{colors.library-cream}"
    rounded: "{rounded.library-input}"
---

# Design System: Waypoint

## Overview

**Creative North Star: "Real streets through game worlds"**

Waypoint makes real navigation feel like a game atlas. The map occupies the main surface; controls borrow the selected game's composition, lettering, material and marker grammar. The user's confirmed direction is: “Keep each game's distinct control layout, but polish it.” Consistency comes from touch sizing, legibility and behavior, rather than identical control positions.

This records the implemented Android system, not a claim of exact game reproduction. The app uses authored graphics, licensed substitute fonts and real geographic data. Reference screenshots remain research evidence. Search, settings and destination flows retain native Android conventions; the atlas is the expressive surface.

**Key Characteristics:**

- Distinct map compositions for 27 game worlds; no general-purpose Classic choice.
- Game-specific cartography, materials, pointers and place markers.
- An animated native welcome screen, uniform world-card naming hierarchy and visible control captions.
- Native touch targets and readable navigation data.
- Actual streets, GPS state and route progress rather than invented gameplay.

Ground truth is `WelcomeScreen.kt`, `MainActivity.kt`, `GameHud.kt`, `GameAtlasHud.kt`, `FantasyAtlases.kt`, `ModernAtlases.kt`, `Cartography.kt`, `MapGlyphs.kt`, `WorldMap.kt`, `NavigationController.kt` and the generated style assets. Frontmatter contains reused representative tokens, not a second copy of every world palette. Android dimensions retain `dp` and `sp`; eight-digit colors use CSS `RRGGBBAA` ordering, converted from Kotlin `AARRGGBB`.

## Colors

Each atlas has a four-role HUD palette: panel, foreground, accent and muted text. Its cartography has separate land, park, water, building, road, casing, label and outline colors. HUD accent and route color are deliberately independent.

### Primary

- **Launcher route lime and arrow ivory** form the original two-facet navigation mark and bent route. These identity colors are scoped to the full-color launcher icon.
- **Default navigation green** marks shared navigation actions and selected states in the neutral dark family.
- **Western cream and scarlet** pair quiet paper controls with a red route.
- **Night cyan and yellow** distinguish technical labels from guidance over the red-building city map.
- **Terminal phosphor** carries the green monochrome interface without making every road as bright as its controls.

### Secondary

- **Slate gold** outlines the blue fantasy instrument surface.
- World-specific accents continue in `hudPalette(world)` and `World.routeColor`; do not replace them with the default green when extending a theme.

### Neutral

- **Launcher map ink and muted compass** support the arrow with a subdued abstract map and open brackets. Android supplies the colors for themed-icon rendering from the monochrome layer.
- **Library ink, cream and muted** support search, settings, saved places and the theme catalogue.
- **Default and western panels** give text a nearly opaque local backing over changing geography.
- **Daylight panel and ink** reverse the contrast for Nudle-style controls. Horizon also uses a light panel with dark text.

**The Route Separation Rule.** Road, route and selected destination must remain distinguishable in the selected world. Map color changes do not justify hiding street connectivity.

**The Paired Contrast Rule.** Choose foreground and panel as a pair from the world palette. For text directly over the map, use the explicit light or dark treatment of that atlas instead of inheriting a panel foreground blindly.

## Typography

Barlow Condensed is the default bold atlas face. Rye supplies the western stamp, Yellowtail the Vice map title, and Cinzel selected fantasy atlas titles. Monocraft, Roboto Condensed and Rajdhani separate pixel, Pip-Boy and technical interfaces. Libre Baskerville supplies readable serif HUD labels; Noto Sans supplies the daylight and contemporary controls. Normal search fields, destination details and settings use Material typography for native text behavior.

The frontmatter records observed representative roles. `AtlasText` normally uses one line with ellipsis and a line height four scaled pixels above the selected text size. Important destination names permit two lines. In both the welcome card and Worlds picker, the map-style name uses the selected HUD face at the uniform `world-style-name` scale, with the game name beneath at the smaller `world-game-name` scale. Picker titles permit two lines. That hierarchy stays constant across game families.

The active theme determines the base HUD face: Pixel World uses Monocraft; Green Terminal uses Roboto Condensed; Night City, Bridges, Manhattan and Surface Map use Rajdhani; Lands Between, The Continent, Bohemia, Highlands and Lost Heaven use Libre Baskerville; Nudle Maps, Horizon and Steelport use Noto Sans. Remaining labels use Barlow Condensed, with explicit serif, script or western overrides in their atlas composables. Share Tech Mono remains a separately bundled terminal face; it does not define the current Pip-Boy HUD.

Native road, locality and POI labels now use generated bundled SDF font ranges, not just Compose overlay fonts. `World.mapFontName()` selects condensed, technical, pixel, atlas or neutral map lettering, and `MapGlyphs.kt` resolves the bundled ranges locally. Unavailable ranges fall back to provider Noto Sans coverage. The internal stack name `Waypoint Classic` means neutral Noto Sans lettering; it is not a selectable world. `docs/map-fonts.md` records the font roles, licenses and substitution limits.

**The Lettering Identity Rule.** Preserve the selected world's display character while keeping native form text and route details readable. Do not turn every game into the same terminal or pixel font.

**The Card Hierarchy Rule.** Every launcher and picker card puts the map-style name at 22sp with 28sp line height, and the game name beneath at 12sp with 17sp line height. Keep this scale uniform while retaining each style's typeface.

## Layout

A native welcome screen precedes the idle map. Its scrollable content includes a short headline and selected-world preview, while a bottom action opens the map. Worlds and Settings remain reachable before map rendering begins. An ordinary fresh launch shows the welcome screen even if a selected place remains in memory. Active guidance and an external destination launch bypass it; merely having a selected place does not.

The normal phone atlas is a full map with edge-anchored Compose layers. Each world owns its arrangement. Shared primitives supply behavior, not a universal header and footer.

| Atlas family | Implemented arrangement |
|---|---|
| Los Santos | Slim upper tabs, right place legend, lower radar-style telemetry and recenter. |
| Old West | Centered location stamp, upper-right search, lower-left circular controls and compass. |
| Night City | Thin technical rails, upper map label, right destination actions and bottom controls. |
| Pixel World | Upper coordinate readout and a centered lower sprite hotbar. |
| Green Terminal | Connected upper tabs, local-map label and a fixed lower location baseline. |
| San Andreas / Vice Coast / Liberty City | Separate corner-title, script-and-compass and restrained tab arrangements. |
| Nudle Maps / Horizon | Light panels, distinct search or racing controls and colored location marks. |
| The Continent / Sheikah Slate / Skyrim | Respectively dark atlas bands, geometric slate bands and a sparse low toolbar. |
| Lands Between / Bohemia / Highlands | Distinct atlas labels, ornament, compass positions and lower actions. |
| Animus / Hope County / Tsushima / The Embrace | Different tab, location, filter and compass arrangements over terrain. |
| Bridges / Manhattan / Surface Map | Left data or tool rails, technical framing and different lower waypoint controls. |
| Steelport / Medici / Lost Heaven | Tablet-style left locations, tactical lower-right actions, or printed upper/lower bars. |

Every map icon action includes a visible caption. `AtlasTool` is a 64dp-wide clickable column with a minimum 70dp height and a 4dp outer gutter; its icon plate is 48dp square with 21dp artwork. Its caption uses Roboto Condensed at the `control-caption` scale, with zero tracking, one line, no soft wrapping and ellipsis on overflow. It sits on its own theme-backed surface. The column merges its semantics into one button. Action rows and search launchers have a minimum 48dp height plus their 4dp outer gutter. Pixel hotbar slots are 64dp wide with a 56dp sprite plate and the same Roboto Condensed caption treatment; the artwork remains pixel-rendered. The location icon caption is Locate. These are code dimensions, not an approval of any clipping encountered in QA.

Below 420dp of available atlas height, `CompactAtlas` provides the implemented compact arrangement. The main screen uses a navigation rail at 600dp width. Selected-place and guidance layouts split left and right below 500dp available height; the lower panel can scroll up to 300dp high. These are current native adaptations, not evidence of completed tablet testing.

Route-preview camera fitting uses the measured map, header and destination-panel bounds from `onGloballyPositioned` in root pixels. `MapViewportInsets` converts those measurements into the unobscured area with 28dp of breathing room and system-inset handling. Portrait uses the clear middle between panels; compact landscape uses the area below the left toolbar and beside the destination panel. The fitted bounds include the route geometry, actual GPS origin and selected destination. Route overview explicitly sets bearing and tilt to zero, keeping the overview north-up and flat rather than inheriting the world's idle camera perspective. Fitting waits for usable measurements and responds to measured-inset, layout and style changes, rather than fixed pixel padding. This describes the implementation, not a visibility guarantee across untested layouts.

Insets reserve the status and gesture areas. A compact, labelled Map info button at the lower edge opens provider attribution; a persistent wide attribution footer is no longer part of the composition. Selected-place and active-guidance states prioritize the next maneuver and destination actions; they share a navigation arrangement colored by the current theme. Worlds is available in idle, preview and active guidance. Choosing a world changes the appearance without clearing the selected destination or route.

**The Distinct Layout Rule.** Keep the game-specific idle composition. Improve spacing and reachability inside that composition before moving controls into a common shell.

**The Visible Action Rule.** Map icon actions need short visible captions as well as semantic descriptions. Use Search, Locate, Worlds, Settings, Map tilt, Mute or Sound on rather than requiring users to infer the icon alone.

## Elevation & Depth

HUD panels primarily use flat color, opacity, borders and local contrast. They do not rely on large ambient shadows. Depth belongs to real geographic relief and selected building extrusions. Night City, Pixel World and Manhattan use different map camera and building treatments; parchment atlases remain flat.

The paper fibre and pixel grass, stone and water textures are authored raster assets. Pixel controls use discrete sprites and nearest-neighbor rendering. Canvas ornament supplies geometric corners, bands and compass details; it is not a replacement for a hand-painted fictional map.

The welcome content enters with a 360ms fade and 420ms upward slide, travelling only 3.5% of its height. Changing the selected welcome world uses a 240ms crossfade. The map pointer and heading rose animate through the shortest arc over 120ms. Native camera following eases over 500ms; explicit recentering uses 700ms. Motion belongs to state changes rather than an always-running decorative loop. It must not imply a heading is known when sensor confidence is insufficient.

Scrolling uses native scroll containers and stable keyed world rows. `MapSwatch` caches and batches thumbnail paths through `drawWithCache`. Its `onDrawBehind` drawing is enclosed by `clipRect`, containing the ground, buildings, streets and route within the preview bounds rather than drawing into the description row. The welcome screen does not create the native map. Once created, a hidden MapView is paused/stopped behind the picker, search and modal surfaces, while preserving navigation state; map updates resume when visible. Heading samples continue into the arbiter while lists or welcome content cover the map, but heading-driven UI emissions are gated unless the map, sensor settings or car display needs them. Returning to a visible heading surface publishes the current reading. When reliability, source and status are unchanged, bearing changes smaller than 0.3 degrees do not trigger a new UI state. These are implemented performance measures, not a measured frame-rate claim.

## Shapes

Most atlas panels are square. Night City, Midnight Run and Manhattan use opposite cut corners; Nudle Maps, Horizon, Old West and The Continent soften panels slightly. Circular location controls remain part of the western family; pixel slots retain their bevelled, rectilinear treatment. The welcome action and selected-world preview use opposite cut corners, while the picker cards use the small library radius.

Theme borders are usually 1dp. Pixel frames are heavier. Gold corner ornament appears in the slate and ornate atlas families; technical families use short corner rails. These shapes belong to the corresponding world rather than becoming decoration on every screen.

## Components

### Adaptive launcher icon

The launcher mark is an original faceted navigation arrow, bent route with an origin dot, and two open compass brackets. The abstract map sits in a separate background layer. Foreground and monochrome vectors use a 108dp canvas and a centered 0.88 scale to keep the mark inside the adaptive-icon safe area. Android owns the final mask. The full-color arrow uses the launcher ivory and lime; the monochrome layer preserves the arrow, route and brackets as one tintable silhouette, omitting the map texture and facet colors. Material You themed colors are supplied by supported Android launchers. This identity component does not replace a world’s HUD, map palette or position marker.

### Atlas controls

`AtlasTool` uses an icon plate and visible caption as one semantic button, with native click feedback, the selected shape and paired foreground/panel colors. The light variant reverses the icon plate colors while the caption retains the theme panel and foreground. `AtlasAction` provides a labelled action; its active state also reverses the pair. `AtlasFind` is a search launcher with a search icon, label and trailing chevron, not an editable field. Its default visible label is Search places, at 16sp/21sp in the world HUD face, with up to two lines.

### Search and transport

The Worlds picker and destination search use native outlined text fields. Destination search retains the library background and Material text, with the selected world’s field/button shape and place-icon accent. The “Place or address” field supports the keyboard Search action; a visible “Search places” button is enabled after two trimmed characters. Submission hides the keyboard. Result rows allow two name lines and three address lines, with ellipsis and a separate provider label; distance appears when a location fix exists. A Plus-Code-only Android response is labelled “Address result for …” with “Android geocoder · address match,” keeping an address candidate distinct from a verified named place. The picker says “Find a game” and lists only the 27 game variants. Car, Motorcycle and Walk are real routing selections rendered as three equal-width radio choices, each with an icon, label and active underline. Their row has a minimum 52dp height. Keyboard, Back and scrolling behavior stay native. The synthetic map swatch is a preview illustration; the welcome card labels it “Style preview.” It is not proof of the actual geographic view or full HUD composition.

### Tabs and panels

`AtlasTabs` distributes MAP, Worlds and Saved across the row, with Settings as a captioned icon control. Text tabs share the row height with a minimum 56dp height. The current map tab reverses the active color pair. Destination cards use theme surfaces, route status, a visibly labelled Save/Saved action and a full-width start action. The normal portrait card uses 16dp padding, 12dp gaps, Material `titleLarge` and an address of up to two lines. Its compact-height variant uses 12dp padding, 8dp gaps, `titleMedium` and a one-line address; the destination name still permits two lines. Compact route preview replaces the vertical location control with a horizontal Locate action of at least 48dp height. These compact choices reserve more space for Start navigation without changing the portrait card. The Save/Saved action is 64dp wide, at least 56dp high and has 2dp content padding; its Roboto Condensed caption uses 12sp/16sp, zero tracking, one line and no soft wrapping. The preview Back, ROUTE, Worlds and Settings toolbar sits above the card on the selected theme's panel backing and shape. Guidance uses a maneuver panel and route footer with remaining time, distance, arrival, and labelled End trip and Steps actions; those footer actions are 64dp wide, have a minimum 64dp height and use 2dp content padding. Their End trip and Steps captions use Roboto Condensed at 12sp/16sp, with zero tracking, one line and no soft wrapping.

### Compass and position

`CompassRose` now uses the reliable heading selected from phone sensors or GPS course. Its fixed top index represents forward travel; the rose rotates beneath it, and the center shows the cardinal direction and degrees. Unavailable heading shows a dash and does not draw a misleading direction. The whole compass is a recenter button with an accessible heading description. Its standard size is 72dp; the ornate variant is 100dp. Cardinal text uses 15sp/18sp in the standard variant and 20sp/24sp in the ornate variant; degree text uses 11sp/14sp. Both use zero tracking. It is not a map-orientation dial.

The map's position marker uses the selected world's arrow, teardrop, chevron, pixel pointer or ring. Unknown heading uses a dot. The accuracy area represents the current measured radius rather than a fixed decorative halo. Map-relative marker rotation remains separate from the heading readout.

**The Reliable Heading Rule.** Show direction only when the sensor/course selection is reliable. Use the unavailable state instead of a frozen or fabricated bearing, and do not substitute map rotation for the live heading.

### Welcome and world selection

The welcome page keeps Open map visible below its scrollable content. The selected-world card and Explore all game worlds action open Worlds. The full-screen picker has a native Back control and keyed lazy rows. Selecting a world returns to the previous map or welcome context and leaves the route intact. Its 22sp/12sp naming hierarchy matches the launcher card. Current previews illustrate appearance; they do not claim live geographic accuracy.

### Map information

Map info is a small labelled text-and-icon control with a minimum 48dp touch height. It opens a native dialog with map-data, tile and conditional terrain credits, rather than placing a broad footer over the map.


### Cartography

`cartography_profiles.py` governs each world's road weight, casing, building treatment, labels, park opacity, terrain and paper. `Cartography.kt` governs initial camera, route width, POI scale and marker families. `generate_styles.py` preserves the underlying road geometry and native camera-expression grammar. Runtime overlays place the route, destination, accuracy area and position marker above the basemap. Night City walking guidance is dotted yellow; Steelport changes from a purple atlas route to dotted green active guidance with a yellow player marker. These are state-specific treatments backed by the researched screenshots.

## Do's and Don'ts

### Do:

- **Do** preserve the selected world's layout, material and lettering while improving contrast and spacing.
- **Do** keep real roads, route guidance and the position marker legible together.
- **Do** retain native touch targets, semantic labels, insets and system navigation behavior.
- **Do** consult the actual map or navigation state in `docs/visual-sourcebook.md` before changing a theme.
- **Do** show only reliable phone heading or travel course in the compass, with a dash when unavailable.
- **Do** keep world-card title sizes uniform and visible captions on map icon controls.
- **Do** retain native scrolling, cached preview paths and paused hidden map rendering.

### Don't:

- **Don't** reduce all themes to one shared layout with changed colors.
- **Don't** reintroduce a non-game world or cancel navigation when changing a world.
- **Don't** add invented health, money, missions or other gameplay data to navigation.
- **Don't** distort geography or hide roads to imitate fictional terrain.
- **Don't** treat posters, cinematics or decorative treasure maps as interchangeable with navigable map references.
- **Don't** describe substitute fonts, authored materials or mobile adaptations as exact proprietary game assets.

This file records current reusable rules. It does not canonize unresolved visual fidelity, clipped text, contrast defects, stale captures or unverified device behavior; those belong in review and validation records, including `docs/validation.md`.
