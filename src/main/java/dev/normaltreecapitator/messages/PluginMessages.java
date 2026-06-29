package dev.normaltreecapitator.messages;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.text.ColorText;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public final class PluginMessages {

    private final NormalTreeCapitator plugin;
    private final File messagesFile;
    private FileConfiguration messages;

    public PluginMessages(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void load() {
        FileConfiguration embedded = loadEmbedded("messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        if (embedded != null) {
            messages.setDefaults(embedded);
            messages.options().copyDefaults(true);
        }
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save messages.yml", e);
        }
    }

    public String prefix() {
        return get("prefix");
    }

    public String get(String path) {
        String value = messages.getString(path);
        return value == null ? "" : value;
    }

    public String format(String path, Map<String, String> replacements) {
        String message = get(path);
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                String value = entry.getValue() == null ? "" : entry.getValue();
                message = message
                        .replace("{" + entry.getKey() + "}", value)
                        .replace("%" + entry.getKey() + "%", value);
            }
        }
        return message;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, null);
    }

    public void send(CommandSender sender, String path, Map<String, String> replacements) {
        String message = format(path, replacements);
        if (message.isEmpty()) {
            return;
        }
        ColorText.send(sender, prefix() + message);
    }

    private FileConfiguration loadEmbedded(String resource) {
        try (InputStream stream = plugin.getResource(resource)) {
            if (stream == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not load embedded " + resource, e);
            return new YamlConfiguration();
        }
    }

    public static Map<String, String> map(String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Replacement pairs must be even-length");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
