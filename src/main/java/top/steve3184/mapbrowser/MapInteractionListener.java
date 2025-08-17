package top.steve3184.mapbrowser;

import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.event.MapClickEvent;
import de.pianoman911.mapengine.api.util.MapClickType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.cef.browser.MapBrowserInstance;

import java.awt.event.MouseEvent;


/**
 * Listens for player interactions (clicks) with MapEngine displays
 * and forwards them to the corresponding browser instance.
 *
 * @param plugin The main MapBrowser plugin instance.
 */
public record MapInteractionListener(MapBrowser plugin) implements Listener {

    /**
     * Handles clicks on a map display.
     * @param event The event fired when a player clicks a map.
     */
    @EventHandler
    public void onMapClick(MapClickEvent event) {
        // Get the specific map display that was clicked.
        IMapDisplay clickedDisplay = event.display();

        // Look up our custom browser display object using the clicked map.
        MapBrowserDisplay browserDisplay = plugin.getDisplayLookup().get(clickedDisplay);

        // Exit if the clicked map is not a browser display or its browser is not active.
        if (browserDisplay == null || browserDisplay.getBrowser() == null) {
            return;
        }

        MapBrowserInstance browser = browserDisplay.getBrowser();

        // Translate the in-game click type to a standard AWT MouseEvent button.
        int awtButtonType = switch (event.clickType()) {
            case MapClickType.RIGHT_CLICK -> MouseEvent.BUTTON3; // Right-click
            default -> MouseEvent.BUTTON1;          // Left-click (or any other type)
        };
        int x = event.x() * browserDisplay.getScale();
        int y = event.y() * browserDisplay.getScale();
        // Send the translated click event to the CEF browser instance.
        browser.sendMouseClick(x, y, awtButtonType);
    }
}