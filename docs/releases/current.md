# Waypoint 2.6.6

This release reduces map-rendering pressure on lower-memory devices and keeps manual zoom and pan from snapping back to your current location.

## Smoother motion on car displays

- Devices marked as low-RAM or with 4 GB of total system memory or less now pace map animation at 30 frames per second. This reduces rendering work on aftermarket car displays and other memory-constrained devices.
- Devices outside that low-memory range keep their existing frame rate.

## Zoom where you choose

- Pinch zoom, pan and rotation now pause automatic location-following when the gesture ends, so a fresh GPS update will not pull the camera back to your current position.
- During active guidance, camera-follow resumes after an eight-second grace period. While browsing, it remains paused until you recenter manually.

## Community support

- A.T.L.A.S. can recover a failed ticket-close action, and its GitHub issue creation avoids duplicate reports.

## Credits for the fixes

Thank you to the people who shared recordings and details that made these issues reproducible.

**Discord**

- **@Aleksandyr** — reported intermittent stutter on a 4 GB aftermarket car display running Waypoint directly. The low-memory map frame pacing addresses the reported rendering pressure.
- **@Jkizzle** — shared the zoom snap-back recording, tracked in public [issue #84](https://github.com/the-geeky-couldron/waypoint/issues/84). Manual camera gestures now pause location-following.

Discord ticket channels are private, so reporters are credited by handle without links to private conversations.

Compatible updates preserve saved places, quests and settings. Android Auto availability in a real vehicle still depends on Google's trusted installation requirements.
