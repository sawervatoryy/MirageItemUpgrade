package org.github.sawervatoryy.mirageItemUpgrade.service;

import org.github.sawervatoryy.mirageItemUpgrade.config.MenuLayout;
import org.github.sawervatoryy.mirageItemUpgrade.model.UpgradeRecipe;
import org.github.sawervatoryy.mirageItemUpgrade.repository.RecipeRepository;
import org.github.sawervatoryy.mirageItemUpgrade.util.ItemMatcher;
import org.github.sawervatoryy.mirageItemUpgrade.util.Numbers;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class UpgradeService {

    private final RecipeRepository recipes;
    private final EconomyService economy;
    private final EnzymeService enzymes;

    public UpgradeService(RecipeRepository recipes, EconomyService economy, EnzymeService enzymes) {
        this.recipes = recipes;
        this.economy = economy;
        this.enzymes = enzymes;
    }

    public UpgradeOutcome attempt(Player player, Inventory inventory, MenuLayout layout) {
        ItemStack main = inventory.getItem(layout.itemSlot());
        if (ItemMatcher.isEmpty(main)) {
            return reject("upgrade.no-item");
        }
        Optional<UpgradeRecipe> found = recipes.find(main);
        if (found.isEmpty()) {
            return reject("upgrade.no-recipe");
        }
        UpgradeRecipe recipe = found.get();

        List<Integer> usedExtras = new ArrayList<>();
        for (int slot : layout.extraSlots()) {
            ItemStack extra = inventory.getItem(slot);
            if (ItemMatcher.isEmpty(extra)) {
                continue;
            }
            if (!ItemMatcher.same(extra, main)) {
                return reject("upgrade.extra-mismatch");
            }
            usedExtras.add(slot);
        }

        double coins = recipe.coins();
        int enzymeCost = recipe.enzymes();
        if (coins > 0) {
            if (!economy.available()) {
                return reject("upgrade.no-economy");
            }
            if (!economy.has(player, coins)) {
                return new UpgradeOutcome.Rejected("upgrade.not-enough-coins",
                        Placeholder.unparsed("need", economy.format(coins)));
            }
        }
        int owned = enzymes.count(player);
        if (enzymeCost > 0 && owned < enzymeCost) {
            return new UpgradeOutcome.Rejected("upgrade.not-enough-enzymes", TagResolver.resolver(
                    Placeholder.unparsed("need", String.valueOf(enzymeCost)),
                    Placeholder.unparsed("have", String.valueOf(owned))));
        }

        enzymes.take(player, enzymeCost);
        if (coins > 0 && !economy.withdraw(player, coins)) {
            enzymes.give(player, enzymeCost);
            return reject("upgrade.payment-failed");
        }

        double chance = recipe.successChance(usedExtras.size());
        boolean success = ThreadLocalRandom.current().nextDouble(100.0) < chance;
        for (int slot : usedExtras) {
            consumeOne(inventory, slot);
        }
        if (success) {
            consumeOne(inventory, layout.itemSlot());
            ItemStack result = recipe.result();
            player.getInventory().addItem(result.clone())
                    .values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            return new UpgradeOutcome.Success(recipe, result, chance);
        }
        boolean lost = recipe.loseOnFail();
        if (lost) {
            consumeOne(inventory, layout.itemSlot());
        }
        return new UpgradeOutcome.Failure(recipe, lost, chance);
    }

    public static TagResolver chanceResolver(double chance) {
        return Placeholder.unparsed("chance", Numbers.plain(chance));
    }

    private static UpgradeOutcome reject(String key) {
        return new UpgradeOutcome.Rejected(key, TagResolver.empty());
    }

    private static void consumeOne(Inventory inventory, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (ItemMatcher.isEmpty(stack)) {
            return;
        }
        if (stack.getAmount() <= 1) {
            inventory.setItem(slot, null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
            inventory.setItem(slot, stack);
        }
    }
}
