# Changelog

## 0.1.6-test.2 — unreleased test candidate

- Resolve successful predator projectile hits to the responsible player.
- Add configurable warning pause using existing species audio before unprovoked player acquisition.
- Add withdrawal threshold, delay, and 15-second pacification after disengaging.
- Protect retaliation from both withdrawal and random break-off for a configurable duration; player and companion hits refresh this duration.
- Preserve non-player hunting and do not assign a companion's damage to its owner.
- Add native and Cloth settings for all six new options; preserve old configuration keys.
- Correct release metadata through the single version property.
- Include development-only TFC GameTests, excluded from distributable JAR.

- Keep one active warning when the nearest visible player changes, preventing repeated warning resets.
- Allow optional companion tests against a selected JAR; default to Wolf Companion 0.17.0.
- Add regression checks for player switching and rejected projectile damage.

This is an unreleased test candidate intended for player validation.

## 0.1.5

Fixed passive livestock retaliation with TFC 4.2.11. When defense is enabled, cows, yaks, musk oxen, goats, and pigs now fight back without fleeing AI interfering. Existing settings and familiarity rules are preserved.

Validation: clean build against TFC 4.2.11; passive retaliation confirmed in game by the mod author.
