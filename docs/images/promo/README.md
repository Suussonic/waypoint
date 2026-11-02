# Waypoint promotional artwork

Original promotional compositions built from sanitised Waypoint Android captures at a fixed public demonstration position. Finished README images are kept here; the source captures and temporary previews stay outside Git. The screens are framed and cropped for presentation; their controls and map contents have not been fabricated.

- **Source screens:** six local demonstration JPGs named `los-santos.jpg`, `old-west.jpg`, `night-city.jpg`, `rivia.jpg`, `hyrule.jpg` and `pixel.jpg`. Remove device status and navigation bars, personal routes and image metadata before using them.
- **Identity:** the original `waypoint-icon.png`, rendered from the Android adaptive icon.
- **Typography:** Barlow Condensed and Noto Sans from `app/src/main/res/font`, under their existing [font licenses](../../licenses/).
- **Generator:** `python3 scripts/create_readme_promo.py --captures /path/to/sanitised-captures` from the repository root; requires Pillow. The capture folder must be outside the repository or under ignored `docs/images/captures/`.
- **Responsive art:** the README uses wide posters on desktop and portrait compositions below 640px. Only the finished images used by the README are committed.
- **Privacy:** no user-provided routes, place queries, device identifiers, notification icons, home/work locations or personal screenshots belong in these assets. Use fixed public landmarks or fictional fixtures for public examples. Keep private QA captures outside Git.

The artwork is not supplied by, or affiliated with, any referenced game or publisher. Game names identify inspiration only. OpenStreetMap/OpenFreeMap attribution is available inside the app through Map info and in the root README.
