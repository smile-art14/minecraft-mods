package com.shenmi.magnet;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class MagnetEnchantmentMod implements ModInitializer {
    public static final String MOD_ID = "magnet_enchantment";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final double ATTRACT_RADIUS = 8.0D;
    private static final double TURN_FACTOR = 0.28D;
    private static final double MIN_PROJECTILE_SPEED = 0.35D;

    public static final Enchantment MAGNET = Registry.register(
            BuiltInRegistries.ENCHANTMENT,
            new ResourceLocation(MOD_ID, "magnet"),
            new MagnetHelmetEnchantment()
    );

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                attractProjectiles(player);
            }
        });

        LOGGER.info("Magnet Enchantment initialized. Radius={} blocks", ATTRACT_RADIUS);
    }

    private static void attractProjectiles(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || EnchantmentHelper.getItemEnchantmentLevel(MAGNET, helmet) <= 0) {
            return;
        }

        AABB searchBox = player.getBoundingBox().inflate(ATTRACT_RADIUS);
        List<Projectile> projectiles = player.serverLevel().getEntitiesOfClass(
                Projectile.class,
                searchBox,
                projectile -> projectile.isAlive()
                        && projectile.getOwner() != player
                        && projectile.getDeltaMovement().lengthSqr() > 0.0001D
        );

        Vec3 target = new Vec3(player.getX(), player.getEyeY() + 0.15D, player.getZ());

        for (Projectile projectile : projectiles) {
            Vec3 toTarget = target.subtract(projectile.position());
            double distance = toTarget.length();
            if (distance < 0.001D) {
                continue;
            }

            Vec3 currentVelocity = projectile.getDeltaMovement();
            double currentSpeed = Math.max(currentVelocity.length(), MIN_PROJECTILE_SPEED);
            double pullSpeed = currentSpeed + Math.min(0.30D, distance * 0.025D);
            Vec3 desiredVelocity = toTarget.scale(1.0D / distance).scale(pullSpeed);
            Vec3 newVelocity = currentVelocity.scale(1.0D - TURN_FACTOR)
                    .add(desiredVelocity.scale(TURN_FACTOR));

            projectile.setDeltaMovement(newVelocity);
            projectile.hasImpulse = true;
        }
    }

    private static final class MagnetHelmetEnchantment extends Enchantment {
        private MagnetHelmetEnchantment() {
            super(Rarity.RARE, EnchantmentCategory.ARMOR_HEAD, new EquipmentSlot[]{EquipmentSlot.HEAD});
        }

        @Override
        public int getMinCost(int level) {
            return 15;
        }

        @Override
        public int getMaxCost(int level) {
            return 45;
        }

        @Override
        public int getMaxLevel() {
            return 1;
        }

        @Override
        public boolean canEnchant(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem armorItem
                    && armorItem.getEquipmentSlot() == EquipmentSlot.HEAD;
        }
    }
}
