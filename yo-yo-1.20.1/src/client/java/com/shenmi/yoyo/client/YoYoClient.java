package com.shenmi.yoyo.client;

import com.shenmi.yoyo.YoYoMod;
import com.shenmi.yoyo.api.PlayerStandingState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.player.Player;

public final class YoYoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(YoYoMod.YOYO_PROJECTILE, YoYoProjectileRenderer::new);
        EntityRendererRegistry.register(YoYoMod.STRANGLE_TETHER, StrangleTetherRenderer::new);
        EntityRendererRegistry.register(YoYoMod.STANDING_SEAT, NoopRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (minecraft.level == null) {
                return;
            }

            for (Player rider : minecraft.level.players()) {
                if (!PlayerStandingState.isAttachedToPlayer(rider)) {
                    continue;
                }

                int targetId = PlayerStandingState.getStandingTargetId(rider);
                var targetEntity = minecraft.level.getEntity(targetId);
                if (!(targetEntity instanceof Player target) || target == rider) {
                    continue;
                }

                double attachedOffset = PlayerStandingState.isSittingOnPlayer(rider)
                        ? -0.70D
                        : 0.04D;
                double attachedY = target.getY() + target.getBbHeight() + attachedOffset;
                rider.setPos(target.getX(), attachedY, target.getZ());
                rider.setDeltaMovement(0.0D, 0.0D, 0.0D);
                rider.fallDistance = 0.0F;

                // Keep interpolation anchored to the carrier as well. Without this,
                // a remote player can visually lerp back toward an old movement-packet
                // position for part of a frame even though the synchronized standing
                // target is already correct.
                rider.xo = target.xo;
                rider.yo = target.yo + target.getBbHeight() + attachedOffset;
                rider.zo = target.zo;
                rider.xOld = target.xOld;
                rider.yOld = target.yOld + target.getBbHeight() + attachedOffset;
                rider.zOld = target.zOld;
            }
        });
    }
}
