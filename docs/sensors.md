# Navigation sensors

Waypoint requests precise location and uses the hardware capabilities available on the phone. No sensor can guarantee perfect navigation; confidence is exposed instead of concealing uncertainty.

- **Fused position:** Google Play services high-accuracy fused location, requested every 1 second (fastest 500 ms, no batching). The provider combines available GNSS, Wi-Fi, cellular and its internal sensor processing. Waypoint does not control or claim which inputs contributed to an individual fix. Initial accurate-fix preference is enabled.
- **Fallback:** Android LocationManager GPS and network providers when Google Play services is missing, fails to start, or supplies no recent accurate fix for eight seconds. Both streams are timestamp-ordered; stale network fixes cannot replace a recent GNSS fix. Fine-location permission remains required.
- **Orientation:** Android `TYPE_ROTATION_VECTOR`, which normally fuses gyroscope, accelerometer and magnetometer. If absent, geomagnetic rotation vector, then accelerometer plus magnetometer. No compass hardware means GPS course only. Game rotation vector is deliberately not used because it lacks a stable north reference.
- **True north:** Android GeomagneticField corrects magnetic declination using current coordinates, altitude when available, and date. The model updates after moving 10 km. Without a location fix the heading is labelled magnetic north.
- **Course:** car/motorcycle enters GPS course at 2.5 m/s and exits below 1.5 m/s; walking uses 1.1 / 0.65 m/s. Bearing and speed confidence must also pass. Course expires after 2.5 seconds, and reacquisition must pass the entry threshold again.
- **Stability:** compass changes follow the shortest arc across 0/360°, with smoothing and a maximum UI update rate of 10 Hz. Low confidence displays an unavailable heading and a position dot. Upright projection blends the top edge with the rear-camera axis instead of freezing near vertical.
- **Position confidence:** fixes older than 15 seconds and out-of-order fixes are discarded. Navigation starts/advances only with reported horizontal accuracy at most 60 m. Losing updates marks the location stale and removes the live speed value. A noisy coordinate is never extrapolated from raw accelerometer integration.
- **GNSS visibility:** Android's GNSS status callback reports satellites visible and used in the fix, when available. This is shown with provider, sensor type, compass status and position accuracy in Settings.
- **Power/lifecycle:** location and sensor listeners stop when both phone/car screens are hidden and navigation is inactive. Active navigation uses the Android location foreground service. No step-counter permission or background-location permission is requested.

Verified on the connected Samsung: Android reported Waypoint's rotation-vector subscription at 20 ms; fused location returned a reported ±4 m stationary position. Physical turning, magnetic interference, tunnels, urban canyons and road-speed transitions require field testing.

Implementation references: [Android position sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_position), [FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient), [LocationRequest.Builder](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest.Builder).

## Pointer correction pass

The pointer uses a dedicated HeadingArbiter. Stationary and slow movement use a fresh, calibrated compass; moving car/motorcycle and confident walking fixes use GPS course with speed/bearing accuracy checks. Separate entry/exit thresholds prevent source chatter, course expires after 2.5 seconds, and compass samples expire after 2 seconds. An uncertain heading renders a non-directional dot, never a falsely confident frozen arrow.

Phone top-edge projection blends continuously with its rear-camera axis when upright, avoiding the previous near-vertical azimuth freeze. Display rotation changes reset the filter and remap sensor axes. Magnetic declination still corrects to true north. Markers are authored with their zero-degree tip pointing upward, including the western and pixel pointers. Rotation animates over the shortest arc in 120 ms; real position and accuracy geometry update independently from heading. The accuracy area uses reported metres, not a fixed pixel halo. Heading-up is the default: while stationary the map follows the reliable phone compass, and while moving it follows GPS course. North-up can be selected with the compass control.

Thirteen pure tests cover upright and flat pitch, cardinal angles, invalid matrices, stationary course rejection, walking course, noisy speed/bearing, source precedence, hysteresis, stale data and accuracy radius across latitudes. Android's actual rotation-vector listener was observed on the connected Samsung. A controlled physical rotation / road test has not been performed, so no absolute compass accuracy claim is made.

## Live compass control

The visible compass uses the same fresh, confidence-checked heading as the player pointer. Its north mark rotates under a fixed top index, with a numeric heading and cardinal label. A dash replaces unreliable readings while the compass remains available as an orientation control. Tapping it toggles between heading-up and north-up without changing the map centre or zoom; the separate Locate control recentres.

## Route following in 0.2.2

Pointer position and camera target use the same interpolation frames while following. Moving GPS course takes priority; when stationary, a reliable phone compass controls camera bearing, with route direction as a fallback if the compass is unavailable. Navigation gestures pause automatic following for eight seconds after the gesture ends.

Rerouting uses at least three accurate fixes over 2.5 seconds. Lateral deviation must exceed the larger of 20 m (15 m walking) and twice the reported accuracy. Travel backwards by more than 25 m or 2.5 times the accuracy also counts when moving. Fixes worse than ±35 m cannot initiate rerouting. Retries back off after failures; the old route remains dimmed while a replacement loads. Closely parallel roads inside GPS uncertainty can remain indistinguishable. This is not a lane-level map-matching engine.
