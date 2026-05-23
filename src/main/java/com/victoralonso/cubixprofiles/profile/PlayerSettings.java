package com.victoralonso.cubixprofiles.profile;

import java.util.UUID;

/** Persistent per-player settings. Loaded from storage on join, evicted on quit. */
public record PlayerSettings(UUID uniqueId, boolean profileVisible) {

    /** Default settings for players not yet in storage. */
    public static PlayerSettings defaults(UUID uuid) {
        return new PlayerSettings(uuid, true);
    }

    public PlayerSettings withVisibility(boolean visible) {
        return new PlayerSettings(uniqueId, visible);
    }
}
