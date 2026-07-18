package dev.normaltreecapitator.update;

/**
 * Latest remote version advertisement ({@code version|downloadUrl} from Pastebin).
 */
public record RemoteVersionInfo(String version, String downloadUrl) {
}
