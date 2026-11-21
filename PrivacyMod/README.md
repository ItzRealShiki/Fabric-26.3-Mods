# Privacy Mod

Standalone Fabric client mod for Minecraft 26.3 (Java 25).

## Current implementation

- Registers the client command `/privacy`.
- Reports a small set of built-in privacy observations.
- Exposes a public Java API so other client mods can register privacy entries.
- Uses `UNKNOWN` as a distinct state; an unverified item is never presented as private.

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

Use stable, namespaced entry IDs (for example, `yourmod.setting-name`). Providers should return factual descriptions and must not label data private unless the integration can verify that claim.

## Important technical limits

A client mod cannot hide the network source IP from the server endpoint, guarantee anonymity, or make protocol/version details disappear. It also cannot control what a server-side plugin chooses to show other players. This project reports observable facts and offers opt-in integration points; it does not spoof client identity, suppress anti-cheat checks, or claim to defeat server inspection.

The current command output is an initial API/dashboard foundation. A graphical settings screen and additional verified checks are planned separately.
