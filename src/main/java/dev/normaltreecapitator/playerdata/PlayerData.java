package dev.normaltreecapitator.playerdata;

public final class PlayerData {

    private boolean enabled;
    /** When true, server structure protection applies to this player (if globally enabled). */
    private boolean structureProtection;
    /** Personal chat language code, or {@code null} to use the server default. */
    private String language;

    public PlayerData(boolean enabled, boolean structureProtection, String language) {
        this.enabled = enabled;
        this.structureProtection = structureProtection;
        this.language = language;
    }

    public static PlayerData defaults(boolean enabled) {
        return new PlayerData(enabled, true, null);
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean structureProtection() {
        return structureProtection;
    }

    public void setStructureProtection(boolean structureProtection) {
        this.structureProtection = structureProtection;
    }

    public String language() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
