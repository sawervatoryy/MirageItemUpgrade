package org.github.sawervatoryy.mirageItemUpgrade.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class GuiItems {

    private GuiItems() {
    }

    public static Component noItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static ItemStack of(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(noItalic(name));
            if (!lore.isEmpty()) {
                meta.lore(lore.stream().map(GuiItems::noItalic).toList());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        });
        return stack;
    }

    public static ItemStack filler(Material material) {
        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(Component.text(" "));
            meta.setHideTooltip(true);
        });
        return stack;
    }

    public static ItemStack withExtraLore(ItemStack base, List<Component> extra) {
        ItemStack stack = base.clone();
        stack.editMeta(meta -> {
            List<Component> lore = new ArrayList<>();
            List<Component> existing = meta.lore();
            if (existing != null) {
                lore.addAll(existing);
            }
            lore.add(Component.empty());
            extra.forEach(line -> lore.add(noItalic(line)));
            meta.lore(lore);
        });
        return stack;
    }
}
