package com.crowsambit.warypredators;

import com.crowsambit.warypredators.config.WaryPredatorsConfig;
import net.dries007.tfc.common.entities.predator.Predator;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-only, transient encounters. Weak keys do not retain unloaded predators/worlds. */
public final class WaryWarningHooks {
    private record Warning(UUID player, long ends, double retreatDistance) {}
    private static final Map<Predator, Warning> WARNINGS = new WeakHashMap<>();

    /** Return true to withhold acquisition; the normal chance roll runs once warning ends. */
    public static boolean gate(Predator predator, Player player) {
        var species = WaryPredatorsConfig.forEntity(predator.getType());
        if (species == null || species.warningSeconds.get() <= 0.0) {
            WARNINGS.remove(predator);
            return false;
        }
        long now = predator.level().getGameTime();
        Warning warning = WARNINGS.get(predator);
        // Do not let switching between nearby players restart the countdown indefinitely.
        if (warning != null) {
            if (!warning.player.equals(player.getUUID())) return true;
            if (now >= warning.ends) {
                WARNINGS.remove(predator);
                return false;
            }
            return true;
        }
        double distance = Math.sqrt(predator.distanceToSqr(player));
        WARNINGS.put(predator, new Warning(player.getUUID(),
            now + Math.max(1L, Math.round(species.warningSeconds.get() * 20.0)), distance + 1.5));
        predator.playAmbientSound();
        return true;
    }

    public static void tick(Predator predator) {
        Warning warning = WARNINGS.get(predator);
        if (warning == null) return;
        var species = WaryPredatorsConfig.forEntity(predator.getType());
        // Any acquired target (including retaliation or animal prey) takes precedence.
        if (!WaryPredatorsConfig.PREDATORS_ENABLED.get() || species == null
                || species.warningSeconds.get() <= 0.0 || !predator.isAlive()
                || predator.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            WARNINGS.remove(predator);
            return;
        }
        Player player = predator.level().getPlayerByUUID(warning.player);
        long now = predator.level().getGameTime();
        if (player == null || !player.isAlive() || player.isSpectator() || player.isCreative()
                || !predator.hasLineOfSight(player)
                || predator.distanceToSqr(player) >= warning.retreatDistance * warning.retreatDistance
                || now > warning.ends + 100L) {
            WARNINGS.remove(predator);
            predator.getBrain().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, 300L);
            return;
        }
        // Stay put and visibly attend to the player. Never install an attack target here.
        predator.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        predator.getNavigation().stop();
        predator.getLookControl().setLookAt(player, 30.0F, 30.0F);
    }

    private WaryWarningHooks() {}
}
