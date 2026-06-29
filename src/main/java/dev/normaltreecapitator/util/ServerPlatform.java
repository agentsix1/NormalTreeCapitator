package dev.normaltreecapitator.util;

/**
 * Detects the server platform at runtime.
 */
public final class ServerPlatform {

    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    private static final boolean PAPER = FOLIA || classExists("com.destroystokyo.paper.PaperConfig");

    private ServerPlatform() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static boolean isPaper() {
        return PAPER;
    }

    public static String displayName() {
        if (FOLIA) {
            return "Folia";
        }
        if (PAPER) {
            return "Paper";
        }
        return "Spigot/Bukkit";
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
