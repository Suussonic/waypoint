# Build and Contribute

## Build requirements

- JDK 17
- Android SDK Platform 36
- Android Build Tools 35.0.0
- A connected Android device or emulator only when you choose to run device tests

Clone the repository and set `ANDROID_HOME` or `sdk.dir` in the ignored `local.properties` file. Then run:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

For Android Auto development, use the Desktop Head Unit and follow the [validation guide](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/testing/android-auto.md).

## Contribute safely

- Keep route geometry, Ferrostar maneuver data, ETA and rerouting independent of map themes.
- Keep public screenshots and fixtures on fixed demonstration places. Never commit personal coordinates, routes, captures or device identifiers.
- Do not add secrets, signing material, API keys or local APKs to Git.
- Give each sourced font, image, audio file and dataset its own verified license and attribution record. A repository-level license does not automatically cover files at linked external locations.
- Run the project checks before opening a pull request and describe the exact Android/device coverage you performed.

Read [CONTRIBUTING.md](https://github.com/the-geeky-couldron/waypoint/blob/dev/CONTRIBUTING.md), [build and release details](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/releasing.md) and [validation results](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/validation.md).
