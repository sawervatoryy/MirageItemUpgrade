package org.github.sawervatoryy.mirageItemUpgrade.config;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record PluginSettings(MenuLayout layout, Enzymes enzymes, Editor editor, Defaults defaults,
                             Map<String, SoundSpec> sounds) {

    public record Enzymes(Material material, boolean glow) {
    }

    public record Editor(double coinStep, int enzymeStep, double chanceStep, double bonusStep, int shiftMultiplier) {
    }

    public record Defaults(double coins, int enzymes, double chance, double bonusPerExtra, boolean loseOnFail,
                           boolean strict) {
    }

    public record SoundSpec(Key key, float volume, float pitch) {

        static Optional<SoundSpec> parse(ConfigurationSection section, ComponentLogger log) {
            String raw = section.getString("sound");
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(new SoundSpec(Key.key(raw.toLowerCase(Locale.ROOT)),
                        (float) section.getDouble("volume", 1.0), (float) section.getDouble("pitch", 1.0)));
            } catch (InvalidKeyException ex) {
                log.warn("Некорректный ключ звука {} в config.yml", raw);
                return Optional.empty();
            }
        }

        public Sound toSound() {
            return Sound.sound(key, Sound.Source.PLAYER, volume, pitch);
        }
    }

    public static PluginSettings load(ConfigurationSection config, ComponentLogger log) {
        MenuLayout layout = MenuLayout.load(config.getConfigurationSection("menu"), log);

        Material enzymeMaterial = Material.matchMaterial(config.getString("enzymes.material", "PRISMARINE_SHARD"));
        if (enzymeMaterial == null || !enzymeMaterial.isItem()) {
            log.warn("Некорректный материал ферментов улучшения, используется PRISMARINE_SHARD");
            enzymeMaterial = Material.PRISMARINE_SHARD;
        }
        Enzymes enzymes = new Enzymes(enzymeMaterial, config.getBoolean("enzymes.glow", true));

        Editor editor = new Editor(
                Math.max(0.01, config.getDouble("editor.coin-step", 100)),
                Math.max(1, config.getInt("editor.enzyme-step", 1)),
                Math.max(0.01, config.getDouble("editor.chance-step", 1)),
                Math.max(0.01, config.getDouble("editor.bonus-step", 1)),
                Math.max(1, config.getInt("editor.shift-multiplier", 10)));

        Defaults defaults = new Defaults(
                Math.max(0, config.getDouble("recipe-defaults.coins", 1000)),
                Math.max(0, config.getInt("recipe-defaults.enzymes", 5)),
                Math.clamp(config.getDouble("recipe-defaults.chance", 50), 0, 100),
                Math.clamp(config.getDouble("recipe-defaults.bonus-per-extra", 10), 0, 100),
                config.getBoolean("recipe-defaults.lose-on-fail", true),
                config.getBoolean("recipe-defaults.strict", true));

        Map<String, SoundSpec> sounds = new HashMap<>();
        ConfigurationSection soundSection = config.getConfigurationSection("sounds");
        if (soundSection != null) {
            for (String name : soundSection.getKeys(false)) {
                ConfigurationSection entry = soundSection.getConfigurationSection(name);
                if (entry != null) {
                    SoundSpec.parse(entry, log).ifPresent(spec -> sounds.put(name, spec));
                }
            }
        }
        return new PluginSettings(layout, enzymes, editor, defaults, Map.copyOf(sounds));
    }

    public void play(Player player, String name) {
        SoundSpec spec = sounds.get(name);
        if (spec != null) {
            player.playSound(spec.toSound());
        }
    }
}
