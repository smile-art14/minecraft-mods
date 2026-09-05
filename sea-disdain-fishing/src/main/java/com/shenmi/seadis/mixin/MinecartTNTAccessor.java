package com.shenmi.seadis.mixin;

import net.minecraft.world.entity.vehicle.MinecartTNT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecartTNT.class)
public interface MinecartTNTAccessor {
	@Accessor("fuse")
	void seaDisdain$setFuse(int fuse);
}
