package org.github.sawervatoryy.mirageItemUpgrade.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public final class ItemMatcher {

    private ItemMatcher() {
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir() || stack.getAmount() <= 0;
    }

    public static ItemStack normalize(ItemStack stack) {
        ItemStack copy = stack.clone();
        copy.setAmount(1);
        copy.editMeta(Damageable.class, meta -> meta.setDamage(0));
        return copy;
    }

    public static boolean matches(ItemStack candidate, ItemStack template, boolean strict) {
        if (isEmpty(candidate) || isEmpty(template)) {
            return false;
        }
        if (!strict) {
            return candidate.getType() == template.getType();
        }
        return normalize(candidate).isSimilar(normalize(template));
    }

    public static boolean same(ItemStack first, ItemStack second) {
        return matches(first, second, true);
    }
}
