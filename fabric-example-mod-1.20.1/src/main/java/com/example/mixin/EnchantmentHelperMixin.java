package com.example.mixin;

import com.example.ExampleMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"), cancellable = true)
	private static void mcdemo$offerChannelingAtEnchantingTable(
			int enchantingPower,
			ItemStack stack,
			boolean allowTreasure,
			CallbackInfoReturnable<List<EnchantmentInstance>> callback
	) {
		List<EnchantmentInstance> available = new ArrayList<>(callback.getReturnValue());
		available.removeIf(instance -> instance.enchantment == ExampleMod.THUNDER_CHANNELING);
		available.removeIf(instance -> instance.enchantment == ExampleMod.FREEZING);
		available.removeIf(instance -> instance.enchantment == ExampleMod.WIND_EYE);
		available.removeIf(instance -> instance.enchantment == ExampleMod.TORNADO);
		available.removeIf(instance -> instance.enchantment == ExampleMod.EXPLOSION);
		available.removeIf(instance -> instance.enchantment == ExampleMod.DESTRUCTION);
		available.removeIf(instance -> instance.enchantment == ExampleMod.SWORD_WAVE_LENGTH);
		available.removeIf(instance -> instance.enchantment == ExampleMod.SWORD_WAVE_DISTANCE);
		if (stack.is(ExampleMod.THUNDER_WAND)) {
			addAtPower(available, ExampleMod.THUNDER_CHANNELING, enchantingPower);
		} else if (stack.is(ExampleMod.FLAME_WAND) || stack.is(ExampleMod.ICE_WAND)) {
			addRangedEnchantments(available, enchantingPower);
			if (stack.is(ExampleMod.ICE_WAND)) {
				addAtPower(available, ExampleMod.FREEZING, enchantingPower);
			}
		} else if (stack.is(ExampleMod.HURRICANE_WAND)) {
			addAtPower(available, ExampleMod.WIND_EYE, enchantingPower);
			addAtPower(available, ExampleMod.TORNADO, enchantingPower);
			addAtPower(available, Enchantments.MULTISHOT, enchantingPower);
		} else if (stack.is(ExampleMod.EXCALIBUR)) {
			addAtPower(available, ExampleMod.EXPLOSION, enchantingPower);
			addAtPower(available, ExampleMod.DESTRUCTION, enchantingPower);
			addAtPower(available, ExampleMod.SWORD_WAVE_LENGTH, enchantingPower);
			addAtPower(available, ExampleMod.SWORD_WAVE_DISTANCE, enchantingPower);
			addAtPower(available, Enchantments.MULTISHOT, enchantingPower);
		} else {
			return;
		}
		callback.setReturnValue(available);
	}

	private static void addRangedEnchantments(List<EnchantmentInstance> available, int enchantingPower) {
		addAtPower(available, Enchantments.POWER_ARROWS, enchantingPower);
		addAtPower(available, Enchantments.PUNCH_ARROWS, enchantingPower);
		addAtPower(available, Enchantments.FLAMING_ARROWS, enchantingPower);
		addAtPower(available, Enchantments.INFINITY_ARROWS, enchantingPower);
		addAtPower(available, Enchantments.MULTISHOT, enchantingPower);
		addAtPower(available, Enchantments.QUICK_CHARGE, enchantingPower);
		addAtPower(available, Enchantments.PIERCING, enchantingPower);
	}

	private static void addAtPower(
			List<EnchantmentInstance> available,
			Enchantment enchantment,
			int enchantingPower
	) {
		boolean alreadyPresent = available.stream()
				.anyMatch(instance -> instance.enchantment == enchantment);
		if (alreadyPresent) {
			return;
		}

		for (int level = enchantment.getMaxLevel(); level >= enchantment.getMinLevel(); level--) {
			if (enchantingPower >= enchantment.getMinCost(level)
					&& enchantingPower <= enchantment.getMaxCost(level)) {
				available.add(new EnchantmentInstance(enchantment, level));
				return;
			}
		}
	}
}
