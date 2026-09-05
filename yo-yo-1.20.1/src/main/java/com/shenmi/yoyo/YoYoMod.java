package com.shenmi.yoyo;

import com.shenmi.yoyo.api.PlayerStandingState;
import com.shenmi.yoyo.combat.StrangleManager;
import com.shenmi.yoyo.effect.StandingOnPlayerEffect;
import com.shenmi.yoyo.enchantment.YoYoPiercingEnchantment;
import com.shenmi.yoyo.entity.StandingSeatEntity;
import com.shenmi.yoyo.entity.StrangleTetherEntity;
import com.shenmi.yoyo.entity.YoYoProjectileEntity;
import com.shenmi.yoyo.item.YoYoItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YoYoMod implements ModInitializer {
    public static final String MOD_ID = "shenmi_yoyo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Item YOYO = Registry.register(
            BuiltInRegistries.ITEM,
            id("yoyo"),
            new YoYoItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(512)
                    .rarity(Rarity.RARE))
    );

    public static final Enchantment PIERCING = Registry.register(
            BuiltInRegistries.ENCHANTMENT,
            id("piercing"),
            new YoYoPiercingEnchantment()
    );

    public static final EntityType<YoYoProjectileEntity> YOYO_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("yoyo_projectile"),
            EntityType.Builder.<YoYoProjectileEntity>of(YoYoProjectileEntity::new, MobCategory.MISC)
                    .sized(0.38F, 0.38F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("yoyo_projectile")
    );

    public static final MobEffect STANDING_ON_PLAYER = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            id("standing_on_player"),
            new StandingOnPlayerEffect()
    );

    public static final EntityType<StrangleTetherEntity> STRANGLE_TETHER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("strangle_tether"),
            EntityType.Builder.<StrangleTetherEntity>of(StrangleTetherEntity::new, MobCategory.MISC)
                    .sized(0.05F, 0.05F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("strangle_tether")
    );

    public static final EntityType<StandingSeatEntity> STANDING_SEAT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("standing_seat"),
            EntityType.Builder.<StandingSeatEntity>of(StandingSeatEntity::new, MobCategory.MISC)
                    .sized(0.05F, 0.05F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("standing_seat")
    );

    public static final CreativeModeTab YOYO_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            id("yoyo_tab"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.shenmi_yoyo.main"))
                    .icon(() -> new ItemStack(YOYO))
                    .displayItems((parameters, output) -> output.accept(YOYO))
                    .build()
    );

    @Override
    public void onInitialize() {
        StrangleManager.initialize();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                if (!PlayerStandingState.isAttachedToPlayer(player)) {
                    continue;
                }

                if (player.isShiftKeyDown() || player.isPassenger()) {
                    PlayerStandingState.stopStanding(player);
                    continue;
                }

                int targetId = PlayerStandingState.getStandingTargetId(player);
                var targetEntity = player.level().getEntity(targetId);
                if (!(targetEntity instanceof Player target)
                        || !target.isAlive()
                        || target == player) {
                    PlayerStandingState.stopStanding(player);
                    continue;
                }

                double attachedY = target.getY() + target.getBbHeight()
                        + (PlayerStandingState.isSittingOnPlayer(player) ? -0.70D : 0.04D);
                player.setPos(target.getX(), attachedY, target.getZ());
                // Position is authoritative while attached. Copying the carrier's
                // velocity makes the overlapping player participate in vanilla
                // push resolution and can feed tiny impulses back into the carrier.
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                player.fallDistance = 0.0F;
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("yoyo")
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            player.getInventory().placeItemBackInInventory(new ItemStack(YOYO));
                            context.getSource().sendSuccess(
                                    () -> Component.translatable("command.shenmi_yoyo.give"),
                                    false
                            );
                            return 1;
                        }))
        );

        LOGGER.info("Shenmi Yo-Yo initialized.");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
