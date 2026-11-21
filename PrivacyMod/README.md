# Privacy Mod

A client-side Fabric mod for Minecraft Java 26.3 (Java 25). Privacy Mod provides a privacy information dashboard and a small API that other client mods can use to register additional privacy entries.

## Usage

- Press **H** to open or close the Privacy Library.
- The dashboard reports observable information and distinguishes private, exposed and partially exposed items.
- Other client mods can register entries through the public API.

## Integration API

Add a required Fabric mod dependency on `privacymod` in the integrating mod's `fabric.mod.json`, then compile against the Privacy Mod API.

Example provider registration from the integrating mod's client initializer:

```java
PrivacyManager.registerProvider(() -> List.of(
    new PrivacyEntry(
        "example.local-cache",
        "Local cache",
        "This cache remains on this client and is not sent to the server.",
        PrivacyVisibility.PRIVATE
    )
));
```

Use stable, namespaced entry IDs (for example, `yourmod.setting-name`). Descriptions should be factual; only mark data private when that can be verified.

## Dependencies

- Minecraft Java 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API
- Java 25 or newer

The ReGlass rendering library is bundled inside the Privacy Mod JAR as a nested Fabric mod. Users should install Privacy Mod; they do not need to install ReGlass separately.

## Important limitations

A client mod cannot hide the network source IP from the server endpoint, guarantee anonymity, or make protocol/version details disappear. It also cannot control what a server-side plugin chooses to show other players. Privacy Mod reports observable facts and offers integration points; it does not spoof client identity or bypass server inspection.

## Build

Build the ReGlass 26.3 library first, then run `gradle build` in this project directory. The repository's GitHub Actions workflow performs these steps automatically.

## Credits and license

Original work by **ItzRealShiki**. If you use this mod on a Minecraft server, follow the server-credit requirement in the repository's LICENSE.
