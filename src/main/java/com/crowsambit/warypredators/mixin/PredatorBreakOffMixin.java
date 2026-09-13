package com.crowsambit.warypredators.mixin;

import com.crowsambit.warypredators.WaryPredatorsHooks;
import net.dries007.tfc.common.entities.predator.Predator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs after each predator AI tick to give wary species a chance to break off an ongoing hunt
 * against a player. customServerAiStep is defined on Predator and inherited by every predator
 * family (bears, cats, crocodiles, and the pack predators - none of the subclasses override it),
 * so this single injection covers all of them.
 */
@Mixin(Predator.class)
public abstract class PredatorBreakOffMixin {

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void warypredators$breakOff(CallbackInfo ci) {
        WaryPredatorsHooks.tickBreakOff((Predator) (Object) this);
    }
}
