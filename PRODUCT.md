# Waypoint

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Implementation choice: Kotlin, Jetpack Compose, Material 3, MapLibre Native. User explicitly requests native Android.

## Product Purpose

Real-time GPS navigation through game-inspired worlds, with optional personal quests and rewards. Navigation remains useful on its own; tasks and progression never gate a route or world.

## Users

The user navigating real streets on an Android phone. This began as a private hobby app. The user now explicitly requests publication to the GitHub account signed in through Helium, GitHub Actions Android builds, an in-app updater and a marketing README. The user has chosen public open-source distribution at https://github.com/the-geeky-couldron/waypoint and completed GitHub CLI authorization through Helium. GitHub Actions builds the APK and publishes signed, versioned releases.

## Capabilities and Constraints

Sensor-assisted real-time navigation: fused GNSS/network positioning, gyroscope/accelerometer/compass heading, true-north correction, accuracy/calibration feedback. Destination search, car, motorcycle, bicycle and walking routes, spoken turn instructions, rerouting, route progress, arrival, and saved destinations. Android Auto now projects the selected world and guidance through car-safe templates with search, saved destinations and map controls. Optional personal quests support tasks, notes, places, reminders and focus sessions. Explicit completion earns local XP, badges, cosmetic frames and authored story chapters; navigation and every world remain freely available. Quests, pictures and progress stay on the phone, survive compatible updates and are removed by uninstall. No accounts, multiplayer, trip sharing, community uploads, game-folder importing or cloud sync are part of this scope. The app uses online OSM map and routing services with Ferrostar guidance; no traffic-aware routing or offline routing is promised. Location and destination are sent to map/geocoding/routing providers only as necessary. Saved places and preferences remain on the device.

## Brand Commitments

Actual game HUD samples recorded in docs/visual-sourcebook.md refine the visual direction. Six supplied screenshots establish full-screen themed maps, bold game-inspired typography, high-contrast place symbols and visible route lines. Thirty-eight game-inspired themes adapt researched cartographic/HUD rules using original graphics, without copying game assets. Working app name: Waypoint. The launcher identity is an original bold navigation arrow, bent route and open compass brackets over an abstract dark map. The supplied dark-map icon is inspiration rather than an asset to reproduce. Android adaptive layers include a monochrome mark for Material You themed icons on supported launchers; the in-app game palettes remain independent.

Public promotional artwork uses original compositions, the original Waypoint icon, licensed fonts and sanitised native app captures. Repository documentation, release notes and examples use fixed public demonstration locations or fictional fixtures. Personal queries, routes, device details, home/work locations and private captures must not enter public materials. The README poster source notes are in `docs/images/promo/README.md`; source captures stay outside Git.

## Product Principles

- Real navigation is the core function.
- The game inspiration changes cartography and visual tone, not road rules.
- GPS state, route progress, and permission problems remain visible.
- Native Android touch targets, Back behavior and accessibility remain intact.

## Confirmed UI refinements

The launch surface is a native animated welcome screen with Open map and world selection. Only game variants are offered. In every launcher/library preview, the map-style name is large and the game name is smaller, with uniform type sizes. Map actions have visible explanatory labels. Distinct per-game placement remains pinned. Theme changes remain available during route preview and active navigation without losing the route.

The release extension checks for stable updates when the app enters the foreground and offers a download prompt on the idle map until the release is installed. Downloads remain explicit. APKs must pass checksum, package, version and signing-identity checks before Android's installer opens. Installation is user-confirmed and disabled during navigation. The prepared token-free update channel requires public GitHub Releases. Public documentation screenshots use explicit demonstration coordinates, never the user's GPS position.

The earlier hardened 0.2.1 build disabled Android Auto. The current implementation enables it with signed host validation, shared phone/car navigation state and a confirmation gate for incoming destinations. Android Auto chooses the last launched navigation app as its default, so Waypoint cannot take over merely by being installed. Phone transport modes remain available. See docs/security.md for the historical review.

Destination search keeps the native game-styled surface and supports both the visible Search places button and the keyboard Search action. Photon results can be supplemented by the device Android geocoder and confirmed spelling aliases; provider coverage determines which names are available. Results retain the provider name, address and coordinates, with provider attribution and an explicit “Address result for …” label when Android returns only a Plus Code. An address match does not confirm a named landmark.

Quests is a prominent main destination beside Map, Worlds and Saved, visible on the idle map. A title is all a new quest needs; Add details reveals optional fields. Completion remains manual, with Undo. Timer expiry after a reboot asks for confirmation before focus credit. Driving and Android Auto suppress celebrations and quiet nonessential quest alerts. Every world uses the same native task controls, with the selected world’s palette, typography, framing and original completion cue.

Shared addresses and supported Google Maps links enter the existing destination confirmation flow. Pasted links and Find in Waypoint text actions use the same validation. Open in another map app hands off a destination, never exact route geometry. The Worlds catalogue supports search, favourites, New, and Preview → Use this world.

The eleven new catalogue entries comprise the nine Issue #7 worlds, Issue #8’s S.T.A.L.K.E.R.-inspired Exclusion Zone and Issue #9’s NieR:Automata-inspired Machine Atlas, for 38 worlds. Phone and projected car share sourced title/reading font roles, with readable adaptations identified in the font records. Every world has an original Car/Motorcycle marker pair plus a world-styled pedal-cycle marker (114 mode/world variants), including the Old West horse and Pixel World minecart. The optional labelled Game-style voice setting uses Android’s selected voice with original world phrases and modest tone changes; each world has nine phrases across opening, journey and arrival, while exact turn instructions remain first.

## Android 2.0 everyday navigation

Bicycle is a distinct travel mode using Valhalla’s bicycle profile and its returned route duration; it does not reuse a car route or manufacture a cycling ETA. Outbound Google Maps directions use bicycling. Saved places can be renamed through a labelled native form; stable identity, address, coordinates and attribution are preserved.

Release checks run by default through Android JobScheduler approximately twice daily, using the existing bounded GitHub release endpoint. Android notification permission and an honest explanation of battery restrictions are contextual; declining either never blocks navigation. The system controls scheduling and delivery. Update notifications open details, with no automatic download or install. iOS remains separate from this Android release.
