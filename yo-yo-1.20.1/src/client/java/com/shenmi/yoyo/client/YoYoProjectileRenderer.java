package com.shenmi.yoyo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.shenmi.yoyo.entity.YoYoProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the thrown yo-yo with a physical yo-yo orientation instead of the
 * vanilla thrown-item billboard. The axle stays horizontal and perpendicular
 * to the horizontal flight direction, so looking along the throw shows the
 * thin side profile rather than the full face.
 */
public final class YoYoProjectileRenderer extends EntityRenderer<YoYoProjectileEntity> {
    private static final float SPIN_DEGREES_PER_TICK = 42.0F;
    private final ItemRenderer itemRenderer;

    public YoYoProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.12F;
    }

    @Override
    public void render(
            YoYoProjectileEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 horizontal = new Vec3(velocity.x, 0.0D, velocity.z);

        float travelYaw;
        if (horizontal.lengthSqr() > 1.0E-5D) {
            travelYaw = (float) Math.toDegrees(Math.atan2(horizontal.x, horizontal.z));
        } else {
            travelYaw = entity.getYRot();
        }

        poseStack.pushPose();
        // The model's axle is its local Z axis. Rotating yaw + 90 degrees puts
        // that axle on the horizontal side axis, perpendicular to flight.
        poseStack.mulPose(Axis.YP.rotationDegrees(travelYaw + 90.0F));
        // Spin around the model's own axle after the flight orientation is set.
        float spin = (entity.tickCount + partialTick) * SPIN_DEGREES_PER_TICK;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

        itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffers,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();

        Entity owner = entity.getOwner();
        if (owner instanceof Player player) {
            Vec3 projectilePosition = entity.getPosition(partialTick)
                    .add(0.0D, entity.getBbHeight() * 0.50D, 0.0D);
            Vec3 handPosition = player.getRopeHoldPosition(partialTick);
            Vec3 toHand = handPosition.subtract(projectilePosition);

            YoYoRopeRenderer.renderRope(
                    poseStack,
                    buffers,
                    new Vec3(0.0D, entity.getBbHeight() * 0.50D, 0.0D),
                    toHand.add(0.0D, entity.getBbHeight() * 0.50D, 0.0D),
                    1.0F
            );
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(YoYoProjectileEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
