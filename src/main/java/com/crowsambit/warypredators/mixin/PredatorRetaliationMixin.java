package com.crowsambit.warypredators.mixin;

import com.crowsambit.warypredators.WaryPredatorsHooks;
import net.dries007.tfc.common.entities.predator.Predator;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolve a projectile's living shooter only after TFC confirms damage was accepted. */
@Mixin(Predator.class)
public abstract class PredatorRetaliationMixin {
    @Inject(method = "hurt", at = @At("RETURN"))
    private void warypredators$retaliate(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValue()) WaryPredatorsHooks.onSuccessfulHit((Predator)(Object)this, source);
    }
}
