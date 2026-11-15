# BearWorld 0.1.0

Android 3D endless-world prototype for Android 8.0+ (tested by CI on an Android 14 emulator).

## Controls
- Left joystick: walk and steer.
- Jump: jump over rocks.
- Drag the empty area on the right: rotate the camera.
- Pause: stop/resume.
- Load GLB: import your own character and map files from device storage.

## Importing the supplied models
1. Extract the supplied GLB ZIP archive in a file manager.
2. In BearWorld, tap **ЗАГРУЗИТЬ GLB**.
3. Select the bear model file first (the filename containing `12.x.x_model` or `bear.glb`).
4. Select one or more map GLB files in the same picker. If the file picker only allows one selection, repeat the import for each map.
5. Imported map models are placed as repeating scenery beside successive generated terrain chunks.

The app starts with a procedural bear and terrain so it can run before importing the external models. It does not include original game source code.
