# Waypoint

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Implementation choice: Kotlin, Jetpack Compose, Material 3, MapLibre Native. User explicitly requests native Android.

## Product Purpose

Real-time GPS navigation with visual themes inspired by GTA V, Red Dead Redemption, and other games. The user explicitly clarified this is navigation, not a playable map.

## Users

The user navigating real streets on an Android phone. This began as a private hobby app. The user now explicitly requests publication to the GitHub account signed in through Helium, GitHub Actions Android builds, an in-app updater and a marketing README. The user has chosen public open-source distribution at https://github.com/the-geeky-couldron/waypoint and completed GitHub CLI authorization through Helium. GitHub Actions builds the APK and publishes signed, versioned releases.

## Capabilities and Constraints

Sensor-assisted real-time navigation: fused GNSS/network positioning, gyroscope/accelerometer/compass heading, true-north correction, accuracy/calibration feedback. Destination search, car, motorcycle and walking routes, spoken turn instructions, rerouting, route progress, arrival, and saved destinations. Visual gamification only: no quests, missions, XP, rewards, or invented gameplay. Android Auto is shown in a reference but is not an explicit requested integration. First version uses online OSM services; no traffic-aware routing or offline routing is promised. Location and destination are sent to map/geocoding/routing providers only as necessary. Saved places and preferences remain on the device.

## Brand Commitments

Actual game HUD samples recorded in docs/visual-sourcebook.md refine the visual direction. Six supplied screenshots establish full-screen themed maps, bold game-inspired typography, high-contrast place symbols and visible route lines. Twenty-seven game-inspired themes adapt researched cartographic/HUD rules using original graphics, without copying game assets. Working app name: Waypoint. The launcher identity is an original bold navigation arrow, bent route and open compass brackets over an abstract dark map. The supplied dark-map icon is inspiration rather than an asset to reproduce. Android adaptive layers include a monochrome mark for Material You themed icons on supported launchers; the in-app game palettes remain independent.

Public promotional artwork uses original compositions, the original Waypoint icon, licensed fonts and sanitised native app captures. Repository documentation, release notes and examples use fixed public demonstration locations or fictional fixtures. Personal queries, routes, device details, home/work locations and private captures must not enter public materials. The README poster source notes are in `docs/images/promo/README.md`; source captures stay outside Git.

## Product Principles

- Real navigation is the core function.
- The game inspiration changes cartography and visual tone, not road rules.
- GPS state, route progress, and permission problems remain visible.
- Native Android touch targets, Back behavior and accessibility remain intact.

## Confirmed UI refinements

The launch surface is a native animated welcome screen with Open map and world selection. Only game variants are offered. In every launcher/library preview, the map-style name is large and the game name is smaller, with uniform type sizes. Map actions have visible explanatory labels. Distinct per-game placement remains pinned. Theme changes remain available during route preview and active navigation without losing the route.

The release extension checks for stable updates when the app enters the foreground and offers a download prompt on the idle map until the release is installed. Downloads remain explicit. APKs must pass checksum, package, version and signing-identity checks before Android's installer opens. Installation is user-confirmed and disabled during navigation. The prepared token-free update channel requires public GitHub Releases. Public documentation screenshots use explicit demonstration coordinates, never the user's GPS position.

Android Auto is disabled and non-exported in the hardened 0.2.1 build pending isolated cross-UID and lifecycle validation. Phone transport modes remain available. Shared links cannot replace active guidance. See docs/security.md.

Destination search keeps the native game-styled surface and supports both the visible Search places button and the keyboard Search action. Photon results can be supplemented by the device Android geocoder and confirmed spelling aliases; provider coverage determines which names are available. Results retain the provider name, address and coordinates, with provider attribution and an explicit “Address result for …” label when Android returns only a Plus Code. An address match does not confirm a named landmark.
