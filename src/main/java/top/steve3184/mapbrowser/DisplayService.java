package top.steve3184.mapbrowser;

import de.pianoman911.mapengine.api.MapEngineApi;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import de.pianoman911.mapengine.api.util.Converter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.cef.browser.MapBrowserInstance;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service class for managing the lifecycle and interactions of MapBrowser displays.
 * Contains optimized and thread-safe logic for display management.
 */
public class DisplayService {

    private final MapBrowser plugin;
    private final MapEngineApi mapEngine;
    private final JCEFManager jcefManager;
    private final Logger logger;

    public DisplayService(MapBrowser plugin, MapEngineApi mapEngine, JCEFManager jcefManager) {
        this.plugin = plugin;
        this.mapEngine = mapEngine;
        this.jcefManager = jcefManager;
        this.logger = plugin.getLogger();
    }

    /**
     * Creates, initializes, and stores a new browser display.
     * @return The newly created MapBrowserDisplay object.
     */
    public MapBrowserDisplay createAndInitializeDisplay(Player creator, String url, Location location, int width, int height) {
        int id = plugin.getNextId().getAndIncrement();
        MapBrowserDisplay displayInfo = new MapBrowserDisplay(id, url, location, width, height);

        calculateAndStoreGeometry(creator, displayInfo);

        if (jcefManager != null) {
            IMapDisplay tempDisplay = mapEngine.displayProvider().createBasic(displayInfo.getCornerA(), displayInfo.getCornerB(), displayInfo.getFacing());
            final int browserWidth = tempDisplay.pixelWidth();
            final int browserHeight = tempDisplay.pixelHeight();
            tempDisplay.destroy();

            MapBrowserInstance browser = jcefManager.createBrowser(displayInfo.getUrl());
            if (browser == null) {
                logger.severe("Failed to create browser instance for display #" + id);
                return null;
            }
            displayInfo.setBrowser(browser);

            SwingUtilities.invokeLater(() -> {
                try {
                    browser.createImmediately();
                    browser.resize(browserWidth, browserHeight);
                    startRenderLoop(displayInfo);
                    logger.info("Successfully created browser and render loop for display #" + displayInfo.getId());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }

        // The viewer map is synchronized to prevent race conditions with the proximity checker
        synchronized (displayInfo.getViewerDisplays()) {
            PlayerDisplay creatorDisplay = createDisplayForPlayer(creator, displayInfo);
            displayInfo.getViewerDisplays().put(creator.getUniqueId(), creatorDisplay);
            plugin.getDisplayLookup().put(creatorDisplay.mapDisplay(), displayInfo);
        }

        plugin.getActiveDisplays().put(id, displayInfo);
        return displayInfo;
    }

    /**
     * Removes a display and cleans up all its associated resources.
     * This is the centralized method for removing displays.
     * @return true if a display was found and removed, false otherwise.
     */
    public boolean removeDisplay(int id) {
        MapBrowserDisplay displayInfo = plugin.getActiveDisplays().remove(id);
        if (displayInfo == null) {
            return false;
        }

        // Synchronize to safely iterate and modify the lookup map
        synchronized (displayInfo.getViewerDisplays()) {
            displayInfo.getViewerDisplays().values().forEach(playerDisplay ->
                    plugin.getDisplayLookup().remove(playerDisplay.mapDisplay())
            );
        }
        displayInfo.cleanup(); // Cleans up browser, task, and viewers
        return true;
    }

    /**
     * Starts a repeating task to check player proximity to displays.
     * This version groups displays by world to avoid unnecessary calculations.
     */
    public BukkitTask startProximityChecker() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getActiveDisplays().isEmpty()) return;

                // Optimization: Group active displays by world to reduce iterations.
                Map<World, List<MapBrowserDisplay>> displaysByWorld = plugin.getActiveDisplays().values().stream()
                        .collect(Collectors.groupingBy(display -> display.getLocation().getWorld()));

                for (Player player : Bukkit.getOnlinePlayers()) {
                    World playerWorld = player.getWorld();
                    List<MapBrowserDisplay> worldDisplays = displaysByWorld.get(playerWorld);

                    if (worldDisplays == null || worldDisplays.isEmpty()) {
                        // If player is in a world with no displays, we might need to clean up if they were viewing before.
                        // This part is complex, for now we assume displays don't move between worlds.
                        // A simpler approach is to iterate displays and check player world, which is what we do next.
                        continue;
                    }

                    for (MapBrowserDisplay display : worldDisplays) {
                        // Further checks are done inside a synchronized block to prevent race conditions.
                        synchronized (display.getViewerDisplays()) {
                            UUID playerUUID = player.getUniqueId();
                            boolean isViewing = display.getViewerDisplays().containsKey(playerUUID);
                            Location displayCenter = display.getLocation();

                            // Check distance within the same world. 32*32 = 1024
                            if (player.getLocation().distanceSquared(displayCenter) <= 1024) {
                                if (!isViewing) {
                                    // Player entered range, create display for them.
                                    PlayerDisplay newPlayerDisplay = createDisplayForPlayer(player, display);
                                    display.getViewerDisplays().put(playerUUID, newPlayerDisplay);
                                    plugin.getDisplayLookup().put(newPlayerDisplay.mapDisplay(), display);
                                }
                            } else {
                                if (isViewing) {
                                    // Player left range, remove their display.
                                    PlayerDisplay oldPlayerDisplay = display.getViewerDisplays().remove(playerUUID);
                                    if (oldPlayerDisplay != null) {
                                        plugin.getDisplayLookup().remove(oldPlayerDisplay.mapDisplay());
                                        oldPlayerDisplay.destroy(player);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 20L); // Run every second, 2 seconds after start
    }

    /**
     * Moves a display to a new location. Thread-safe.
     */
    public void moveDisplay(Player player, MapBrowserDisplay display, Location newLocation) {
        synchronized (display.getViewerDisplays()) {
            // 1. Clean up visuals and render task
            cleanupPlayerVisuals(display);
            if (display.getRenderTask() != null) display.getRenderTask().cancel();

            // 2. Update properties and geometry
            display.setLocation(newLocation);
            calculateAndStoreGeometry(player, display);

            // 3. Restart render loop
            startRenderLoop(display);

            // 4. Create the first new display for the command issuer
            PlayerDisplay newPlayerDisplay = createDisplayForPlayer(player, display);
            display.getViewerDisplays().put(player.getUniqueId(), newPlayerDisplay);
            plugin.getDisplayLookup().put(newPlayerDisplay.mapDisplay(), display);
        }
    }

    /**
     * Resizes a display. Thread-safe.
     */
    public void resizeDisplay(Player player, MapBrowserDisplay display, int newWidth, int newHeight) {
        synchronized (display.getViewerDisplays()) {
            final MapBrowserInstance browser = display.getBrowser();
            if (browser == null) return;

            // 1. Clean up visuals and render task
            cleanupPlayerVisuals(display);
            if (display.getRenderTask() != null) display.getRenderTask().cancel();

            // 2. Update properties and geometry
            display.setWidth(newWidth);
            display.setHeight(newHeight);
            calculateAndStoreGeometry(player, display);

            // 3. Resize browser on AWT thread
            IMapDisplay tempDisplay = mapEngine.displayProvider().createBasic(display.getCornerA(), display.getCornerB(), display.getFacing());
            final int newPixelWidth = tempDisplay.pixelWidth() * display.getScale();
            final int newPixelHeight = tempDisplay.pixelHeight() * display.getScale();
            tempDisplay.destroy();
            SwingUtilities.invokeLater(() -> browser.resize(newPixelWidth, newPixelHeight));

            // 4. Restart render loop and create first new display
            startRenderLoop(display);
            PlayerDisplay newPlayerDisplay = createDisplayForPlayer(player, display);
            display.getViewerDisplays().put(player.getUniqueId(), newPlayerDisplay);
            plugin.getDisplayLookup().put(newPlayerDisplay.mapDisplay(), display);
        }
    }

    public void scaleDisplay(Player player, MapBrowserDisplay display, int newScale) {
        synchronized (display.getViewerDisplays()) {
            final MapBrowserInstance browser = display.getBrowser();
            if (browser == null) return;
            display.setScale(newScale);
            SwingUtilities.invokeLater(() -> browser.resize(display.getWidth() * 128 * newScale, display.getHeight() * 128 * newScale));
        }
    }

    // Helper to clean player visuals safely
    private void cleanupPlayerVisuals(MapBrowserDisplay display) {
        display.getViewerDisplays().forEach((uuid, playerDisplay) -> {
            plugin.getDisplayLookup().remove(playerDisplay.mapDisplay());
            Player viewer = Bukkit.getPlayer(uuid);
            if(viewer != null) playerDisplay.destroy(viewer);
        });
        display.getViewerDisplays().clear();
    }

    public void cleanupAllDisplays() {
        plugin.getActiveDisplays().values().forEach(this::cleanupPlayerVisuals);
        plugin.getActiveDisplays().values().forEach(MapBrowserDisplay::cleanup);
        plugin.getActiveDisplays().clear();
        plugin.getDisplayLookup().clear();
    }

    /**
     * Scales down pixel data by a given integer factor using the nearest-neighbor (top-left) method.
     *
     * @param originalPixelData The original pixel data array from the browser frame.
     * @param originalWidth     The width of the original frame.
     * @param originalHeight    The height of the original frame.
     * @param scale             The integer scaling factor. For example, a scale of 2 will reduce the
     *                          image dimensions by half (e.g., 256x256 becomes 128x128). Must be 1 or greater.
     * @return A new integer array containing the scaled-down pixel data.
     * @throws IllegalArgumentException if the scale factor is less than 1 or if the input data
     *                                  length doesn't match the specified dimensions.
     */
    public static int[] scalePixelData(int[] originalPixelData, int originalWidth, int originalHeight, int scale) {
        if (scale < 1) {
            throw new IllegalArgumentException("Scale factor must be 1 or greater.");
        }
        if (originalPixelData.length != originalWidth * originalHeight) {
            throw new IllegalArgumentException("Original pixel data array length does not match the provided dimensions.");
        }
        if (scale == 1) {
            return originalPixelData.clone();
        }
        int newWidth = originalWidth / scale;
        int newHeight = originalHeight / scale;

        int[] scaledPixelData = new int[newWidth * newHeight];
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                int originalX = x * scale;
                int originalY = y * scale;
                int originalIndex = originalY * originalWidth + originalX;
                int scaledIndex = y * newWidth + x;
                scaledPixelData[scaledIndex] = originalPixelData[originalIndex];
            }
        }
        return scaledPixelData;
    }

    private void startRenderLoop(MapBrowserDisplay displayInfo) {
        final MapBrowserInstance browser = displayInfo.getBrowser();
        if (browser == null) {
            logger.warning("Cannot start render loop for display #" + displayInfo.getId() + ", browser is missing.");
            return;
        }
        BukkitRunnable renderLoop = new BukkitRunnable() {
            @Override
            public void run() {
                if (displayInfo.getBrowser() == null) { this.cancel(); return; }
                int[] tmpPixelData = browser.getAndUpdatePixelData();
                if (tmpPixelData != null) {
                    int[] pixelData = scalePixelData(tmpPixelData, browser.getPixelWidth(), browser.getPixelHeight(), displayInfo.getScale());
                    displayInfo.setLastPixelData(pixelData);
                    // Synchronize when accessing the viewer list to avoid concurrent modification
                    synchronized (displayInfo.getViewerDisplays()) {
                        for (PlayerDisplay playerDisplay : displayInfo.getViewerDisplays().values()) {
                            IDrawingSpace drawingSpace = playerDisplay.drawingSpace();
                            if (drawingSpace != null) {
                                try {
                                    drawingSpace.pixels(pixelData, 0, 0, displayInfo.getWidth() * 128, displayInfo.getHeight() * 128);
                                    drawingSpace.flush();
                                } catch (ArrayIndexOutOfBoundsException ignored) {}
                            }
                        }
                    }
                }
            }
        };
        displayInfo.setRenderTask(renderLoop.runTaskTimerAsynchronously(plugin, 0L, 2L));
    }

    public void calculateAndStoreGeometry(Player viewer, MapBrowserDisplay displayInfo) {
        Vector direction = viewer.getLocation().getDirection().setY(0).normalize();
        Vector rightVec = direction.getCrossProduct(new Vector(0, 1, 0)).normalize();
        Vector downVec = new Vector(0, -1, 0);
        BlockVector rightStep = new BlockVector(
                Math.round(rightVec.getX()),
                Math.round(rightVec.getY()),
                Math.round(rightVec.getZ())
        );
        BlockVector downStep = new BlockVector(0, -1, 0);
        BlockVector cornerA = displayInfo.getLocation().toVector().toBlockVector();
        BlockVector totalOffset = rightStep.clone().multiply(displayInfo.getWidth() - 1)
                .add(downStep.clone().multiply(displayInfo.getHeight() - 1)).toBlockVector();
        BlockVector cornerB = cornerA.clone().add(totalOffset).toBlockVector();
        BlockFace facing = viewer.getFacing().getOppositeFace();
        displayInfo.setCornerA(cornerA);
        displayInfo.setCornerB(cornerB);
        displayInfo.setFacing(facing);
    }

    public PlayerDisplay createDisplayForPlayer(Player player, MapBrowserDisplay sourceDisplay) {
        BlockVector cornerA = sourceDisplay.getCornerA();
        BlockVector cornerB = sourceDisplay.getCornerB();
        BlockFace facing = sourceDisplay.getFacing();

        IMapDisplay display = mapEngine.displayProvider().createBasic(cornerA, cornerB, facing);
        IDrawingSpace drawingSpace = mapEngine.pipeline().createDrawingSpace(display);
        drawingSpace.ctx().receivers().add(player);
        drawingSpace.ctx().buffering(true);
        drawingSpace.ctx().converter(Converter.FLOYD_STEINBERG);

        int[] lastFrame = sourceDisplay.getLastPixelData();
        if (lastFrame != null && sourceDisplay.getBrowser() != null && lastFrame.length == sourceDisplay.getWidth() * sourceDisplay.getHeight() * 128 * 128) {
            drawingSpace.pixels(lastFrame, 0, 0, sourceDisplay.getWidth() * 128, sourceDisplay.getHeight() * 128);
            drawingSpace.flush();
        }

        display.spawn(player, 0);

        return new PlayerDisplay(display, drawingSpace);
    }
}