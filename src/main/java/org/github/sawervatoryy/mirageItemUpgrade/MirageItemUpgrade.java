package org.github.sawervatoryy.mirageItemUpgrade;

import org.github.sawervatoryy.mirageItemUpgrade.command.UpgradeCommand;
import org.github.sawervatoryy.mirageItemUpgrade.config.Lang;
import org.github.sawervatoryy.mirageItemUpgrade.config.PluginSettings;
import org.github.sawervatoryy.mirageItemUpgrade.gui.Menu;
import org.github.sawervatoryy.mirageItemUpgrade.gui.MenuListener;
import org.github.sawervatoryy.mirageItemUpgrade.gui.MenuManager;
import org.github.sawervatoryy.mirageItemUpgrade.hook.PapiHook;
import org.github.sawervatoryy.mirageItemUpgrade.repository.RecipeRepository;
import org.github.sawervatoryy.mirageItemUpgrade.service.EconomyService;
import org.github.sawervatoryy.mirageItemUpgrade.service.EnzymeService;
import org.github.sawervatoryy.mirageItemUpgrade.service.UpgradeService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class MirageItemUpgrade extends JavaPlugin {

    private volatile PluginSettings settings;
    private Lang lang;
    private RecipeRepository recipes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = PluginSettings.load(getConfig(), getComponentLogger());
        lang = new Lang(this);
        lang.load();
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PapiHook.install(lang);
        }

        recipes = new RecipeRepository(this);
        EconomyService economy = new EconomyService();
        EnzymeService enzymes = new EnzymeService(this, this::settings, lang);
        UpgradeService upgrades = new UpgradeService(recipes, economy, enzymes);
        MenuManager menus = new MenuManager(this, lang, recipes, upgrades, economy);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        UpgradeCommand command = new UpgradeCommand(this, lang, menus, enzymes);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.build(), lang.raw("command.description"), List.of("itemupgrade")));

        recipes.load();
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) {
                player.closeInventory();
            }
        }
        if (recipes != null) {
            recipes.close();
        }
    }

    public CompletableFuture<Void> reload() {
        reloadConfig();
        settings = PluginSettings.load(getConfig(), getComponentLogger());
        lang.load();
        return recipes.load();
    }

    public PluginSettings settings() {
        return settings;
    }

    public RecipeRepository recipes() {
        return recipes;
    }
}
