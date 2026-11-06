# Waypoint promotional images

The finished JPGs in this folder are composed from Waypoint captures at public London demonstration positions. Working captures stay outside the tracked repository; the app screens are framed, cropped and resized for presentation without changing their map contents or controls.

- **Phone maps:** `ReadmeScreenshotsTest` captures Los Santos, Old West, Night City, The Continent, Sheikah Slate, Pixel World and Exclusion Zone from the current app. Its fixed public road point keeps the vehicle marker on the mapped street. The device status and navigation bars are cropped before making the posters.
- **Android Auto:** the Exclusion Zone panels show the current app in the Desktop Head Unit at two simulated points on the same public London route. Both captures show the vehicle on the route. They demonstrate the rendered host screen, not a physical drive or the phone's location.
- **Identity and type:** the poster uses Waypoint's launcher icon and the Barlow Condensed and Noto Sans fonts supplied with the app. Their licenses are in [`docs/licenses`](../../licenses/).

From the repository root, stage the sanitized phone JPGs and the two DHU PNGs in ignored local folders, then run:

```sh
python3 scripts/create_readme_promo.py --captures /path/to/phone-captures --car-captures /path/to/car-captures
python3 scripts/create_car_readme_promo.py --captures /path/to/car-captures
python3 scripts/check_repo_hygiene.py
```

The optional image composition scripts require Pillow; the Android build does not. The README uses wide posters on desktop and portrait versions below 640px. Keep personal routes, queries, notifications, identifiers and device-derived locations out of public media. The games named in the README identify stylistic inspiration and do not imply affiliation.
