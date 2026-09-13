package com.crowsambit.warypredators;

import com.crowsambit.warypredators.config.WaryPredatorsConfig;
import net.dries007.tfc.common.entities.predator.Predator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Runtime wariness logic for TFC predators. Kept as plain (non-mixin) code so it's easy to read
 * and reason about; the mixins just forward into these methods.
 *
 * Two levers, both per-species and config-driven:
 *   1) Acquisition gate (gateAttackTarget): a merely-visible player only becomes a target if
 *      within provoke distance AND a per-species attack-chance roll succeeds. On a failed roll the
 *      predator is pacified toward that player for a while, so it doesn't simply re-roll every tick
 *      and eventually always attack - it genuinely leaves the player alone for a stretch. This is
 *      what makes a wolf usually ignore a nearby player instead of charging on sight.
 *   2) Break-off (tickBreakOff): while actively hunting a *player*, a per-second chance to lose
 *      interest and disengage, modeling the "investigative attack that isn't pressed" seen in real
 *      wary predators.
 *
 * Untouched on purpose:
 *   - Predator.hurt() sets ATTACK_TARGET directly when the player actually strikes the animal.
 *     That path is never gated - hit it and it fights back, at any range, guaranteed.
 *   - Animal-vs-animal hunting (non-player targets) is left entirely to TFC.
 */
public final class WaryPredatorsHooks {

    // How long (ticks) a predator stays pacified toward a player after declining to attack.
    // 20 ticks = 1 second. ~15s feels like "it sized you up and moved on" without being permanent.
    private static final long DECLINE_PACIFY_TICKS = 300L;

    public static void gateAttackTarget(Predator predator, CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        if (!WaryPredatorsConfig.PREDATORS_ENABLED.get()) return;   // feature off -> vanilla (attack on sight)

        Optional<? extends LivingEntity> target = cir.getReturnValue();
        if (target == null || target.isEmpty()) return;

        LivingEntity entity = target.get();
        if (!(entity instanceof Player player)) {
            // Real prey animal - leave TFC's hunting behavior alone.
            return;
        }

        // Already actively fighting this exact player (e.g. it was hit, or an ongoing fight): don't re-gate.
        Optional<LivingEntity> existing = predator.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (existing.isPresent() && existing.get() == player) return;

        var type = predator.getType();

        // Distance gate.
        double provokeSqr = WaryPredatorsConfig.provokeDistanceSqr(type);
        if (predator.distanceToSqr(player) > provokeSqr) {
            cir.setReturnValue(Optional.empty());
            return;
        }

        // Chance gate. On failure, pacify toward the player for a while instead of re-rolling each tick.
        double chance = WaryPredatorsConfig.attackChance(type);
        if (chance < 1.0 && predator.getRandom().nextDouble() >= chance) {
            predator.getBrain().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, DECLINE_PACIFY_TICKS);
            cir.setReturnValue(Optional.empty());
        }
        // else: within range and roll succeeded (or chance == 1.0) -> allow the attack through unchanged.
    }

    /**
     * Called each server AI tick (from the Predator.customServerAiStep mixin). If the predator is
     * hunting a player and this species has a break-off chance, roll it (scaled from per-second to
     * per-tick) and disengage on success.
     */
    public static void tickBreakOff(Predator predator) {
        if (!WaryPredatorsConfig.PREDATORS_ENABLED.get()) return;   // feature off -> vanilla, never break off

        double perSecond = WaryPredatorsConfig.breakOffChance(predator.getType());
        if (perSecond <= 0.0) return;

        Optional<LivingEntity> attackTarget = predator.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (attackTarget.isEmpty() || !(attackTarget.get() instanceof Player)) return;

        // Convert a per-second probability to per-tick (20 ticks/sec) so the config value is intuitive.
        double perTick = 1.0 - Math.pow(1.0 - perSecond, 1.0 / 20.0);
        if (predator.getRandom().nextDouble() < perTick) {
            predator.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            predator.getBrain().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, DECLINE_PACIFY_TICKS);
        }
    }

    private WaryPredatorsHooks() {}
}
