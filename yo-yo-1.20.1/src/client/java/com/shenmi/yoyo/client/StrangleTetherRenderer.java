package com.shenmi.yoyo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shenmi.yoyo.entity.StrangleTetherEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the visible strangling rope: one continuous segment from the attacker's
 * hand to the target neck plus a loop around the neck itself.
 */
public final class StrangleTetherRenderer extends EntityRenderer<StrangleTetherEntity> {
    public StrangleTetherRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            StrangleTetherEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        Entity owner = entity.getOwnerEntity();
        LivingEntity target = entity.getTargetEntity();
        if (!(owner instanceof Player player) || target == null) {
            return;
        }

        Vec3 tetherPosition = entity.getPosition(partialTick);
        Vec3 handPosition = player.getRopeHoldPosition(partialTick);
        Vec3 localHand = handPosition.subtract(tetherPosition);

        YoYoRopeRenderer.renderRope(
                poseStack,
                buffers,
                Vec3.ZERO,
                localHand,
                0.45F
        );

        double loopRadius = Math.max(0.16D, target.getBbWidth() * 0.43D);
        YoYoRopeRenderer.renderLoop(
                poseStack,
                buffers,
                loopRadius,
                loopRadius * 0.82D
        );

        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(StrangleTetherEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
