package org.github.sawervatoryy.mirageItemUpgrade.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract sealed class Menu implements InventoryHolder permits UpgradeMenu, RecipeListMenu, RecipeEditorMenu {

    protected Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void handleClick(InventoryClickEvent event);

    public void handleDrag(InventoryDragEvent event) {
        int size = inventory.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < size) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public void handleClose(InventoryCloseEvent event) {
    }
}
