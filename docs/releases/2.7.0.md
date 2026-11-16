# Waypoint 2.7.0

This release makes place symbols easier to read, steadies the navigation view, restores the speed readout in landscape, and adds optional ways to support Waypoint.

## Map icons that read at a glance

- Increased landmark symbol size by about 20% across all 38 worlds. Each world keeps its own original artwork; the source atlases are unchanged.
- Nearby search-result markers now scale with zoom, keeping their symbols easy to distinguish beside themed places.

## A steady navigation view

- Navigation now keeps the selected 2D or 3D pitch while speed and turn framing change map zoom. Speed-based zoom no longer makes the map tilt on its own.

## Landscape guidance at a glance

- Phone landscape guidance shows current speed, units and the posted speed limit when one is available.
- When GPS is ready, a stopped vehicle shows zero speed. When GPS is unavailable, the readout shows a dash instead of a misleading zero.

## Support Waypoint if you choose

- Settings and the project pages now link directly to Waypoint on Ko-fi for optional tips.
- The link opens Ko-fi in the browser. Payment details stay with Ko-fi; Waypoint does not embed a third-party payment widget or handle payment information.

## The release journey

- The website now has a separate, animated page for Waypoint's public release history, with a live GitHub snapshot of the first public APK and the cumulative APK download total.
- GitHub counts download events, including updates and repeat downloads. They do not represent unique people or active installs.

## Credits for the reports

Thank you to **Venus Starlight** for sharing the navigation video and landscape HUD report in the Waypoint Discord. The report helped reproduce the changing map pitch at lower speeds and the missing speed and speed-limit readout in landscape. The camera and HUD fixes are included here.

Discord tickets are private, so this acknowledgement does not link to the ticket.

Compatible updates preserve saved places, quests and settings. Android Auto availability in a real vehicle still depends on Google's trusted installation requirements.
