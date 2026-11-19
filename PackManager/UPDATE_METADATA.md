# PackManager update metadata

PackManager does not infer updates from Minecraft compatibility or the `pack.mcmeta` pack format.

Resource-pack creators can optionally provide a `packmanager.json` file in the resource pack root. The file is intended to describe the creator's update source:

```json
{
  "schemaVersion": 1,
  "projectId": "example-pack",
  "version": "1.2.0",
  "updateManifest": "https://example.com/example-pack/packmanager-update.json"
}
```

The URL may point to a JSON manifest maintained by the pack creator:

```json
{
  "schemaVersion": 1,
  "projectId": "example-pack",
  "latestVersion": "1.3.0",
  "downloadUrl": "https://example.com/example-pack/download"
}
```

The project ID must match. PackManager should show an update only when the creator-provided manifest reports a newer version. Missing, invalid, unreachable, or mismatched metadata must result in **Update status unknown**, never an assumed outdated status.

Only use trusted HTTPS update sources. Ask the player before opening a download page or replacing an installed pack. Keep a backup before replacing files.
