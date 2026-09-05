package com.example.mixin;

import com.example.ExampleMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentCategory.class)
public abstract class EnchantmentCategoryMixin {
	@Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
	private void mcdemo$allowRangedEnchantments(Item item, CallbackInfoReturnable<Boolean> callback) {
		EnchantmentCategory category = (EnchantmentCategory) (Object) this;
		if (item == ExampleMod.FLAME_WAND
				&& (category == EnchantmentCategory.BOW || category == EnchantmentCategory.CROSSBOW)) {
			callback.setReturnValue(true);
		}
	}
}
