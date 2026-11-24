# Neon Coast: Last Run

An original compact 3D open-world story game for Android. It borrows broad genre ideas—coastal open-world exploration, driving, character switching and police pursuit—but uses its own setting, characters and plot. It is not a GTA VI port.

## Story chapter 1
Kai and Lina investigate a scheme to force residents and small businesses out of the Old Port. They meet a source, recover a memory card, escape a patrol, switch protagonists and bring evidence to a radio journalist. The chapter is told through in-game dialogue scenes.

## Playable features
- Coastal 3D city with downtown, market blocks, beach, marina and motel district.
- Two switchable protagonists with separate saved positions.
- Five connected story missions with original dialogue and rewards.
- Police pursuit with a two-star wanted indicator.
- Drivable sports car, traffic, pedestrians, touch joystick, camera swipe, brake and nitro.
- In-game phone panel with messages, current objective and local news.
- Mini-map, cash display, speedometer and local save of mission progress.

## Android
- Package: `com.verity.neoncoast`
- Version: `0.2.0`
- Minimum Android: 8.0 (API 26)
- Target: Android 14 (API 34)
- Landscape orientation; no runtime permissions or network access required.

Three.js is bundled locally into the APK during the GitHub Actions build. The app does not download game data at launch.

## Build
The workflow `.github/workflows/build-neon-coast.yml` builds a debug APK, checks its package/version, runs an Android 14 emulator launch smoke test and publishes a release asset.
