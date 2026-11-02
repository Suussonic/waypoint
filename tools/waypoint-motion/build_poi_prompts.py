"""Write one imagegen prompt per world-specific 4x3 map icon atlas."""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
STYLES = {
    "los_santos": "Grand Theft Auto V inspired monochrome atlas blips: blunt white silhouettes, narrow charcoal shadow, tiny controlled lime accent",
    "old_west": "Red Dead Redemption 2 inspired western map engraving: aged ivory and dark umber silhouettes with slightly irregular hand-inked edges",
    "night_city": "Cyberpunk 2077 inspired street interface: angular cyan linework on very dark navy with restrained electric yellow highlights",
    "pixel": "Minecraft inspired 16 by 16 pixel-art sprites: crisp square pixels, no antialiasing, muted block-world colors and dark one-pixel keyline",
    "green_terminal": "Fallout Pip-Boy inspired phosphor screen: luminous single-color green line icons, terminal scanline character, dark green keyline",
    "san_andreas": "Grand Theft Auto San Andreas inspired 1990s urban map blips: bold off-white block silhouettes and black outlines with a little muted orange",
    "heat": "Need for Speed Heat inspired nocturnal racing HUD: high-contrast magenta and electric cyan pictograms, angular street-sign geometry",
    "rivia": "The Witcher 3 inspired dark-fantasy map marks: weathered cream woodcut symbols, moss-green and oxblood accents",
    "vice_coast": "Vice City inspired 1980s coastal pictograms: Miami pastel pink, aqua and cream screen print with crisp dark outlines",
    "liberty": "Grand Theft Auto IV inspired city atlas blips: cool ivory and amber silhouettes with sturdy black stroke",
    "ctos": "Watch Dogs 2 inspired playful civic-tech map marks: flat coral, sky blue and tangerine with precise white interior cuts",
    "hyrule": "Breath of the Wild Sheikah Slate inspired glyphs: luminous teal etched linework, faded gold corners, ancient mechanical precision",
    "skyrim": "The Elder Scrolls V Skyrim inspired compass icons: weathered silver-white engraved silhouettes, cold graphite contour, Nordic restraint",
    "lands_between": "Elden Ring inspired cartographic marks: antique gold and parchment line engraving, weathered umber depth, compact rune-like framing",
    "horizon": "Forza Horizon inspired bright road-map pictograms: vivid hot pink, cerulean and white with clean racing-sign silhouettes",
    "assassin": "Assassin's Creed Origins inspired ancient Egyptian map marks: sand-gold pictograms, ink-black contour, subtle geometric carving",
    "far_cry": "Far Cry 5 inspired rural American map marks: weathered cream, pine green and faded red screen-print silhouettes",
    "ghost": "Ghost of Tsushima inspired ink-brush atlas symbols: ivory wash, black sumi contour, one scarlet stamp accent",
    "bridges": "Death Stranding inspired sensor display: cold cyan thin-line pictograms, blue-gray technical framing, extremely clear centers",
    "horizon_wilds": "Horizon Zero Dawn inspired machine-wilderness map marks: ivory linework, pale copper and teal details, carved tribal geometry",
    "spider": "Marvel's Spider-Man inspired comic city-map pictograms: punchy white-red-blue silhouettes with hand-inked comic contour",
    "hogwarts": "Hogwarts Legacy inspired old academy atlas: parchment gold and navy engraved icons, restrained brass filigree",
    "starfield": "Starfield inspired space survey display: ivory and orbital blue technical pictograms with hairline orange mission accent",
    "kingdom": "Kingdom Come Deliverance II inspired medieval manuscript marks: hand-inked umber woodcut shapes, pale vellum and wheat-gold highlights",
    "saints": "Saints Row The Third inspired street HUD: saturated violet, hot pink and white bold stencil silhouettes",
    "just_cause": "Just Cause 3 inspired Mediterranean tactical atlas: turquoise, sandy cream and terracotta military-map pictograms",
    "mafia": "Mafia Definitive Edition inspired 1930s city atlas: sepia and ivory art deco pictograms with charcoal print grain",
}

SUBJECTS = (
    "map pin", "fork and plate for food", "coffee cup", "pub drink glass",
    "bed for lodging", "fuel pump", "parked car in bay", "shopping bag",
    "landmark monument", "medical cross", "tree", "stack of coins",
)


def main() -> None:
    output = ROOT / "docs/poi-world-prompts.jsonl"
    with output.open("w") as stream:
        for world, visual in STYLES.items():
            prompt = (
                "Use case: stylized-concept. Asset type: original Waypoint navigation-map "
                "icon sprite sheet, 4 columns by 3 rows, exactly twelve square cells, "
                "equal spacing, no margins between cells. Reference role: the linked "
                "GTA icon comparison image, Minecraft map icon texture category, and "
                "Red Dead icon sheet supply visual grammar and small-size readability; "
                "draw fresh symbols rather than pasting source pixels. "
                f"World treatment: {visual}. "
                "Draw one centered icon in each cell, left-to-right top-to-bottom, "
                "in this exact order: " + "; ".join(SUBJECTS) + ". "
                "Every icon should be a single distinct silhouette, with 15 percent "
                "empty padding inside its cell, clear at 32 physical pixels. "
                "Use hand-directed shape choices, restrained texture, and consistent "
                "stroke weight. This is map UI artwork, not a scene or a mockup. "
                "Entire background must be perfectly uniform pure chroma magenta "
                "#FF00FF, including space between icons, for later alpha removal. "
                "No tile backgrounds, no boxes, no grid lines, no labels, no letters, "
                "no game logos, no characters, no watermarks, no duplicate icons."
            )
            job = {
                "prompt": prompt, "use_case": "stylized-concept",
                "model": "gpt-image-2", "size": "1024x768",
                "quality": "high", "output_format": "png",
                "out": f"poi_{world}.png",
            }
            stream.write(json.dumps(job) + "\n")
    print(output)


if __name__ == "__main__":
    main()
