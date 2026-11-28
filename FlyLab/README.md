# FlyLab

Offline Android mini-game collection for Android 8+.

## Games
- Maze: steer a virtual yellow marker; the simulated fly chooses cells farther from it.
- Color observer: manually record which color zone a real fly chooses to land on.
- Fly race: collect tokens on a virtual loop track.
- Precision landing: tap a virtual landing pad at the right moment.
- Draw a route: sketch a path for the virtual fly to follow.

## Important limits
A phone display cannot reliably steer or detect a real fly. The color observer is a manual observation log, not an automatic detector. Color responses vary by species and conditions. No camera, microphone, location, network, or other Android permissions are requested.

## Build
gradle -p FlyLab --no-daemon :app:assembleDebug
