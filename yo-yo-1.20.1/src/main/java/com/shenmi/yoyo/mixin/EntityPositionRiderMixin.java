package com.shenmi.yoyo.mixin;

import com.shenmi.yoyo.api.PlayerStandingState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps a rider marked with STANDING_ON_PLAYER on top of a player's head.
 * This runs on both server and client, so position stays identical for every
 * observer while still using vanilla's directly synchronized passenger link.
 */
@Mixin(Entity.class)
public abstract class EntityPositionRiderMixin {
    @Inject(
            method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shenmiYoyo$positionStandingPassenger(
            Entity passenger,
            Entity.MoveFunction moveFunction,
            CallbackInfo ci
    ) {
        Entity vehicle = (Entity) (Object) this;
        if (!(vehicle instanceof Player)) {
            return;
        }
        if (!(passenger instanceof LivingEntity living)) {
            return;
        }
        if (!PlayerStandingState.isStandingOnPlayer(living)) {
            return;
        }

        double y = vehicle.getY() + vehicle.getBbHeight() + 0.04D;
        moveFunction.accept(passenger, vehicle.getX(), y, vehicle.getZ());
        ci.cancel();
    }
}
