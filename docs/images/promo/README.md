# Waypoint promotional artwork

Original promotional compositions built from sanitised Waypoint Android captures at a fixed public London demonstration position. Finished README images are kept here; the source captures and temporary previews stay outside Git. The screens are framed and cropped for presentation; their controls and map contents have not been fabricated.

- **Phone screens:** six local demonstration JPGs named `los-santos.jpg`, `old-west.jpg`, `night-city.jpg`, `rivia.jpg`, `hyrule.jpg` and `pixel.jpg`. The opt-in `ReadmeScreenshotsTest` captures fixed London map states; crop the device status and navigation bars, then strip metadata before generating the posters. The 1.1.0 posters use fresh captures from the connected phone.
- **Android Auto screens:** `old-west-guidance.png` and `night-city-guidance.png` are actual 1280 × 720 Desktop Head Unit screenshots. The opt-in `CarReadmeCaptureTest` feeds a public London route and simulated guidance state to the projected Waypoint session, then switches the phone-selected world. The screenshots show the host controls and the app's real map/template rendering; they do not represent a physical drive or the phone's location.
- **Identity:** the original `waypoint-icon.png`, rendered from the Android adaptive icon.
- **Typography:** Barlow Condensed and Noto Sans from `app/src/main/res/font`, under their existing [font licenses](../../licenses/).
- **Generators:** `python3 scripts/create_readme_promo.py --captures /path/to/sanitised-phone-captures` and `python3 scripts/create_car_readme_promo.py --captures /path/to/dhu-captures` from the repository root; both require Pillow. The capture folders must be outside the repository or under ignored `docs/images/captures/`.
- **Responsive art:** the README uses wide posters on desktop and portrait compositions below 640px. `android-auto.jpg` and `android-auto-mobile.jpg` show two worlds on the same simulated route. Only the finished images used by the README are committed.
- **Privacy:** no user-provided routes, place queries, device identifiers, notification icons, home/work locations or personal screenshots belong in these assets. Use fixed public landmarks or fictional fixtures for public examples. Keep private QA captures outside Git.

The artwork is not supplied by, or affiliated with, any referenced game or publisher. Game names identify inspiration only. OpenStreetMap/OpenFreeMap attribution is available inside the app through Map info and in the root README.
