# Player Hotbar Display

Standalone Fabric mod for Minecraft Java 26.3.

## Goal
Display the nine hotbar slots of nearby players above their heads.

## Compatibility
The mod is designed as a common Fabric mod and must be installed on the server and on clients. The server sends hotbar snapshots only to clients that have the mod's network payload registered. An unmodified vanilla server does not expose other players' full hotbar contents to clients, so universal client-only support on arbitrary servers is not technically possible.

## Current implementation
- Standalone project with its own Gradle configuration.
- Server-side hotbar synchronization for players in the same dimension.
- Client-side receiver and cache for synchronized hotbar snapshots.
- Synchronization runs every 5 ticks and sends only to compatible clients.

The above-head item rendering and an end-to-end build check are still pending; this repository state is not yet a finished playable release.

## Build
Requires Java 25. Run:

```bash
gradle --no-daemon build
```

The JAR is produced in `build/libs/`.

## License
This project follows the repository's ItzRealShiki Attribution & Server Credit License 1.0. Retain attribution and the required server credit.
