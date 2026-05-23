package com.victoralonso.cubixprofiles.profile.storage;

import com.victoralonso.cubixprofiles.profile.PlayerSettings;
import com.victoralonso.cubixprofiles.profile.ProfileSnapshot;

import java.util.Optional;
import java.util.UUID;

public interface StorageManager {

    // ---- profile snapshot ----

    /** Persist a snapshot. Called from an async thread. */
    void save(ProfileSnapshot snapshot);

    /** Load by UUID. Called from an async thread. */
    Optional<ProfileSnapshot> load(UUID uniqueId);

    /** Load by username (case-insensitive). Called from an async thread. */
    Optional<ProfileSnapshot> loadByUsername(String username);

    /** Remove all stored data for this player. */
    void delete(UUID uniqueId);

    // ---- player settings ----

    /** Persist player settings. Called from an async thread. */
    void saveSettings(PlayerSettings settings);

    /** Load player settings. Returns empty if the player has never stored settings. */
    Optional<PlayerSettings> loadSettings(UUID uniqueId);

    // ---- lifecycle ----

    /** Release resources (connection pool, file handles, etc.). */
    void close();
}
