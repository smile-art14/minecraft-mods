package com.shenmi.seadis.mixin;

import com.shenmi.seadis.SeaDisdainFishingMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void seaDisdain$explodeInsideFishingPlayer(CallbackInfo callback) {
		AbstractHurtingProjectile projectile = (AbstractHurtingProjectile) (Object) this;
		if (!(projectile instanceof DragonFireball fireball)
				|| fireball.level().isClientSide
				|| !fireball.isAlive()
				|| !fireball.getTags().contains(SeaDisdainFishingMod.CAUGHT_DRAGON_FIREBALL_TAG)
				|| !(fireball.getOwner() instanceof Player player)) {
			return;
		}

		Vec3 movement = fireball.getDeltaMovement();
		AABB sweptBounds = fireball.getBoundingBox()
				.expandTowards(movement.scale(-1.0D))
				.inflate(0.05D);
		if (sweptBounds.intersects(player.getBoundingBox())) {
			((DragonFireballInvoker) fireball).seaDisdain$invokeOnHit(new EntityHitResult(player));
		}
	}
}
