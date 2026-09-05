package com.shenmi.xray.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.shenmi.xray.XRayClient;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies X-Ray brightness to the final vanilla lightmap instead of pretending
 * that the LocalPlayer owns a NIGHT_VISION potion effect. The lightmap is a
 * purely client-side 16x16 texture, so this works the same in single-player,
 * LAN and dedicated-server multiplayer and cannot be overwritten by server
 * potion synchronization.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Shadow @Final
    private NativeImage lightPixels;

    @Shadow @Final
    private DynamicTexture lightTexture;

    @Inject(method = "updateLightTexture", at = @At("TAIL"))
    private void xray$applyFinalFullbright(float partialTick, CallbackInfo ci) {
        if (!XRayClient.isEnabled()) {
            return;
        }

        float strength = Math.max(0.0F, Math.min(1.0F, XRayClient.getStrength()));
        if (strength <= 0.0F) {
            return;
        }

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int abgr = lightPixels.getPixelRGBA(x, y);
                int red = abgr & 0xFF;
                int green = (abgr >>> 8) & 0xFF;
                int blue = (abgr >>> 16) & 0xFF;
                int alpha = (abgr >>> 24) & 0xFF;

                red = xray$brighten(red, strength);
                green = xray$brighten(green, strength);
                blue = xray$brighten(blue, strength);

                int boosted = (alpha << 24)
                        | (blue << 16)
                        | (green << 8)
                        | red;
                lightPixels.setPixelRGBA(x, y, boosted);
            }
        }

        // Vanilla has already uploaded its normal map by this point. Upload our
        // final post-processed map once more so no remote-server potion packet can
        // restore the dark map later in the same update.
        lightTexture.upload();
    }

    private static int xray$brighten(int channel, float strength) {
        return Math.min(255, Math.max(0,
                Math.round(channel + (255 - channel) * strength)
        ));
    }
}
