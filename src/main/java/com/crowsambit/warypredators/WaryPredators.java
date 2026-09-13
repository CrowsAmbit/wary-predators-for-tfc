package com.crowsambit.warypredators;

import com.crowsambit.warypredators.config.WaryPredatorsConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Config registration lives here. In NeoForge 21.x, config registration moved to
 * ModContainer#registerConfig, and the ModContainer is injected into the @Mod constructor.
 *
 * Settings GUI strategy (soft/optional Cloth):
 *  - Base: register NeoForge's own ConfigurationScreen so the mod always gets a categorized,
 *    slider-and-tooltip config UI from the Mods list, with no external dependency.
 *  - Optional upgrade: if Cloth Config is installed, hand off to the Cloth-backed screen instead
 *    (see ClothConfigScreens). The Cloth code lives in its own class that is only classloaded
 *    when cloth_config is present, so the mod runs fine without it.
 */
@Mod(WaryPredators.MOD_ID)
public final class WaryPredators {

    public static final String MOD_ID = "warypredators";

    // NOTE: if IntelliJ flags the constructor signature, match burnttfc's known-working @Mod class -
    // NeoForge's injected-argument set has shifted across 1.21.x point releases.
    public WaryPredators(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        modContainer.registerConfig(ModConfig.Type.COMMON, WaryPredatorsConfig.SPEC);

        // Rammer retaliation (Phase 1) runs on the game bus: one listener records provocations when
        // a player hits a ramming animal, the other drives the pursuit each tick.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(WaryRammerRetaliation::onIncomingDamage);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(WaryRammerRetaliation::onEntityTick);

        // Passive-defender retaliation (Phase 2): livestock fight back with a short escalating burst.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(WaryDefenderRetaliation::onIncomingDamage);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(WaryDefenderRetaliation::onEntityTick);

        if (dist == Dist.CLIENT) {
            registerConfigScreen(modContainer);
        }
    }

    private static void registerConfigScreen(ModContainer modContainer) {
        boolean clothPresent = ModList.get().isLoaded("cloth_config");
        if (clothPresent) {
            // Delegate to a class that imports Cloth types. Only referenced here, inside the
            // isLoaded guard, so the JVM won't try to load Cloth classes when Cloth is absent.
            try {
                com.crowsambit.warypredators.client.ClothConfigScreens.register(modContainer);
                return;
            } catch (Throwable t) {
                // If anything about the Cloth integration fails (API drift, etc.), fall through to
                // NeoForge's built-in screen rather than breaking the mod.
            }
        }
        // NeoForge's built-in configuration screen. In this NeoForge build, IConfigScreenFactory is
        // createScreen(ModContainer, Screen), and ConfigurationScreen has a matching (ModContainer,
        // Screen) constructor - so a method reference satisfies the functional interface directly.
        modContainer.registerExtensionPoint(
            IConfigScreenFactory.class,
            net.neoforged.neoforge.client.gui.ConfigurationScreen::new
        );
    }
}
