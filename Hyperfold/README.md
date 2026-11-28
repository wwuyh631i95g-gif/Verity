# HYPERFOLD — The Fourth Axis

A portrait Android puzzle game inspired by the video’s shifting cube / tesseract imagery. You play from an outside observer’s viewpoint: four tilted world-slices are shown at once, linked by the W axis. The little rotating wireframe is a perspective projection of a 4D hypercube (tesseract).

## How to play
- Tap a visible tile to travel there. The game finds an open route through the current slice.
- Use the four directional controls, or swipe across the world, for one-step movement.
- Press **W− / W+** to fold between the four slices of the fourth coordinate.
- Find the four glowing echo shards. Each shard exists in only one W-slice.
- After collecting all four, reach the gold exit ring in slice W 4.

The world is drawn by a native Android Canvas view and uses no network, account, storage, camera, microphone, overlay, or location permissions.

## Android build
- Application ID: `com.hyperfold.axis`
- Minimum Android: 8.0 (API 26)
- Target / compile SDK: 34
- Java 17, Android Gradle Plugin 8.6.1, Gradle 8.7
- Intended first device: HONOR X8b, Android 14
