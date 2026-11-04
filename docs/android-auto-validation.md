# Android Auto integration validation

Waypoint is a projected navigation app built with the Android for Cars App Library.
Android Auto owns launcher eligibility. Developer mode and Unknown sources cannot force
this class of app into a real vehicle's launcher. Use a trusted Play testing channel for
real-car testing, and Google's Desktop Head Unit (DHU) for local development.
See [Google's testing requirements](https://developer.android.com/training/cars/testing).

## Implementation

- `WaypointCarSession` owns connection lifetime. Menu changes and temporary loss of
  visibility do not disconnect it. Session identities prevent a late disconnect from
  clearing a newer connection.
- The map publishes navigation start, progress and end to the host. Destruction clears
  its callback, and a new map republishes ongoing phone guidance.
- Incoming navigation proposals return to the root confirmation screen. Active-trip
  protections, lifecycle guards and proposal/session tokens still apply.
- Location denial has a parked-only permission action and an explicit refresh action.
- Native toggles reflect current phone preferences and ignore stale callbacks.
- Worlds use six categories with nested groups of six or fewer rows. Saved/recent places use the host row
  budget, retaining all places through nested groups. Search explains empty/error states.
- Phone setup presents connection and precise-location status, a host-settings shortcut,
  and the actual distribution requirement.
- Host certificate validation remains enabled. No installer spoofing, permissive host
  validator, forced foreground takeover or hidden exported component was introduced.

## Repeatable checks

Use an unlocked development phone and reserve it exclusively for these tests. The Preview
package is distinct from the release package. Device tests use synthetic public locations;
they are not road tests. No release credentials are required.

```sh
./gradlew -PtestBuildType=preview :app:assembleDebug :app:assemblePreview \
  :app:assemblePreviewAndroidTest :app:testDebugUnitTest :app:lintDebug
python3 scripts/check_packaged_manifest.py debug
adb install -r app/build/outputs/apk/preview/app-preview.apk
adb install -r app/build/outputs/apk/androidTest/preview/app-preview-androidTest.apk
adb shell am instrument -w -r \
  -e class com.waypoint.app.CarSessionTest,com.waypoint.app.CarCompatibilityTest,com.waypoint.app.CarMenusTest,com.waypoint.app.CarNavigationSyncTest,com.waypoint.app.NavigationJourneyTest \
  com.waypoint.app.preview.test/androidx.test.runner.AndroidJUnitRunner
```

Run permission recovery separately, because revoking a runtime permission kills the
app process. This changes only the development Preview package. Record its original
permission grants first and restore those grants after testing.

```sh
adb shell pm revoke com.waypoint.app.preview android.permission.ACCESS_FINE_LOCATION
adb shell pm revoke com.waypoint.app.preview android.permission.ACCESS_COARSE_LOCATION
adb shell am instrument -w -r \
  -e class com.waypoint.app.CarPermissionRecoveryTest \
  com.waypoint.app.preview.test/androidx.test.runner.AndroidJUnitRunner
```

The adaptive phone settings test temporarily changes display size, density, rotation and
font scale, restores them in `finally`, and saves private captures under the Preview app's
external files directory `android-auto-review/`:

```sh
adb shell am instrument -w -r \
  -e class com.waypoint.app.AndroidAutoSettingsUiTest \
  com.waypoint.app.preview.test/androidx.test.runner.AndroidJUnitRunner
```

The opt-in host journey requires an operator at the DHU. It fetches a public London route,
seeds app-local simulated input, backgrounds the phone activity, and waits for actual host
Start and End actions. It verifies Ferrostar progress and the foreground service itself
(not notification visibility, which depends on notification permission). It does not set
global mock location. Instrumentation can affect background execution eligibility, so this
is not proof of every Android version's real-vehicle background policy.

```sh
adb shell am instrument -w -r -e hostJourney true \
  -e class com.waypoint.app.CarHostJourneyTest \
  com.waypoint.app.preview.test/androidx.test.runner.AndroidJUnitRunner
```

Check instrumentation's `OK (N tests)` result: ADB's exit code alone does not establish
that tests passed. Do not run the opt-in app-local camera fixtures during other tests.

## Device and host acceptance

Record results for launcher discovery, permission denial/recovery, search and saved places,
route preview, actual Start while the phone activity is backgrounded, GPS loss and route
failure, voice/unit synchronization, switching worlds, incoming intent confirmation,
phone guidance after disconnection, and reattachment. Inspect small and wide DHU layouts,
day/night and rotary operation. DHU success does not establish real-vehicle eligibility,
wireless reliability, driving performance, or compatibility with every manufacturer's host.

Current execution results and remaining limitations are recorded in `docs/validation.md`.
