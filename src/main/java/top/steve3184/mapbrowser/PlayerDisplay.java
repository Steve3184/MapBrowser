package top.steve3184.mapbrowser;

import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import org.bukkit.entity.Player;

public record PlayerDisplay(IMapDisplay mapDisplay, IDrawingSpace drawingSpace) {

    public void destroy(Player player) {
        if (drawingSpace != null) {
            drawingSpace.destroy();
        }
        if (mapDisplay != null) {
            mapDisplay.destroy();
            if (player != null && player.isOnline()) {
                mapDisplay.despawn(player);
            }
        }
    }
}