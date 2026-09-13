package com.crowsambit.warypredators;

import com.crowsambit.warypredators.config.WaryPredatorsConfig;
import net.dries007.tfc.common.entities.prey.RammingPrey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 1: makes TFC ramming animals (boar, moose, wildebeest, bison) fight back when a player
 * attacks them, including with a thrown weapon from range - something vanilla TFC never does
 * (its RammingPrey.hurt only clears a pathfinding memory; the ram AI only targets by proximity).
 *
 * How it works, all leaning on TFC's own ram behavior (verified from bytecode):
 *  - On a player hit (melee or projectile), we record a per-mob "provocation" (which player, and
 *    an expiry time) and clear any ram cooldown so it can charge immediately.
 *  - Each tick while provoked, when the mob is not already mid-charge and off cooldown, we set its
 *    RAM_TARGET to the player's CURRENT position. TFC's updateActivity then promotes the mob into
 *    the RAM activity and RamTargetTFC charges toward that point, dealing ram damage on contact.
 *    Re-aiming each cooldown cycle produces genuine pursuit, at any range the player hit from.
 *  - Between charges we nudge WALK_TARGET toward the player to close the gap.
 *  - The provocation simply expires after the configured duration.
 *
 * This never fires the flee (AVOID) activity, because that is gated on AVOID_TARGET, which TFC only
 * sets for predators - not players. So a provoked rammer charges rather than runs.
 */
public final class WaryRammerRetaliation {

    private record Provocation(UUID playerId, long expiryTick) {}

    // Server-side only. Provoked rammer UUID -> who provoked it and when the grudge expires.
    private static final Map<UUID, Provocation> PROVOKED = new HashMap<>();

    // Within this distance (blocks) and off cooldown, a provoked rammer launches a charge; beyond it,
    // it keeps advancing on the player until close enough. Charges cover the closing distance fast.
    private static final double CHARGE_TRIGGER_DISTANCE = 14.0;

    /** Records a provocation whenever a player damages a ramming animal. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!WaryPredatorsConfig.RAMMERS_ENABLED.get()) return;
        if (!WaryPredatorsConfig.RAMMERS_RETALIATE.get()) return;

        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (!(victim instanceof RammingPrey)) return;

        // getEntity() resolves to the throwing player for projectiles (spears), or the player for melee.
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player) || player.isSpectator()) return;

        long ticks = (long) (WaryPredatorsConfig.RAMMERS_RETALIATE_SECONDS.get() * 20.0);
        PROVOKED.put(victim.getUUID(), new Provocation(player.getUUID(), victim.level().getGameTime() + ticks));
        // Charge back immediately instead of waiting out any leftover ram cooldown.
        victim.getBrain().eraseMemory(MemoryModuleType.RAM_COOLDOWN_TICKS);
    }

    /** Drives pursuit for a provoked rammer, once per tick, after its own AI has ticked. */
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof RammingPrey rammer)) return;
        if (rammer.level().isClientSide) return;

        Provocation prov = PROVOKED.get(rammer.getUUID());
        if (prov == null) return;

        if (rammer.level().getGameTime() >= prov.expiryTick() || !rammer.isAlive()) {
            PROVOKED.remove(rammer.getUUID());
            return;
        }

        Player player = rammer.level().getPlayerByUUID(prov.playerId());
        if (player == null || !player.isAlive() || player.isSpectator() || player.level() != rammer.level()) {
            PROVOKED.remove(rammer.getUUID());
            return;
        }

        Brain<?> brain = rammer.getBrain();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));

        // If a charge is already underway, leave it to TFC's RamTargetTFC.
        if (brain.hasMemoryValue(MemoryModuleType.RAM_TARGET)) return;

        double dist = Math.sqrt(rammer.distanceToSqr(player));
        boolean offCooldown = !brain.hasMemoryValue(MemoryModuleType.RAM_COOLDOWN_TICKS);

        // Close enough and ready: launch a charge that goes THROUGH the player (not braking at them).
        // RamTargetTFC ends the ram when it reaches RAM_TARGET, so we aim a couple blocks PAST the
        // player; the charge path then crosses the player and RamTargetTFC's collision check lands the
        // hit. We also set a real, run-up-scaled damage multiplier the way TFC's own ram does.
        if (offCooldown && dist <= CHARGE_TRIGGER_DISTANCE && dist > 0.5) {
            net.minecraft.world.phys.Vec3 from = rammer.position();
            net.minecraft.world.phys.Vec3 to = player.position();
            net.minecraft.world.phys.Vec3 dir = to.subtract(from).normalize();
            net.minecraft.world.phys.Vec3 through = to.add(dir.scale(2.0)); // 2 blocks beyond the player
            float mult = net.minecraft.util.Mth.clamp(0.8f + (float) dist * 0.12f, 0.8f, 2.0f);
            rammer.setAttackDamageMultiplier(mult);
            brain.setMemory(MemoryModuleType.RAM_TARGET, through);
        } else {
            // Not charging this tick: keep advancing on the player (entity-tracked, so it follows a
            // moving target) so the pursuit stays relentless for the whole provocation window.
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(player, 1.5f, 0));
        }
    }

    /** True while the given rammer is in its retaliation window (used to bypass the wariness gate). */
    public static boolean isProvoked(Entity rammer) {
        Provocation prov = PROVOKED.get(rammer.getUUID());
        return prov != null && rammer.level().getGameTime() < prov.expiryTick();
    }

    private WaryRammerRetaliation() {}
}
