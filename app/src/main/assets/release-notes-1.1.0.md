# Waypoint 1.1.0

Your map now travels with you onto Android Auto. The selected game world, route and guidance move between the phone and car screen while Waypoint keeps the controls appropriate to each display.

### Android Auto

- Open Waypoint from the car launcher to see a live map in the world selected on your phone. Search places, open saved or recent destinations, preview a route, and use the car's recenter, zoom and pan controls.
- Follow a visible route line, moving position marker, next-turn cue, remaining distance and ETA. The car map turns with travel and looks farther ahead between turns, then closer near a maneuver.
- Shared destinations ask for confirmation before lookup or route preview. Google Maps remains available from the car launcher; Android Auto uses the navigation app you opened most recently.

### Navigation and phone experience

- Ferrostar now drives turn progress, spoken cues and rerouting for car, motorcycle and walking journeys. OSRM and Valhalla still supply the roads and routes; MapLibre draws the themed maps.
- Locate returns to a useful street-level view, and 2D/3D tilt works across all 27 worlds. The phone and car keep the selected world's palette when it changes during a trip.
- Route previews clear an outdated line while a new route is requested. Search handles more spelling variations and shows the provider's actual place name and address when available.
- The launch, world gallery and onboarding flow have received additional layout, motion and readability refinements.

### Reliability and updates

- Navigation actions and car callbacks are bound to the trip or destination that created them. Car display input and geocoder result counts are bounded, and signed releases check main-branch ancestry before publishing.
- An older installed release checks for this stable update when Waypoint enters the foreground. Review these notes in the update prompt, choose **Download update**, then confirm installation in Android. Saved places and settings remain on your phone after a compatible update.

Download **Waypoint.apk** from this release or open **Settings → App updates** in Waypoint. Maps, search and routes need internet access. Live traffic, offline routing and lane guidance are not included. A physical vehicle and long-duration driving remain to be tested.

