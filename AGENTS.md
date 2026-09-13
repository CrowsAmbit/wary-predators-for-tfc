# Repository guidance

## Project baseline

- Mod: Wary Predators for TFC
- Mod ID: `warypredators`
- Language: Java 21
- Minecraft: 1.21.1
- Loader: NeoForge 21.1.x
- Required mod: TerraFirmaCraft 4.2.5 or newer for Minecraft 1.21.1
- License: MIT

## Build and validation

Use the committed Gradle wrapper rather than a system Gradle installation.

```bash
./gradlew build
```

The current build expects the TFC filename configured by `tfc_jar_name` in
`gradle.properties` to be present under `libs/`. Never commit dependency JARs,
Gradle caches, IntelliJ state, the development `run/` directory, or build output.

Before completing a code change:

1. Run the clean Gradle build when dependencies are available.
2. Confirm that generated mod metadata identifies Minecraft 1.21.1, NeoForge,
   TFC, the `warypredators` mod ID, and the MIT license correctly.
3. For AI or mixin changes, test the affected species in a NeoForge development
   client with the configured TFC version.
4. Do not alter established default behavior or configuration keys without
   documenting the compatibility impact.

## Source conventions

- Keep common gameplay behavior independent of client-only configuration-screen
  integration.
- Treat Cloth Config as optional; the mod must continue to load without it.
- Keep mixins narrowly targeted and document assumptions tied to TFC internals.
- Preserve existing configuration keys when practical so upgrades retain player
  settings.
- Update `README.md` and `CURSEFORGE_DESCRIPTION.md` when player-visible behavior
  changes.
- Change the version only as part of an intentional release preparation step.

