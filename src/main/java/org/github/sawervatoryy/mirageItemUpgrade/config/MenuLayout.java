package org.github.sawervatoryy.mirageItemUpgrade.config;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record MenuLayout(int itemSlot, int resultSlot, int infoSlot, int confirmSlot, int closeSlot,
                         List<Integer> extraSlots, Material filler) {

    public static final int SIZE = 54;
    public static final int MAX_EXTRAS = 5;

    public MenuLayout {
        extraSlots = List.copyOf(extraSlots);
    }

    public static MenuLayout defaults() {
        return new MenuLayout(20, 24, 22, 49, 53, List.of(38, 39, 40, 41, 42), Material.BLACK_STAINED_GLASS_PANE);
    }

    public static MenuLayout load(ConfigurationSection section, ComponentLogger log) {
        MenuLayout defaults = defaults();
        if (section == null) {
            return defaults;
        }
        int item = section.getInt("item-slot", defaults.itemSlot());
        int result = section.getInt("result-slot", defaults.resultSlot());
        int info = section.getInt("info-slot", defaults.infoSlot());
        int confirm = section.getInt("confirm-slot", defaults.confirmSlot());
        int close = section.getInt("close-slot", defaults.closeSlot());
        List<Integer> extras = new ArrayList<>(section.getIntegerList("extra-slots"));
        if (extras.isEmpty()) {
            extras = new ArrayList<>(defaults.extraSlots());
        }
        Material filler = Material.matchMaterial(section.getString("filler", "BLACK_STAINED_GLASS_PANE"));
        if (filler == null || !filler.isItem()) {
            log.warn("Некорректный материал заполнителя, используется значение по умолчанию");
            filler = defaults.filler();
        }
        Set<Integer> seen = new HashSet<>();
        List<Integer> all = new ArrayList<>(extras);
        all.addAll(List.of(item, result, info, confirm, close));
        boolean valid = extras.size() <= MAX_EXTRAS;
        for (int slot : all) {
            if (slot < 0 || slot >= SIZE || !seen.add(slot)) {
                valid = false;
            }
        }
        if (!valid) {
            log.warn("Раскладка меню в config.yml некорректна (слоты должны быть уникальны, в диапазоне 0-53, доп. слотов не больше 5). Используется раскладка по умолчанию");
            return new MenuLayout(defaults.itemSlot(), defaults.resultSlot(), defaults.infoSlot(),
                    defaults.confirmSlot(), defaults.closeSlot(), defaults.extraSlots(), filler);
        }
        return new MenuLayout(item, result, info, confirm, close, extras, filler);
    }

    public boolean isInteractive(int slot) {
        return slot == itemSlot || extraSlots.contains(slot);
    }
}
