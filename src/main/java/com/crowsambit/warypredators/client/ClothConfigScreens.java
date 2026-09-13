package com.crowsambit.warypredators.client;

import com.crowsambit.warypredators.config.WaryPredatorsConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * OPTIONAL Cloth Config integration. This class is ONLY classloaded when the "cloth_config" mod is
 * present (see WaryPredators#registerConfigScreen), so its imports of me.shedaniel.clothconfig2.*
 * are safe - they never resolve when Cloth is absent.
 *
 * IMPORTANT (could not be compile-tested in this environment):
 *  - Building against Cloth requires adding the Cloth Config NeoForge artifact to build.gradle.
 *    For 1.21.1 that's the 15.0.x line, e.g. via CurseMaven:
 *        compileOnly "curse.maven:cloth-config-348521:5729126"   // v15.0.140, 1.21.1 NeoForge
 *    or the equivalent on shedaniel's / Modrinth's maven. Mark it optional in neoforge.mods.toml.
 *  - If you'd rather not depend on Cloth at all, delete this class and the cloth branch in
 *    WaryPredators#registerConfigScreen; the NeoForge built-in screen already provides a good UI.
 *  - The Cloth API is stable but I built this from its documented shape, not a local compile, so
 *    give it a once-over in IntelliJ. Every value here mirrors the ModConfigSpec so the two stay
 *    in sync; Cloth writes back into the same spec values via their setSaveConsumer callbacks.
 */
public final class ClothConfigScreens {

    public static void register(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, new IConfigScreenFactory() {
            @Override
            public net.minecraft.client.gui.screens.Screen createScreen(
                    ModContainer container,
                    net.minecraft.client.gui.screens.Screen parent) {
                return build(parent);
            }
        });
    }

    private static net.minecraft.client.gui.screens.Screen build(net.minecraft.client.gui.screens.Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("warypredators.config.title"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory predators = builder.getOrCreateCategory(
            Component.translatable("warypredators.config.category.predators"));

        predators.addEntry(eb.startBooleanToggle(
                Component.translatable("warypredators.config.predators_enabled"),
                WaryPredatorsConfig.PREDATORS_ENABLED.get())
            .setDefaultValue(true)
            .setTooltip(wrapTooltip("warypredators.config.predators_enabled.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.PREDATORS_ENABLED::set)
            .build());
        predators.addEntry(eb.startDoubleField(
                Component.translatable("warypredators.config.default_provoke_distance"),
                WaryPredatorsConfig.DEFAULT_PROVOKE_DISTANCE.get())
            .setDefaultValue(8.0).setMin(0.5).setMax(64.0)
            .setTooltip(wrapTooltip("warypredators.config.default_provoke_distance.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.DEFAULT_PROVOKE_DISTANCE::set)
            .build());
        predators.addEntry(eb.startDoubleField(
                Component.translatable("warypredators.config.default_attack_chance"),
                WaryPredatorsConfig.DEFAULT_ATTACK_CHANCE.get())
            .setDefaultValue(1.0).setMin(0.0).setMax(1.0)
            .setTooltip(wrapTooltip("warypredators.config.default_attack_chance.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.DEFAULT_ATTACK_CHANCE::set)
            .build());
        predators.addEntry(eb.startDoubleField(
                Component.translatable("warypredators.config.default_break_off_chance"),
                WaryPredatorsConfig.DEFAULT_BREAK_OFF_CHANCE.get())
            .setDefaultValue(0.0).setMin(0.0).setMax(1.0)
            .setTooltip(wrapTooltip("warypredators.config.default_break_off_chance.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.DEFAULT_BREAK_OFF_CHANCE::set)
            .build());

        addSpecies(eb, predators, "grizzly_bear", WaryPredatorsConfig.forEntityByName("grizzly_bear"));
        addSpecies(eb, predators, "black_bear",   WaryPredatorsConfig.forEntityByName("black_bear"));
        addSpecies(eb, predators, "polar_bear",   WaryPredatorsConfig.forEntityByName("polar_bear"));
        addSpecies(eb, predators, "cougar",       WaryPredatorsConfig.forEntityByName("cougar"));
        addSpecies(eb, predators, "panther",      WaryPredatorsConfig.forEntityByName("panther"));
        addSpecies(eb, predators, "lion",         WaryPredatorsConfig.forEntityByName("lion"));
        addSpecies(eb, predators, "sabertooth",   WaryPredatorsConfig.forEntityByName("sabertooth"));
        addSpecies(eb, predators, "tiger",        WaryPredatorsConfig.forEntityByName("tiger"));
        addSpecies(eb, predators, "crocodile",    WaryPredatorsConfig.forEntityByName("crocodile"));
        addSpecies(eb, predators, "wolf",         WaryPredatorsConfig.forEntityByName("wolf"));
        addSpecies(eb, predators, "hyena",        WaryPredatorsConfig.forEntityByName("hyena"));
        addSpecies(eb, predators, "direwolf",     WaryPredatorsConfig.forEntityByName("direwolf"));

        ConfigCategory rammers = builder.getOrCreateCategory(
            Component.translatable("warypredators.config.category.rammers"));
        rammers.addEntry(eb.startBooleanToggle(
                Component.translatable("warypredators.config.rammers_enabled"),
                WaryPredatorsConfig.RAMMERS_ENABLED.get())
            .setDefaultValue(true)
            .setTooltip(wrapTooltip("warypredators.config.rammers_enabled.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.RAMMERS_ENABLED::set)
            .build());
        rammers.addEntry(eb.startDoubleField(
                Component.translatable("warypredators.config.default_ram_distance"),
                WaryPredatorsConfig.DEFAULT_RAM_DISTANCE.get())
            .setDefaultValue(6.0).setMin(0.5).setMax(64.0)
            .setTooltip(wrapTooltip("warypredators.config.default_ram_distance.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.DEFAULT_RAM_DISTANCE::set)
            .build());
        rammers.addEntry(eb.startDoubleField(
                Component.translatable("warypredators.config.default_ram_chance"),
                WaryPredatorsConfig.DEFAULT_RAM_CHANCE.get())
            .setDefaultValue(0.70).setMin(0.0).setMax(1.0)
            .setTooltip(wrapTooltip("warypredators.config.default_ram_chance.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.DEFAULT_RAM_CHANCE::set)
            .build());
        rammers.addEntry(eb.startBooleanToggle(
                Component.translatable("warypredators.config.rammers_retaliate"),
                WaryPredatorsConfig.RAMMERS_RETALIATE.get())
            .setDefaultValue(true)
            .setTooltip(wrapTooltip("warypredators.config.rammers_retaliate.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.RAMMERS_RETALIATE::set)
            .build());
        rammers.addEntry(eb.startDoubleField(
                Component.translatable("warypredators.config.rammers_retaliate_seconds"),
                WaryPredatorsConfig.RAMMERS_RETALIATE_SECONDS.get())
            .setDefaultValue(12.0).setMin(1.0).setMax(120.0)
            .setTooltip(wrapTooltip("warypredators.config.rammers_retaliate_seconds.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.RAMMERS_RETALIATE_SECONDS::set)
            .build());
        addRammer(eb, rammers, "boar",       WaryPredatorsConfig.forRammerByName("boar"));
        addRammer(eb, rammers, "moose",      WaryPredatorsConfig.forRammerByName("moose"));
        addRammer(eb, rammers, "bison",      WaryPredatorsConfig.forRammerByName("bison"));
        addRammer(eb, rammers, "wildebeest", WaryPredatorsConfig.forRammerByName("wildebeest"));

        ConfigCategory defenders = builder.getOrCreateCategory(
            Component.translatable("warypredators.config.category.defenders"));
        defenders.addEntry(eb.startBooleanToggle(
                Component.translatable("warypredators.config.defenders_enabled"),
                WaryPredatorsConfig.DEFENDERS_ENABLED.get())
            .setDefaultValue(false)
            .setTooltip(wrapTooltip("warypredators.config.defenders_enabled.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.DEFENDERS_ENABLED::set)
            .build());
        defenders.addEntry(eb.startDoubleField(
                Component.translatable("warypredators.config.defenders_burst_seconds"),
                WaryPredatorsConfig.DEFENDERS_BURST_SECONDS.get())
            .setDefaultValue(4.5).setMin(1.0).setMax(60.0)
            .setTooltip(wrapTooltip("warypredators.config.defenders_burst_seconds.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.DEFENDERS_BURST_SECONDS::set)
            .build());
        defenders.addEntry(eb.startDoubleField(
                Component.translatable("warypredators.config.defenders_familiarity_threshold"),
                WaryPredatorsConfig.DEFENDERS_FAMILIARITY_THRESHOLD.get())
            .setDefaultValue(0.3).setMin(0.0).setMax(2.0)
            .setTooltip(wrapTooltip("warypredators.config.defenders_familiarity_threshold.tooltip"))
            .setSaveConsumer(WaryPredatorsConfig.DEFENDERS_FAMILIARITY_THRESHOLD::set)
            .build());
        addDefender(eb, defenders, "cow",     WaryPredatorsConfig.forDefenderByName("cow"));
        addDefender(eb, defenders, "yak",     WaryPredatorsConfig.forDefenderByName("yak"));
        addDefender(eb, defenders, "musk_ox", WaryPredatorsConfig.forDefenderByName("musk_ox"));
        addDefender(eb, defenders, "goat",    WaryPredatorsConfig.forDefenderByName("goat"));
        addDefender(eb, defenders, "pig",     WaryPredatorsConfig.forDefenderByName("pig"));

        builder.setSavingRunnable(() -> {
            // ModConfigSpec values write to the backing config on set(); ensure it's flushed.
            WaryPredatorsConfig.SPEC.save();
        });
        return builder.build();
    }

    private static void addSpecies(ConfigEntryBuilder eb, ConfigCategory cat, String name,
                                   WaryPredatorsConfig.Species s) {
        if (s == null) return;
        // Each animal is a collapsible sub-category (header = species name), collapsed by default,
        // holding its three settings labeled generically (Provoke Distance / Attack Chance / Break-Off).
        SubCategoryBuilder sub = eb
            .startSubCategory(Component.translatable("warypredators.config.name." + name))
            .setExpanded(false);
        sub.add(eb.startDoubleField(
                Component.translatable("warypredators.config.field.provoke_distance"),
                s.provokeDistance.get())
            .setDefaultValue(s.provokeDistanceDefault).setMin(0.5).setMax(64.0)
            .setTooltip(wrapTooltip("warypredators.config." + name + ".provoke_distance.tooltip"))
            .setSaveConsumer(s.provokeDistance::set)
            .build());
        sub.add(eb.startDoubleField(
                Component.translatable("warypredators.config.field.attack_chance"),
                s.attackChance.get())
            .setDefaultValue(s.attackChanceDefault).setMin(0.0).setMax(1.0)
            .setTooltip(wrapTooltip("warypredators.config." + name + ".attack_chance.tooltip"))
            .setSaveConsumer(s.attackChance::set)
            .build());
        sub.add(eb.startDoubleField(
                Component.translatable("warypredators.config.field.break_off_chance"),
                s.breakOffChance.get())
            .setDefaultValue(s.breakOffChanceDefault).setMin(0.0).setMax(1.0)
            .setTooltip(wrapTooltip("warypredators.config." + name + ".break_off_chance.tooltip"))
            .setSaveConsumer(s.breakOffChance::set)
            .build());
        cat.addEntry(sub.build());
    }

    private static void addRammer(ConfigEntryBuilder eb, ConfigCategory cat, String name,
                                  WaryPredatorsConfig.Rammer r) {
        if (r == null) return;
        SubCategoryBuilder sub = eb
            .startSubCategory(Component.translatable("warypredators.config.name." + name))
            .setExpanded(false);
        sub.add(eb.startDoubleField(
                Component.translatable("warypredators.config.field.ram_distance"),
                r.ramDistance.get())
            .setDefaultValue(r.ramDistanceDefault).setMin(0.5).setMax(64.0)
            .setTooltip(wrapTooltip("warypredators.config." + name + ".ram_distance.tooltip"))
            .setSaveConsumer(r.ramDistance::set)
            .build());
        sub.add(eb.startDoubleField(
                Component.translatable("warypredators.config.field.ram_chance"),
                r.ramChance.get())
            .setDefaultValue(r.ramChanceDefault).setMin(0.0).setMax(1.0)
            .setTooltip(wrapTooltip("warypredators.config." + name + ".ram_chance.tooltip"))
            .setSaveConsumer(r.ramChance::set)
            .build());
        cat.addEntry(sub.build());
    }

    /**
     * Splits a tooltip into short lines so Cloth renders it as a narrow column instead of one wide
     * banner that spans the settings GUI (which obscured the value being edited). We resolve the
     * translation here (client-side, language is loaded by the time a screen opens) and word-wrap it
     * to a fixed width; each resulting line becomes its own Component, which is how Cloth's
     * setTooltip(Component...) expects multi-line tooltips.
     */
    private static Component[] wrapTooltip(String key) {
        // Prefer pixel-accurate wrapping via the active font; fall back to a char heuristic if the
        // font isn't available for any reason.
        String text = Component.translatable(key).getString();
        final int maxWidthPx = 220; // roughly a third of a typical GUI width - keeps the box narrow
        java.util.List<Component> lines = new java.util.ArrayList<>();
        try {
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            for (net.minecraft.util.FormattedCharSequence seq :
                    font.split(Component.literal(text), maxWidthPx)) {
                lines.add(Component.literal(flatten(seq)));
            }
        } catch (Throwable t) {
            lines.clear();
            wrapByChars(text, 44, lines);
        }
        if (lines.isEmpty()) wrapByChars(text, 44, lines);
        return lines.toArray(new Component[0]);
    }

    /** Reconstructs the plain string from a FormattedCharSequence (our tooltips are unstyled text). */
    private static String flatten(net.minecraft.util.FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    /** Greedy word-wrap fallback at a character-count width. */
    private static void wrapByChars(String text, int maxChars, java.util.List<Component> out) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > maxChars) {
                out.add(Component.literal(line.toString()));
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(Component.literal(line.toString()));
    }

    private static void addDefender(ConfigEntryBuilder eb, ConfigCategory cat, String name,
                                    WaryPredatorsConfig.Defender d) {
        if (d == null) return;
        SubCategoryBuilder sub = eb
            .startSubCategory(Component.translatable("warypredators.config.name." + name))
            .setExpanded(false);
        sub.add(eb.startBooleanToggle(
                Component.translatable("warypredators.config.field.defend_enabled"),
                d.enabled.get())
            .setDefaultValue(true)
            .setTooltip(wrapTooltip("warypredators.config." + name + ".defend_enabled.tooltip"))
            .setSaveConsumer(d.enabled::set)
            .build());
        sub.add(eb.startDoubleField(
                Component.translatable("warypredators.config.field.defend_damage"),
                d.hitDamage.get())
            .setDefaultValue(d.hitDamageDefault).setMin(0.0).setMax(40.0)
            .setTooltip(wrapTooltip("warypredators.config." + name + ".defend_damage.tooltip"))
            .setSaveConsumer(d.hitDamage::set)
            .build());
        cat.addEntry(sub.build());
    }

    private ClothConfigScreens() {}
}