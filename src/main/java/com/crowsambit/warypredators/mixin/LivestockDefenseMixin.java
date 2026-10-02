package com.crowsambit.warypredators.mixin;

import com.crowsambit.warypredators.WaryDefenderRetaliation;
import net.dries007.tfc.common.entities.livestock.TFCAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep TFC's activity selection from returning a defending animal to panic/avoidance. */
@Mixin(TFCAnimal.class)
public abstract class LivestockDefenseMixin {
    @Inject(method = "tickBrain", at = @At("HEAD"), cancellable = true, remap = false)
    private void warypredators$defend(CallbackInfo ci) {
        if (WaryDefenderRetaliation.tickDefendingBrain((TFCAnimal) (Object) this)) {
            ci.cancel();
        }
    }
}
