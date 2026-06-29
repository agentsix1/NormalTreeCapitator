package dev.normaltreecapitator.playerdata;

public final class PlayerData {

    private boolean enabled;

    public PlayerData(boolean enabled) {
        this.enabled = enabled;
    }

    public static PlayerData defaults(boolean enabled) {
        return new PlayerData(enabled);
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
