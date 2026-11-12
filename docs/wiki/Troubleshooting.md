# Troubleshooting

## The map is blank or has missing features

Saved offline areas keep their downloaded map tiles and open place names searchable without a connection. Routes, live Google suggestions, photos and current place details still need internet. For live search or routing failures, check the connection and try again later if a public endpoint is unavailable or rate-limited. Zooming can reveal additional POI symbols.

## My location is missing or inaccurate

Tap **Locate**, grant precise location and enable Android location services. A clear view of the sky improves GNSS accuracy. Open Settings in Waypoint to inspect reported position accuracy, heading status and sensor details. See [sensor behavior](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/sensors.md).

## Spoken guidance is silent

Check the Android text-to-speech engine, selected English voice and media volume. In Waypoint, turn **Voice guidance** off and on to retry engine initialization. Visual guidance remains available if the device speech engine is unavailable.

## The day/night map does not change immediately

Waypoint uses the device's local clock and checks the phase every minute. Day treatment runs from 06:00; night treatment begins at 19:00. Confirm the phone's time zone and system clock.

## The route or ETA looks different from another navigation app

Waypoint and other apps can use different route providers and profiles. Waypoint has no live-traffic provider configured; its remaining ETA uses the selected OSRM or Valhalla route through Ferrostar. Review the route before you start; ETA is not a guaranteed arrival time.

## The speed limit sign does not appear

The sign shows only while guiding by car on the phone with **Speed limit warning** on, and only where OpenStreetMap has a usable `maxspeed` on the road you are on. Many roads have none, and the public Overpass service can be slow or busy; the sign returns when a later lookup succeeds. Android Auto does not show it yet.

## Bus & transit shows no trips

Transitous covers cities where open timetable feeds exist, so some areas have no results. Check that GPS has a fix (the trip starts from your position) and that you are online, then try again. Results are a trip list, not live stop-by-stop guidance.

## Waypoint is not in Android Auto's launcher

An APK installed from GitHub may not satisfy Android's trusted distribution requirement for Car App Library apps. See [[Android Auto]]; enabling **Unknown sources** does not solve launcher eligibility.
