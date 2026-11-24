# Neon Coast: Last Run

A compact, original 3D open-world action/driving game prototype for Android.

This is an original coastal city, original characters and original story. It is inspired by the broad open-world driving/adventure genre; it does not contain GTA VI code, maps, characters, missions, trademarks or Rockstar assets.

## First playable slice
- A procedural 3D coastal city with downtown, residential blocks, a beach and a marina.
- Third-person on-foot movement and a drivable sports car.
- Touch joystick, swipe camera, action, handbrake and nitro controls.
- Moving civilian traffic and pedestrians.
- A three-step story mission: meet a contact, collect a package and deliver it to the marina.
- Mini-map, speed display, objective tracker and local save for mission progress.

## Android
- Package: `com.verity.neoncoast`
- Minimum Android: 8.0 (API 26)
- Target: Android 14 (API 34)
- Landscape orientation; no runtime permissions and no network access required.

Three.js is bundled locally into the APK during the GitHub Actions build. The app does not download game data at launch.

## Build
The workflow `.github/workflows/build-neon-coast.yml` builds a debug APK, checks the package, runs an Android 14 emulator launch smoke test and uploads the APK as a workflow artifact.
