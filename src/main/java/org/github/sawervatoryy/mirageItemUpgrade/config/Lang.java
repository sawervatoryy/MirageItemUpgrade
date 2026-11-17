package org.github.sawervatoryy.mirageItemUpgrade.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiFunction;

public final class Lang {

    private final JavaPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private volatile YamlConfiguration yaml = new YamlConfiguration();
    private volatile BiFunction<Player, String, String> processor = (player, text) -> text;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "lang.yml");
        if (!file.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("lang.yml")) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ex) {
            plugin.getComponentLogger().error("Не удалось прочитать встроенный lang.yml", ex);
        }
        yaml = loaded;
    }

    public void setProcessor(BiFunction<Player, String, String> processor) {
        this.processor = processor;
    }

    public String raw(String key) {
        String value = yaml.getString(key);
        return value == null ? "<red>[lang:" + key + "]" : value;
    }

    public Component get(String key, TagResolver... resolvers) {
        return render(null, raw(key), resolvers);
    }

    public Component get(Player player, String key, TagResolver... resolvers) {
        return render(player, raw(key), resolvers);
    }

    public List<Component> list(Player player, String key, TagResolver... resolvers) {
        return yaml.getStringList(key).stream().map(line -> render(player, line, resolvers)).toList();
    }

    public List<Component> list(String key, TagResolver... resolvers) {
        return list(null, key, resolvers);
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        Player player = sender instanceof Player p ? p : null;
        sender.sendMessage(render(player, raw(key), resolvers));
    }

    private Component render(Player player, String text, TagResolver[] resolvers) {
        String processed = player == null ? text : processor.apply(player, text);
        TagResolver all = TagResolver.builder()
                .resolver(Placeholder.parsed("prefix", yaml.getString("prefix", "")))
                .resolvers(resolvers)
                .build();
        return mini.deserialize(processed, all);
    }
}
