package dev.normaltreecapitator.text;

import dev.normaltreecapitator.NormalTreeCapitator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class ColorText {

    private static final char AMPERSAND = '&';
    private static final char SECTION = '§';

    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.builder()
            .character(SECTION)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final String FALLBACK_PREFIX = "&9[NormalTreeCapitator]&f ";

    private ColorText() {
    }

    public static Component component(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return SECTION_SERIALIZER.deserialize(toLegacySection(input));
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(component(message));
    }

    public static String prefix() {
        try {
            return NormalTreeCapitator.getInstance().messages().prefix();
        } catch (IllegalStateException ignored) {
            return FALLBACK_PREFIX;
        }
    }

    private static String toLegacySection(String input) {
        return org.bukkit.ChatColor.translateAlternateColorCodes(AMPERSAND, input);
    }
}
