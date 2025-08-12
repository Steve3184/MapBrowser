package top.steve3184.mapbrowser;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles player connection events to correctly manage display visibility.
 * - Creates displays for players who join if they are in range.
 * - Cleans up displays for players who leave the server.
 */
public class PlayerConnectionListener implements Listener {

    private final MapBrowser plugin;
    private final DisplayService displayService;

    public PlayerConnectionListener(MapBrowser plugin, DisplayService displayService) {
        this.plugin = plugin;
        this.displayService = displayService;
    }

    /**
     * Fired when a player leaves the server.
     * This method iterates through all active displays and removes the player's
     * personal display instance to prevent memory leaks.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Iterate over all displays to clean up the disconnected player's data
        for (MapBrowserDisplay display : plugin.getActiveDisplays().values()) {
            // Use synchronized block to prevent race conditions with proximity checker or commands
            synchronized (display.getViewerDisplays()) {
                if (display.getViewerDisplays().containsKey(playerUUID)) {
                    PlayerDisplay playerDisplay = display.getViewerDisplays().remove(playerUUID);
                    if (playerDisplay != null) {
                        // We don't need to call playerDisplay.destroy() because the client-side
                        // entities are automatically removed when the player disconnects.
                        // We just need to remove it from our server-side tracking maps.
                        plugin.getDisplayLookup().remove(playerDisplay.mapDisplay());
                    }
                }
            }
        }
    }

    /**
     * Fired when a player joins the server.
     * We run a check after a 1-tick delay to ensure the player is fully loaded.
     * This check determines if the player spawned within range of any displays
     * and creates them if necessary.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Run this with a 1-tick delay to ensure the player is fully initialized in the world.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return; // Player might have left immediately

            for (MapBrowserDisplay display : plugin.getActiveDisplays().values()) {
                // Check if the player is in the same world and in range
                if (display.getLocation().getWorld().equals(player.getWorld()) &&
                        display.getLocation().distanceSquared(player.getLocation()) <= 1024) { // 32*32 range

                    // Synchronize to safely add the new viewer
                    synchronized (display.getViewerDisplays()) {
                        // Double-check they aren't already a viewer (shouldn't happen on join, but safe)
                        if (!display.getViewerDisplays().containsKey(player.getUniqueId())) {
                            PlayerDisplay newPlayerDisplay = displayService.createDisplayForPlayer(player, display);
                            display.getViewerDisplays().put(player.getUniqueId(), newPlayerDisplay);
                            plugin.getDisplayLookup().put(newPlayerDisplay.mapDisplay(), display);
                        }
                    }
                }
            }
        }, 1L); // 1-tick delay
    }
}