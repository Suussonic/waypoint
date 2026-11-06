# Development layout

Waypoint is one Android application module. Kotlin source files are grouped by feature under `app/src/main/java/com/waypoint/app/`; their existing `com.waypoint.app` package remains stable, including Android manifest and test entry points.

| Folder | Responsibility |
| --- | --- |
| `core/` | Application entry point, shared state model and JSON safety |
| `map/` | Live map, cartography, textures, POI categories and map motion |
| `navigation/` | Routing, guidance, route preview, sensor heading and sharing |
| `car/` | Android Auto service, projected map, templates and controls |
| `worlds/` | World catalogue, map themes, HUD variants and previews |
| `onboarding/` | Welcome flow, globe and first-run choices |
| `places/` | Search and saved destinations |
| `quests/` | Quest storage, reminders, progression and screens |
| `voice/` | Recorded cues, speech selection and audio decoding |
| `updates/` | Release checks, alerts and update installation |
| `feedback/` | Feedback forms and issue links |
| `ui/` | Shared colors, type scale and named interface artwork |

## Assets

- `app/src/main/assets/styles/` contains Waypoint-authored MapLibre style JSON: day and night for every world, plus the Dark Zone holographic variants. MapLibre renders them; no sample style or preset is bundled.
- `app/src/main/assets/poi/`, `map-markers/`, `map-materials/` and `map-textures/` contain the runtime place symbols, destination markers and map materials.
- `app/src/main/res/drawable-nodpi/` contains Android image resources, including descriptively named `waypoint_*` control and world-card images. Android requires drawable resources directly in a qualified resource directory, so the filename prefix keeps these groups identifiable.
- `app/src/main/res/raw/` holds only videos referenced by the app. Optional Remotion source lives in `tools/waypoint-motion/`.
- `docs/images/promo/` holds finished README compositions. Raw device captures and temporary image work stay outside the tracked tree.

Asset recipes are in `scripts/` and `tools/`. The shipped app builds from the checked-in assets; regeneration tools are optional. See [artwork notes](../game-artwork-sources.md), [map icon notes](../poi-icon-sources.md) and [promotional image notes](../images/promo/README.md).

## Checks

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
python3 scripts/check_repo_hygiene.py
node scripts/validate_styles.cjs
```

`python3 scripts/validate_map_assets.py` validates the generated images and needs Pillow in the optional artwork environment. Device tests run against the isolated Preview package; see the [Android Auto test guide](../testing/android-auto.md) and [validation record](../validation.md).
