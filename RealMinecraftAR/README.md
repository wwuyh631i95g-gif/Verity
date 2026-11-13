# BlockWorld AR V3 — Honor X8b friendly camera + motion version

This is a lightweight Android-only build. It does not depend on ARCore, Sceneform, Google Play Services for AR, Internet, or any remote model downloads.

## What V3 does
- Uses the Android rear camera preview and draws the game over the live camera image.
- Includes 40 selectable block types and a 9-slot hotbar.
- Places screen-tapped blocks into a compass/step-based local world coordinate space. When the phone turns, blocks shift in view; when the built-in step detector reports walking, the player position moves and nearby objects change apparent distance.
- Includes 10 hand-drawn voxel-style 3D mob figures. Tapping empty space spawns a mob; tapping a mob damages it. Hostile mobs walk toward the player and affect the in-game heart counter when close.
- Includes hearts, hunger, XP, friend-name markers and a selectable placement distance of 1–8 metres.
- Saves placed blocks, mobs, friend markers and approximate player position locally.

## Permissions
Only CAMERA is requested. Motion/step sensors do not require a runtime permission. The app does not request location, microphone, contacts, storage, Bluetooth or Internet access.

Camera access cannot be omitted if the app is to show a live view of the real room. If the user refuses the camera permission, the app still opens its interface, but cannot show the room image.

## Honest tracking limits
V3 is a compatibility-oriented approximation designed to avoid ARCore dependency on devices that are not certified for ARCore. It uses the phone's compass/rotation sensor and step detector to estimate heading and walking displacement. It is not visual SLAM: step estimates can drift, compass readings can be affected by metal/electronics, and objects will not be perfectly locked to walls/floors or correctly hidden behind real furniture. It is nevertheless more useful than fixed screen stickers because turning and detected walking update the objects' world-relative positions. Tracking quality depends on sensors exposed by the handset.

The 3D characters are original cube-based drawings, not official Minecraft models or textures. Friend tags are local markers, not real-time multiplayer. The local world is stored on the phone; there is no shared network world.
