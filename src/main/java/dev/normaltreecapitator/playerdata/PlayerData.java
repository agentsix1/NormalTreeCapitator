package dev.normaltreecapitator.playerdata;

public final class PlayerData {

    private boolean enabled;
    /** Personal chat language code, or {@code null} to use the server default. */
    private String language;

    public PlayerData(boolean enabled, String language) {
        this.enabled = enabled;
        this.language = language;
    }

    public static PlayerData defaults(boolean enabled) {
        return new PlayerData(enabled, null);
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String language() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
