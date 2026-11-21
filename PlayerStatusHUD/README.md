# Player Status HUD

Client-side Fabric mod for Minecraft Java 26.3.

## Features
- Player status cards for other players within 100 blocks.
- Cards are hidden when blocks obstruct the line of sight.
- Player skin head and name.
- Health shown in hearts, including half-heart values.
- Four armor item icons with vanilla-style durability bars.
- Original ReGlass liquid-glass player cards with rounded corners and a thin grey contour.
- Lightweight HUD data collection; no custom networking.
- Requires Privacy Mod and ReGlass 2.1.0 or newer.

## Build
Run `gradle build` with Java 25. Build ReGlass 26.3 first so its API JAR is available on the compile classpath.
