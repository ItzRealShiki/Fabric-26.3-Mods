# Player Status HUD

A client-side Fabric mod for Minecraft Java 26.3 (Java 25). It displays compact status cards for nearby players.

## ReGlass scope

Liquid-glass effects are used only by this mod's own player-status overlay. Vanilla Minecraft screens and standard HUD elements retain their original appearance.

## Features

- Player status cards for other players within 100 blocks
- Cards are hidden when blocks obstruct the line of sight
- Player skin head and name
- Health shown in hearts, including half-heart values
- Four armor item icons with vanilla-style durability bars
- Liquid-glass styling

## Dependencies

- Minecraft Java 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API
- Java 25 or newer
- **Privacy Mod** (required)

Privacy Mod bundles the ReGlass rendering library. Install Privacy Mod alongside Player Status HUD; a separate ReGlass installation is not required.


The mod-only ReGlass patch is applied during the GitHub Actions build.

## Build

Build ReGlass 26.3 and Privacy Mod first so their API classes are available, then run `gradle build` in this project directory. The repository's GitHub Actions workflow performs these steps automatically.

## Credits and license

Original work by **ItzRealShiki**. If you use this mod on a Minecraft server, follow the server-credit requirement in the repository's LICENSE.
