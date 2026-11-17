package org.github.sawervatoryy.mirageItemUpgrade.gui;

import org.github.sawervatoryy.mirageItemUpgrade.config.Lang;
import org.github.sawervatoryy.mirageItemUpgrade.config.MenuLayout;
import org.github.sawervatoryy.mirageItemUpgrade.config.PluginSettings;
import org.github.sawervatoryy.mirageItemUpgrade.model.RecipeDraft;
import org.github.sawervatoryy.mirageItemUpgrade.util.ItemMatcher;
import org.github.sawervatoryy.mirageItemUpgrade.util.Numbers;
import org.github.sawervatoryy.mirageItemUpgrade.util.Tasks;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class RecipeEditorMenu extends Menu {

    private static final int INPUT_LABEL_SLOT = 11;
    private static final int RESULT_LABEL_SLOT = 15;
    private static final int INPUT_SLOT = 20;
    private static final int ARROW_SLOT = 22;
    private static final int RESULT_SLOT = 24;
    private static final int COINS_SLOT = 37;
    private static final int ENZYMES_SLOT = 38;
    private static final int CHANCE_SLOT = 39;
    private static final int BONUS_SLOT = 40;
    private static final int LOSE_SLOT = 41;
    private static final int STRICT_SLOT = 42;
    private static final int BACK_SLOT = 45;
    private static final int SAVE_SLOT = 49;
    private static final int DELETE_SLOT = 53;
    private static final double MAX_COINS = 1_000_000_000D;
    private static final int MAX_ENZYMES = 100_000;

    private final MenuManager menus;
    private final Player player;
    private final RecipeDraft draft;

    public RecipeEditorMenu(MenuManager menus, Player player, RecipeDraft draft) {
        this.menus = menus;
        this.player = player;
        this.draft = draft;
        Lang lang = menus.lang();
        this.inventory = draft.id == null
                ? Bukkit.createInventory(this, MenuLayout.SIZE, lang.get(player, "menu.editor.title-new"))
                : Bukkit.createInventory(this, MenuLayout.SIZE,
                lang.get(player, "menu.editor.title-edit", Placeholder.unparsed("id", draft.id)));
        refresh();
    }

    private void refresh() {
        Lang lang = menus.lang();
        PluginSettings.Editor editor = menus.settings().editor();
        ItemStack filler = GuiItems.filler(menus.settings().layout().filler());
        for (int slot = 0; slot < MenuLayout.SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(INPUT_LABEL_SLOT, GuiItems.of(Material.PAPER,
                lang.get(player, "menu.editor.input-label-name"), lang.list(player, "menu.editor.input-label-lore")));
        inventory.setItem(RESULT_LABEL_SLOT, GuiItems.of(Material.PAPER,
                lang.get(player, "menu.editor.result-label-name"), lang.list(player, "menu.editor.result-label-lore")));
        inventory.setItem(INPUT_SLOT, draft.input != null ? draft.input.clone()
                : GuiItems.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE, lang.get(player, "menu.editor.input-empty-name"),
                lang.list(player, "menu.editor.input-empty-lore")));
        inventory.setItem(RESULT_SLOT, draft.result != null ? draft.result.clone()
                : GuiItems.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE, lang.get(player, "menu.editor.result-empty-name"),
                lang.list(player, "menu.editor.result-empty-lore")));
        inventory.setItem(ARROW_SLOT, GuiItems.of(Material.ARROW, lang.get(player, "menu.editor.arrow-name"), List.of()));

        TagResolver multiplier = Placeholder.unparsed("multiplier", String.valueOf(editor.shiftMultiplier()));
        inventory.setItem(COINS_SLOT, GuiItems.of(Material.GOLD_INGOT, lang.get(player, "menu.editor.coins-name"),
                lang.list(player, "menu.editor.coins-lore",
                        Placeholder.unparsed("value", Numbers.plain(draft.coins)),
                        Placeholder.unparsed("step", Numbers.plain(editor.coinStep())), multiplier)));
        inventory.setItem(ENZYMES_SLOT, GuiItems.of(Material.PRISMARINE_SHARD, lang.get(player, "menu.editor.enzymes-name"),
                lang.list(player, "menu.editor.enzymes-lore",
                        Placeholder.unparsed("value", String.valueOf(draft.enzymes)),
                        Placeholder.unparsed("step", String.valueOf(editor.enzymeStep())), multiplier)));
        inventory.setItem(CHANCE_SLOT, GuiItems.of(Material.EXPERIENCE_BOTTLE, lang.get(player, "menu.editor.chance-name"),
                lang.list(player, "menu.editor.chance-lore",
                        Placeholder.unparsed("value", Numbers.plain(draft.chance)),
                        Placeholder.unparsed("step", Numbers.plain(editor.chanceStep())), multiplier)));
        inventory.setItem(BONUS_SLOT, GuiItems.of(Material.AMETHYST_SHARD, lang.get(player, "menu.editor.bonus-name"),
                lang.list(player, "menu.editor.bonus-lore",
                        Placeholder.unparsed("value", Numbers.plain(draft.bonusPerExtra)),
                        Placeholder.unparsed("step", Numbers.plain(editor.bonusStep())), multiplier)));
        inventory.setItem(LOSE_SLOT, GuiItems.of(Material.TNT, lang.get(player, "menu.editor.lose-name"),
                lang.list(player, "menu.editor.lose-lore", state(draft.loseOnFail))));
        inventory.setItem(STRICT_SLOT, GuiItems.of(Material.COMPARATOR, lang.get(player, "menu.editor.strict-name"),
                lang.list(player, "menu.editor.strict-lore", state(draft.strict))));

        inventory.setItem(BACK_SLOT, GuiItems.of(Material.ARROW, lang.get(player, "menu.editor.back-name"),
                lang.list(player, "menu.editor.back-lore")));
        inventory.setItem(SAVE_SLOT, GuiItems.of(Material.EMERALD, lang.get(player, "menu.editor.save-name"),
                lang.list(player, "menu.editor.save-lore")));
        if (draft.id != null) {
            inventory.setItem(DELETE_SLOT, GuiItems.of(Material.RED_CONCRETE, lang.get(player, "menu.editor.delete-name"),
                    lang.list(player, "menu.editor.delete-lore")));
        }
    }

    private TagResolver state(boolean value) {
        return Placeholder.component("state", menus.lang().get(value ? "word.enabled" : "word.disabled"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory() != inventory) {
            if (event.getClick().isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK) {
                event.setCancelled(true);
                if (event.getClick().isShiftClick()) {
                    placeFromInventory(event.getCurrentItem());
                }
            }
            return;
        }
        event.setCancelled(true);
        PluginSettings.Editor editor = menus.settings().editor();
        switch (event.getRawSlot()) {
            case INPUT_SLOT -> {
                ItemStack cursor = event.getCursor();
                draft.input = ItemMatcher.isEmpty(cursor) ? null : single(cursor);
                refresh();
            }
            case RESULT_SLOT -> {
                ItemStack cursor = event.getCursor();
                draft.result = ItemMatcher.isEmpty(cursor) ? null : cursor.clone();
                refresh();
            }
            case COINS_SLOT -> {
                draft.coins = round(Math.clamp(draft.coins + delta(event, editor.coinStep()), 0, MAX_COINS));
                refresh();
            }
            case ENZYMES_SLOT -> {
                draft.enzymes = (int) Math.clamp(
                        Math.round(draft.enzymes + delta(event, editor.enzymeStep())), 0, MAX_ENZYMES);
                refresh();
            }
            case CHANCE_SLOT -> {
                draft.chance = round(Math.clamp(draft.chance + delta(event, editor.chanceStep()), 0, 100));
                refresh();
            }
            case BONUS_SLOT -> {
                draft.bonusPerExtra = round(Math.clamp(draft.bonusPerExtra + delta(event, editor.bonusStep()), 0, 100));
                refresh();
            }
            case LOSE_SLOT -> {
                draft.loseOnFail = !draft.loseOnFail;
                refresh();
            }
            case STRICT_SLOT -> {
                draft.strict = !draft.strict;
                refresh();
            }
            case SAVE_SLOT -> save();
            case BACK_SLOT -> Tasks.sync(menus.plugin(), player, () -> menus.openList(player, 0));
            case DELETE_SLOT -> delete();
            default -> {
            }
        }
    }

    private void placeFromInventory(ItemStack current) {
        if (ItemMatcher.isEmpty(current)) {
            return;
        }
        if (draft.input == null) {
            draft.input = single(current);
        } else {
            draft.result = current.clone();
        }
        refresh();
    }

    private void save() {
        Lang lang = menus.lang();
        if (!draft.isComplete()) {
            lang.send(player, "editor.incomplete");
            menus.settings().play(player, "error");
            return;
        }
        String id = draft.id != null ? draft.id : menus.recipes().nextId();
        menus.recipes().put(draft.toRecipe(id));
        lang.send(player, "editor.saved", Placeholder.unparsed("id", id));
        menus.settings().play(player, "success");
        Tasks.sync(menus.plugin(), player, () -> menus.openList(player, 0));
    }

    private void delete() {
        if (draft.id == null) {
            return;
        }
        String id = draft.id;
        menus.recipes().remove(id);
        menus.lang().send(player, "editor.deleted", Placeholder.unparsed("id", id));
        Tasks.sync(menus.plugin(), player, () -> menus.openList(player, 0));
    }

    private double delta(InventoryClickEvent event, double step) {
        double multiplier = event.getClick().isShiftClick() ? menus.settings().editor().shiftMultiplier() : 1;
        if (event.getClick().isLeftClick()) {
            return step * multiplier;
        }
        if (event.getClick().isRightClick()) {
            return -step * multiplier;
        }
        return 0;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.clone();
        copy.setAmount(1);
        return copy;
    }
}
