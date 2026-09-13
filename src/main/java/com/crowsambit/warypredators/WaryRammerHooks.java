package com.crowsambit.warypredators;

import com.crowsambit.warypredators.config.WaryPredatorsConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wariness gating for TFC's RammingPrey animals - boar, moose, wildebeest, and bison - which are
 * NOT predators and use a completely separate AI path (PrepareRamNearestTargetTFC). All four route
 * through the same chooseRamPosition method the mixin gates, so this one hook covers every rammer;
 * it looks up per-species distance/chance by entity type (falling back to the configured defaults
 * for any other rammer, e.g. one added by another mod that reuses this AI).
 *
 * Non-player ram targets are left untouched, so these animals still ram other mobs normally.
 */
public final class WaryRammerHooks {

    public static void gateRam(PathfinderMob rammingPrey, LivingEntity target, CallbackInfo ci) {
        if (!(target instanceof Player player)) return;              // only gate rams aimed at players
        if (!WaryPredatorsConfig.RAMMERS_ENABLED.get()) return;      // feature disabled -> vanilla behavior
        if (WaryRammerRetaliation.isProvoked(rammingPrey)) return;   // provoked -> charge regardless of wary gate

        var type = rammingPrey.getType();

        double ramDist = WaryPredatorsConfig.ramDistance(type);
        if (rammingPrey.distanceToSqr(player) > ramDist * ramDist) {
            ci.cancel();
            return;
        }

        double chance = WaryPredatorsConfig.ramChance(type);
        if (chance < 1.0 && rammingPrey.getRandom().nextDouble() >= chance) {
            ci.cancel();
        }
        // else: in range and roll passed -> let the ram proceed.
    }

    private WaryRammerHooks() {}
}
