# BearWorld 0.2.0 — connected endless exploration

Android 8.0+ 3D exploration prototype. It replaces the old narrow straight runner with a connected, open-ended grid of neighbouring terrain chunks. The player can travel north/south/east/west and the app creates nearby chunks while releasing distant ones.

## Included in 0.2.0
- Five procedural zone styles: green valley, sand coast, snow valley, turtle village and big house.
- Seam-overlapped terrain tiles and crossing paths so the neighbouring regions connect without boundary walls.
- A closer follow camera with full 360-degree orbit.
- Imported bear models are normalised to a fixed approximate height before placement.
- Local collisions for trees, rocks, houses and imported map meshes; movement resolves X and Z separately so the character does not simply move through solid scenery.
- GLB import still accepts the player's own model and map segments. Imported maps are fitted into the generated regions and their mesh bounds contribute simple solid collision boxes.

## Controls
- Left joystick: walk in every horizontal direction.
- Drag the right side of the view: orbit camera around the bear.
- Jump: jump.
- Pause: stop or resume.
- Load GLB: choose your bear and map segment files.

## Important asset note
The repository currently contains the procedural generator and GLB import path, but not the user's original Snow Valley / Turtle Village / Big House map GLB files. The five procedural regions are newly generated placeholders, not exact copies of those maps. Import the original map GLBs to use those exact locations in the connected chunk system. The app does not contain or redistribute Super Bear Adventure game files.
