# CoreMod

A Fabric client mod for Minecraft Java 26.3.

## Implemented

- Right Shift opens the CoreMod menu.
- Main menu sections for Meta GUI, Farm Finder, Schematic Catalog, Custom Scoreboard and Settings.
- Four configurable quick-command slots, stored in `.minecraft/config/coremod.json`.
- UI labels are rendered in Unicode small caps and English.
- No printer, auto-place, auto-farm, auto-sell, macros or automated clicking.

## Planned

- Inventory entry button with left/right placement setting.
- Four Creative-tab-style quick buttons above the inventory.
- Live market data with timestamp and stale-cache fallback.
- Farm recommendations based on user-entered balance, costs and market data.
- Schematic catalog with previews, material lists and external links; Litematica remains independent.
- Full custom scoreboard editor and rendering.

This project is an early UI/configuration foundation. Planned features are not yet implemented.

## Build

Use Java 25 and run `gradle build` in this project directory.