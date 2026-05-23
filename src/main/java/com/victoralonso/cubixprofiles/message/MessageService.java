package com.victoralonso.cubixprofiles.message;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MessageService {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final String defaultLang;
    private final Map<String, Map<String, String>> langCache = new ConcurrentHashMap<>();

    public MessageService(JavaPlugin plugin, String defaultLang) {
        this.plugin      = plugin;
        this.defaultLang = defaultLang;
        load(defaultLang);
    }

    // ---- loading ----

    private Map<String, String> load(String lang) {
        Map<String, String> cached = langCache.get(lang);
        if (cached != null) return cached;

        // Prefer the external (user-editable) file
        var external = new File(plugin.getDataFolder(), "messages/" + lang + ".yml");

        // Copy bundled file to data folder only on first run — never overwrites
        if (!external.exists() && plugin.getResource("messages/" + lang + ".yml") != null) {
            try { plugin.saveResource("messages/" + lang + ".yml", false); }
            catch (Exception ignored) {}
        }
        YamlConfiguration cfg;
        if (external.exists()) {
            cfg = YamlConfiguration.loadConfiguration(external);
        } else {
            InputStream stream = plugin.getResource("messages/" + lang + ".yml");
            if (stream == null) {
                if (!lang.equals(defaultLang)) return load(defaultLang);
                plugin.getLogger().warning("No messages file found for language: " + lang);
                return Map.of();
            }
            cfg = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        var map = new HashMap<String, String>();
        // getKeys(true) walks every path recursively; isString filters out section nodes
        for (String key : cfg.getKeys(true)) {
            if (cfg.isString(key)) map.put(key, cfg.getString(key, key));
        }
        var result = Collections.unmodifiableMap(map);
        langCache.putIfAbsent(lang, result);
        return result;
    }

    // ---- locale resolution ----

    private String langFor(Audience audience) {
        if (audience instanceof Player player) {
            String code = player.locale().getLanguage(); // "en", "es", "fr", …
            var external = new File(plugin.getDataFolder(), "messages/" + code + ".yml");
            if (external.exists() || plugin.getResource("messages/" + code + ".yml") != null) {
                return code;
            }
        }
        return defaultLang;
    }

    // ---- parsing ----

    private Component parse(String raw, TagResolver... resolvers) {
        return resolvers.length == 0
                ? miniMessage.deserialize(raw)
                : miniMessage.deserialize(raw, TagResolver.resolver(resolvers));
    }

    // ---- public API ----

    /** Resolve key using the default language (console, non-player contexts). */
    public Component get(String key, TagResolver... resolvers) {
        String raw = load(defaultLang).getOrDefault(key, "<" + key + ">");
        return parse(raw, resolvers);
    }

    /** Send using the audience's client locale, falling back to default. */
    public void send(Audience audience, String key, TagResolver... resolvers) {
        String lang = langFor(audience);
        var map = load(lang);
        String raw = map.getOrDefault(key, load(defaultLang).getOrDefault(key, "<" + key + ">"));
        audience.sendMessage(parse(raw, resolvers));
    }

    public void sendConsole(String key, TagResolver... resolvers) {
        plugin.getServer().getConsoleSender().sendMessage(get(key, resolvers));
    }

    /** Parse an arbitrary MiniMessage string — used for menu titles and item names. */
    public Component parseRaw(String raw, TagResolver... resolvers) {
        return parse(raw, resolvers);
    }
}
