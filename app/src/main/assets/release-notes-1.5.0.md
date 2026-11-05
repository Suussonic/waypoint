# Waypoint 1.5.0

This update fixes silent voice guidance, improves favourite-game selection, respects UK
English spelling and adds Twitter / X to onboarding. Thank you to everyone who reported
these problems! 💚

### 🔊 Voice guidance

- Start the Android text-to-speech engine explicitly so installed voices can provide turn instructions.
- Treat engine startup separately from missing language data. Prefer the device's English dialect, with another installed English voice as a fallback.
- Retry failed initialization when starting navigation or turning voice back on. Discard queued instructions when muted or when the trip ends, and avoid replaying old turns after a delayed startup.

### 🎮 Onboarding

- Select several favourite games with clear checkmarks, a selection count and accessible checked states. Rapid taps retain earlier choices.
- Use UK English spelling when the device is set to English (United Kingdom), including “favourite”, “colours” and “recentre”.
- Choose **Twitter / X** when telling us where you found Waypoint. The referral list scrolls on smaller screens and with larger text.

### 🚘 Android Auto: launcher availability

Settings and the setup guide now explain why a GitHub-installed APK may be absent from
**Customize launcher** and the car display. Google's **Unknown sources** option does not
apply to Car App Library apps like Waypoint. Real-car testing needs a supported trusted
installation channel, such as Google Play Internal App Sharing or an Internal Test Track.
This release does not claim to remove that distribution requirement or establish a Play channel.

### 🙌 Community shout-outs

- 🔊 **[@nikkvfox](https://github.com/nikkvfox)** — thanks for flagging missing voice guidance even with speech languages installed! [#1](https://github.com/the-geeky-couldron/waypoint/issues/1)
- 🚘 **[@RoseMariXOXOXO](https://github.com/RoseMariXOXOXO)** — thanks for reporting the missing Android Auto launcher entry and the troubleshooting you had already tried. [#2](https://github.com/the-geeky-couldron/waypoint/issues/2)
- 🧭 **[@kiri-responsib](https://github.com/kiri-responsib)** — thanks for the detailed report covering both Android Auto visibility and silent voice guidance. [#3](https://github.com/the-geeky-couldron/waypoint/issues/3)
- 🎮 🇬🇧 🐦 **[@harrydbarnes](https://github.com/harrydbarnes)** — a triple shout-out for catching the favourite-game selection, UK spelling and missing Twitter option! [#4](https://github.com/the-geeky-couldron/waypoint/issues/4) · [#5](https://github.com/the-geeky-couldron/waypoint/issues/5) · [#6](https://github.com/the-geeky-couldron/waypoint/issues/6)

Thank you for taking the time to report these. Your feedback helped pinpoint the changes above. 💚

### ✅ Validation

The local debug build, lint and 133 unit tests passed. Seven focused tests passed on a
physical Android 17 phone, including speech initialization, onboarding persistence, UK/US
resources, Android Auto host validation and simulated car templates. The tester confirmed
hearing the synthetic voice instruction. Google's Desktop Head Unit also showed both the
installed release and local test build in its launcher; both opened and rendered their maps.
Real-car launcher availability and real-road navigation still require validation; #2 remains open.

