package com.shenmi.advancedenchanting.screen;

import com.shenmi.advancedenchanting.AdvancedEnchantingMod;
import com.shenmi.advancedenchanting.network.AdvancedEnchantingNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdvancedEnchantingMenu extends AbstractContainerMenu {
    public static final int BUTTON_APPLY = 1;
    public static final int BUTTON_LEVEL_BASE = 2_000;
    public static final int BUTTON_ENCHANT_BASE = 100_000;
    private static final int ITEM_SLOT = 0;
    private static final int LAPIS_SLOT = 1;

    private final Container container;
    private final ContainerData data;
    private final Player owner;
    private final Map<Integer, Integer> selectedLevels = new LinkedHashMap<>();
    private ItemStack lastInput;
    private boolean applying;

    public AdvancedEnchantingMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(2), new SimpleContainerData(3));
    }

    public AdvancedEnchantingMenu(int syncId, Inventory playerInventory, Container container) {
        this(syncId, playerInventory, container, new SimpleContainerData(3));
    }

    private AdvancedEnchantingMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(AdvancedEnchantingMod.MENU_TYPE, syncId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;
        this.owner = playerInventory.player;
        this.lastInput = container.getItem(ITEM_SLOT).copy();
        data.set(0, -1);
        data.set(1, 1);
        container.startOpen(playerInventory.player);

        addSlot(new Slot(container, ITEM_SLOT, 18, 43) {
            @Override public boolean mayPlace(ItemStack stack) { return isSupportedItem(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(container, LAPIS_SLOT, 18, 73) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.LAPIS_LAZULI); }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 44 + column * 18, 136 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 44 + column * 18, 194));
        }
        addDataSlots(data);
    }

    public static boolean isSupportedItem(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK) || !compatibleEnchantments(stack).isEmpty();
    }

    public static List<Enchantment> compatibleEnchantments(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        Map<Enchantment, Integer> existing = EnchantmentHelper.getEnchantments(stack);
        boolean book = stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
        return BuiltInRegistries.ENCHANTMENT.stream()
                .filter(enchantment -> !enchantment.isCurse())
                .filter(enchantment -> book || enchantment.canEnchant(stack))
                .filter(enchantment -> existing.keySet().stream().allMatch(other ->
                        other == enchantment || enchantment.isCompatibleWith(other)))
                .sorted(Comparator.comparing(enchantment ->
                        BuiltInRegistries.ENCHANTMENT.getKey(enchantment).toString()))
                .toList();
    }

    public int getSelectedEnchantmentId() { return data.get(0); }
    public int getSelectedLevel() { return Math.max(1, data.get(1)); }
    public int getSelectionRevision() { return data.get(2); }
    public int getSelectedCount() { return selectedLevels.size(); }
    public boolean isEnchantmentSelected(int rawId) { return selectedLevels.containsKey(rawId); }
    public int getLevelForEnchantment(int rawId) { return selectedLevels.getOrDefault(rawId, 0); }
    public Map<Integer, Integer> getSelectedLevelsSnapshot() { return new LinkedHashMap<>(selectedLevels); }

    public void syncClientSelections(Map<Integer, Integer> snapshot, int focusedRawId, int focusedLevel, int revision) {
        if (!owner.level().isClientSide) return;
        selectedLevels.clear();
        selectedLevels.putAll(snapshot);
        data.set(0, focusedRawId);
        data.set(1, Math.max(1, focusedLevel));
        data.set(2, revision);
    }

    public Enchantment getSelectedEnchantment() {
        return BuiltInRegistries.ENCHANTMENT.byId(getSelectedEnchantmentId());
    }

    public int getExperienceCost() {
        int total = 0;
        for (Map.Entry<Integer, Integer> entry : selectedLevels.entrySet()) {
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.byId(entry.getKey());
            if (enchantment != null) total += entry.getValue() * rarityMultiplier(enchantment);
        }
        return total;
    }

    private static int rarityMultiplier(Enchantment enchantment) {
        int multiplier = switch (enchantment.getRarity()) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case VERY_RARE -> 4;
        };
        return multiplier;
    }

    public int getLapisCost() {
        return selectedLevels.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean canSelect(Enchantment enchantment) {
        if (enchantment == null || !compatibleEnchantments(container.getItem(ITEM_SLOT)).contains(enchantment)) return false;
        return selectedLevels.keySet().stream()
                .map(BuiltInRegistries.ENCHANTMENT::byId)
                .allMatch(other -> other == null || other == enchantment || enchantment.isCompatibleWith(other));
    }

    public boolean canApply(Player player) {
        ItemStack input = container.getItem(ITEM_SLOT);
        List<Enchantment> compatible = compatibleEnchantments(input);
        if (selectedLevels.isEmpty()) return false;
        for (Map.Entry<Integer, Integer> entry : selectedLevels.entrySet()) {
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.byId(entry.getKey());
            if (enchantment == null || !compatible.contains(enchantment)
                    || entry.getValue() < 1 || entry.getValue() > enchantment.getMaxLevel()
                    || !canSelect(enchantment)) return false;
        }
        return player.getAbilities().instabuild
                || (player.experienceLevel >= getExperienceCost()
                && container.getItem(LAPIS_SLOT).getCount() >= getLapisCost());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId >= BUTTON_ENCHANT_BASE) {
            Enchantment selected = BuiltInRegistries.ENCHANTMENT.byId(buttonId - BUTTON_ENCHANT_BASE);
            if (selected == null) return false;
            int rawId = BuiltInRegistries.ENCHANTMENT.getId(selected);
            if (selectedLevels.containsKey(rawId)) {
                selectedLevels.remove(rawId);
                if (getSelectedEnchantmentId() == rawId) focusFirstSelection();
                bumpSelectionRevision();
                broadcastChanges();
                return true;
            }
            if (canSelect(selected)) {
                int level = Math.min(Math.max(1, EnchantmentHelper.getItemEnchantmentLevel(selected,
                        container.getItem(ITEM_SLOT))), selected.getMaxLevel());
                selectedLevels.put(rawId, level);
                data.set(0, rawId);
                data.set(1, level);
                bumpSelectionRevision();
                broadcastChanges();
                return true;
            }
            return false;
        }
        if (buttonId >= BUTTON_LEVEL_BASE) {
            Enchantment selected = getSelectedEnchantment();
            int requested = buttonId - BUTTON_LEVEL_BASE;
            int rawId = getSelectedEnchantmentId();
            if (selected != null && selectedLevels.containsKey(rawId)
                    && requested >= 1 && requested <= selected.getMaxLevel()) {
                selectedLevels.put(rawId, requested);
                data.set(1, requested);
                bumpSelectionRevision();
                broadcastChanges();
                return true;
            }
            return false;
        }
        if (buttonId == BUTTON_APPLY && canApply(player)) {
            if (!player.level().isClientSide) applySelectedEnchantments(player);
            broadcastChanges();
            return true;
        }
        return false;
    }

    private void applySelectedEnchantments(Player player) {
        int experienceCost = getExperienceCost();
        int lapisCost = getLapisCost();
        ItemStack input = container.getItem(ITEM_SLOT);
        Map<Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(input));
        selectedLevels.forEach((rawId, level) -> {
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.byId(rawId);
            if (enchantment != null) enchantments.put(enchantment, level);
        });

        applying = true;
        if (input.is(Items.BOOK)) {
            ItemStack output = new ItemStack(Items.ENCHANTED_BOOK);
            if (input.hasCustomHoverName()) output.setHoverName(input.getHoverName());
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                EnchantedBookItem.addEnchantment(output, new EnchantmentInstance(entry.getKey(), entry.getValue()));
            }
            container.setItem(ITEM_SLOT, output);
        } else {
            EnchantmentHelper.setEnchantments(enchantments, input);
            container.setChanged();
        }

        if (!player.getAbilities().instabuild) {
            container.removeItem(LAPIS_SLOT, lapisCost);
            player.giveExperienceLevels(-experienceCost);
        }
        applying = false;
        lastInput = container.getItem(ITEM_SLOT).copy();
        clearSelections(true);
    }

    @Override
    public void slotsChanged(Container changedContainer) {
        super.slotsChanged(changedContainer);
        if (changedContainer == container && !owner.level().isClientSide && !applying) {
            ItemStack currentInput = container.getItem(ITEM_SLOT);
            if (!ItemStack.isSameItemSameTags(lastInput, currentInput) || lastInput.getCount() != currentInput.getCount()) {
                lastInput = currentInput.copy();
                clearSelections(true);
                if (owner instanceof ServerPlayer serverPlayer) {
                    AdvancedEnchantingNetworking.sendMenuState(serverPlayer, this);
                }
            }
            broadcastChanges();
        }
    }

    private void focusFirstSelection() {
        if (selectedLevels.isEmpty()) {
            data.set(0, -1);
            data.set(1, 1);
            return;
        }
        Map.Entry<Integer, Integer> first = selectedLevels.entrySet().iterator().next();
        data.set(0, first.getKey());
        data.set(1, first.getValue());
    }

    private void clearSelections(boolean incrementRevision) {
        selectedLevels.clear();
        data.set(0, -1);
        data.set(1, 1);
        if (incrementRevision) bumpSelectionRevision();
    }

    private void bumpSelectionRevision() {
        data.set(2, data.get(2) + 1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        if (index < 2) {
            if (!moveItemStackTo(original, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (original.is(Items.LAPIS_LAZULI)) {
            if (!moveItemStackTo(original, LAPIS_SLOT, LAPIS_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (isSupportedItem(original)) {
            if (!moveItemStackTo(original, ITEM_SLOT, ITEM_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (index < 29) {
            if (!moveItemStackTo(original, 29, 38, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 2, 29, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
