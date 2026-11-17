package org.github.sawervatoryy.mirageItemUpgrade.service;

import org.github.sawervatoryy.mirageItemUpgrade.model.UpgradeRecipe;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;

public sealed interface UpgradeOutcome {

    record Rejected(String key, TagResolver resolver) implements UpgradeOutcome {
    }

    record Success(UpgradeRecipe recipe, ItemStack result, double chance) implements UpgradeOutcome {
    }

    record Failure(UpgradeRecipe recipe, boolean itemLost, double chance) implements UpgradeOutcome {
    }
}
