# PvP Mod

A modular, performance-focused client-side PvP mod for **Minecraft Java 26.3** and **Fabric**.

## Requirements
- Minecraft Java 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API 0.166.2+26.3
- Java 25

## Controls
- **Right Shift** — open the module grid
- **Main Menu** — open the settings and module overview from the center of the grid
- **Escape** — close the menu

## Modules
- Fixed Shields
- Shield Status
- Totem Size
- Particles
- Small Held Items
- Fullbright
- Health Indicators
- Keystrokes
- Armor HUD
- Potion Effects

Module preferences are stored in `config/pvpmod.properties`.

## Performance principles
- No per-frame configuration file access.
- Configuration is written only when a setting changes.
- Client-only initialization.
- Feature implementations are isolated so disabled modules can avoid registering unnecessary work.

## Current development status
The initial project foundation includes the Right Shift menu, module grid, main menu, persistent feature toggles, and hand support preference. Individual visual and gameplay modules are being implemented in follow-up commits.

## Credits
The Small Held Items feature is based on the resource pack supplied for this project. Its original author credit and license notice will be retained when its assets are integrated.
