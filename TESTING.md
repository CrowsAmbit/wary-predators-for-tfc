# 0.1.6-test.2 validation

This branch is an unreleased candidate. The public release remains 0.1.5.

## Build and automated checks

Use Java 21 and the committed wrapper. Put the TFC 4.2.11 JAR named in
`gradle.properties` and `Patchouli-1.21.1-93-NEOFORGE.jar` in `libs/`.

```sh
./gradlew clean build runGameTestServer
```

For the optional Wolf Companion integration check, put its 0.17.0 JAR in `libs/`:

```sh
./gradlew clean build runGameTestServer -PcompanionTests
```

Select another companion build with
`-Pcompanion_jar=/absolute/path/to/companion.jar`. Without Wolf Companion,
the companion-specific test is skipped; the core checks still run. Optional
Cloth Config remains compile-only; test both configuration screens in the client.
GameTest code and the arena structure are excluded from distributable JARs.

## Fresh results — October 24, 2026

- All Java sources compile with `javac --release 21` against cached development dependencies.
- All **16 required NeoForge/TFC GameTests passed**, with TFC 4.2.11,
  NeoForge 21.1.235 and Wolf Companion 0.17.0 loaded, without Cloth Config.
  This includes both new regression checks: changing nearby players cannot
  restart a warning, and rejected damage cannot provoke retaliation.
- Tests ran directly using the cached NeoForge development launch arguments.
  The clean Gradle command was attempted but blocked by environment socket
  restrictions. This is not a fresh clean Gradle build pass.
- The previous test.1 record includes a clean build and visual Cloth screen checks.
  Those are historical results, not fresh client validation of test.2.

## Player checks before stable release

Install only one Wary Predators JAR. Test in Survival with a backed-up world;
new worlds are not required and old configuration keys are preserved.

1. Approach a polar bear without hitting it. At the default 12-block provoke
   distance, expect a two-second warning. Leave that range or break line of
   sight during the warning: it should be pacified for 15 seconds.
2. Let it commit. Stay farther than 18 blocks from the moving animal for two
   continuous seconds: it should disengage. Return inside to reset the timer.
3. Land actual thrown-spear and arrow hits from beyond provoke range. Verify
   immediate retaliation, a 12-second protection period, and refresh on new hits.
   Terrain, low-health retreat and home range remain TFC-controlled.
4. Repeat around Audrey with Wolf Companion 0.17.0. Warnings alone should not
   trigger defense; a committed attack should. Hits from Audrey belong to her,
   and player withdrawal must not cancel an animal's combat against her.
5. Check wolves/hyenas/direwolves as moving packs, crocodiles at water edges,
   and cats. All predators use the global warning duration in this candidate.
   Pack followers retain TFC alpha targeting; coordinated pack retreat is absent.
6. Check boar/bison ramming and enabled livestock defense, including babies,
   familiarity thresholds and master/species toggles. Their logic is unchanged.
7. Open the native settings without Cloth, then the Cloth settings with Cloth
   installed. Verify the six encounter settings, saving, restart, and toggles.
8. With two real players, check warning ownership, disengagement and damage
   retaliation. Pacification is animal-wide, so it affects both players.

Stable release gate: clean wrapper build, affected-species client checks,
actual projectile flight, moving packs, extended companion combat and
multiplayer behavior must be recorded before promotion to 0.1.6.
