package top.steve3184.mapbrowser;

import de.pianoman911.mapengine.api.MapEngineApi;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main plugin class for MapBrowser.
 * Manages plugin lifecycle and holds core components and data.
 */
public final class MapBrowser extends JavaPlugin {

    private JCEFManager jcefManager;
    private DisplayService displayService;
    private BukkitTask proximityCheckTask;

    // Core data structures, managed by DisplayService but owned by the main plugin class.
    private final Map<Integer, MapBrowserDisplay> activeDisplays = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<IMapDisplay, MapBrowserDisplay> displayLookup = new ConcurrentHashMap<>();

    // Plugin Lifecycle
    @Override
    public void onEnable() {
        // Load MapEngine API from Bukkit services.
        // Fields
        MapEngineApi mapEngine = Bukkit.getServicesManager().load(MapEngineApi.class);
        if (mapEngine == null) {
            getLogger().severe("MapEngine API not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load configuration and initialize JCEF.
        saveDefaultConfig();
        PluginConfig pluginConfig = new PluginConfig(this.getConfig());
        getLogger().info("Configuration loaded.");
        saveDefaultScripts();
        this.jcefManager = new JCEFManager();
        try {
            jcefManager.initialize(pluginConfig, getDataFolder());
        } catch (UnsupportedPlatformException e) {
            getLogger().severe("----------------------------------------------------");
            getLogger().severe("Unsupported Platform: " + e.getMessage());
            getLogger().severe("MapBrowser will now be disabled.");
            getLogger().severe("----------------------------------------------------");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } catch (Exception e) {
            getLogger().severe("Failed to initialize JCEF! The browser feature will not work.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize services and handlers.
        this.displayService = new DisplayService(this, mapEngine, jcefManager);
        CommandHandler commandHandler = new CommandHandler(this, displayService);
        PlayerConnectionListener connectionListener = new PlayerConnectionListener(this, displayService);
        // Register commands and event listeners.
        Objects.requireNonNull(getCommand("mapbrowser")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("mapbrowser")).setTabCompleter(commandHandler);
        getServer().getPluginManager().registerEvents(new MapInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(connectionListener, this);

        // Start tasks.
        this.proximityCheckTask = displayService.startProximityChecker();

        getLogger().info("MapBrowser has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling MapBrowser... Cleaning up displays...");
        // Cancel scheduled tasks.
        if (this.proximityCheckTask != null && !this.proximityCheckTask.isCancelled()) {
            this.proximityCheckTask.cancel();
        }

        // Clean up all active displays via the service.
        if (displayService != null) {
            displayService.cleanupAllDisplays();
        }

        // Shutdown the browser manager.
        if (jcefManager != null) {
            jcefManager.shutdown();
        }
        getLogger().info("MapBrowser has been disabled and all displays have been cleaned up.");
    }

    /**
     * Copies default JavaScript snippets from the JAR to the plugin's data folder.
     * This method should be called in the onEnable() method of your plugin.
     */
    public void saveDefaultScripts() {
        File snippetsDir = new File(getDataFolder(), "snippets");
        if (!snippetsDir.exists()) {
            if (snippetsDir.mkdirs()) {
                getLogger().info("Created snippets directory.");
            } else {
                getLogger().severe("Could not create snippets directory.");
                return;
            }
        }

        // List of your default scripts in the resources/snippets folder
        String[] defaultScripts = {"vKeyboard.js"};

        for (String scriptName : defaultScripts) {
            // The path must match the location inside your JAR file
            String resourcePath = "snippets/" + scriptName;
            File scriptFile = new File(snippetsDir, scriptName);

            // The 'false' argument prevents overwriting if the file already exists.
            // Users can customize their scripts without them being overwritten on restart.
            if (!scriptFile.exists()) {
                try {
                    saveResource(resourcePath, false);
                    getLogger().info("Saved default script: " + scriptName);
                } catch (IllegalArgumentException e) {
                    // This happens if the resource doesn't exist in the JAR
                    getLogger().warning("Could not save default script '" + scriptName + "'. It was not found in the JAR at path: " + resourcePath);
                }
            }
        }
    }

    // Getters for Services and Data
    public Map<Integer, MapBrowserDisplay> getActiveDisplays() {
        return activeDisplays;
    }

    public AtomicInteger getNextId() {
        return nextId;
    }

    public Map<IMapDisplay, MapBrowserDisplay> getDisplayLookup() {
        return displayLookup;
    }
}