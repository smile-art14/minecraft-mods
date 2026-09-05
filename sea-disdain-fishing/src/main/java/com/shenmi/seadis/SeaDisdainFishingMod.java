package com.shenmi.seadis;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SeaDisdainFishingMod implements ModInitializer {
	public static final String MOD_ID = "sea_disdain";
	public static final String CAUGHT_DRAGON_FIREBALL_TAG = "sea_disdain.caught_dragon_fireball";
	private static final String LEGACY_ENCHANTMENT_NAMESPACE = "mcdemo";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Enchantment SEA_DISDAIN = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			new ResourceLocation(LEGACY_ENCHANTMENT_NAMESPACE, "sea_disdain"),
			new SeaDisdainEnchantment()
	);

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries ->
				entries.accept(createSeaDisdainBook())
		);
		LOGGER.info("Sea Disdain Fishing initialized.");
	}

	private static ItemStack createSeaDisdainBook() {
		ItemStack book = EnchantedBookItem.createForEnchantment(
				new EnchantmentInstance(SEA_DISDAIN, 1)
		);
		book.setHoverName(Component.translatable("item.sea_disdain.sea_disdain_book"));
		return book;
	}

	private static final class SeaDisdainEnchantment extends Enchantment {
		private SeaDisdainEnchantment() {
			super(Rarity.VERY_RARE, EnchantmentCategory.FISHING_ROD,
					new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
		}

		@Override
		public int getMinCost(int level) {
			return 20;
		}

		@Override
		public int getMaxCost(int level) {
			return 50;
		}

		@Override
		public int getMaxLevel() {
			return 1;
		}
	}
}
