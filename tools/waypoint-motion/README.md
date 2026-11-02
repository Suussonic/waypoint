# Waypoint motion source

The three MP4 files in `app/src/main/res/raw/` are used by the Android welcome and onboarding screens. This Remotion project contains their editable compositions. Its `public/` images are generated copies of Android resources and are ignored by Git.

From the repository root:

```sh
python3 tools/waypoint-motion/prepare_public_assets.py
npm ci --prefix tools/waypoint-motion
cd tools/waypoint-motion
npx remotion render src/index.ts WaypointIntro ../../app/src/main/res/raw/waypoint_intro.mp4
npx remotion render src/index.ts OnboardingGlobe ../../app/src/main/res/raw/onboarding_globe_motion.mp4
npx remotion render src/index.ts IconCollageScene ../../app/src/main/res/raw/onboarding_icons_motion.mp4
```

Review each rendered clip in the app before replacing a shipped video. Render previews, temporary files and `node_modules/` stay local. The Android build uses the three checked-in runtime MP4s; it does not require Node or Remotion.
