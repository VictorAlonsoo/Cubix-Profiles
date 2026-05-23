package com.victoralonso.cubixprofiles;

import com.victoralonso.cubixprofiles.command.ProfileCommand;
import com.victoralonso.cubixprofiles.compatibility.CapabilityDetector;
import com.victoralonso.cubixprofiles.config.ConfigManager;
import com.victoralonso.cubixprofiles.cosmetics.CosmeticsManager;
import com.victoralonso.cubixprofiles.cosmetics.hmccosmetics.HMCCosmeticsListener;
import com.victoralonso.cubixprofiles.cosmetics.hmccosmetics.HMCCosmeticsProvider;
import com.victoralonso.cubixprofiles.menu.ItemFactory;
import com.victoralonso.cubixprofiles.menu.MenuLayout;
import com.victoralonso.cubixprofiles.menu.MenuListener;
import com.victoralonso.cubixprofiles.message.MessageService;
import com.victoralonso.cubixprofiles.placeholder.CubixPlaceholderExpansion;
import com.victoralonso.cubixprofiles.profile.ProfileListener;
import com.victoralonso.cubixprofiles.profile.ProfileService;
import com.victoralonso.cubixprofiles.profile.storage.MySQLStorage;
import com.victoralonso.cubixprofiles.profile.storage.SQLiteStorage;
import com.victoralonso.cubixprofiles.profile.storage.StorageManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.logging.Level;

public final class CubixProfiles extends JavaPlugin {

    private ConfigManager configManager;
    private MessageService messages;
    private StorageManager storage;
    private CosmeticsManager cosmeticsManager;
    private ProfileService profileService;
    private CapabilityDetector caps;
    private MenuLayout menuLayout;
    private ItemFactory itemFactory;

    private BukkitTask updateTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager    = new ConfigManager(this);
        messages         = new MessageService(this, configManager.language());
        cosmeticsManager = new CosmeticsManager();

        // Storage backend
        try {
            storage = switch (configManager.storageType().toLowerCase()) {
                case "mysql" -> new MySQLStorage(this, configManager);
                default      -> new SQLiteStorage(this);
            };
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize storage backend, disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        profileService = new ProfileService(this, storage, cosmeticsManager);
        getServer().getPluginManager().registerEvents(new ProfileListener(profileService), this);

        // Optional cosmetics integrations
        registerCosmeticsProviders();

        // GUI
        caps        = new CapabilityDetector();
        menuLayout  = new MenuLayout(this);
        itemFactory = new ItemFactory(caps);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        // Command — registered once; reads live config via plugin getters on each call
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        "profile",
                        "View a player's equipment profile.",
                        new ProfileCommand(this)
                )
        );

        // Optional PlaceholderAPI integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CubixPlaceholderExpansion(profileService, getPluginMeta().getVersion()).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }

        scheduleUpdateTask();
        messages.sendConsole("plugin-enabled");
    }

    @Override
    public void onDisable() {
        if (updateTask != null) updateTask.cancel();
        if (messages  != null) messages.sendConsole("plugin-disabled");
        if (storage   != null) storage.close();
    }

    // ---- reload ----

    /**
     * Reloads config.yml, messages, menu layout, and cosmetics providers.
     * Storage backend is NOT reloaded to avoid data loss.
     */
    public void reload() {
        reloadConfig();
        configManager.reload();
        messages   = new MessageService(this, configManager.language());
        menuLayout = new MenuLayout(this);

        cosmeticsManager.reload();
        registerCosmeticsProviders();

        if (updateTask != null) updateTask.cancel();
        scheduleUpdateTask();
    }

    // ---- cosmetics ----

    private void registerCosmeticsProviders() {
        if (getServer().getPluginManager().isPluginEnabled("HMCCosmetics")
                && configManager.hmcCosmeticsEnabled()) {
            cosmeticsManager.register(new HMCCosmeticsProvider(configManager));
            getServer().getPluginManager()
                    .registerEvents(new HMCCosmeticsListener(profileService), this);
            getLogger().info("HMCCosmetics integration enabled.");
        }
    }

    // ---- update task ----

    private void scheduleUpdateTask() {
        int interval = configManager.updateInterval();
        if (interval <= 0) return;
        updateTask = getServer().getScheduler().runTaskTimer(this, () ->
                getServer().getOnlinePlayers().forEach(profileService::capture),
                interval, interval);
    }

    // ---- getters ----

    public ConfigManager configManager()     { return configManager; }
    public MessageService messages()         { return messages; }
    public ProfileService profileService()   { return profileService; }
    public CosmeticsManager cosmeticsManager(){ return cosmeticsManager; }
    public CapabilityDetector caps()         { return caps; }
    public MenuLayout menuLayout()           { return menuLayout; }
    public ItemFactory itemFactory()         { return itemFactory; }
}
