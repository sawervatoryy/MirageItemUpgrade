package org.github.sawervatoryy.mirageItemUpgrade.gui;

import org.github.sawervatoryy.mirageItemUpgrade.config.Lang;
import org.github.sawervatoryy.mirageItemUpgrade.config.MenuLayout;
import org.github.sawervatoryy.mirageItemUpgrade.config.PluginSettings;
import org.github.sawervatoryy.mirageItemUpgrade.model.UpgradeRecipe;
import org.github.sawervatoryy.mirageItemUpgrade.service.UpgradeOutcome;
import org.github.sawervatoryy.mirageItemUpgrade.service.UpgradeService;
import org.github.sawervatoryy.mirageItemUpgrade.util.ItemMatcher;
import org.github.sawervatoryy.mirageItemUpgrade.util.Numbers;
import org.github.sawervatoryy.mirageItemUpgrade.util.Tasks;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public final class UpgradeMenu extends Menu {

    private final MenuManager menus;
    private final Player player;
    private final MenuLayout layout;

    public UpgradeMenu(MenuManager menus, Player player) {
        this.menus = menus;
        this.player = player;
        this.layout = menus.settings().layout();
        this.inventory = Bukkit.createInventory(this, MenuLayout.SIZE, menus.lang().get(player, "menu.upgrade.title"));
        build();
    }

    private void build() {
        Lang lang = menus.lang();
        ItemStack filler = GuiItems.filler(layout.filler());
        for (int slot = 0; slot < MenuLayout.SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(layout.itemSlot(), null);
        layout.extraSlots().forEach(slot -> inventory.setItem(slot, null));
        inventory.setItem(layout.confirmSlot(), GuiItems.of(Material.EMERALD,
                lang.get(player, "menu.upgrade.confirm-name"), lang.list(player, "menu.upgrade.confirm-lore")));
        inventory.setItem(layout.closeSlot(), GuiItems.of(Material.BARRIER,
                lang.get(player, "menu.upgrade.close-name"), List.of()));
        showIdle();
    }

    private void showIdle() {
        Lang lang = menus.lang();
        inventory.setItem(layout.resultSlot(), GuiItems.of(Material.GRAY_STAINED_GLASS_PANE,
                lang.get(player, "menu.upgrade.result-empty-name"), lang.list(player, "menu.upgrade.result-empty-lore")));
        inventory.setItem(layout.infoSlot(), GuiItems.of(Material.PAPER,
                lang.get(player, "menu.upgrade.info-idle-name"), lang.list(player, "menu.upgrade.info-idle-lore")));
    }

    private void refresh() {
        Lang lang = menus.lang();
        ItemStack main = inventory.getItem(layout.itemSlot());
        if (ItemMatcher.isEmpty(main)) {
            showIdle();
            return;
        }
        Optional<UpgradeRecipe> found = menus.recipes().find(main);
        if (found.isEmpty()) {
            inventory.setItem(layout.resultSlot(), GuiItems.of(Material.BARRIER,
                    lang.get(player, "menu.upgrade.result-none-name"), lang.list(player, "menu.upgrade.result-none-lore")));
            inventory.setItem(layout.infoSlot(), GuiItems.of(Material.PAPER,
                    lang.get(player, "menu.upgrade.info-idle-name"), lang.list(player, "menu.upgrade.info-idle-lore")));
            return;
        }
        UpgradeRecipe recipe = found.get();
        int extras = countExtras(main);
        inventory.setItem(layout.resultSlot(), recipe.result());
        inventory.setItem(layout.infoSlot(), GuiItems.of(Material.PAPER,
                lang.get(player, "menu.upgrade.info-name"),
                lang.list(player, "menu.upgrade.info-lore",
                        Placeholder.unparsed("coins", menus.economy().format(recipe.coins())),
                        Placeholder.unparsed("enzymes", String.valueOf(recipe.enzymes())),
                        Placeholder.unparsed("base-chance", Numbers.plain(recipe.chance())),
                        Placeholder.unparsed("extras", String.valueOf(extras)),
                        Placeholder.unparsed("max-extras", String.valueOf(layout.extraSlots().size())),
                        Placeholder.unparsed("bonus", Numbers.plain(recipe.bonusPerExtra())),
                        Placeholder.unparsed("chance", Numbers.plain(recipe.successChance(extras))),
                        Placeholder.component("lose", lang.get(recipe.loseOnFail() ? "word.enabled" : "word.disabled")))));
    }

    private int countExtras(ItemStack main) {
        int count = 0;
        for (int slot : layout.extraSlots()) {
            ItemStack extra = inventory.getItem(slot);
            if (!ItemMatcher.isEmpty(extra) && ItemMatcher.same(extra, main)) {
                count++;
            }
        }
        return count;
    }

    private void scheduleRefresh() {
        Tasks.sync(menus.plugin(), player, this::refresh);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        if (raw < 0) {
            return;
        }
        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (raw < inventory.getSize()) {
            if (layout.isInteractive(raw)) {
                scheduleRefresh();
                return;
            }
            event.setCancelled(true);
            if (raw == layout.confirmSlot()) {
                confirm();
            } else if (raw == layout.closeSlot()) {
                Tasks.sync(menus.plugin(), player, player::closeInventory);
            }
            return;
        }
        if (event.getClick().isShiftClick()) {
            event.setCancelled(true);
            moveFromBottom(event);
            return;
        }
        scheduleRefresh();
    }

    private void moveFromBottom(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (ItemMatcher.isEmpty(current)) {
            return;
        }
        ItemStack main = inventory.getItem(layout.itemSlot());
        if (ItemMatcher.isEmpty(main)) {
            inventory.setItem(layout.itemSlot(), current.clone());
            event.setCurrentItem(null);
        } else if (ItemMatcher.same(current, main)) {
            for (int slot : layout.extraSlots()) {
                if (ItemMatcher.isEmpty(inventory.getItem(slot))) {
                    ItemStack single = current.clone();
                    single.setAmount(1);
                    inventory.setItem(slot, single);
                    current.setAmount(current.getAmount() - 1);
                    event.setCurrentItem(current.getAmount() <= 0 ? null : current);
                    break;
                }
            }
        }
        refresh();
    }

    private void confirm() {
        Lang lang = menus.lang();
        PluginSettings settings = menus.settings();
        switch (menus.upgrades().attempt(player, inventory, layout)) {
            case UpgradeOutcome.Rejected rejected -> {
                lang.send(player, rejected.key(), rejected.resolver());
                settings.play(player, "error");
            }
            case UpgradeOutcome.Success success -> {
                lang.send(player, "upgrade.success",
                        Placeholder.component("result", success.result().effectiveName()),
                        UpgradeService.chanceResolver(success.chance()));
                settings.play(player, "success");
            }
            case UpgradeOutcome.Failure failure -> {
                lang.send(player, failure.itemLost() ? "upgrade.failure-lost" : "upgrade.failure-kept",
                        UpgradeService.chanceResolver(failure.chance()));
                settings.play(player, "failure");
            }
        }
        refresh();
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        int size = inventory.getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < size && !layout.isInteractive(raw)) {
                event.setCancelled(true);
                return;
            }
        }
        scheduleRefresh();
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        giveBack(layout.itemSlot());
        layout.extraSlots().forEach(this::giveBack);
    }

    private void giveBack(int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (ItemMatcher.isEmpty(stack)) {
            return;
        }
        inventory.setItem(slot, null);
        player.getInventory().addItem(stack)
                .values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }
}
