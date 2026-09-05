package com.shenmi.advancedenchanting.network;

import com.shenmi.advancedenchanting.AdvancedEnchantingMod;
import com.shenmi.advancedenchanting.screen.AdvancedEnchantingMenu;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class AdvancedEnchantingNetworking {
    public static final ResourceLocation MENU_ACTION = AdvancedEnchantingMod.id("menu_action");
    public static final ResourceLocation MENU_STATE = AdvancedEnchantingMod.id("menu_state");

    private AdvancedEnchantingNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(MENU_ACTION,
                (server, player, handler, buffer, responseSender) -> {
                    int containerId = buffer.readVarInt();
                    int buttonId = buffer.readVarInt();

                    server.execute(() -> {
                        if (!(player.containerMenu instanceof AdvancedEnchantingMenu menu)
                                || menu.containerId != containerId) {
                            return;
                        }

                        menu.clickMenuButton(player, buttonId);
                        sendMenuState(player, menu);
                    });
                });
    }

    public static void sendMenuState(ServerPlayer player, AdvancedEnchantingMenu menu) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(menu.containerId);
        buffer.writeVarInt(menu.getSelectedEnchantmentId());
        buffer.writeVarInt(menu.getSelectedLevel());
        buffer.writeVarInt(menu.getSelectionRevision());

        Map<Integer, Integer> snapshot = menu.getSelectedLevelsSnapshot();
        buffer.writeVarInt(snapshot.size());
        snapshot.forEach((rawId, level) -> {
            buffer.writeVarInt(rawId);
            buffer.writeVarInt(level);
        });

        ServerPlayNetworking.send(player, MENU_STATE, buffer);
    }
}
