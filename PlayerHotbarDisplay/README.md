# Player Hotbar Display

A standalone Fabric mod for Minecraft Java 26.3. It is built as its own project and produces its own JAR; it is not a feature inside another mod.

## Features
- Displays the nine hotbar item icons in a camera-facing row above other players.
- Synchronizes hotbar contents from the server to compatible clients.
- Updates the displayed contents every 5 server ticks.
- Uses vanilla item models, including item textures and enchantment glint.

## Installation and compatibility
Install this same mod JAR on the server and on every client that should see the hotbar displays. The server sends the data only to clients that have registered this mod's network payload.

A client-only mod cannot discover other players' complete hotbars on an unmodified vanilla server. Therefore it cannot work on arbitrary servers without server-side support. This is a Minecraft networking limitation, not a restriction of the rendering code.

## Build
Requires Java 25. From this directory run:

```bash
gradle --no-daemon build
```

The standalone mod JAR is created in `build/libs/`.

## License
This project follows the repository's ItzRealShiki Attribution & Server Credit License 1.0. Retain attribution and the required server credit.
