package dev.normaltreecapitator.messages;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.playerdata.PlayerData;
import dev.normaltreecapitator.text.ColorText;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PluginMessages {

    public static final String DEFAULT_LANGUAGE = "EN-us";
    public static final String[] BUNDLED_LANGUAGES = {
            "EN-us", "EN-gb", "EN-sg", "DE-de", "ES-es", "PT-br"
    };

    private final NormalTreeCapitator plugin;
    private final File languagesDir;
    private final File legacyMessagesFile;
    private final Map<String, FileConfiguration> byLanguage = new ConcurrentHashMap<>();
    private String activeLanguage = DEFAULT_LANGUAGE;

    public PluginMessages(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.languagesDir = new File(plugin.getDataFolder(), "languages");
        this.legacyMessagesFile = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void load() {
        if (!languagesDir.exists() && !languagesDir.mkdirs()) {
            plugin.getLogger().warning("Could not create languages folder: " + languagesDir.getAbsolutePath());
        }

        migrateLegacyMessages();
        extractBundledLanguages();

        FileConfiguration embeddedEnglish = loadEmbedded("languages/" + DEFAULT_LANGUAGE + ".yml");
        byLanguage.clear();
        for (String code : availableLanguages()) {
            File languageFile = resolveLanguageFile(code);
            FileConfiguration loaded = YamlConfiguration.loadConfiguration(languageFile);
            if (embeddedEnglish != null) {
                loaded.setDefaults(embeddedEnglish);
                loaded.options().copyDefaults(true);
            }
            try {
                loaded.save(languageFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not save " + languageFile.getName(), e);
            }
            byLanguage.put(code.toLowerCase(Locale.ROOT), loaded);
        }

        String requested = resolveRequestedLanguage();
        File activeFile = resolveLanguageFile(requested);
        activeLanguage = stripExtension(activeFile.getName());
        if (!byLanguage.containsKey(activeLanguage.toLowerCase(Locale.ROOT))) {
            activeLanguage = DEFAULT_LANGUAGE;
        }

        plugin.getLogger().info("[TreeCap] language=" + activeLanguage
                + " file=" + new File(languagesDir, activeLanguage + ".yml").getAbsolutePath());
    }

    public String activeLanguage() {
        return activeLanguage;
    }

    /**
     * Language codes available for {@code /tc language} — bundled plus any extra
     * {@code .yml} files in the languages folder.
     */
    public List<String> availableLanguages() {
        extractBundledLanguages();
        Map<String, String> byLower = new TreeMap<>();
        for (String bundled : BUNDLED_LANGUAGES) {
            byLower.put(bundled.toLowerCase(Locale.ROOT), bundled);
        }
        File[] files = languagesDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String code = stripExtension(file.getName());
                if (code.isBlank()) {
                    continue;
                }
                byLower.putIfAbsent(code.toLowerCase(Locale.ROOT), code);
            }
        }
        return List.copyOf(byLower.values());
    }

    public Optional<String> matchLanguage(String requested) {
        if (requested == null || requested.isBlank()) {
            return Optional.empty();
        }
        String wanted = requested.trim();
        for (String code : availableLanguages()) {
            if (code.equalsIgnoreCase(wanted)) {
                return Optional.of(code);
            }
        }
        return Optional.empty();
    }

    public String resolveLanguageCode(CommandSender sender) {
        if (sender instanceof Player player
                && plugin.playerData() != null
                && plugin.config() != null) {
            PlayerData data = plugin.playerData().get(player.getUniqueId(), plugin.config());
            String personal = data.language();
            if (personal != null && !personal.isBlank()) {
                Optional<String> matched = matchLanguage(personal);
                if (matched.isPresent()) {
                    return matched.get();
                }
            }
        }
        return activeLanguage;
    }

    public String prefix() {
        return get(activeLanguage, "prefix");
    }

    public String prefix(CommandSender sender) {
        return get(sender, "prefix");
    }

    public String get(String path) {
        return get(activeLanguage, path);
    }

    public String get(CommandSender sender, String path) {
        return get(resolveLanguageCode(sender), path);
    }

    public String get(String languageCode, String path) {
        FileConfiguration bundle = bundleFor(languageCode);
        String value = bundle.getString(path);
        return value == null ? "" : value;
    }

    public String format(String path, Map<String, String> replacements) {
        return format(activeLanguage, path, replacements);
    }

    public String format(CommandSender sender, String path, Map<String, String> replacements) {
        return format(resolveLanguageCode(sender), path, replacements);
    }

    public String format(String languageCode, String path, Map<String, String> replacements) {
        String message = get(languageCode, path);
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
        String message = format(sender, path, replacements);
        if (message.isEmpty()) {
            return;
        }
        ColorText.send(sender, prefix(sender) + message);
    }

    private FileConfiguration bundleFor(String languageCode) {
        if (languageCode != null) {
            FileConfiguration bundle = byLanguage.get(languageCode.toLowerCase(Locale.ROOT));
            if (bundle != null) {
                return bundle;
            }
        }
        FileConfiguration fallback = byLanguage.get(DEFAULT_LANGUAGE.toLowerCase(Locale.ROOT));
        if (fallback != null) {
            return fallback;
        }
        return new YamlConfiguration();
    }

    private void migrateLegacyMessages() {
        File englishFile = new File(languagesDir, DEFAULT_LANGUAGE + ".yml");
        if (!legacyMessagesFile.exists() || englishFile.exists()) {
            return;
        }
        try {
            Files.move(legacyMessagesFile.toPath(), englishFile.toPath());
            plugin.getLogger().info("Moved messages.yml to languages/" + DEFAULT_LANGUAGE + ".yml");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not migrate messages.yml to languages/" + DEFAULT_LANGUAGE + ".yml", e);
        }
    }

    private void extractBundledLanguages() {
        for (String language : BUNDLED_LANGUAGES) {
            File dest = new File(languagesDir, language + ".yml");
            if (dest.exists()) {
                continue;
            }
            String resource = "languages/" + language + ".yml";
            try {
                plugin.saveResource(resource, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Missing bundled language: " + resource, e);
            }
        }
    }

    private String resolveRequestedLanguage() {
        if (plugin.config() != null) {
            String configured = plugin.config().language();
            if (configured != null && !configured.isBlank()) {
                return configured.trim();
            }
        }
        return DEFAULT_LANGUAGE;
    }

    private File resolveLanguageFile(String requested) {
        File exact = new File(languagesDir, requested + ".yml");
        if (exact.isFile()) {
            return exact;
        }

        File[] files = languagesDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            String wanted = requested.toLowerCase(Locale.ROOT);
            for (File file : files) {
                String base = stripExtension(file.getName());
                if (base.equalsIgnoreCase(wanted)) {
                    return file;
                }
            }
        }

        plugin.getLogger().warning("Language \"" + requested + "\" not found in languages/. "
                + "Falling back to " + DEFAULT_LANGUAGE + ". Available: "
                + String.join(", ", BUNDLED_LANGUAGES));

        File fallback = new File(languagesDir, DEFAULT_LANGUAGE + ".yml");
        if (!fallback.exists()) {
            plugin.saveResource("languages/" + DEFAULT_LANGUAGE + ".yml", false);
        }
        return fallback;
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
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
