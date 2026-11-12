# BlockWorld AR — V1

Standalone Android camera-overlay prototype for Android 6.0+.

Included:
- Live camera preview with a Minecraft-style HUD.
- 40 selectable block types and a 9-slot hotbar.
- Tap to place blocks in the camera view; tap an existing block to break it.
- 10 voxel-style creature sprites: Zombie, Skeleton, Creeper, Enderman, Spider, Slime, Pig, Cow, Sheep and Chicken.
- Tap empty camera space in Mobs mode to spawn; tap a creature to deal virtual game damage.
- Hearts and hunger HUD; nearby spawned mobs can reduce the in-game hearts.
- Manually positioned local friend name tags.

Important V1 limitations:
- This is a camera-overlay prototype, not a full ARCore spatial-mapping engine. Placed objects are screen-anchored and do not remain attached to a real-world surface when the phone moves.
- Friend tags are local/manual. Live multiplayer position sharing needs a server or peer-to-peer connection and is not included in V1.
- Camera permission is required to show the live camera background. The app does not request location, microphone, contacts or storage access.
- All block and creature visuals are drawn by the app; no Minecraft texture files are bundled.
