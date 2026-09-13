# Wary Predators

Makes TFC predators (and the boar) wary of distant players instead of attacking on sight, with
per-species, in-game-configurable behavior.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Required dependency:** TerraFirmaCraft
- **License:** MIT

## What changed vs. vanilla TFC

TFC predators attack any *visible* player immediately - no distance or provocation check
(confirmed from TFC 4.2.6 bytecode). This mod adds a wariness layer:

- **Distance gate** - a predator only *considers* a merely-visible player once they're within a
  per-species provoke distance.
- **Attack chance** - within that distance, it commits only with a per-species probability. On a
  failed roll it's pacified toward that player for ~15s (it "sized you up and moved on") instead
  of re-rolling every tick. This is what makes a wolf usually ignore a nearby player.
- **Break-off** - while hunting a *player*, a per-second chance to lose interest and disengage
  ("investigative attack, not pressed" - real wary-predator behavior).
- **Untouched:** getting hit always provokes instantly at any range (via `Predator.hurt`), and
  animal-vs-animal hunting is left to TFC.

Covers bears, cougar/panther/lion/sabertooth/tiger, wolf/hyena/direwolf, crocodile, **and the boar**.

## Ramming animals are separate

The Boar is not a TFC `Predator` - it's `RammingPrey`, and so are three others: **Moose, Wildebeest,
and Bison**. All four use the same charge/ram AI (`PrepareRamNearestTargetTFC.chooseRamPosition`),
gated by one mixin, and each has its own config entry (Ram Distance + Ram Chance) under the
"Ramming Animals" category, plus a shared enable toggle and a fallback default. No break-off (a ram
is a single lunge, not a sustained chase). Defaults reflect real behavior: boars are readily
aggressive; moose are dangerous when approached and stand their ground; bison flee at range but
charge hard once you're close; wildebeest are skittish prey that mostly flee.

## Realism basis for wolf wariness

Wild wolves overwhelmingly avoid humans - a human doesn't match their prey "search image," and
even a curious wolf tends to make brief investigative approaches it doesn't press. That's why the
wolf defaults use a short provoke distance (3 blocks), a low attack chance (0.15), and a real
break-off chance (0.10/s) rather than a flat trigger radius. Hyena and direwolf are progressively
bolder.

## Settings GUI

Open the Mods list, select Wary Predators, click **Config**.

- **Base (no dependency):** NeoForge's built-in `ConfigurationScreen` - categorized, with sliders,
  tooltips, and real names ("Dire Wolf", not "tfc:direwolf") from the language file.
- **Optional upgrade:** if **Cloth Config** is installed, the mod hands off to a Cloth-backed
  screen instead. Cloth is a *soft* dependency - the mod works fine without it.

## Files of note

```
src/main/java/com/crowsambit/warypredators/
  WaryPredators.java              - entry point; registers config + config screen (Cloth-aware)
  WaryPredatorsHooks.java         - predator acquisition gate + break-off logic
  WaryRammerHooks.java            - ram gate for boar/moose/wildebeest/bison
  config/WaryPredatorsConfig.java - per-species named config values + runtime lookup
  client/ClothConfigScreens.java  - OPTIONAL Cloth screen (only classloaded if Cloth present)
  mixin/PredatorAiMixin.java       - gates PredatorAi.getAttackTarget
  mixin/PredatorBreakOffMixin.java - break-off, on Predator.customServerAiStep
  mixin/PrepareRamMixin.java       - ram gate on PrepareRamNearestTargetTFC.chooseRamPosition
src/main/templates/neoforge.mods.toml  - templated; version is expanded into the description
src/main/resources/assets/warypredators/lang/en_us.json  - human-readable names/labels
src/main/resources/logo.png            - PLACEHOLDER logo (replace with your own)
src/main/resources/warypredators.mixins.json
```

## Build

1. **Wrapper:** open the folder in IntelliJ as a Gradle project and use the committed Gradle
   wrapper. From a terminal, invoke it with `./gradlew`.
2. **TFC jar:** drop `TerraFirmaCraft-NeoForge-1_21_1-4_2_6.jar` into `libs/` (name must match
   `tfc_jar_name` in `gradle.properties`). `compileOnly` - not bundled.
3. **Cloth (optional):** `build.gradle` pulls Cloth Config `compileOnly` via CurseMaven for the
   optional screen. If that file id ever fails to resolve, update it from Cloth's CurseForge files
   page, or remove the Cloth dependency + `client/ClothConfigScreens.java` + the cloth branch in
   `WaryPredators.java` to build without it.
4. Run the `build` task -> `build/libs/warypredators-<version>.jar`.

## Flagged for your review (couldn't compile-test here)

- **`@Mod` constructor** now takes `(IEventBus, ModContainer, Dist)`. If NeoForge on your toolchain
  injects a different set, match burnttfc's working @Mod class.
- **Cloth screen** (`ClothConfigScreens`) is written against Cloth's documented API, not a local
  compile (I can't fetch the Cloth jar in my environment). Give it a once-over in IntelliJ. It's
  fully optional - if it fights you, delete it and the mod still ships with NeoForge's own screen.
- **logo.png is a rough placeholder** (a wary-wolf badge). Replace with a real logo when you have one.
- **Boar tuning** and the wolf wariness numbers are first-pass; treat the first in-game session as
  balance tuning as usual.

## Testing suggestions

- Approach a grizzly from just outside 8 blocks (no aggro) then cross inside (aggro).
- Hit a passive predator from beyond its provoke distance - should aggro instantly (hurt path).
- Sit near a wolf repeatedly - it should usually *not* attack, occasionally approach, and sometimes
  break off. Over many encounters, attacks should feel rare.
- Walk near a boar - should ram far more readily than a wolf, but not literally on sight from across
  the render distance.
