# Fabric 26.3 Mods

A collection of independent Fabric client-side mods for Minecraft Java Edition.

## Projects

| Project | Minecraft | Description | Status |
|---|---:|---|---|
| [PrivacyMod](PrivacyMod/) | 26.3 | Privacy information dashboard and shared privacy API. Press **H** to open the library. | In development |
| [PlayerStatusHUD](PlayerStatusHUD/) | 26.3 | Player status cards showing health and armor information. Requires PrivacyMod. | In development |
| [CoreMod](CoreMod/) | 26.3 | Client utility menu and configurable quick-command slots. | Early development |
| [SimpleLitematica](SimpleLitematica/) | 26.3 | Schematic management and preview/building helpers. | In development |
| [LifestealCore client mod](LifestealCore-Mod%2026.2/) | 26.2 | Server-controlled FastPlace and FastBreak support for compatible LifestealCore servers. | Version-specific |

Each project has its own source code, build configuration and README. Projects are built independently unless a dependency is documented above.

## Downloads

Published release builds will be available on the repository's **[Releases](../../releases)** page. Choose the release matching your Minecraft version and follow each mod's dependency notes.

Do not install JAR files from an untrusted source. For development builds, see the individual project README files.

## Build requirements

- Java 25 for Minecraft 26.3 projects
- Fabric Loader and Fabric API versions listed in each project's `gradle.properties` and `fabric.mod.json`
- Gradle, using the project-specific build instructions

## Repository layout

- `.github/workflows/` — automated build workflows
- Each top-level mod directory — one independent mod project

The existing directory names are kept stable so the build workflows and project paths continue to work.

## License

No repository-wide license is declared here. Check the individual project metadata and any third-party component notices before reusing code. A license can be added without a paid GitHub plan, but the repository owner must choose the terms.