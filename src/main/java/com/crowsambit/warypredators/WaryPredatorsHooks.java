package com.crowsambit.warypredators;

import com.crowsambit.warypredators.config.WaryPredatorsConfig;
import net.dries007.tfc.common.entities.predator.Predator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map;

public final class WaryPredatorsHooks {
    private static final long DECLINE_PACIFY_TICKS = 300L;
    // Weak entity keys avoid leaking unloaded animals or carrying grudges between worlds.
    private static final Map<Predator, Encounter> ENCOUNTERS = new WeakHashMap<>();
    private static final class Encounter {
        UUID warningPlayer;
        long warningUntil;
        long retaliationUntil;
        UUID retreatPlayer;
        long retreatSince = -1;
    }
    private static Encounter encounter(Predator p) { return ENCOUNTERS.computeIfAbsent(p, k -> new Encounter()); }
    private static long ticks(double seconds) { return (long)Math.ceil(seconds * 20.0); }
    private static boolean retaliating(Predator p, Encounter e) { return e != null && p.level().getGameTime() < e.retaliationUntil; }

    public static void onSuccessfulHit(Predator predator, DamageSource source) {
        if (predator.level().isClientSide || !predator.isAlive() || !WaryPredatorsConfig.PREDATORS_ENABLED.get()) return;
        if (!(source.getEntity() instanceof LivingEntity attacker) || !attacker.isAlive()
                || (attacker instanceof Player player && (player.isCreative() || player.isSpectator()))) return;
        var e = encounter(predator);
        e.warningPlayer = null;
        e.retreatSince = -1;
        e.retaliationUntil = predator.level().getGameTime() + ticks(WaryPredatorsConfig.RETALIATION_SECONDS.get());
        // Non-player combat remains TFC's responsibility. Never redirect a wolf's blow at its owner.
        if (attacker instanceof Player) {
            predator.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, attacker);
            predator.getBrain().eraseMemory(MemoryModuleType.PACIFIED);
            predator.getBrain().eraseMemory(MemoryModuleType.HUNTED_RECENTLY);
            predator.getBrain().setActiveActivityIfPossible(Activity.FIGHT);
            predator.setSleeping(false);
        }
    }

    public static void gateAttackTarget(Predator predator, CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
        if (!WaryPredatorsConfig.PREDATORS_ENABLED.get()) return;
        var target = cir.getReturnValue();
        if (target == null || target.isEmpty() || !(target.get() instanceof Player player)) return;
        if (predator.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == player) return;
        var e = ENCOUNTERS.get(predator);
        if (retaliating(predator, e)) return;
        if (predator.distanceToSqr(player) > WaryPredatorsConfig.provokeDistanceSqr(predator.getType())) {
            if (e != null && e.warningPlayer != null) finishWarning(predator, e);
            cir.setReturnValue(Optional.empty()); return;
        }
        if (e != null && e.warningPlayer != null && !player.getUUID().equals(e.warningPlayer)) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        if (e != null && player.getUUID().equals(e.warningPlayer)) {
            if (WaryPredatorsConfig.WARNINGS_ENABLED.get() && predator.level().getGameTime() < e.warningUntil) {
                cir.setReturnValue(Optional.empty()); return;
            }
            e.warningPlayer = null; // The successful attack roll is kept throughout this warning.
            return;
        }
        double chance = WaryPredatorsConfig.attackChance(predator.getType());
        if (chance < 1.0 && predator.getRandom().nextDouble() >= chance) {
            predator.getBrain().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, DECLINE_PACIFY_TICKS);
            cir.setReturnValue(Optional.empty()); return;
        }
        if (WaryPredatorsConfig.WARNINGS_ENABLED.get() && WaryPredatorsConfig.WARNING_SECONDS.get() > 0) {
            e = encounter(predator);
            e.warningPlayer = player.getUUID();
            e.warningUntil = predator.level().getGameTime() + ticks(WaryPredatorsConfig.WARNING_SECONDS.get());
            predator.playSound(predator.getAttackSound(), 1.0f, predator.getVoicePitch());
            predator.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
            predator.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            predator.getNavigation().stop();
            cir.setReturnValue(Optional.empty());
        }
    }

    private static void finishWarning(Predator predator, Encounter e) {
        e.warningPlayer = null;
        predator.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        predator.getBrain().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, DECLINE_PACIFY_TICKS);
    }
    private static void disengage(Predator p, Encounter e) {
        p.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        p.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        p.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        p.getNavigation().stop();
        p.getBrain().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, DECLINE_PACIFY_TICKS);
        p.setAggressive(false);
        if (e != null) { e.warningPlayer = null; e.retreatSince = -1; }
    }

    /** Called after TFC's server brain tick. Only player hunts can be disengaged. */
    public static void tickBreakOff(Predator predator) {
        if (predator.level().isClientSide) return;
        if (!WaryPredatorsConfig.PREDATORS_ENABLED.get()) { ENCOUNTERS.remove(predator); return; }
        var e = ENCOUNTERS.get(predator);
        var target = predator.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (e != null && e.warningPlayer != null) {
            Player warning = predator.level().getPlayerByUUID(e.warningPlayer);
            if (target != null) e.warningPlayer = null;
            else if (warning == null || !warning.isAlive() || warning.isCreative() || warning.isSpectator()
                    || !predator.hasLineOfSight(warning)
                    || predator.distanceToSqr(warning) > WaryPredatorsConfig.provokeDistanceSqr(predator.getType())) {
                finishWarning(predator, e);
            } else {
                predator.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(warning, true));
                predator.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                predator.getNavigation().stop();
            }
        }
        if (!(target instanceof Player player)) { if(e != null) e.retreatSince = -1; return; }
        // A companion hit also locks combat; backing away must not pacify a predator fighting Audrey.
        if (retaliating(predator, e)) { e.retreatSince = -1; return; }
        if (WaryPredatorsConfig.RETREAT_ENABLED.get()) {
            e = encounter(predator);
            double distance = Math.sqrt(WaryPredatorsConfig.provokeDistanceSqr(predator.getType())) + WaryPredatorsConfig.RETREAT_MARGIN.get();
            if (predator.distanceToSqr(player) > distance * distance) {
                if (!player.getUUID().equals(e.retreatPlayer) || e.retreatSince < 0) {
                    e.retreatPlayer = player.getUUID(); e.retreatSince = predator.level().getGameTime();
                }
                if (predator.level().getGameTime() - e.retreatSince >= ticks(WaryPredatorsConfig.RETREAT_SECONDS.get())) {
                    disengage(predator, e); return;
                }
            } else e.retreatSince = -1;
        } else if (e != null) e.retreatSince = -1;
        double perSecond = WaryPredatorsConfig.breakOffChance(predator.getType());
        if (perSecond > 0 && predator.getRandom().nextDouble() < 1.0 - Math.pow(1.0 - perSecond, 1.0 / 20.0)) disengage(predator, e);
    }
    private WaryPredatorsHooks() {}
}
