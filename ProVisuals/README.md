# ProVisuals

Client-side visual customization mod for Minecraft Java 26.3 / Fabric.

## Design
- Modern, minimal dark interface
- Violet accent
- English in-game UI
- Dedicated Error Center

## Modules
- World Visuals: Glowing Ores, Ground Reflections, Fullbright
- Player Visuals: Armor Outline Glow, Glowing Armor Trims, Visual Friends
- Combat Visuals: Item Outline Glow, Hits — Energy Wave
- HUD & Interface: Armor HUD, Target HUD, Inventory HUD, Effects HUD
- Music: Lyrics in the Sky integration and the ProVisuals Music Island

## Music integration
ProVisuals bundles Lyrics in the Sky 1.3.0-dev.24 for Minecraft 26.3 as a nested Fabric mod. Its original menus, synced lyric timing, font resources, song-based palettes, display styles and media-session detection are retained. Use the Music page in ProVisuals to open its settings. The release download is checksum-verified during the build.

The ProVisuals Music Island shows the current track and playback progress. While the Minecraft chat screen is open, its previous / play-pause / next controls send system media commands. Controls are supported on Windows (system media keys) and Linux/BSD (MPRIS); other platforms currently show the widget without playback control.

Lyrics in the Sky uses its own online lyrics sources and local cache. Review its settings and source notice before enabling optional online lookup or an account connection.

## Error Center
Runtime events are stored in the Minecraft config directory as provisuals-diagnostics.log. The Error Center displays recent entries and can copy them to the clipboard.

## Current implementation status
- Menu, module registry, persistent configuration and diagnostics are implemented.
- Fullbright is connected to the client gamma option.
- Lyrics in the Sky is packaged as a nested mod.
- Music Island display and chat-click media controls are implemented.
- Other visual modules are registered and save preferences; their render hooks are still being implemented. Ground Reflections currently has saved quality, intensity and surface settings, not an active reflection shader.

## Build
Requires Java 25, Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.166.2+26.3 and Gradle 9.8.0.

From this directory run: gradle --no-daemon clean build

Output: build/libs/provisuals-0.1.0.jar
