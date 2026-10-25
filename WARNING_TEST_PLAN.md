# Warning encounter validation

Status: pending in-game validation; not release-ready.

Use Minecraft 1.21.1, NeoForge 21.1.x and TFC 4.2.11 in a disposable survival test world.
Set black bear attack chance to 1 to make escalation deterministic, then restore defaults.

- Enter provoke distance: one ambient call, animal pauses and faces player for about 3 seconds.
- Stay close: attack begins after warning. With attack chance 0: no attack, no warning repeated every tick.
- Back away 1.5 blocks during warning: no attack; about 15 seconds before reconsideration.
- Break line of sight: encounter clears. Reappear during pacification: no new warning.
- Hit during warning, including thrown weapons: immediate retaliation; no warning-induced movement freeze.
- Cats, crocodiles, polar bears: no new warning delay at default settings.
- Set warning duration to 0, including during warning: original gating resumes.
- Disable predator gate during warning: original TFC behavior resumes.
- Kill/unload animal; disconnect, change dimension, die or switch to creative/spectator: no stale warning lock.
- Two players: switching candidate targets cannot continually restart warning; neither player freezes the animal indefinitely.
- Wolves: verify alpha/follower propagation, retaliation and Wolf Companion compatibility.
- Animal prey: verify hunting still works and overrides any player warning.
- Rammers and livestock: smoke-test unchanged 0.1.5 behavior.
- Run with and without Cloth Config; change duration and confirm save/reload persistence.

Build checks: clean wrapper build and inspect generated neoforge.mods.toml identity,
Minecraft/loader/TFC dependencies, MIT license and development version before packaging.
