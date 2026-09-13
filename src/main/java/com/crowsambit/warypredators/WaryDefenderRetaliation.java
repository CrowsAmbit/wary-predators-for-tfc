package com.crowsambit.warypredators;

import com.crowsambit.warypredators.config.WaryPredatorsConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
 * Phase 2: makes a chosen set of otherwise-passive TFC livestock (cow, yak, musk ox, goat, pig)
 * defend themselves with a short, escalating burst when a player attacks them, instead of only
 * fleeing. These animals have NO attack of their own (their LivestockAi just runs vanilla
 * AnimalPanic to flee), so we both steer them at the player and apply the blow ourselves.
 *
 * Behavior:
 *  - On a player hit (melee or thrown), start/refresh a burst window. Refresh-on-hit is the
 *    "escalation": a lone poke gets one defensive reaction, but continued attacks keep it fighting.
 *  - Each tick during the window: clear IS_PANICKING and steer WALK_TARGET at the player (overriding
 *    the flee), face the player, and when within reach and off the per-hit cooldown, deal the
 *    configured damage plus knockback.
 *  - When the window lapses (player stops hitting), we stop overriding and the animal reverts to its
 *    normal flee/idle behavior.
 *
 * Babies never defend.
 */
public final class WaryDefenderRetaliation {

    private static final class State {
        UUID playerId;
        long expiryTick;
        long nextHitTick;
    }

    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private static final double REACH = 2.2;              // blocks within which a blow lands
    private static final long HIT_INTERVAL_TICKS = 16L;   // ~0.8s between blows
    private static final float KNOCKBACK = 0.5f;
    private static final float APPROACH_SPEED = 1.4f;

    /** Starts or refreshes a defender's burst whenever a player damages it. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!WaryPredatorsConfig.DEFENDERS_ENABLED.get()) return;

        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || victim.isBaby()) return;

        WaryPredatorsConfig.Defender cfg = WaryPredatorsConfig.defenderFor(victim.getType());
        if (cfg == null || !cfg.enabled.get()) return;

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player) || player.isSpectator()) return;

        // Familiarity gate: an animal you've raised and bonded with won't turn on you. Only half-wild
        // or barely-tamed livestock (familiarity below the threshold) defend themselves.
        if (victim instanceof net.dries007.tfc.common.entities.livestock.TFCAnimalProperties props
                && props.getFamiliarity() >= WaryPredatorsConfig.DEFENDERS_FAMILIARITY_THRESHOLD.get()) {
            return;
        }

        long ticks = (long) (WaryPredatorsConfig.DEFENDERS_BURST_SECONDS.get() * 20.0);
        State st = ACTIVE.computeIfAbsent(victim.getUUID(), k -> new State());
        st.playerId = player.getUUID();
        st.expiryTick = victim.level().getGameTime() + ticks;   // refresh window (escalation)
    }

    /** Drives the defensive burst each tick while active. */
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;

        State st = ACTIVE.get(mob.getUUID());
        if (st == null) return;

        WaryPredatorsConfig.Defender cfg = WaryPredatorsConfig.defenderFor(mob.getType());
        long now = mob.level().getGameTime();
        if (cfg == null || !cfg.enabled.get() || now >= st.expiryTick || !mob.isAlive() || mob.isBaby()) {
            ACTIVE.remove(mob.getUUID());
            return;
        }

        Player player = mob.level().getPlayerByUUID(st.playerId);
        if (player == null || !player.isAlive() || player.isSpectator() || player.level() != mob.level()) {
            ACTIVE.remove(mob.getUUID());
            return;
        }

        Brain<?> brain = mob.getBrain();
        // Override the flee: clear panic and steer at the player.
        brain.eraseMemory(MemoryModuleType.IS_PANICKING);
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(player, APPROACH_SPEED, 0));

        // Land a blow when in reach and off cooldown.
        if (now >= st.nextHitTick && mob.distanceToSqr(player) <= REACH * REACH) {
            float dmg = cfg.hitDamage.get().floatValue();
            if (dmg > 0.0f) {
                player.hurt(mob.level().damageSources().mobAttack(mob), dmg);
                player.knockback(KNOCKBACK, mob.getX() - player.getX(), mob.getZ() - player.getZ());
            }
            st.nextHitTick = now + HIT_INTERVAL_TICKS;
        }
    }

    private WaryDefenderRetaliation() {}
}
