package com.shenmi.xray.mixin;

import com.shenmi.xray.XRayClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void xray$handleKeys(CallbackInfo callbackInfo) {
        XRayClient.tick((Minecraft) (Object) this);
    }
}
