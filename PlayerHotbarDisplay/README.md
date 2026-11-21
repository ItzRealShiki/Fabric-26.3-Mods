# Player Hotbar Display

Standalone Fabric mod for Minecraft Java 26.3.

## Goal
Show the nine hotbar slots of other players above their heads.

## Installation
This feature requires the mod on **both the server and every client**. The server must explicitly send each player's hotbar contents to tracking clients; a client-only mod cannot reliably read other players' inventories.

## Build
Requires Java 25. Run:

```bash
gradle --no-daemon build
```

The JAR is produced in `build/libs/`.

## License
This project follows the repository's ItzRealShiki Attribution & Server Credit License 1.0. Retain attribution and the required server credit.
