package com.crowsambit.warypredators.mixin;

import com.crowsambit.warypredators.WaryRammerHooks;
import net.dries007.tfc.common.entities.ai.prey.PrepareRamNearestTargetTFC;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gates the Boar's ram-target commitment. chooseRamPosition(PathfinderMob, LivingEntity) is called
 * from both the initial target scan and the mid-ram retarget; injecting at HEAD (cancellable)
 * covers both. Cancelling simply means "don't lock in a ram against this player right now."
 *
 * chooseRamPosition is private, but Mixin can target private methods. It's generic in the class
 * (E extends ...), but the concrete descriptor at this call site uses PathfinderMob/LivingEntity.
 */
@Mixin(PrepareRamNearestTargetTFC.class)
public abstract class PrepareRamMixin {

    @Inject(method = "chooseRamPosition", at = @At("HEAD"), cancellable = true)
    private void warypredators$gateRamOnPlayer(PathfinderMob rammingPrey, LivingEntity target, CallbackInfo ci) {
        WaryRammerHooks.gateRam(rammingPrey, target, ci);
    }
}
