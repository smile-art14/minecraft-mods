package com.shenmi.xray.mixin;

import com.shenmi.xray.XRayClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectMixin {
    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void xray$reportClientNightVision(MobEffect effect, CallbackInfoReturnable<Boolean> callbackInfo) {
        if ((Object) this instanceof LocalPlayer
                && effect == MobEffects.NIGHT_VISION
                && XRayClient.isEnabled()) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true)
    private void xray$provideClientNightVision(MobEffect effect,
                                                CallbackInfoReturnable<MobEffectInstance> callbackInfo) {
        if ((Object) this instanceof LocalPlayer
                && effect == MobEffects.NIGHT_VISION
                && XRayClient.isEnabled()) {
            // Return a synthetic client-only effect object without touching the
            // entity's real MobEffect map. Remote server synchronization therefore
            // cannot remove or overwrite X-Ray's visual state.
            callbackInfo.setReturnValue(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    1200,
                    0,
                    true,
                    false,
                    false
            ));
        }
    }
}
