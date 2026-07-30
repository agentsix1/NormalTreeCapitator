package dev.normaltreecapitator.update;

/**
 * Latest remote version advertisement ({@code version|downloadUrl} from GitHub {@code version.txt}).
 */
public record RemoteVersionInfo(String version, String downloadUrl) {
}
