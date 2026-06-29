package dev.normaltreecapitator.update;

import dev.normaltreecapitator.NormalTreeCapitator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;

final class ModrinthVersionFetcher {

    static final String PROJECT_SLUG = "normal-tree-capitator";
    static final String CHANGELOG_URL = "https://modrinth.com/plugin/normal-tree-capitator/changelog";
    private static final String API_URL =
            "https://api.modrinth.com/v2/project/" + PROJECT_SLUG + "/version";

    private final NormalTreeCapitator plugin;
    private final HttpClient client;

    ModrinthVersionFetcher(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    Optional<String> fetchLatestRelease() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", userAgent())
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                plugin.getLogger().log(
                        Level.FINE,
                        "Modrinth version check returned HTTP {0} for {1}",
                        new Object[]{response.statusCode(), API_URL}
                );
                return Optional.empty();
            }

            return parseLatestRelease(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().log(Level.FINE, "Could not fetch latest version from Modrinth", e);
            return Optional.empty();
        }
    }

    private String userAgent() {
        return "NormalTreeCapitator/" + plugin.getDescription().getVersion()
                + " (https://github.com/agentsix1/NormalTreeCapitator)";
    }

    /**
     * Modrinth returns versions newest-first. Prefer the first {@code release} entry.
     */
    private static Optional<String> parseLatestRelease(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        String firstVersion = null;
        int index = 0;
        while (index < json.length()) {
            int numberKey = json.indexOf("\"version_number\"", index);
            if (numberKey < 0) {
                break;
            }

            Optional<String> number = readJsonStringValue(json, numberKey + "\"version_number\"".length());
            if (number.isEmpty()) {
                index = numberKey + 1;
                continue;
            }

            if (firstVersion == null) {
                firstVersion = number.get();
            }

            int windowEnd = Math.min(json.length(), numberKey + 400);
            String window = json.substring(numberKey, windowEnd);
            int typeKey = window.indexOf("\"version_type\"");
            if (typeKey >= 0) {
                Optional<String> type = readJsonStringValue(window, typeKey + "\"version_type\"".length());
                if (type.isPresent() && "release".equalsIgnoreCase(type.get())) {
                    return number;
                }
            }

            index = numberKey + 1;
        }

        return Optional.ofNullable(firstVersion);
    }

    private static Optional<String> readJsonStringValue(String json, int fromIndex) {
        int quoteStart = json.indexOf('"', fromIndex);
        if (quoteStart < 0) {
            return Optional.empty();
        }
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) {
            return Optional.empty();
        }
        String value = json.substring(quoteStart + 1, quoteEnd);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
