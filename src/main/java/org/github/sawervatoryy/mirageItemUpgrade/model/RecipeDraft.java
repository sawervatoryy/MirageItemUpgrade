package org.github.sawervatoryy.mirageItemUpgrade.model;

import org.github.sawervatoryy.mirageItemUpgrade.config.PluginSettings;
import org.bukkit.inventory.ItemStack;

public final class RecipeDraft {

    public String id;
    public ItemStack input;
    public ItemStack result;
    public double coins;
    public int enzymes;
    public double chance;
    public double bonusPerExtra;
    public boolean loseOnFail;
    public boolean strict;

    public static RecipeDraft fresh(PluginSettings.Defaults defaults) {
        RecipeDraft draft = new RecipeDraft();
        draft.coins = defaults.coins();
        draft.enzymes = defaults.enzymes();
        draft.chance = defaults.chance();
        draft.bonusPerExtra = defaults.bonusPerExtra();
        draft.loseOnFail = defaults.loseOnFail();
        draft.strict = defaults.strict();
        return draft;
    }

    public static RecipeDraft from(UpgradeRecipe recipe) {
        RecipeDraft draft = new RecipeDraft();
        draft.id = recipe.id();
        draft.input = recipe.input();
        draft.result = recipe.result();
        draft.coins = recipe.coins();
        draft.enzymes = recipe.enzymes();
        draft.chance = recipe.chance();
        draft.bonusPerExtra = recipe.bonusPerExtra();
        draft.loseOnFail = recipe.loseOnFail();
        draft.strict = recipe.strict();
        return draft;
    }

    public boolean isComplete() {
        return input != null && result != null;
    }

    public UpgradeRecipe toRecipe(String recipeId) {
        return new UpgradeRecipe(recipeId, input, result, coins, enzymes, chance, bonusPerExtra, loseOnFail, strict);
    }
}
