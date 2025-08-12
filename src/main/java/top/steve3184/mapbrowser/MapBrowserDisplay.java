package top.steve3184.mapbrowser;

import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;
import org.cef.browser.MapBrowserInstance;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A data class that holds all components and information for a single browser display instance.
 * This includes the browser itself, its in-game representation, and its properties.
 */
public class MapBrowserDisplay {

    // --- Core Properties ---
    private final int id; // The unique identifier for this display.
    private String url; // The current URL the browser is displaying.
    private Location location; // The top-left-front anchor location in the world.
    private int width; // The width of the display in blocks/maps.
    private int height; // The height of the display in blocks/maps.

    // --- Browser & Rendering Components ---
    private MapBrowserInstance browser; // The underlying CEF browser instance.
    private IDrawingSpace drawingSpace; // The MapEngine surface used for drawing pixels.
    private BukkitTask renderTask; // The scheduled task that copies pixels from the browser to the map.
    private IMapDisplay masterMapDisplay; // The core, server-side MapEngine display.

    // --- Player-Specific Visuals ---
    // A map of client-side displays, one for each player currently viewing.
    private final Map<UUID, PlayerDisplay> viewerDisplays = new ConcurrentHashMap<>();

    // --- Geometric Properties ---
    private BlockVector cornerA; // The first corner block of the display area.
    private BlockVector cornerB; // The second corner block of the display area.
    private BlockFace facing; // The direction the item frames are facing.

    private volatile int[] lastPixelData = null; // The last pixel frame data
    /**
     * Constructs a new MapBrowserDisplay with its initial properties.
     *
     * @param id The unique ID for this display.
     * @param url The initial URL to load.
     * @param location The initial location in the world.
     * @param width The width in blocks.
     * @param height The height in blocks.
     */
    public MapBrowserDisplay(int id, String url, Location location, int width, int height) {
        this.id = id;
        this.url = url;
        this.location = location;
        this.width = width;
        this.height = height;
    }

    public int getId() { return id; }
    public String getUrl() { return url; }
    public Location getLocation() { return location; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public MapBrowserInstance getBrowser() { return browser; }

    public BukkitTask getRenderTask() { return this.renderTask; }
    public Map<UUID, PlayerDisplay> getViewerDisplays() { return viewerDisplays; }
    public BlockVector getCornerA() { return cornerA; }
    public BlockVector getCornerB() { return cornerB; }
    public BlockFace getFacing() { return facing; }
    public int[] getLastPixelData() { return lastPixelData; }

    public void setUrl(String url) { this.url = url; }
    public void setLocation(Location location) { this.location = location; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setBrowser(MapBrowserInstance browser) { this.browser = browser; }
    public void setRenderTask(BukkitTask renderTask) { this.renderTask = renderTask; }
    public void setCornerA(BlockVector cornerA) { this.cornerA = cornerA; }
    public void setCornerB(BlockVector cornerB) { this.cornerB = cornerB; }
    public void setFacing(BlockFace facing) { this.facing = facing; }
    public void setLastPixelData(int[] lastPixelData) { this.lastPixelData = lastPixelData; }


    /**
     * Performs a full cleanup, destroying all visual components and the browser instance.
     * This is used when the display is permanently removed.
     */
    public void cleanup() {
        // First, clean up all in-game visuals.
        if (renderTask != null && !renderTask.isCancelled()) {
            renderTask.cancel();
        }
        viewerDisplays.forEach((uuid, playerDisplay) -> {
            playerDisplay.destroy(Bukkit.getPlayer(uuid));
        });
        viewerDisplays.clear();

        // Then, close the underlying browser instance.
        if (browser != null) {
            browser.loadURL("about:blank");
            browser.close(true);
            browser = null;
        }
        lastPixelData = null;
    }
}