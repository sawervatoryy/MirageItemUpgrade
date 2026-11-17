package org.github.sawervatoryy.mirageItemUpgrade.util;

import java.util.Locale;

public final class Numbers {

    private Numbers() {
    }

    public static String plain(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.2f", value);
    }
}
