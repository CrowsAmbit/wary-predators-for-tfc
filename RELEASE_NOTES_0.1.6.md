# Wary Predators 0.1.6 — draft release notes

Status: pending player validation; current candidate is 0.1.6-test.2.

- Predators identify the player responsible for successful projectile hits,
  including hits beyond normal provoke distance.
- Unprovoked attacks offer a configurable warning pause using existing species
  attack sounds. Backing out of provoke range or breaking line of sight during
  the pause ends the encounter.
- Sustained withdrawal can end a player hunt. Successful hits temporarily
  protect retaliation from warning delays, withdrawal and random break-off.
- Add six controls to native and optional Cloth settings. Existing per-species
  distances, attack chances and configuration keys are preserved.
- Preserve animal hunting, companion damage attribution, ramming and livestock
  behavior. Fix warning restarts when the nearest player changes.

Defaults: warning 2 seconds; withdrawal beyond provoke range plus 6 blocks for
2 seconds; retaliation protection 12 seconds; pacification 15 seconds.
Warnings and withdrawal default to enabled for all predator species. Turn them
off independently, or disable the predator master toggle to restore TFC behavior.

Minecraft 1.21.1 / NeoForge. Validated candidate dependencies: TFC 4.2.11,
NeoForge 21.1.235 and Wolf Companion 0.17.0. Wolf Companion is optional.
Pack followers retain TFC alpha targeting, and pacification is animal-wide.
See TESTING.md for results and remaining release checks.
