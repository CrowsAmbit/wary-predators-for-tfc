package com.crowsambit.warypredators.mixin;

import com.crowsambit.warypredators.WaryPredatorsHooks;
import net.dries007.tfc.common.entities.ai.predator.PredatorAi;
import net.dries007.tfc.common.entities.predator.Predator;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Note: PredatorAi.getDisturbedAttackTarget(Predator) internally calls
 * PredatorAi.getAttackTarget(Predator) directly (a same-class static call), so gating this one
 * method also covers the REST/disturbed-wakeup path without a second injection.
 *
 * PackPredatorAi.getAttackTarget (wolves/hyenas/direwolves) defers to this exact method for the
 * pack alpha, and AmphibiousPredatorAi (crocodiles) calls it directly too - so this single
 * injection point covers every TFC predator family as of 4.2.6.
 */
@Mixin(PredatorAi.class)
public abstract class PredatorAiMixin {

    @Inject(method = "getAttackTarget", at = @At("RETURN"), cancellable = true)
    private static void warypredators$gateOnProvokeDistance(
        Predator predator,
        CallbackInfoReturnable<Optional<? extends LivingEntity>> cir
    ) {
        WaryPredatorsHooks.gateAttackTarget(predator, cir);
    }
}
