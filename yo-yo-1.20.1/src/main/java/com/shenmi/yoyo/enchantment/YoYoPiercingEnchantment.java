package com.shenmi.yoyo.enchantment;

import com.shenmi.yoyo.YoYoMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Lets a thrown yo-yo continue through additional living targets before it
 * starts returning to its owner. Level N adds N extra targets.
 */
public final class YoYoPiercingEnchantment extends Enchantment {
    public YoYoPiercingEnchantment() {
        super(
                Rarity.RARE,
                EnchantmentCategory.WEAPON,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}
        );
    }

    @Override
    public int getMinCost(int level) {
        return 12 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.is(YoYoMod.YOYO);
    }
}
