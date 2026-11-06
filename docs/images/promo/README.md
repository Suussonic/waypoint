# Waypoint promotional images

The finished JPGs in this folder are composed from Waypoint captures at public London demonstration positions. Working captures stay outside the tracked repository; the app screens are framed, cropped and resized for presentation without changing their map contents or controls.

- **Phone maps:** `ReadmeScreenshotsTest` captures Los Santos, Old West, Night City, The Continent, Sheikah Slate, Pixel World, Exclusion Zone and Machine Atlas from the current app, always in the light map theme. Run it with `-e demo heath -e worlds OLD_WEST` for the Hampstead Heath hero capture, where woodland shows. Its fixed public road point keeps the vehicle marker on the mapped street. The device status and navigation bars are cropped before making the posters.
- **Android Auto:** the Old West panels show the current app in the Desktop Head Unit, in the light map theme, at two simulated points on a public Hampstead Heath route. `CarReadmeCaptureTest` ignores the phone's real GPS and feeds its own fixes through the real route, Start and guidance flow; run the DHU with a single-pane layout (for example `dpi = 213` at 1280x720) so no media card appears. Both captures show the vehicle on the route. They demonstrate the rendered host screen, not a physical drive or the phone's location.
- **Identity and type:** the poster uses Waypoint's launcher icon and the Barlow Condensed and Noto Sans fonts supplied with the app. Their licenses are in [`docs/licenses`](../../licenses/).

From the repository root, stage the sanitized phone JPGs (including `old-west-hero.jpg`) and the two DHU PNGs (`old-west-before-turn.png`, `old-west-after-turn.png`) in ignored local folders, then run:

```sh
python3 scripts/create_readme_promo.py --captures /path/to/phone-captures --car-captures /path/to/car-captures
python3 scripts/create_car_readme_promo.py --captures /path/to/car-captures
python3 scripts/check_repo_hygiene.py
```

The optional image composition scripts require Pillow; the Android build does not. The README uses wide posters on desktop and portrait versions below 640px. Keep personal routes, queries, notifications, identifiers and device-derived locations out of public media. The games named in the README identify stylistic inspiration and do not imply affiliation.
