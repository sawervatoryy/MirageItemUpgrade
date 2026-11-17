package org.github.sawervatoryy.mirageItemUpgrade.gui;

import org.github.sawervatoryy.mirageItemUpgrade.MirageItemUpgrade;
import org.github.sawervatoryy.mirageItemUpgrade.config.Lang;
import org.github.sawervatoryy.mirageItemUpgrade.config.PluginSettings;
import org.github.sawervatoryy.mirageItemUpgrade.model.RecipeDraft;
import org.github.sawervatoryy.mirageItemUpgrade.repository.RecipeRepository;
import org.github.sawervatoryy.mirageItemUpgrade.service.EconomyService;
import org.github.sawervatoryy.mirageItemUpgrade.service.UpgradeService;
import org.bukkit.entity.Player;

public final class MenuManager {

    private final MirageItemUpgrade plugin;
    private final Lang lang;
    private final RecipeRepository recipes;
    private final UpgradeService upgrades;
    private final EconomyService economy;

    public MenuManager(MirageItemUpgrade plugin, Lang lang, RecipeRepository recipes, UpgradeService upgrades,
                       EconomyService economy) {
        this.plugin = plugin;
        this.lang = lang;
        this.recipes = recipes;
        this.upgrades = upgrades;
        this.economy = economy;
    }

    public MirageItemUpgrade plugin() {
        return plugin;
    }

    public Lang lang() {
        return lang;
    }

    public PluginSettings settings() {
        return plugin.settings();
    }

    public RecipeRepository recipes() {
        return recipes;
    }

    public UpgradeService upgrades() {
        return upgrades;
    }

    public EconomyService economy() {
        return economy;
    }

    public void openUpgrade(Player player) {
        player.openInventory(new UpgradeMenu(this, player).getInventory());
    }

    public void openList(Player player, int page) {
        player.openInventory(new RecipeListMenu(this, player, page).getInventory());
    }

    public void openEditor(Player player, RecipeDraft draft) {
        player.openInventory(new RecipeEditorMenu(this, player, draft).getInventory());
    }
}
