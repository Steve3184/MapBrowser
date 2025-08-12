package top.steve3184.mapbrowser;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.MapBrowserInstance;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandlerAdapter;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of the JCEF (Java Chromium Embedded Framework) application.
 * This class handles the initialization, configuration, and shutdown of the browser engine.
 */
public class JCEFManager {

    private static final Logger LOGGER = Logger.getLogger(JCEFManager.class.getName());
    private CefApp cefApp;
    private CefClient cefClient;

    /**
     * Initializes the CefApp with settings from the plugin configuration.
     * This method sets up installation paths, logging, debugging, and other browser arguments.
     *
     * @param config The plugin's configuration object.
     * @param dataFolder The plugin's data folder, used for default paths.
     * @throws CefInitializationException If JCEF fails to initialize.
     * @throws IOException If there is an I/O error.
     * @throws InterruptedException If the thread is interrupted during initialization.
     * @throws UnsupportedPlatformException If the OS is not supported by JCEF.
     */
    public void initialize(PluginConfig config, File dataFolder) throws CefInitializationException, IOException, InterruptedException, UnsupportedPlatformException {
        String platformIdentifier = getPlatformIdentifier();
        CefAppBuilder builder = new CefAppBuilder();
        builder.setProgressHandler(new FancyProgressHandler(LOGGER));
        String mirrorBaseUrl = config.getJcefMirror();
        if (mirrorBaseUrl != null && !mirrorBaseUrl.trim().isEmpty() || platformIdentifier == null) {
            if (!mirrorBaseUrl.endsWith("/")) {
                mirrorBaseUrl += "/";
            }
            String finalDownloadUrl = mirrorBaseUrl + platformIdentifier + ".zip";
            LOGGER.info("Using custom JCEF download url: " + finalDownloadUrl);
            builder.setMirrors(Collections.singleton(finalDownloadUrl));
        } else {
            LOGGER.info("Using default JCEF download repository.");
        }
        // --- Configure JCEF Installation Path ---
        String customPath = config.getCustomJcefPath();
        if (customPath != null && !customPath.trim().isEmpty()) {
            File customDir = new File(customPath);
            if (customDir.exists() && customDir.isDirectory()) {
                builder.setInstallDir(customDir);
                if (config.getIsSkipDownload()) {
                    builder.setSkipInstallation(true); // Use existing installation.
                }
                LOGGER.info("Using custom JCEF path: " + customPath);
            } else {
                LOGGER.warning("Invalid custom JCEF path: " + customPath + ". Falling back to default.");
                builder.setInstallDir(new File(dataFolder, "jcef-bundle"));
            }
        } else {
            builder.setInstallDir(new File(dataFolder, "jcef-bundle"));
            LOGGER.info("Using default JCEF installation path.");
        }

        // --- Configure User Data and Logging ---
        Path userDataPath = Paths.get(config.getUserDataDir());
        if (!userDataPath.isAbsolute()) {
            userDataPath = dataFolder.toPath().resolve(userDataPath);
        }
        builder.getCefSettings().cache_path = userDataPath.toFile().getAbsolutePath();
        LOGGER.info("Using User Data Directory: " + builder.getCefSettings().cache_path);

        if ("file".equalsIgnoreCase(config.getChromeLogOutput())) {
            File logFile = new File(dataFolder, "cef.log");
            builder.getCefSettings().log_file = logFile.getAbsolutePath();
            builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_FATAL;
            LOGGER.info("Chrome log will be redirected to: " + logFile.getAbsolutePath());
        } else {
            builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;
            LOGGER.info("Chrome log will be output to the console.");
        }

        // --- Configure Rendering and Debugging ---
        builder.getCefSettings().windowless_rendering_enabled = true; // Required for off-screen rendering.
        if (config.isRemoteDebuggingEnabled()) {
            int port = config.getRemoteDebuggingPort();
            builder.getCefSettings().remote_debugging_port = port;
            LOGGER.info("JCEF remote debugging enabled on port: " + port);
            LOGGER.info("To debug, open Chromium and navigate to: chrome://inspect");
        } else {
            LOGGER.info("JCEF remote debugging is disabled.");
        }

        // --- Add Command Line Arguments ---
        builder.addJcefArgs(
                "--off-screen-rendering-enabled",
                "--off-screen-frame-rate=20"
        );
        if (!config.isEnableSound()) {
            builder.addJcefArgs("--mute-audio");
            LOGGER.info("Browser audio is muted.");
        }
        if (config.getCustomChromeArgs() != null && !config.getCustomChromeArgs().isEmpty()) {
            LOGGER.info("Adding custom chrome args: " + String.join(" ", config.getCustomChromeArgs()));
            builder.addJcefArgs(config.getCustomChromeArgs().toArray(new String[0]));
        }
        if (!Objects.equals(config.getUserAgent(), "")) {
            LOGGER.info("Using custom user-agent: " +  config.getUserAgent());
            builder.getCefSettings().user_agent = config.getUserAgent();
        }

        // --- Set App Handler and Build ---
        builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            @Override
            public void stateHasChanged(CefApp.CefAppState state) {
                if (state == CefApp.CefAppState.TERMINATED) {
                    LOGGER.info("JCEF App has been terminated.");
                }
            }
        });

        LOGGER.info("Initializing JCEF...");
        this.cefApp = builder.build();
        LOGGER.info("JCEF Initialized.");

        // --- Create a single client for all browsers ---
        this.cefClient = this.cefApp.createClient();

        // Disable the right-click context menu on all browsers.
        this.cefClient.addContextMenuHandler(new CefContextMenuHandlerAdapter() {
            @Override
            public void onBeforeContextMenu(CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
                model.clear();
            }
        });
    }

    /**
     * Shuts down the JCEF application and disposes of all resources.
     */
    public void shutdown() {
        LOGGER.info("Shutting down JCEF...");
        if (cefClient != null) {
            cefClient.dispose();
        }
        if (cefApp != null) {
            cefApp.dispose();
        }
        LOGGER.info("JCEF shutdown complete.");
    }

    /**
     * Creates a new off-screen browser instance.
     *
     * @param url The initial URL to load.
     * @return A new MapBrowserInstance, or null if JCEF is not initialized.
     */
    @Nullable
    public MapBrowserInstance createBrowser(String url) {
        if (cefClient == null) {
            LOGGER.severe("Cannot create browser, CefClient is not initialized.");
            return null;
        }
        // All browsers created will share the same CefClient instance.
        return new MapBrowserInstance(cefClient, url);
    }

    private String getPlatformIdentifier() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (arch.equals("amd64")) {
            if (os.contains("win")) {
                return "windows_amd64";
            }
            if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                return "linux_amd64";
            }
        }
        return null; // Unsupport Platform
    }
}