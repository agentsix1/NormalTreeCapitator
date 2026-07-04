package dev.normaltreecapitator.util;

import dev.normaltreecapitator.config.TreeCapitatorConfig;

/**
 * Tree capitator sneak gate (no drill integration).
 * <ul>
 *   <li>{@code must-sneak: true} — tree cap while sneaking</li>
 *   <li>{@code must-sneak: false} — tree cap while standing (sneaking uses vanilla break)</li>
 * </ul>
 */
public final class TreeCapSneak {

    private TreeCapSneak() {
    }

    public static boolean allowsActivation(TreeCapitatorConfig config, boolean sneaking) {
        if (config == null) {
            return false;
        }
        if (config.mustSneak()) {
            return sneaking;
        }
        return !sneaking;
    }
}
