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

/**
 * Reads the latest plugin version from a Pastebin raw paste.
 * <p>
 * Expected layout (one line): {@code version|download url}
 * Example: {@code v1.0.3|https://modrinth.com/plugin/normal-tree-capitator/versions}
 */
final class PastebinVersionFetcher {

    static final String RAW_URL = "https://pastebin.com/raw/nc6CbGem";

    private final NormalTreeCapitator plugin;
    private final HttpClient client;

    PastebinVersionFetcher(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    Optional<RemoteVersionInfo> fetchLatestRelease() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RAW_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", userAgent())
                    .header("Accept", "text/plain")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                plugin.getLogger().log(
                        Level.FINE,
                        "Pastebin version check returned HTTP {0} for {1}",
                        new Object[]{response.statusCode(), RAW_URL}
                );
                return Optional.empty();
            }

            return parse(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().log(Level.FINE, "Could not fetch latest version from Pastebin", e);
            return Optional.empty();
        }
    }

    private String userAgent() {
        return "NormalTreeCapitator/" + plugin.getDescription().getVersion()
                + " (https://github.com/agentsix1/NormalTreeCapitator)";
    }

    static Optional<RemoteVersionInfo> parse(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        String line = body.lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                .findFirst()
                .orElse("");
        if (line.isEmpty()) {
            return Optional.empty();
        }
        int sep = line.indexOf('|');
        if (sep <= 0 || sep >= line.length() - 1) {
            return Optional.empty();
        }
        String version = line.substring(0, sep).trim();
        String url = line.substring(sep + 1).trim();
        if (version.isEmpty() || url.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RemoteVersionInfo(version, url));
    }
}
