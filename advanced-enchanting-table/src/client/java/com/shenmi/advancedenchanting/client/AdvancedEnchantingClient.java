package com.shenmi.advancedenchanting.client;

import com.shenmi.advancedenchanting.AdvancedEnchantingMod;
import com.shenmi.advancedenchanting.network.AdvancedEnchantingNetworking;
import com.shenmi.advancedenchanting.screen.AdvancedEnchantingMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AdvancedEnchantingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(AdvancedEnchantingMod.MENU_TYPE, AdvancedEnchantingScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(AdvancedEnchantingNetworking.MENU_STATE,
                (client, handler, buffer, responseSender) -> {
                    int containerId = buffer.readVarInt();
                    int focusedRawId = buffer.readVarInt();
                    int focusedLevel = buffer.readVarInt();
                    int revision = buffer.readVarInt();
                    int count = buffer.readVarInt();
                    Map<Integer, Integer> snapshot = new LinkedHashMap<>();
                    for (int i = 0; i < count; i++) {
                        snapshot.put(buffer.readVarInt(), buffer.readVarInt());
                    }

                    client.execute(() -> {
                        if (client.player != null
                                && client.player.containerMenu instanceof AdvancedEnchantingMenu menu
                                && menu.containerId == containerId) {
                            menu.syncClientSelections(snapshot, focusedRawId, focusedLevel, revision);
                        }
                    });
                });
    }
}
