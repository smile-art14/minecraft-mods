package com.shenmi.advancedenchanting;

import com.shenmi.advancedenchanting.block.AdvancedEnchantingTableBlock;
import com.shenmi.advancedenchanting.blockentity.AdvancedEnchantingTableBlockEntity;
import com.shenmi.advancedenchanting.network.AdvancedEnchantingNetworking;
import com.shenmi.advancedenchanting.screen.AdvancedEnchantingMenu;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.flag.FeatureFlags;

public final class AdvancedEnchantingMod implements ModInitializer {
    public static final String MOD_ID = "advanced_enchanting";

    public static final Block ADVANCED_ENCHANTING_TABLE = new AdvancedEnchantingTableBlock(
            BlockBehaviour.Properties.of().strength(5.0F, 1200.0F).sound(SoundType.STONE).lightLevel(state -> 7));
    public static BlockEntityType<AdvancedEnchantingTableBlockEntity> BLOCK_ENTITY_TYPE;
    public static MenuType<AdvancedEnchantingMenu> MENU_TYPE;

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.BLOCK, id("advanced_enchanting_table"), ADVANCED_ENCHANTING_TABLE);
        Registry.register(BuiltInRegistries.ITEM, id("advanced_enchanting_table"),
                new BlockItem(ADVANCED_ENCHANTING_TABLE, new Item.Properties()));

        BLOCK_ENTITY_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("advanced_enchanting_table"),
                BlockEntityType.Builder.of(AdvancedEnchantingTableBlockEntity::new, ADVANCED_ENCHANTING_TABLE).build(null));
        MENU_TYPE = Registry.register(BuiltInRegistries.MENU, id("advanced_enchanting_table"),
                new MenuType<>(AdvancedEnchantingMenu::new, FeatureFlags.DEFAULT_FLAGS));
        AdvancedEnchantingNetworking.registerServerReceivers();

        ResourceKey<CreativeModeTab> wandsTab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB, new ResourceLocation("mcdemo", "wands"));
        ItemGroupEvents.modifyEntriesEvent(wandsTab)
                .register(entries -> entries.accept(ADVANCED_ENCHANTING_TABLE));
    }
}
