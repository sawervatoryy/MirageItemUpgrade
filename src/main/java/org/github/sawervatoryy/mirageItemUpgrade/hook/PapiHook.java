package org.github.sawervatoryy.mirageItemUpgrade.hook;

import org.github.sawervatoryy.mirageItemUpgrade.config.Lang;
import me.clip.placeholderapi.PlaceholderAPI;

public final class PapiHook {

    private PapiHook() {
    }

    public static void install(Lang lang) {
        lang.setProcessor((player, text) -> PlaceholderAPI.setPlaceholders(player, text));
    }
}
