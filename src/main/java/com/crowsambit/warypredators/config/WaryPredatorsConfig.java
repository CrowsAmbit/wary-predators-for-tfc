package com.crowsambit.warypredators.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for Wary Predators.
 *
 * Design notes:
 * - Every tunable is an individual, named config value (not a string list). This is what lets
 *   both NeoForge's built-in ConfigurationScreen and Cloth Config render each entry as a labeled
 *   slider with a tooltip, and it's why the on-screen names read as "Dire Wolf" rather than
 *   "tfc:direwolf" (the human-readable label comes from the translation keys in lang/en_us.json,
 *   keyed off the config path built here).
 * - Species are keyed internally by their TFC entity id (needed to look the entity type up at
 *   runtime), but that id never appears in the UI.
 *
 * Behavior model (see WaryPredatorsHooks for the runtime side):
 *   provokeDistance  - a visible player must be at least this close before the predator will even
 *                      *consider* attacking. Being hit by the player, or already being in a fight,
 *                      ignores this entirely.
 *   attackChance     - once a player is within provoke distance, the probability (per consideration)
 *                      that the predator actually commits. Low values model "wary, investigative"
 *                      animals (e.g. wolves) that usually decline. On a failed roll the predator is
 *                      pacified toward that player for a while instead of re-rolling every tick.
 *   breakOffChance   - while actively hunting a *player*, the per-second probability the predator
 *                      loses interest and disengages ("investigative attack, not pressed"). 0 disables.
 */
public final class WaryPredatorsConfig {

    public static final ModConfigSpec SPEC;

    /** One entry per configurable species. */
    public static final class Species {
        public final String id;                 // TFC entity id, e.g. "tfc:wolf"
        public final ModConfigSpec.DoubleValue provokeDistance;
        public final ModConfigSpec.DoubleValue attackChance;
        public final ModConfigSpec.DoubleValue breakOffChance;
        // Defaults captured at definition time (ModConfigSpec.DoubleValue has no getDefault()).
        public final double provokeDistanceDefault;
        public final double attackChanceDefault;
        public final double breakOffChanceDefault;

        Species(String id, ModConfigSpec.DoubleValue d, ModConfigSpec.DoubleValue a, ModConfigSpec.DoubleValue b,
                double dDef, double aDef, double bDef) {
            this.id = id;
            this.provokeDistance = d;
            this.attackChance = a;
            this.breakOffChance = b;
            this.provokeDistanceDefault = dDef;
            this.attackChanceDefault = aDef;
            this.breakOffChanceDefault = bDef;
        }
    }

    // Master toggle + global fallback for any predator not explicitly listed below.
    public static final ModConfigSpec.BooleanValue PREDATORS_ENABLED;
    public static final ModConfigSpec.DoubleValue DEFAULT_PROVOKE_DISTANCE;
    public static final ModConfigSpec.DoubleValue DEFAULT_ATTACK_CHANCE;
    public static final ModConfigSpec.DoubleValue DEFAULT_BREAK_OFF_CHANCE;

    // ---- Rammers (RammingPrey: boar, moose, wildebeest, bison) ------------------------------
    // These use a different AI path than predators (they charge/ram rather than hunt), so they get
    // their own settings: a shared enable toggle, a fallback default, and per-species distance/chance.
    // No break-off - a ram is a single lunge, not a sustained chase.
    public static final ModConfigSpec.BooleanValue RAMMERS_ENABLED;
    public static final ModConfigSpec.BooleanValue RAMMERS_RETALIATE;
    public static final ModConfigSpec.DoubleValue RAMMERS_RETALIATE_SECONDS;
    public static final ModConfigSpec.DoubleValue DEFAULT_RAM_DISTANCE;
    public static final ModConfigSpec.DoubleValue DEFAULT_RAM_CHANCE;

    /** One entry per configurable ramming animal. */
    public static final class Rammer {
        public final String id;                  // TFC entity id, e.g. "tfc:boar"
        public final ModConfigSpec.DoubleValue ramDistance;
        public final ModConfigSpec.DoubleValue ramChance;
        public final double ramDistanceDefault;
        public final double ramChanceDefault;

        Rammer(String id, ModConfigSpec.DoubleValue d, ModConfigSpec.DoubleValue c, double dDef, double cDef) {
            this.id = id;
            this.ramDistance = d;
            this.ramChance = c;
            this.ramDistanceDefault = dDef;
            this.ramChanceDefault = cDef;
        }
    }

    private static final Map<String, Rammer> RAMMERS = new LinkedHashMap<>();

    // ---- Passive defenders (livestock self-defense) ----------------------------------------
    public static final ModConfigSpec.BooleanValue DEFENDERS_ENABLED;
    public static final ModConfigSpec.DoubleValue DEFENDERS_BURST_SECONDS;
    public static final ModConfigSpec.DoubleValue DEFENDERS_FAMILIARITY_THRESHOLD;

    /** One entry per configurable passive defender. */
    public static final class Defender {
        public final String id;                       // TFC entity id, e.g. "tfc:cow"
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue hitDamage;
        public final double hitDamageDefault;

        Defender(String id, ModConfigSpec.BooleanValue en, ModConfigSpec.DoubleValue d, double dDef) {
            this.id = id;
            this.enabled = en;
            this.hitDamage = d;
            this.hitDamageDefault = dDef;
        }
    }

    private static final Map<String, Defender> DEFENDERS = new LinkedHashMap<>();

    // Per-species entries, keyed by a short config name (also the translation-key suffix).
    private static final Map<String, Species> SPECIES = new LinkedHashMap<>();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // ---- Predators -------------------------------------------------------------------
        // Section layout mirrors the rammers section: a master on/off toggle, then the fallback
        // defaults (for any predator not individually listed), then each species as its own entry.
        //  - bears: wary but will defend space if approached closely
        //  - ambush cats: strike from short range
        //  - crocodile: pure close-range ambush
        //  - wolves/hyenas/direwolves: pack hunters, but real wolves are extremely wary of humans -
        //    modeled with a short provoke distance AND a low attack chance plus a real break-off chance.
        builder.comment(
                   "Predators (bears, big cats, wolves/hyenas/direwolves, crocodiles). 'Enable Predator",
                   "Gate' turns the whole predator system on or off; the defaults apply to any predator not",
                   "listed individually; each species below overrides those defaults."
               )
               .push("predators");

        PREDATORS_ENABLED = builder
            .comment("Master toggle. If false, ALL predators behave exactly as vanilla TFC (attack on sight).")
            .translation("warypredators.config.predators_enabled")
            .define("predatorsEnabled", true);
        DEFAULT_PROVOKE_DISTANCE = builder
            .comment("Fallback provoke distance (blocks) for any predator not listed individually.")
            .translation("warypredators.config.default_provoke_distance")
            .defineInRange("defaultProvokeDistance", 8.0, 0.5, 64.0);
        DEFAULT_ATTACK_CHANCE = builder
            .comment("Fallback attack chance (0.0-1.0) once a player is within provoke distance.")
            .translation("warypredators.config.default_attack_chance")
            .defineInRange("defaultAttackChance", 1.0, 0.0, 1.0);
        DEFAULT_BREAK_OFF_CHANCE = builder
            .comment("Fallback per-second chance (0.0-1.0) to disengage from a player mid-hunt. 0 disables.")
            .translation("warypredators.config.default_break_off_chance")
            .defineInRange("defaultBreakOffChance", 0.0, 0.0, 1.0);

        defineSpecies(builder, "grizzly_bear", "tfc:grizzly_bear", 6.0, 0.90, 0.02);
        defineSpecies(builder, "black_bear",   "tfc:black_bear",   4.0, 0.15, 0.05);
        defineSpecies(builder, "polar_bear",   "tfc:polar_bear",  12.0, 1.00, 0.00);
        defineSpecies(builder, "cougar",       "tfc:cougar",       5.0, 0.75, 0.05);
        defineSpecies(builder, "panther",      "tfc:panther",      5.0, 0.75, 0.05);
        defineSpecies(builder, "lion",         "tfc:lion",         7.0, 0.85, 0.02);
        defineSpecies(builder, "sabertooth",   "tfc:sabertooth",   9.0, 0.95, 0.00);
        defineSpecies(builder, "tiger",        "tfc:tiger",        9.0, 0.95, 0.00);
        defineSpecies(builder, "crocodile",    "tfc:crocodile",    5.0, 0.95, 0.00);
        // Wolves & kin: wary. Short trigger range, low commit chance, will break off.
        defineSpecies(builder, "wolf",         "tfc:wolf",         3.0, 0.15, 0.10);
        defineSpecies(builder, "hyena",        "tfc:hyena",        5.0, 0.40, 0.05);
        defineSpecies(builder, "direwolf",     "tfc:direwolf",     6.0, 0.60, 0.02);

        builder.pop();

        // ---- Rammers (RammingPrey, separate AI path) -------------------------------------
        // Boar, moose, wildebeest, and bison all use TFC's ramming AI. Defaults reflect real
        // behavior: boars are readily aggressive; moose are dangerous when approached; bison flee
        // until you're close, then charge hard; wildebeest are skittish prey that mostly flee.
        builder.comment(
                   "Ramming animals (boar, moose, wildebeest, bison) use TFC's charge/ram AI, not the",
                   "predator hunt AI, so they are configured separately here. 'Ram Distance' is how close",
                   "a player must be before the animal considers charging; 'Ram Chance' is how likely it",
                   "commits once you're in range. Being hit always provokes instantly regardless."
               )
               .push("rammers");
        RAMMERS_ENABLED = builder
            .comment("Master toggle. If false, ALL ramming animals behave exactly as vanilla TFC (no gating).")
            .translation("warypredators.config.rammers_enabled")
            .define("rammersEnabled", true);
        RAMMERS_RETALIATE = builder
            .comment(
                "If true, a ramming animal that a player hits (melee OR thrown weapon) will turn and charge",
                "that player back, at any range, ignoring the wariness distance/chance gate for a while.",
                "This is a realism addition on top of vanilla TFC, which normally never retaliates to ranged hits."
            )
            .translation("warypredators.config.rammers_retaliate")
            .define("rammersRetaliate", true);
        RAMMERS_RETALIATE_SECONDS = builder
            .comment("How long (seconds) a provoked ramming animal keeps pursuing the player who hit it.")
            .translation("warypredators.config.rammers_retaliate_seconds")
            .defineInRange("rammersRetaliateSeconds", 12.0, 1.0, 120.0);
        DEFAULT_RAM_DISTANCE = builder
            .comment("Fallback ram distance (blocks) for any ramming animal not listed individually.")
            .translation("warypredators.config.default_ram_distance")
            .defineInRange("defaultRamDistance", 6.0, 0.5, 64.0);
        DEFAULT_RAM_CHANCE = builder
            .comment("Fallback ram chance (0.0-1.0) for any ramming animal not listed individually.")
            .translation("warypredators.config.default_ram_chance")
            .defineInRange("defaultRamChance", 0.70, 0.0, 1.0);

        defineRammer(builder, "boar",       "tfc:boar",       8.0, 0.85);
        defineRammer(builder, "moose",      "tfc:moose",      6.0, 0.75);
        defineRammer(builder, "bison",      "tfc:bison",      5.0, 0.80);
        defineRammer(builder, "wildebeest", "tfc:wildebeest", 3.0, 0.15);

        builder.pop();

        // ---- Passive defenders (livestock that fight back when attacked) ------------------
        // Cow, yak, musk ox, goat, pig are TFC livestock with no attack of their own - they only
        // flee. This section lets a chosen set of them defend with a short "burst": when a player
        // hits one, it turns, closes in, and lands blows for a few seconds, then flees. Every fresh
        // hit refreshes the window, so a determined attacker gets a real scrap ("escalation").
        builder.comment(
                   "Passive livestock that defend themselves with a short, escalating burst when a player",
                   "attacks them (melee or thrown), instead of only fleeing. Being hit refreshes the window."
               )
               .push("defenders");
        DEFENDERS_ENABLED = builder
            .comment(
                "Master toggle. Default OFF - this is opt-in. When true, cow/yak/musk ox/goat/pig",
                "defend themselves; when false they only flee, exactly like vanilla TFC."
            )
            .translation("warypredators.config.defenders_enabled")
            .define("defendersEnabled", false);
        DEFENDERS_BURST_SECONDS = builder
            .comment("Base defensive-burst length (seconds). Each new hit the player lands refreshes it.")
            .translation("warypredators.config.defenders_burst_seconds")
            .defineInRange("defendersBurstSeconds", 4.5, 1.0, 60.0);
        DEFENDERS_FAMILIARITY_THRESHOLD = builder
            .comment(
                "Familiarity at or above which a livestock animal will NOT fight back - it trusts you and",
                "just flees like vanilla. TFC familiarity runs 0.0-1.0; 0.3 is where an animal is bonded",
                "enough to breed. Below this, half-wild or barely-tamed animals still defend themselves.",
                "Set above 1.0 to make even fully-tame animals fight back."
            )
            .translation("warypredators.config.defenders_familiarity_threshold")
            .defineInRange("defendersFamiliarityThreshold", 0.3, 0.0, 2.0);

        defineDefender(builder, "cow",     "tfc:cow",     3.0);
        defineDefender(builder, "yak",     "tfc:yak",     3.0);
        defineDefender(builder, "musk_ox", "tfc:musk_ox", 4.0);
        defineDefender(builder, "goat",    "tfc:goat",    2.0);
        defineDefender(builder, "pig",     "tfc:pig",     2.0);

        builder.pop();

        SPEC = builder.build();
    }

    private static void defineDefender(ModConfigSpec.Builder builder, String name, String entityId, double dmg) {
        builder.push(name);
        ModConfigSpec.BooleanValue en = builder
            .comment("Whether this animal fights back when attacked.")
            .translation("warypredators.config." + name + ".defend_enabled")
            .define("enabled", true);
        ModConfigSpec.DoubleValue d = builder
            .comment("Damage this animal deals to the player per defensive blow (half-hearts x2 = one heart per 2.0).")
            .translation("warypredators.config." + name + ".defend_damage")
            .defineInRange("hitDamage", dmg, 0.0, 40.0);
        builder.pop();
        DEFENDERS.put(name, new Defender(entityId, en, d, dmg));
    }

    private static void defineRammer(ModConfigSpec.Builder builder, String name, String entityId,
                                     double dist, double chance) {
        builder.push(name);
        ModConfigSpec.DoubleValue d = builder
            .comment("Ram distance in blocks - how close a player must be before this animal considers charging.")
            .translation("warypredators.config." + name + ".ram_distance")
            .defineInRange("ramDistance", dist, 0.5, 64.0);
        ModConfigSpec.DoubleValue c = builder
            .comment("Ram chance (0.0-1.0) once the player is within ram distance.")
            .translation("warypredators.config." + name + ".ram_chance")
            .defineInRange("ramChance", chance, 0.0, 1.0);
        builder.pop();
        RAMMERS.put(name, new Rammer(entityId, d, c, dist, chance));
    }

    private static void defineSpecies(ModConfigSpec.Builder builder, String name, String entityId,
                                      double dist, double chance, double breakOff) {
        builder.push(name);
        ModConfigSpec.DoubleValue d = builder
            .comment("Provoke distance in blocks - how close a player must be before this animal considers attacking.")
            .translation("warypredators.config." + name + ".provoke_distance")
            .defineInRange("provokeDistance", dist, 0.5, 64.0);
        ModConfigSpec.DoubleValue a = builder
            .comment("Attack chance (0.0-1.0) once the player is within provoke distance.")
            .translation("warypredators.config." + name + ".attack_chance")
            .defineInRange("attackChance", chance, 0.0, 1.0);
        ModConfigSpec.DoubleValue b = builder
            .comment("Per-second chance (0.0-1.0) to break off an attack on a player mid-hunt. 0 disables.")
            .translation("warypredators.config." + name + ".break_off_chance")
            .defineInRange("breakOffChance", breakOff, 0.0, 1.0);
        builder.pop();
        SPECIES.put(name, new Species(entityId, d, a, b, dist, chance, breakOff));
    }

    /** Look up a species' config values by its short config name (used by the Cloth screen). */
    public static Species forEntityByName(String name) {
        return SPECIES.get(name);
    }

    // ---- Runtime lookup ------------------------------------------------------------------
    // Built lazily from entity id -> Species so the hooks can resolve a predator's settings.
    private static Map<ResourceLocation, Species> byEntityId;

    private static Map<ResourceLocation, Species> byEntityId() {
        if (byEntityId == null) {
            Map<ResourceLocation, Species> m = new LinkedHashMap<>();
            for (Species s : SPECIES.values()) {
                ResourceLocation id = ResourceLocation.tryParse(s.id);
                if (id != null) m.put(id, s);
            }
            byEntityId = m;
        }
        return byEntityId;
    }

    /** Species-specific settings for the given entity type, or null if it should use the defaults. */
    public static Species forEntity(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null ? null : byEntityId().get(id);
    }

    public static double provokeDistanceSqr(EntityType<?> type) {
        Species s = forEntity(type);
        double d = s != null ? s.provokeDistance.get() : DEFAULT_PROVOKE_DISTANCE.get();
        return d * d;
    }

    public static double attackChance(EntityType<?> type) {
        Species s = forEntity(type);
        return s != null ? s.attackChance.get() : DEFAULT_ATTACK_CHANCE.get();
    }

    public static double breakOffChance(EntityType<?> type) {
        Species s = forEntity(type);
        return s != null ? s.breakOffChance.get() : DEFAULT_BREAK_OFF_CHANCE.get();
    }

    // ---- Rammer lookup -------------------------------------------------------------------
    /** Look up a rammer's config values by its short config name (used by the Cloth screen). */
    public static Rammer forRammerByName(String name) {
        return RAMMERS.get(name);
    }

    private static Map<ResourceLocation, Rammer> rammersByEntityId;

    private static Map<ResourceLocation, Rammer> rammersByEntityId() {
        if (rammersByEntityId == null) {
            Map<ResourceLocation, Rammer> m = new LinkedHashMap<>();
            for (Rammer r : RAMMERS.values()) {
                ResourceLocation id = ResourceLocation.tryParse(r.id);
                if (id != null) m.put(id, r);
            }
            rammersByEntityId = m;
        }
        return rammersByEntityId;
    }

    public static double ramDistance(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Rammer r = id == null ? null : rammersByEntityId().get(id);
        return r != null ? r.ramDistance.get() : DEFAULT_RAM_DISTANCE.get();
    }

    public static double ramChance(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Rammer r = id == null ? null : rammersByEntityId().get(id);
        return r != null ? r.ramChance.get() : DEFAULT_RAM_CHANCE.get();
    }

    // ---- Defender lookup -----------------------------------------------------------------
    /** Look up a defender's config values by its short config name (used by the Cloth screen). */
    public static Defender forDefenderByName(String name) {
        return DEFENDERS.get(name);
    }

    private static Map<ResourceLocation, Defender> defendersByEntityId;

    private static Map<ResourceLocation, Defender> defendersByEntityId() {
        if (defendersByEntityId == null) {
            Map<ResourceLocation, Defender> m = new LinkedHashMap<>();
            for (Defender d : DEFENDERS.values()) {
                ResourceLocation id = ResourceLocation.tryParse(d.id);
                if (id != null) m.put(id, d);
            }
            defendersByEntityId = m;
        }
        return defendersByEntityId;
    }

    /** The defender config for this entity type, or null if this type is not a configured defender. */
    public static Defender defenderFor(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null ? null : defendersByEntityId().get(id);
    }

    private WaryPredatorsConfig() {}
}
