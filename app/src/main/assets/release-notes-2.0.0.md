# Waypoint 2.0.0

Waypoint 2.0 brings personal quests, 38 worlds and more useful everyday navigation to Android. Your everyday tasks now have a place in Waypoint. Open **Quests** beside Map, Worlds and Saved, then **Add a quest**. A title is all you need.

### 📝 Quests, at your pace

- Create, edit, complete and reopen personal tasks. Add notes, a place, a reminder or a character portrait when useful.
- Navigate to an attached place using the normal route preview. Arriving never completes your task automatically.
- Use 15, 25 or 45-minute focus sessions, or choose a custom duration. Pause and resume; closing the app does not reset your timer. After a reboot, confirm an expired session before it earns focus credit.
- Choose a photo with Android’s picker, preview the centre crop, and keep a resized copy privately on this phone.

### ✨ Small wins, shared across your worlds

- Complete a quest for 20 XP. A finished focus session of at least ten minutes adds 10 XP once per quest.
- Earn a level every 100 XP, achievement badges, avatar frames and short story chapters. Undo reverses completion credit.
- Navigation and every world stay available immediately. Hide progression in Your journey whenever you prefer.
- Quieter driving: quest celebrations wait until the trip ends, and nonessential quest alerts stay silent during driving navigation and Android Auto use.

### 🗺️ Places move with you

Share an address or supported Google Maps link into Waypoint, paste it in search, or use **Find in Waypoint** when selecting address text. Review the destination before using it. From a place, choose **Open in another map app** to hand it off. The receiving app calculates its own route.

### 🚲 Bicycle routes, and places with your own names

Choose **Bicycle** beside Car, Motorcycle and Walk. Waypoint requests a dedicated cycling route and cycling arrival estimate from Valhalla. A pedal-cycle marker follows the route in your chosen world, and opening directions in Google Maps preserves cycling mode.

In **Saved**, open a place’s options and choose **Rename**. Use familiar names such as Work or Mum’s house; the address and exact location stay intact. Your name is retained across launches and appears when you choose that destination.

### 🎮 Eleven new worlds

Dark Zone, Sea Chart, DOS Atlas, Aincrad, Tactical Radar, Vector Command, Field Ops, 16-bit Quest, Home Console, **Exclusion Zone** and **Machine Atlas** join the catalogue. Exclusion Zone brings S.T.A.L.K.E.R.-inspired PDA framing, muted terrain and green routes. Machine Atlas brings NieR:Automata-inspired beige panels, charcoal lettering and sculpted map buildings. All **38 worlds** are bundled in the app. Search, use **Favourites** or **New**, then **Preview → Use this world**.

### 🐎 Travel in your world's style

Car and Motorcycle modes now have themed transport markers on the phone and Android Auto: a horse in Old West, a minecart in Pixel World, ships, fantasy mounts and world-styled modern vehicles. They keep the same real driving or motorcycle routes. Android Auto uses a clearer vehicle marker, a lower road-following view and route previews that account for the car's panels.

### ✨ Smoother browsing and movement

Vehicle position and the following camera now animate together between GPS updates, including through turns. Repeated sensor readings no longer restart the movement. Main screens use short transitions, quest rows settle into place, and world thumbnails load off the UI thread. Android’s Remove animations setting remains supported.

Worlds remembers your scroll position, search and filter when you preview or apply a world. Return to the catalogue where you left it, even after recreating the screen.

### 🔤 Lettering that belongs to each world

Dark Zone uses Borda, Tactical Radar uses Stratum 2, Pixel World uses Minecraft Seven, Skyrim uses Futura Condensed and Sea Chart uses Windlass titles. Shared font roles keep the phone and car HUD consistent, with readable fallback lettering for other languages.

### 🗣️ A voice for your world

Each of the 38 worlds now has nine rotating original phrases for starting out, travelling and arriving. Navigation adds brief original phrases and subtle changes in tone to match the selected world: “Let’s ride, partner” in Old West or “Course plotted, captain” in Sea Chart. Distances, turn directions and street names stay unchanged and come first. Imminent turns keep the instruction short. Turn off **Game-style voice** in Settings for standard wording and tone. Both modes use your installed Android speech engine, on the phone and Android Auto.

### 🔔 Know when a new version arrives

Waypoint now checks its existing GitHub releases in the background by default, using Android’s scheduler—no account, paid service or new backend. Allow notifications when prompted to receive a new-release alert. The battery-access explanation tells you why Android restrictions can delay checks and where to review them. Checks are scheduled about twice a day; Android decides when they run.

Tapping an alert opens update details. It never downloads or installs an APK automatically. Denying notifications or leaving battery restrictions in place still lets you use the app and check for updates in Settings.

### 🚘 Android Auto refinements

Saved destinations and worlds fit the car menu limits, including the expanded catalogue. Settings reflect the phone's current choices, distance units follow your preference, and ending a trip clears its guidance session. The phone includes a labelled Android Auto setup page with launcher troubleshooting. U-turn symbols follow the route’s actual turn direction, and vehicle heading follows the current road segment through tight turns.

### 🙌 Community shout-outs

🎮 Huge thanks to **[@Balzamus](https://github.com/Balzamus)** for the ideas in [#7](https://github.com/the-geeky-couldron/waypoint/issues/7) that helped shape quests, map sharing and the new worlds! Your detailed suggestions gave this update its direction. 💚🧭

☢️ A big thank-you to **[@matvejsyrygin256-dotcom](https://github.com/matvejsyrygin256-dotcom)** for requesting the S.T.A.L.K.E.R. world in [#8](https://github.com/the-geeky-couldron/waypoint/issues/8). Your idea became **Exclusion Zone**! 🗺️📟

🤖 Thanks to **[@6HentaiMaster9](https://github.com/6HentaiMaster9)** for the NieR:Automata request in [#9](https://github.com/the-geeky-couldron/waypoint/issues/9). Your enthusiasm brought **Machine Atlas** to the world selector! 🗺️

📍 Thank you **[@artmartins28](https://github.com/artmartins28)** for suggesting personal names for saved places in [#10](https://github.com/the-geeky-couldron/waypoint/issues/10). Work, home and the places that matter are easier to recognise. 💚

🚲 Cheers to **[@VoidKittyKat](https://github.com/VoidKittyKat)** for asking for bicycle navigation in [#11](https://github.com/the-geeky-couldron/waypoint/issues/11). Your request added a dedicated cycling option! 🧭

🚗 Thanks again to **[@RoseMariXOXOXO](https://github.com/RoseMariXOXOXO)** ([#2](https://github.com/the-geeky-couldron/waypoint/issues/2)) and **[@kiri-responsib](https://github.com/kiri-responsib)** ([#3](https://github.com/the-geeky-couldron/waypoint/issues/3)) for the Android Auto reports that kept these car improvements moving. The trusted-installation requirement below still applies.

💚 And thanks to the reporters behind the earlier 1.5.0 fixes: **[@nikkvfox](https://github.com/nikkvfox)** ([#1](https://github.com/the-geeky-couldron/waypoint/issues/1), voice guidance), **@kiri-responsib** (#3, voice guidance), and **[@harrydbarnes](https://github.com/harrydbarnes)** ([#4](https://github.com/the-geeky-couldron/waypoint/issues/4), favourite worlds; [#5](https://github.com/the-geeky-couldron/waypoint/issues/5), UK wording; [#6](https://github.com/the-geeky-couldron/waypoint/issues/6), the Twitter link).

### 💬 Help shape the next update

Your feedback is essential to Waypoint. Have a world, feature or improvement in mind? **[Request a feature](https://github.com/the-geeky-couldron/waypoint/issues/new?title=Feature%20request)**. Found a bug, a confusing screen or something you love? **[Send feedback](https://github.com/the-geeky-couldron/waypoint/issues/new)**.

Tell us what you want to do and what would make the app easier or more enjoyable. You do not need technical knowledge to contribute. Your requests helped build this release, and they will help choose what comes next. 🙌💚

### On your phone

Quests, portraits and progress are local. Compatible updates preserve them; uninstalling removes them. There is no account, subscription, Render service, cloud backup or synchronization. Multiplayer, live trip sharing, community world uploads and game-folder importing are outside this release. New worlds arrive through normal owner-published app updates.

Version 1.5.0 and its release assets remain unchanged. Android Auto still needs a supported trusted installation channel to appear in real-car launchers; this update does not remove that distribution requirement.

This release is Android-only. The contributor-led iOS port in [#12](https://github.com/the-geeky-couldron/waypoint/issues/12) remains a separate work in progress.

