package org.github.sawervatoryy.mirageItemUpgrade.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class Tasks {

    private Tasks() {
    }

    public static void sync(Plugin plugin, Player player, Runnable action) {
        player.getScheduler().run(plugin, task -> action.run(), null);
    }
}
