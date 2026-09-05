package com.shenmi.yoyo.client.mixin;

import com.shenmi.yoyo.api.PlayerStandingState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla marks every passenger model as "riding", bending the legs into a
 * seated pose. A player carrying our synchronized standing marker must remain
 * in the normal standing model pose instead.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z"
            )
    )
    private boolean shenmiYoyo$keepStandingPose(LivingEntity entity) {
        if (PlayerStandingState.isSittingOnPlayer(entity)) {
            return true;
        }
        if (PlayerStandingState.isStandingOnPlayer(entity)) {
            return false;
        }
        return entity.isPassenger();
    }
}
