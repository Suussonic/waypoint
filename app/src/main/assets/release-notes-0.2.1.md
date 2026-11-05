# Waypoint 0.2.1

Real streets. Game worlds. A safer journey.

Waypoint 0.2.1 is the first public GitHub-built Android release, with all 27 game-inspired worlds and manual in-app updates.

### What changed

- Incoming destination links cannot interrupt active guidance. Cancelled or outdated address lookups no longer replace a newer destination.
- Network requests cancel promptly; response sizes, JSON structure, route complexity, coordinates and timing values are checked before use.
- Route preparation moves off the main thread, and invalid sensor fixes are rejected.
- Gradle is updated to 8.14.6 with a verified distribution checksum. Release builds do not consume shared Gradle caches.
- GitHub Actions explicitly installs the Android SDK, fixing the missing `sdkmanager` failure.
- A refreshed screenshot gallery, installation guide and public security notes explain the app and its limits.

### Android Auto

The experimental Android Auto component is disabled pending isolated validation of its inter-app boundary. Car, motorcycle and walking navigation on the phone remain available.

### Install

Download **Waypoint.apk** below (Android 8.0+). Compatible upgrades preserve saved places and settings. Future releases appear in **Settings → App updates**; Android always asks before installation.

Every world is included. No ads, accounts or subscriptions. Maps and routing require an internet connection. Real-road and battery testing remain outstanding. The source security review does not certify the app as invulnerable; see `docs/security.md` for findings, fixes and remaining validation.

