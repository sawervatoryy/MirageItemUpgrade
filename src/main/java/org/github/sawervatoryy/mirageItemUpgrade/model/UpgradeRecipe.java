package org.github.sawervatoryy.mirageItemUpgrade.model;

import org.github.sawervatoryy.mirageItemUpgrade.util.ItemMatcher;
import org.bukkit.inventory.ItemStack;

public record UpgradeRecipe(String id, ItemStack input, ItemStack result, double coins, int enzymes,
                            double chance, double bonusPerExtra, boolean loseOnFail, boolean strict) {

    public UpgradeRecipe {
        input = input.clone();
        input.setAmount(1);
        result = result.clone();
    }

    @Override
    public ItemStack input() {
        return input.clone();
    }

    @Override
    public ItemStack result() {
        return result.clone();
    }

    public boolean accepts(ItemStack candidate) {
        return ItemMatcher.matches(candidate, input, strict);
    }

    public double successChance(int extras) {
        return Math.min(100.0, chance + extras * bonusPerExtra);
    }
}
