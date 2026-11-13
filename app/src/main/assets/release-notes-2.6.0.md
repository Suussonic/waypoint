# Waypoint 2.6.0

This update adds a speed-limit warning while you drive, a first version of bus and transit directions, and a way into the Waypoint Discord from inside the app.

### Speed limit warning

- While you navigate by car, the phone HUD shows the posted limit as a round sign beside your speed, in km/h or mph to match your units setting.
- Go more than 3 km/h or mph over and your speed turns red and the sign grows. Waypoint also says the limit and "Slow down" once through the guidance voice, then stays quiet for at least a minute so it never nags. Muting guidance mutes the alert too.
- The limit comes from the `maxspeed` tag on OpenStreetMap roads, so it appears only where mappers have recorded one. Where there is no tag, nothing is shown rather than a guess.
- Turn it off any time in Settings under **Speed limit warning**. It runs only while guiding by car on the phone; Android Auto does not show it yet.
- To look up the limit, Waypoint sends your current position to the public Overpass API about once a minute while guiding. Nothing is sent when the setting is off.

### Bus and transit directions

- Pick a destination, then tap **Bus & transit** next to Steps, or choose it from More. Waypoint lists the next trips from where you are now: departure and arrival times, which line to take, its direction, where to board and where to get off, how many stops, and the walking between.
- Buses, trams, metro and trains are included where open timetable data exists. Times marked "live" come from the operator's real-time feed.
- Timetables come from Transitous, which merges open transit feeds. Coverage depends on the city, so some places have no results yet; Waypoint says so plainly instead of showing an empty list.
- This first version is a trip planner. It does not draw the transit leg on the map or guide you stop by stop.
- Planning a trip sends your position and the chosen destination to api.transitous.org.

### Join the Waypoint Discord

- Settings now has **Join the Waypoint Discord**, and the What's New window shown after you update has **Join Discord** and **Visit site** buttons beside **Explore map**. The server has a private channel for each of the 38 worlds, plus release notes and bug reports. Open invite: https://discord.gg/TQ9cEx6rsu

### Website

The day and night section on the Waypoint site now compares real map captures of the same place, Jaisalmer in Old West and Jaipur in Pixel, with a slider and a world switcher. The site also has a Discord section and the new Upstar badge.

### Thanks for the reports and ideas

- **[@YS0008](https://github.com/YS0008)** asked for bus routes and which bus to take in [#51](https://github.com/the-geeky-couldron/waypoint/issues/51).

Compatible updates preserve your saved places, quests, and settings. Maps, search, routes and transit still need an internet connection. Android Auto availability in a real vehicle still depends on Google's trusted installation requirements.
