package com.example.mixin;

import com.example.ExampleMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	@Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
	private void mcdemo$allowChannelingOnThunderWand(
			ItemStack stack,
			CallbackInfoReturnable<Boolean> callback
	) {
		Object enchantment = this;
		boolean isRangedWandEnchantment = enchantment == Enchantments.POWER_ARROWS
				|| enchantment == Enchantments.PUNCH_ARROWS
				|| enchantment == Enchantments.FLAMING_ARROWS
				|| enchantment == Enchantments.INFINITY_ARROWS
				|| enchantment == Enchantments.MULTISHOT
				|| enchantment == Enchantments.QUICK_CHARGE
				|| enchantment == Enchantments.PIERCING;
		boolean isProjectileWand = stack.is(ExampleMod.FLAME_WAND) || stack.is(ExampleMod.ICE_WAND);
		boolean isHurricaneMultishot = enchantment == Enchantments.MULTISHOT
				&& stack.is(ExampleMod.HURRICANE_WAND);
		boolean isExcaliburMultishot = enchantment == Enchantments.MULTISHOT
				&& stack.is(ExampleMod.EXCALIBUR);
		boolean isSwallowQuickCharge = enchantment == Enchantments.QUICK_CHARGE
				&& stack.getItem() instanceof SwordItem;
		if ((isRangedWandEnchantment && isProjectileWand)
				|| isHurricaneMultishot
				|| isExcaliburMultishot
				|| isSwallowQuickCharge) {
			callback.setReturnValue(true);
		}
	}
}
