package com.shenmi.advancedenchanting.client;

import com.shenmi.advancedenchanting.AdvancedEnchantingMod;
import com.shenmi.advancedenchanting.network.AdvancedEnchantingNetworking;
import com.shenmi.advancedenchanting.screen.AdvancedEnchantingMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AdvancedEnchantingScreen extends AbstractContainerScreen<AdvancedEnchantingMenu> {
    private static final int PAGE_SIZE = 5;
    private static final int ACTION_PREVIOUS = -1;
    private static final int ACTION_NEXT = -2;

    private static final int COLOR_FRAME = 0xFF0C0912;
    private static final int COLOR_PANEL = 0xFF191225;
    private static final int COLOR_PANEL_LIGHT = 0xFF241A33;
    private static final int COLOR_PURPLE = 0xFF73528C;
    private static final int COLOR_PURPLE_BRIGHT = 0xFFA47BC0;
    private static final int COLOR_GOLD_DARK = 0xFF704500;
    private static final int COLOR_GOLD = 0xFFD99C20;
    private static final int COLOR_GOLD_BRIGHT = 0xFFFFD66B;
    private static final int COLOR_CYAN = 0xFF58D9C2;
    private static final int COLOR_TEXT = 0xFFE9DFF2;
    private static final int COLOR_MUTED = 0xFF9A8CA8;
    private static final int COLOR_DISABLED = 0xFF5E5666;

    private final List<ClickRegion> clickRegions = new ArrayList<>();
    private List<Enchantment> visibleChoices = List.of();
    private int page;
    private int pageCount = 1;
    private int lastItemFingerprint;
    private int lastSelected = Integer.MIN_VALUE;
    private int lastLevel = Integer.MIN_VALUE;
    private int lastRevision = Integer.MIN_VALUE;

    public AdvancedEnchantingScreen(AdvancedEnchantingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 218;
        inventoryLabelX = 44;
        inventoryLabelY = 125;
        titleLabelX = 26;
        titleLabelY = 8;
    }

    @Override
    protected void init() {
        super.init();
        rebuildRegions();
    }

    private void rebuildRegions() {
        clearWidgets();
        clickRegions.clear();
        List<Enchantment> choices = AdvancedEnchantingMenu.compatibleEnchantments(menu.getSlot(0).getItem());
        pageCount = Math.max(1, (choices.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Mth.clamp(page, 0, pageCount - 1);
        int from = page * PAGE_SIZE;
        int to = Math.min(choices.size(), from + PAGE_SIZE);
        visibleChoices = choices.subList(from, to);

        for (int i = 0; i < visibleChoices.size(); i++) {
            Enchantment enchantment = visibleChoices.get(i);
            int rawId = BuiltInRegistries.ENCHANTMENT.getId(enchantment);
            if (menu.isEnchantmentSelected(rawId) || menu.canSelect(enchantment)) {
                clickRegions.add(new ClickRegion(leftPos + 57, topPos + 36 + i * 15, 137, 14,
                        AdvancedEnchantingMenu.BUTTON_ENCHANT_BASE + rawId));
            }
        }
        if (page > 0) clickRegions.add(new ClickRegion(leftPos + 57, topPos + 22, 15, 12, ACTION_PREVIOUS));
        if (page + 1 < pageCount) clickRegions.add(new ClickRegion(leftPos + 179, topPos + 22, 15, 12, ACTION_NEXT));

        Enchantment selected = menu.getSelectedEnchantment();
        boolean focused = selected != null && menu.isEnchantmentSelected(menu.getSelectedEnchantmentId());
        if (focused && menu.getSelectedLevel() > 1) {
            clickRegions.add(new ClickRegion(leftPos + 202, topPos + 58, 17, 16,
                    AdvancedEnchantingMenu.BUTTON_LEVEL_BASE + menu.getSelectedLevel() - 1));
        }
        if (focused && menu.getSelectedLevel() < selected.getMaxLevel()) {
            clickRegions.add(new ClickRegion(leftPos + 222, topPos + 58, 17, 16,
                    AdvancedEnchantingMenu.BUTTON_LEVEL_BASE + menu.getSelectedLevel() + 1));
        }
        if (minecraft != null && minecraft.player != null && menu.canApply(minecraft.player)) {
            clickRegions.add(new ClickRegion(leftPos + 201, topPos + 82, 39, 31,
                    AdvancedEnchantingMenu.BUTTON_APPLY));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawFrame(graphics);
        drawInputPanel(graphics);
        drawEnchantmentPanel(graphics, mouseX, mouseY);
        drawControlPanel(graphics, mouseX, mouseY);
        drawInventoryPanel(graphics);
    }

    private void drawFrame(GuiGraphics graphics) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOR_FRAME);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF120E1A);
        graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5, topPos + 19, 0xFF1D1428);
        graphics.fill(leftPos + 5, topPos + 18, leftPos + imageWidth - 5, topPos + 19, COLOR_GOLD_DARK);
        graphics.fill(leftPos + 7, topPos + 21, leftPos + imageWidth - 7, topPos + 122, COLOR_PANEL);
        graphics.fill(leftPos + 7, topPos + 121, leftPos + imageWidth - 7, topPos + 123, COLOR_GOLD_DARK);
        graphics.renderItem(new ItemStack(AdvancedEnchantingMod.ADVANCED_ENCHANTING_TABLE), leftPos + 7, topPos + 3);
    }

    private void drawInputPanel(GuiGraphics graphics) {
        drawPanel(graphics, leftPos + 11, topPos + 25, 42, 92, COLOR_PURPLE);
        graphics.drawCenteredString(font, Component.translatable("label.advanced_enchanting.input"),
                leftPos + 32, topPos + 29, COLOR_GOLD_BRIGHT);
        drawDecoratedSlot(graphics, leftPos + 17, topPos + 42, COLOR_GOLD);
        drawDecoratedSlot(graphics, leftPos + 17, topPos + 72, COLOR_CYAN);
        graphics.drawCenteredString(font, Component.translatable("label.advanced_enchanting.lapis"),
                leftPos + 32, topPos + 94, COLOR_MUTED);
    }

    private void drawEnchantmentPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos + 55, topPos + 25, 141, 92, COLOR_PURPLE);
        graphics.drawCenteredString(font, Component.translatable("label.advanced_enchanting.available_page",
                        page + 1, pageCount),
                leftPos + 125, topPos + 27, COLOR_GOLD_BRIGHT);

        drawSmallButton(graphics, leftPos + 57, topPos + 22, 15, 12, "<",
                page > 0, isHovered(leftPos + 57, topPos + 22, 15, 12, mouseX, mouseY));
        drawSmallButton(graphics, leftPos + 179, topPos + 22, 15, 12, ">",
                page + 1 < pageCount, isHovered(leftPos + 179, topPos + 22, 15, 12, mouseX, mouseY));
        ItemStack input = menu.getSlot(0).getItem();
        if (input.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("label.advanced_enchanting.empty_hint"),
                    leftPos + 125, topPos + 67, COLOR_MUTED);
            return;
        }
        if (visibleChoices.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("label.advanced_enchanting.no_options"),
                    leftPos + 125, topPos + 67, COLOR_DISABLED);
            return;
        }

        for (int i = 0; i < visibleChoices.size(); i++) {
            Enchantment enchantment = visibleChoices.get(i);
            int rawId = BuiltInRegistries.ENCHANTMENT.getId(enchantment);
            boolean selected = menu.isEnchantmentSelected(rawId);
            boolean focused = rawId == menu.getSelectedEnchantmentId();
            boolean enabled = selected || menu.canSelect(enchantment);
            int x = leftPos + 57;
            int y = topPos + 36 + i * 15;
            boolean hovered = isHovered(x, y, 137, 14, mouseX, mouseY);
            drawChoice(graphics, x, y, enchantment, selected, focused, enabled, hovered);
        }
    }

    private void drawChoice(GuiGraphics graphics, int x, int y, Enchantment enchantment,
                            boolean selected, boolean focused, boolean enabled, boolean hovered) {
        int border = focused ? COLOR_GOLD_BRIGHT : (selected ? COLOR_CYAN : COLOR_PURPLE);
        int background = hovered && enabled ? 0xFF39284B : COLOR_PANEL_LIGHT;
        if (!enabled) background = 0xFF17131C;
        graphics.fill(x, y, x + 137, y + 14, border);
        graphics.fill(x + 1, y + 1, x + 136, y + 13, background);
        graphics.fill(x + 4, y + 4, x + 10, y + 10, selected ? COLOR_CYAN : 0xFF0D0A12);
        if (!selected) {
            graphics.fill(x + 5, y + 5, x + 9, y + 9, enabled ? COLOR_PURPLE_BRIGHT : COLOR_DISABLED);
        }
        String rawName = Component.translatable(enchantment.getDescriptionId()).getString();
        String name = font.plainSubstrByWidth(rawName, 103);
        int textColor = enabled ? COLOR_TEXT : COLOR_DISABLED;
        graphics.drawString(font, name, x + 14, y + 3, textColor, false);
        if (selected) {
            int level = menu.getLevelForEnchantment(BuiltInRegistries.ENCHANTMENT.getId(enchantment));
            if (level > 0) graphics.drawString(font, "Lv." + level, x + 108, y + 3, COLOR_GOLD_BRIGHT, false);
        }
    }

    private void drawControlPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos + 198, topPos + 25, 43, 92, COLOR_PURPLE);
        graphics.drawCenteredString(font, Component.translatable("label.advanced_enchanting.level_short"),
                leftPos + 219, topPos + 30, COLOR_GOLD_BRIGHT);

        Enchantment selected = menu.getSelectedEnchantment();
        boolean focused = selected != null && menu.isEnchantmentSelected(menu.getSelectedEnchantmentId());
        String levelText = focused ? menu.getSelectedLevel() + "/" + selected.getMaxLevel() : "--";
        graphics.drawCenteredString(font, levelText, leftPos + 219, topPos + 45, COLOR_TEXT);

        boolean minusEnabled = focused && menu.getSelectedLevel() > 1;
        boolean plusEnabled = focused && menu.getSelectedLevel() < selected.getMaxLevel();
        drawSmallButton(graphics, leftPos + 202, topPos + 58, 17, 16, "-", minusEnabled,
                isHovered(leftPos + 202, topPos + 58, 17, 16, mouseX, mouseY));
        drawSmallButton(graphics, leftPos + 222, topPos + 58, 17, 16, "+", plusEnabled,
                isHovered(leftPos + 222, topPos + 58, 17, 16, mouseX, mouseY));

        boolean canApply = minecraft != null && minecraft.player != null && menu.canApply(minecraft.player);
        drawApplyButton(graphics, leftPos + 201, topPos + 82, 39, 31, canApply,
                isHovered(leftPos + 201, topPos + 82, 39, 31, mouseX, mouseY));
    }

    private void drawInventoryPanel(GuiGraphics graphics) {
        graphics.fill(leftPos + 40, topPos + 130, leftPos + 210, topPos + 217, COLOR_PURPLE);
        graphics.fill(leftPos + 42, topPos + 132, leftPos + 208, topPos + 216, COLOR_PANEL);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawInventorySlot(graphics, leftPos + 43 + column * 18, topPos + 135 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawInventorySlot(graphics, leftPos + 43 + column * 18, topPos + 193);
        }
        graphics.fill(leftPos + 43, topPos + 191, leftPos + 207, topPos + 192, COLOR_GOLD_DARK);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int border) {
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, COLOR_PANEL_LIGHT);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, COLOR_PANEL);
    }

    private void drawDecoratedSlot(GuiGraphics graphics, int x, int y, int accent) {
        graphics.fill(x - 2, y - 2, x + 20, y + 20, COLOR_FRAME);
        graphics.fill(x - 1, y - 1, x + 19, y + 19, accent);
        graphics.fill(x, y, x + 18, y + 18, 0xFF0D0A12);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF15101D);
    }

    private void drawInventorySlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, COLOR_PURPLE);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF110D18);
    }

    private void drawSmallButton(GuiGraphics graphics, int x, int y, int width, int height, String text,
                                 boolean enabled, boolean hovered) {
        int border = enabled ? (hovered ? COLOR_GOLD_BRIGHT : COLOR_PURPLE_BRIGHT) : COLOR_DISABLED;
        int fill = enabled ? (hovered ? 0xFF443058 : COLOR_PANEL_LIGHT) : 0xFF17131C;
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.drawCenteredString(font, text, x + width / 2, y + (height - 8) / 2, enabled ? COLOR_TEXT : COLOR_DISABLED);
    }

    private void drawApplyButton(GuiGraphics graphics, int x, int y, int width, int height,
                                 boolean enabled, boolean hovered) {
        int border = enabled ? (hovered ? COLOR_GOLD_BRIGHT : COLOR_GOLD) : COLOR_DISABLED;
        int fill = enabled ? (hovered ? 0xFF6B430C : 0xFF4C310B) : 0xFF1B171E;
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
        int color = enabled ? COLOR_GOLD_BRIGHT : COLOR_DISABLED;
        graphics.drawCenteredString(font, Component.translatable("button.advanced_enchanting.apply_line1"),
                x + width / 2, y + 7, color);
        graphics.drawCenteredString(font, Component.translatable("button.advanced_enchanting.apply_line2"),
                x + width / 2, y + 17, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, COLOR_GOLD_BRIGHT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COLOR_MUTED, false);

        boolean canApply = minecraft != null && minecraft.player != null && menu.canApply(minecraft.player);
        int summaryColor = menu.getSelectedCount() == 0 ? COLOR_MUTED : (canApply ? COLOR_CYAN : 0xFFFF7777);
        Component summary = Component.translatable("label.advanced_enchanting.summary",
                menu.getSelectedCount(), menu.getExperienceCost(), menu.getLapisCost());
        graphics.drawCenteredString(font, summary, imageWidth / 2, 111, summaryColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ClickRegion region : clickRegions) {
                if (region.contains(mouseX, mouseY)) {
                    if (region.action == ACTION_PREVIOUS) {
                        page--;
                        rebuildRegions();
                    } else if (region.action == ACTION_NEXT) {
                        page++;
                        rebuildRegions();
                    } else {
                        sendButton(region.action);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButton(int id) {
        if (minecraft == null || minecraft.player == null) return;
        if (!menu.clickMenuButton(minecraft.player, id)) return;

        var buffer = PacketByteBufs.create();
        buffer.writeVarInt(menu.containerId);
        buffer.writeVarInt(id);
        ClientPlayNetworking.send(AdvancedEnchantingNetworking.MENU_ACTION, buffer);
        rebuildRegions();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack stack = menu.getSlot(0).getItem();
        int fingerprint = Objects.hash(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getTag(), stack.getCount());
        boolean revisionChanged = menu.getSelectionRevision() != lastRevision;
        if (revisionChanged) {
            lastRevision = menu.getSelectionRevision();
        }
        if (revisionChanged || fingerprint != lastItemFingerprint || menu.getSelectedEnchantmentId() != lastSelected
                || menu.getSelectedLevel() != lastLevel) {
            lastItemFingerprint = fingerprint;
            lastSelected = menu.getSelectedEnchantmentId();
            lastLevel = menu.getSelectedLevel();
            rebuildRegions();
        }
    }

    private static boolean isHovered(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private record ClickRegion(int x, int y, int width, int height, int action) {
        boolean contains(double mouseX, double mouseY) {
            return isHovered(x, y, width, height, mouseX, mouseY);
        }
    }
}
