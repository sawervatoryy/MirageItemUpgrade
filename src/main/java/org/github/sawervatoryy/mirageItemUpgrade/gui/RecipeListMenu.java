package org.github.sawervatoryy.mirageItemUpgrade.gui;

import org.github.sawervatoryy.mirageItemUpgrade.config.Lang;
import org.github.sawervatoryy.mirageItemUpgrade.config.MenuLayout;
import org.github.sawervatoryy.mirageItemUpgrade.model.RecipeDraft;
import org.github.sawervatoryy.mirageItemUpgrade.model.UpgradeRecipe;
import org.github.sawervatoryy.mirageItemUpgrade.util.Numbers;
import org.github.sawervatoryy.mirageItemUpgrade.util.Tasks;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RecipeListMenu extends Menu {

    private static final int PER_PAGE = 45;
    private static final int PREV_SLOT = 45;
    private static final int CLOSE_SLOT = 47;
    private static final int CREATE_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final MenuManager menus;
    private final Player player;
    private final int page;
    private final int pages;
    private final Map<Integer, String> slotRecipes = new HashMap<>();

    public RecipeListMenu(MenuManager menus, Player player, int requestedPage) {
        this.menus = menus;
        this.player = player;
        List<UpgradeRecipe> all = menus.recipes().sorted();
        this.pages = Math.max(1, (int) Math.ceil(all.size() / (double) PER_PAGE));
        this.page = Math.clamp(requestedPage, 0, pages - 1);
        Lang lang = menus.lang();
        this.inventory = Bukkit.createInventory(this, MenuLayout.SIZE, lang.get(player, "menu.list.title",
                Placeholder.unparsed("page", String.valueOf(page + 1)),
                Placeholder.unparsed("pages", String.valueOf(pages))));
        build(all);
    }

    private void build(List<UpgradeRecipe> all) {
        Lang lang = menus.lang();
        ItemStack filler = GuiItems.filler(menus.settings().layout().filler());
        for (int slot = 0; slot < MenuLayout.SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        int from = page * PER_PAGE;
        int to = Math.min(all.size(), from + PER_PAGE);
        for (int index = from; index < to; index++) {
            UpgradeRecipe recipe = all.get(index);
            int slot = index - from;
            slotRecipes.put(slot, recipe.id());
            inventory.setItem(slot, icon(recipe));
        }
        if (page > 0) {
            inventory.setItem(PREV_SLOT, GuiItems.of(Material.ARROW, lang.get(player, "menu.list.prev-name"), List.of()));
        }
        if (page < pages - 1) {
            inventory.setItem(NEXT_SLOT, GuiItems.of(Material.ARROW, lang.get(player, "menu.list.next-name"), List.of()));
        }
        inventory.setItem(CLOSE_SLOT, GuiItems.of(Material.BARRIER, lang.get(player, "menu.list.close-name"), List.of()));
        inventory.setItem(CREATE_SLOT, GuiItems.of(Material.EMERALD,
                lang.get(player, "menu.list.create-name"), lang.list(player, "menu.list.create-lore")));
    }

    private ItemStack icon(UpgradeRecipe recipe) {
        Lang lang = menus.lang();
        ItemStack result = recipe.result();
        return GuiItems.withExtraLore(recipe.input(), lang.list(player, "menu.list.recipe-lore",
                Placeholder.unparsed("id", recipe.id()),
                Placeholder.component("result", result.effectiveName()),
                Placeholder.unparsed("amount", String.valueOf(result.getAmount())),
                Placeholder.unparsed("coins", Numbers.plain(recipe.coins())),
                Placeholder.unparsed("enzymes", String.valueOf(recipe.enzymes())),
                Placeholder.unparsed("chance", Numbers.plain(recipe.chance())),
                Placeholder.unparsed("bonus", Numbers.plain(recipe.bonusPerExtra())),
                Placeholder.component("lose", lang.get(recipe.loseOnFail() ? "word.enabled" : "word.disabled")),
                Placeholder.component("strict", lang.get(recipe.strict() ? "word.enabled" : "word.disabled"))));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory() != inventory) {
            if (event.getClick().isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        String id = slotRecipes.get(slot);
        if (id != null) {
            handleRecipeClick(event, id);
            return;
        }
        switch (slot) {
            case PREV_SLOT -> {
                if (page > 0) {
                    Tasks.sync(menus.plugin(), player, () -> menus.openList(player, page - 1));
                }
            }
            case NEXT_SLOT -> {
                if (page < pages - 1) {
                    Tasks.sync(menus.plugin(), player, () -> menus.openList(player, page + 1));
                }
            }
            case CLOSE_SLOT -> Tasks.sync(menus.plugin(), player, player::closeInventory);
            case CREATE_SLOT -> Tasks.sync(menus.plugin(), player,
                    () -> menus.openEditor(player, RecipeDraft.fresh(menus.settings().defaults())));
            default -> {
            }
        }
    }

    private void handleRecipeClick(InventoryClickEvent event, String id) {
        Optional<UpgradeRecipe> recipe = menus.recipes().byId(id);
        if (recipe.isEmpty()) {
            return;
        }
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            menus.recipes().remove(id);
            menus.lang().send(player, "editor.deleted", Placeholder.unparsed("id", id));
            Tasks.sync(menus.plugin(), player, () -> menus.openList(player, page));
        } else if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
            Tasks.sync(menus.plugin(), player, () -> menus.openEditor(player, RecipeDraft.from(recipe.get())));
        }
    }
}
