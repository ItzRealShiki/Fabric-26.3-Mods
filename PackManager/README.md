# PackManager

A client-side Fabric mod for Minecraft Java 26.3.

PackManager extends Minecraft's existing **Options → Resource Packs** screen and keeps the familiar vanilla interface. It does not add a separate key binding or standalone screen.

## Update information

PackManager does **not** use Minecraft version compatibility to decide whether a resource pack has an update. Update checks rely on update metadata supplied by the resource-pack creator. If a pack does not provide supported update metadata, its update status is shown as **Unknown**.

## Build

Use Java 25 and run:

```bash
./gradlew build
```
