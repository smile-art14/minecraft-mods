package com.example.mixin.client;

import com.example.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void mcdemo$compressSwallowSword(ItemStack stack, ItemDisplayContext displayContext,
                                             boolean leftHanded, PoseStack poseStack,
                                             MultiBufferSource buffers, int light, int overlay,
                                             BakedModel model, CallbackInfo callback) {
        int stage = ExampleMod.getSwallowStage(stack);
        if (stage <= 0 || !ExampleMod.isSwallowSword(stack)) return;

        float progress = (float)stage / (float)ExampleMod.SWALLOW_STAGE_COUNT;
        float lengthScale = 1.0F - progress * 0.78F;

        // Vanilla sword sprites run roughly from lower-left (hilt) to upper-right (tip).
        // Rotate that diagonal onto the X axis, compress only along the blade direction,
        // then rotate back.  The pivot keeps the hilt visually stable while the blade
        // retracts toward it in GUI, first/third person and dropped-item rendering.
        float pivotX = -0.23F;
        float pivotY = -0.23F;
        poseStack.translate(pivotX, pivotY, 0.0F);
        poseStack.mulPose(Axis.ZN.rotationDegrees(45.0F));
        poseStack.scale(lengthScale, 1.0F, 1.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.translate(-pivotX, -pivotY, 0.0F);
    }
}
