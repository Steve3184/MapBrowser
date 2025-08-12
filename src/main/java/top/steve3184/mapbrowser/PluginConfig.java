package top.steve3184.mapbrowser;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class PluginConfig {

    private final String customJcefPath;
    private final boolean isSkipDownload;
    private final boolean remoteDebuggingEnabled;
    private final int remoteDebuggingPort;
    private final boolean enableSound;
    private final List<String> customChromeArgs;
    private final String userDataDir;
    private final String chromeLogOutput;
    private final String jcefMirror;
    private final String userAgent;


    public PluginConfig(FileConfiguration config) {
        this.customJcefPath = config.getString("jcef.custom-install-path", "");
        this.isSkipDownload = config.getBoolean("jcef.skip-download", false);
        this.jcefMirror = config.getString("jcef.mirror", "");
        this.userAgent = config.getString("browser.user-agent", "");
        this.remoteDebuggingEnabled = config.getBoolean("browser.remote-debugging.enabled", false);
        this.remoteDebuggingPort = config.getInt("browser.remote-debugging.port", 9222);
        this.enableSound = config.getBoolean("browser.enable-sound", false);
        this.customChromeArgs = config.getStringList("browser.custom-chrome-args");
        this.userDataDir = config.getString("browser.user-data-dir", "userdata");
        this.chromeLogOutput = config.getString("browser.chrome-log-output", "console").toLowerCase();
    }

    // --- Getters ---
    public String getCustomJcefPath() { return customJcefPath; }
    public boolean getIsSkipDownload() { return isSkipDownload; }
    public boolean isRemoteDebuggingEnabled() { return remoteDebuggingEnabled; }
    public int getRemoteDebuggingPort() { return remoteDebuggingPort; }
    public boolean isEnableSound() { return enableSound; }
    public List<String> getCustomChromeArgs() { return customChromeArgs; }
    public String getUserDataDir() { return userDataDir; }
    public String getChromeLogOutput() { return chromeLogOutput; }
    public String getJcefMirror() { return jcefMirror; }
    public String getUserAgent() { return userAgent; }
}