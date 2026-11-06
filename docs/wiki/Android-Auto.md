# Android Auto

Waypoint projects the phone's selected world and navigation session to the Android Auto screen. Search, saved places, route preview, map movement, recentering, zoom, distance units and voice settings use host-safe car templates. Selecting another world on the phone changes the car map without ending the route.

## Enable Waypoint

1. Open **Android Auto** in phone settings and select **Customize launcher**.
2. Enable **Waypoint** if it is listed.
3. Reconnect the phone, choose Waypoint in the car launcher and grant precise location on the phone if prompted.

## If Waypoint is missing

A GitHub-sideloaded APK may be installable on a phone but absent from the car launcher and **Customize launcher**. Android's **Unknown sources** setting does not make an ineligible Car App Library app appear. The maintainer has not configured a Google Play Internal App Sharing or internal testing track for this project. Do not treat an APK reinstall or relaxed host certificate check as a fix.

The Desktop Head Unit (DHU) is the local development test path. A successful DHU session does not prove real-car distribution eligibility. Follow the [Android Auto setup and testing guide](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/guide.md#android-auto) and [maintainer checklist](https://github.com/the-geeky-couldron/waypoint/blob/dev/docs/releasing.md#android-auto-distribution-and-testing).

## Shared destination behavior

An incoming destination goes through a confirmation flow. Waypoint does not silently replace an active destination. Opening car menus does not end the connected session; disconnecting the head unit leaves phone guidance running.
