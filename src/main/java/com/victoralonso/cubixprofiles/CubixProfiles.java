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
import com.victoralonso.cubixprofiles.profile.SettingsService;
import com.victoralonso.cubixprofiles.profile.storage.MySQLStorage;
import com.victoralonso.cubixprofiles.profile.storage.SQLiteStorage;
import com.victoralonso.cubixprofiles.profile.storage.StorageManager;
import com.victoralonso.cubixprofiles.rank.RankManager;
import com.victoralonso.cubixprofiles.rank.luckperms.LuckPermsRankListener;
import com.victoralonso.cubixprofiles.rank.luckperms.LuckPermsRankProvider;
import com.victoralonso.cubixprofiles.rank.vault.VaultRankProvider;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.logging.Level;

public final class CubixProfiles extends JavaPlugin {

    private ConfigManager configManager;
    private MessageService messages;
    private StorageManager storage;
    private CosmeticsManager cosmeticsManager;
    private RankManager rankManager;
    private ProfileService profileService;
    private SettingsService settingsService;
    private CapabilityDetector caps;
    private MenuLayout selfMenuLayout;
    private MenuLayout otherMenuLayout;
    private ItemFactory itemFactory;

    private BukkitTask updateTask;
    @Nullable private EventSubscription<UserDataRecalculateEvent> lpSubscription;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager    = new ConfigManager(this);
        messages         = new MessageService(this, configManager.language());
        cosmeticsManager = new CosmeticsManager();
        rankManager      = new RankManager();

        // Storage backend
        try {
            storage = switch (configManager.storageType().toLowerCase()) {
                case "mysql" -> new MySQLStorage(this, configManager);
                default      -> new SQLiteStorage(this, configManager.sqliteFile());
            };
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize storage backend, disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        profileService  = new ProfileService(this, storage, cosmeticsManager, rankManager);
        settingsService = new SettingsService(this, storage);

        registerRankProviders();
        registerCosmeticsProviders();

        getServer().getPluginManager().registerEvents(
                new ProfileListener(profileService, settingsService), this);

        // GUI
        caps             = new CapabilityDetector();
        selfMenuLayout   = new MenuLayout(this, "menu-self.yml");
        otherMenuLayout  = new MenuLayout(this, "menu-other.yml");
        itemFactory      = new ItemFactory(caps);
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
            new CubixPlaceholderExpansion(profileService, settingsService, getPluginMeta().getVersion()).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }

        scheduleUpdateTask();
        messages.sendConsole("plugin.enabled");
    }

    @Override
    public void onDisable() {
        if (lpSubscription != null) { lpSubscription.close(); lpSubscription = null; }
        if (updateTask     != null) updateTask.cancel();
        if (messages       != null) messages.sendConsole("plugin.disabled");
        if (storage        != null) storage.close();
    }

    // ---- reload ----

    /**
     * Reloads config, messages, menu layout, and rank/cosmetics providers.
     * Storage is NOT reloaded to avoid data loss.
     */
    public void reload() {
        reloadConfig();
        configManager.reload();
        messages        = new MessageService(this, configManager.language());
        selfMenuLayout  = new MenuLayout(this, "menu-self.yml");
        otherMenuLayout = new MenuLayout(this, "menu-other.yml");

        // Close existing LP subscription before re-registering providers
        if (lpSubscription != null) { lpSubscription.close(); lpSubscription = null; }
        rankManager.reload();
        registerRankProviders();

        cosmeticsManager.reload();
        registerCosmeticsProviders();

        if (updateTask != null) updateTask.cancel();
        scheduleUpdateTask();
    }

    // ---- rank providers ----

    private void registerRankProviders() {
        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            rankManager.register(new LuckPermsRankProvider());
            lpSubscription = LuckPermsRankListener.subscribe(profileService);
            getLogger().info("LuckPerms rank integration enabled.");
        } else if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            rankManager.register(new VaultRankProvider());
            getLogger().info("Vault rank integration enabled.");
        }
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

    public ConfigManager   configManager()      { return configManager; }
    public MessageService  messages()           { return messages; }
    public ProfileService  profileService()     { return profileService; }
    public SettingsService settingsService()    { return settingsService; }
    public CosmeticsManager cosmeticsManager()  { return cosmeticsManager; }
    public RankManager     rankManager()        { return rankManager; }
    public CapabilityDetector caps()             { return caps; }
    public MenuLayout      selfMenuLayout()     { return selfMenuLayout; }
    public MenuLayout      otherMenuLayout()    { return otherMenuLayout; }
    public ItemFactory     itemFactory()        { return itemFactory; }
}
