package com.shenmi.seadis.mixin;

import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DragonFireball.class)
public interface DragonFireballInvoker {
	@Invoker("onHit")
	void seaDisdain$invokeOnHit(HitResult hitResult);
}
