# POI atlas visual audit

These contact sheets show every checked-in daytime atlas next to its night
variant. Each pair has all 49 live category cells in the order of
[`poi_categories.json`](../app/src/main/assets/poi_categories.json); the last
grid cell is intentionally empty. The sheets are generated from runtime PNGs
with `python3 scripts/generate_poi_contact_sheets.py`.

| Sheet | Worlds |
| --- | --- |
| [01](assets/poi-audit/contact-01.webp) | Aincrad, Assassin, Bridges, Counter Strike, CTOS, Defcon |
| [02](assets/poi-audit/contact-02.webp) | Division, DOS Atlas, Far Cry, Field Ops, Ghost, Green Terminal |
| [03](assets/poi-audit/contact-03.webp) | Heat, Hogwarts, Home Console, Horizon, Horizon Wilds, Hyrule |
| [04](assets/poi-audit/contact-04.webp) | Just Cause, Kingdom, Lands Between, Liberty, Los Santos, Mafia |
| [05](assets/poi-audit/contact-05.webp) | Machine Atlas, Night City, Old West, Pixel, Retro Quest, Rivia |
| [06](assets/poi-audit/contact-06.webp) | Saints, San Andreas, Sea Chart, Skyrim, Spider, Stalker |
| [07](assets/poi-audit/contact-07.webp) | Starfield, Vice Coast |

The 36 ImageGen world sheets have reviewed row and column boundaries in
[`poi-crop-manifest.json`](../tools/imagegen/poi-crop-manifest.json). It also
records the original sheet SHA-256 hashes and provenance for 92 cell overrides.
The override PNGs are tracked in [`poi-overrides`](../tools/imagegen/poi-overrides).
Machine Atlas and Night City use their dedicated generators.

Install the pinned renderer with
`python3 -m pip install -r tools/imagegen/requirements.txt`. To check the
checked-in assets, run `python3 scripts/validate_map_assets.py` and
`python3 scripts/import_imagegen_poi_cell_repairs.py --check`. When the ignored
original sheets are available, also run
`python3 scripts/build_poi_overrides.py --check` to compare every override
with its documented source recipe.
