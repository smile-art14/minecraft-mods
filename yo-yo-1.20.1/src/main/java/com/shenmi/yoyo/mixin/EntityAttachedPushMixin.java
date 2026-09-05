package com.shenmi.yoyo.mixin;

import com.shenmi.yoyo.api.PlayerStandingState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player visually attached to another player's head is not a vanilla
 * passenger, so their physical boxes may overlap. Suppress collision pushing
 * only for that bound pair; normal player/player collisions are unchanged.
 */
@Mixin(Entity.class)
public abstract class EntityAttachedPushMixin {
    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shenmiYoyo$skipAttachedPairPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (PlayerStandingState.isAttachedPair(self, other)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "canCollideWith(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shenmiYoyo$skipAttachedPairCollision(
            Entity other,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Entity self = (Entity) (Object) this;
        if (PlayerStandingState.isAttachedPair(self, other)) {
            cir.setReturnValue(false);
        }
    }
}
