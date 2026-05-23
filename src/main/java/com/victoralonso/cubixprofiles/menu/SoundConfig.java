package com.victoralonso.cubixprofiles.menu;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * Immutable sound configuration for menu click events.
 * Compatible with vanilla keys ("minecraft:ui.button.click") and resource-pack keys.
 */
public record SoundConfig(
        String key,          // namespace:key
        Sound.Source source, // Adventure source — controls which volume slider affects this sound
        float volume,
        float pitch
) {
    public Sound toAdventure() {
        return Sound.sound(Key.key(key), source, volume, pitch);
    }
}
