package com.victoralonso.cubixprofiles.menu;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MenuLayout {

    private final String titleRaw;
    private final int size;
    private final int headSlot;
    private final Map<String, Integer> equipmentSlots = new HashMap<>();
    private final Map<String, Integer> cosmeticSlots  = new HashMap<>();
    private final List<MenuItemConfig> items = new ArrayList<>();
    private final @Nullable SoundConfig globalClickSound;

    public MenuLayout(JavaPlugin plugin) {
        var file = new File(plugin.getDataFolder(), "menu.yml");
        if (!file.exists()) plugin.saveResource("menu.yml", false);
        var cfg = YamlConfiguration.loadConfiguration(file);

        titleRaw = cfg.getString("title", "<bold><#FFFFFF>Profile: <#5AB0FF><player>");
        size     = cfg.getInt("size", 54);
        headSlot = cfg.getInt("head-slot", 4);

        loadSlotMap(cfg.getConfigurationSection("equipment-slots"), equipmentSlots);
        loadSlotMap(cfg.getConfigurationSection("cosmetic-slots"),  cosmeticSlots);

        globalClickSound = parseSoundConfig(cfg.getConfigurationSection("click-sound"), true);

        var itemsSection = cfg.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String id : itemsSection.getKeys(false)) {
                var sec = itemsSection.getConfigurationSection(id);
                if (sec == null) continue;
                var item = parseItem(id, sec);
                if (item != null) items.add(item);
            }
        }
    }

    // ---- slot map loading ----

    private static void loadSlotMap(ConfigurationSection section, Map<String, Integer> target) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            target.put(key, section.getInt(key));
        }
    }

    // ---- item parsing ----

    private MenuItemConfig parseItem(String id, ConfigurationSection sec) {
        boolean enabled = sec.getBoolean("enabled", true);
        List<Integer> slotList = parseSlots(sec);
        if (slotList.isEmpty()) return null;

        String matName = sec.getString("material", "AIR");
        Material material = Material.matchMaterial(matName);
        if (material == null) material = Material.AIR;

        return new MenuItemConfig(
                id, enabled, slotList, material,
                sec.getString("name", null),
                sec.getStringList("lore"),
                sec.getInt("custom-model-data", 0),
                sec.getString("item-model", null),
                sec.getString("tooltip-style", null),
                sec.getBoolean("hide-tooltip", false),
                sec.getStringList("actions"),
                parseSoundConfig(sec.getConfigurationSection("sound"), false)
        );
    }

    private List<Integer> parseSlots(ConfigurationSection sec) {
        if (sec.contains("slot") && !sec.contains("slots")) {
            return List.of(sec.getInt("slot"));
        }
        if (sec.isList("slots")) {
            return sec.getIntegerList("slots");
        }
        if (sec.isString("slots") || sec.isInt("slots")) {
            return parseSlotString(sec.getString("slots", ""));
        }
        if (sec.contains("slot")) {
            return List.of(sec.getInt("slot"));
        }
        return List.of();
    }

    private List<Integer> parseSlotString(String raw) {
        var result = new ArrayList<Integer>();
        for (String part : raw.split(",")) {
            part = part.strip();
            if (part.contains("-")) {
                var bounds = part.split("-", 2);
                try {
                    int from = Integer.parseInt(bounds[0].strip());
                    int to   = Integer.parseInt(bounds[1].strip());
                    for (int i = from; i <= to; i++) result.add(i);
                } catch (NumberFormatException ignored) {}
            } else {
                try { result.add(Integer.parseInt(part)); }
                catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    // ---- sound config parsing ----

    /**
     * Parses a sound configuration section.
     * @param sec      the YAML section to parse, or null
     * @param checkEnabled when true the section must have enabled: true (default) to return non-null
     */
    private static @Nullable SoundConfig parseSoundConfig(@Nullable ConfigurationSection sec,
                                                           boolean checkEnabled) {
        if (sec == null) return null;
        if (checkEnabled && !sec.getBoolean("enabled", true)) return null;

        String key = sec.getString("sound", "minecraft:ui.button.click");

        Sound.Source source = Sound.Source.MASTER;
        try {
            source = Sound.Source.valueOf(sec.getString("source", "MASTER").toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        float volume = (float) sec.getDouble("volume", 1.0);
        float pitch  = (float) sec.getDouble("pitch",  1.0);
        return new SoundConfig(key, source, volume, pitch);
    }

    // ---- getters ----

    public String titleRaw()            { return titleRaw; }
    public int size()                   { return size; }
    public int headSlot()               { return headSlot; }

    /** Equipment slot by name (helmet, chestplate, …). Returns -1 if not configured. */
    public int equipmentSlot(String key){ return equipmentSlots.getOrDefault(key, -1); }

    /** Cosmetic slot by key (e.g. "hmcc_HELMET"). Returns -1 if not configured. */
    public int cosmeticSlot(String key) { return cosmeticSlots.getOrDefault(key, -1); }

    public List<MenuItemConfig> items() { return items; }

    /** Global click sound, or null if disabled / not configured. */
    public @Nullable SoundConfig globalClickSound() { return globalClickSound; }
}
