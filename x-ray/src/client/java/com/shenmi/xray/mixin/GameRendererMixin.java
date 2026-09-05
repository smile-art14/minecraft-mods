package com.shenmi.xray.mixin;

import com.shenmi.xray.XRayClient;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void xray$setNightVisionStrength(
            LivingEntity entity,
            float tickDelta,
            CallbackInfoReturnable<Float> callbackInfo
    ) {
        if (XRayClient.isEnabled()) {
            callbackInfo.setReturnValue(XRayClient.getStrength());
        }
    }
}
