# SimpleLitematica

Fabric client mod for Minecraft Java 26.3.

## Commands
- `/slitematica` – open the schematic manager.
- `/litesave <name> <x1> <y1> <z1> <x2> <y2> <z2>` – save a cuboid to the custom `.slite` JSON format.
- `/liteprint` – print the loaded schematic in Creative mode (singleplayer in this first build).
- `/liteprint slow` – reserved for sequential placement; currently reports that the sequential printer is not yet implemented.

## Files
Drop `.slite` files into `.minecraft/simplelitematica/schematics`. The manager scans the folder when opened.

## Current implementation status
The file format, region export, file manager, layer selection, rotation and offset controls are implemented. The translucent in-world ghost renderer and true timed slow-print sequence are still in development. The first printer uses the block's default state; block properties and block entities are not yet preserved.
