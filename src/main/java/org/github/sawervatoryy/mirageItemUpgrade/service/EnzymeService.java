package org.github.sawervatoryy.mirageItemUpgrade.service;

import org.github.sawervatoryy.mirageItemUpgrade.config.Lang;
import org.github.sawervatoryy.mirageItemUpgrade.config.PluginSettings;
import org.github.sawervatoryy.mirageItemUpgrade.gui.GuiItems;
import org.github.sawervatoryy.mirageItemUpgrade.util.ItemMatcher;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

public final class EnzymeService {

    private final NamespacedKey key;
    private final Supplier<PluginSettings> settings;
    private final Lang lang;

    public EnzymeService(JavaPlugin plugin, Supplier<PluginSettings> settings, Lang lang) {
        this.key = new NamespacedKey(plugin, "upgrade_enzyme");
        this.settings = settings;
        this.lang = lang;
    }

    public ItemStack create(int amount) {
        ItemStack stack = new ItemStack(settings.get().enzymes().material(), amount);
        boolean glow = settings.get().enzymes().glow();
        stack.editMeta(meta -> {
            meta.displayName(GuiItems.noItalic(lang.get("enzyme.name")));
            meta.lore(lang.list("enzyme.lore").stream().map(GuiItems::noItalic).toList());
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            if (glow) {
                meta.setEnchantmentGlintOverride(true);
            }
        });
        return stack;
    }

    public boolean isEnzyme(ItemStack stack) {
        return !ItemMatcher.isEmpty(stack) && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public int count(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (isEnzyme(stack)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public boolean take(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (count(player) < amount) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (!isEnzyme(stack)) {
                continue;
            }
            int taken = Math.min(remaining, stack.getAmount());
            remaining -= taken;
            if (taken == stack.getAmount()) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - taken);
                inventory.setItem(slot, stack);
            }
        }
        return true;
    }

    public void give(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        player.getInventory().addItem(create(amount))
                .values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }
}
