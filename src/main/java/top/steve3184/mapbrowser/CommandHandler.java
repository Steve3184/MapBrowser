package top.steve3184.mapbrowser;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.cef.browser.MapBrowserInstance;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all command execution and tab completion for the MapBrowser plugin.
 */
public class CommandHandler implements CommandExecutor, TabCompleter {

    private final MapBrowser plugin;
    private final DisplayService displayService;

    public CommandHandler(MapBrowser plugin, DisplayService displayService) {
        this.plugin = plugin;
        this.displayService = displayService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by a player.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("mapbrowser.command.base")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create" -> handleCreateCommand(player, args);
            case "list" -> handleListCommand(player);
            case "remove" -> handleRemoveCommand(player, args);
            case "modify" -> handleModifyCommand(player, args);
            case "input" -> handleInputCommand(player, args);
            case "keys" -> handleKeysCommand(player, args);
            case "near" -> handleNearCommand(player);
            case "executejs" -> handleExecuteJsCommand(player, args);
            default -> sendHelpMessage(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        final List<String> completions = new ArrayList<>();
        final List<String> subCommands = Arrays.asList("create", "list", "remove", "modify", "input", "keys", "near", "executeJs");

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], subCommands, completions);
        }

        String subCommand = args[0].toLowerCase();

        // Enhanced Tab-Completion for /mb create
        if (subCommand.equals("create")) {
            Block targetBlock = player.getTargetBlock(null, 10); // Look up to 10 blocks away
            if (args.length == 2) completions.add(String.valueOf(targetBlock.getX()));
            if (args.length == 3) completions.add(String.valueOf(targetBlock.getY()));
            if (args.length == 4) completions.add(String.valueOf(targetBlock.getZ()));
            if (args.length == 5) completions.add("<url>");
            if (args.length == 6) completions.add("[width]");
            if (args.length == 7) completions.add("[height]");
            return completions;
        }

        final List<String> idRequiredCommands = Arrays.asList("remove", "modify", "input", "keys", "executejs");
        if (args.length == 2 && idRequiredCommands.contains(subCommand)) {
            List<String> ids = plugin.getActiveDisplays().keySet().stream().map(String::valueOf).collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[1], ids, completions);
        }

        if (subCommand.equals("modify")) {
            if (args.length == 3) {
                return StringUtil.copyPartialMatches(args[2], Arrays.asList("url", "devtools", "pos", "size", "refresh"), completions);
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("devtools")) {
                return StringUtil.copyPartialMatches(args[3], Arrays.asList("on", "off"), completions);
            }
            if (args[2].equalsIgnoreCase("size")) {
                if (args.length == 4) {
                    return StringUtil.copyPartialMatches(args[3], List.of("<width>"), completions);
                }
                if (args.length == 5) {
                    return StringUtil.copyPartialMatches(args[4], List.of("<height>"), completions);
                }
            }
            if (args[2].equalsIgnoreCase("pos")) {
                Block targetBlock = player.getTargetBlock(null, 10);
                if (args.length == 4) {
                    return StringUtil.copyPartialMatches(args[3], List.of(String.valueOf(targetBlock.getX())), completions);
                }
                if (args.length == 5) {
                    return StringUtil.copyPartialMatches(args[4], List.of(String.valueOf(targetBlock.getY())), completions);
                }
                if (args.length == 6) {
                    return StringUtil.copyPartialMatches(args[5], List.of(String.valueOf(targetBlock.getZ())), completions);
                }
            }
        }

        if (subCommand.equals("keys")) {
            if (args.length == 3) {
                return StringUtil.copyPartialMatches(args[2], KeyMap.getKeyNames(), completions);
            }
            if (args.length == 4) {
                return StringUtil.copyPartialMatches(args[3], Arrays.asList("pressDown", "pressUp", "click"), completions);
            }
        }

        return Collections.emptyList();
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("mapbrowser.command.create")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }
        if (args.length < 5) {
            player.sendMessage(Component.text("Usage: /mb create <x> <y> <z> <url> [width] [height]", NamedTextColor.RED));
            return;
        }

        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            String url = args[4];
            int width = args.length > 5 ? Integer.parseInt(args[5]) : 1;
            int height = args.length > 6 ? Integer.parseInt(args[6]) : 1;
            if (width <= 0 || height <= 0) {
                player.sendMessage(Component.text("Width and height must be positive.", NamedTextColor.RED));
                return;
            }

            Location location = new Location(player.getWorld(), x, y, z);
            // Call the service and get the created display back
            MapBrowserDisplay newDisplay = displayService.createAndInitializeDisplay(player, url, location, width, height);

            if (newDisplay != null) {
                player.sendMessage(Component.text("Successfully created map display #", NamedTextColor.GREEN)
                        .append(Component.text(newDisplay.getId(), NamedTextColor.WHITE)));
            } else {
                player.sendMessage(Component.text("Failed to create map display.", NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid number format for coordinates, width, or height.", NamedTextColor.RED));
        }
    }

    private void handleListCommand(Player player) {
        if (!player.hasPermission("mapbrowser.command.list")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }
        Map<Integer, MapBrowserDisplay> activeDisplays = plugin.getActiveDisplays();
        if (activeDisplays.isEmpty()) {
            player.sendMessage(Component.text("No active map displays found.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("--- Active Map Displays ---", NamedTextColor.GOLD));
        activeDisplays.forEach((id, display) -> {
            Location loc = display.getLocation();
            player.sendMessage(
                    Component.text("#" + id + ": ", NamedTextColor.YELLOW)
                            .append(Component.text(display.getUrl(), NamedTextColor.WHITE))
                            .append(Component.text("\n    at (", NamedTextColor.GRAY))
                            .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.GREEN))
                            .append(Component.text(") Size: ", NamedTextColor.GRAY))
                            .append(Component.text(display.getWidth() + "x" + display.getHeight(), NamedTextColor.AQUA))
                            .append(Component.text(" | Viewers: " + display.getViewerDisplays().size(), NamedTextColor.GRAY))
            );
        });
    }

    private void handleRemoveCommand(Player player, String[] args) {
        if (!player.hasPermission("mapbrowser.command.remove")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /mb remove <id>", NamedTextColor.RED));
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            MapBrowserDisplay displayInfo = plugin.getActiveDisplays().remove(id);

            if (displayInfo == null) {
                player.sendMessage(Component.text("Map display with ID #" + id + " not found.", NamedTextColor.RED));
                return;
            }

            // Clean up resources associated with the display.
            displayInfo.getViewerDisplays().keySet().forEach(uuid -> plugin.getDisplayLookup().remove(uuid));
            displayInfo.cleanup();

            player.sendMessage(Component.text("Successfully removed map display #" + id + ".", NamedTextColor.GREEN));

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid ID format. It must be a number.", NamedTextColor.RED));
        }
    }

    private void handleModifyCommand(Player player, String[] args) {
        if (!player.hasPermission("mapbrowser.command.modify")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sendHelpMessage(player);
            return;
        }

        MapBrowserDisplay display = getDisplayById(args[1], player);
        if (display == null) return;

        final MapBrowserInstance browser = display.getBrowser();
        if (browser == null) {
            player.sendMessage(Component.text("Browser for display #" + display.getId() + " is not available.", NamedTextColor.RED));
            return;
        }

        String property = args[2].toLowerCase();

        switch (property) {
            case "refresh":
                browser.reload();
                player.sendMessage(Component.text("Refreshed display #" + display.getId(), NamedTextColor.GREEN));
                break;
            case "url":
                if (args.length != 4) {
                    player.sendMessage(Component.text("Usage: /mb modify " + display.getId() + " url <new_url>", NamedTextColor.RED));
                    return;
                }
                String newUrl = args[3];
                display.getBrowser().loadURL(newUrl);
                display.setUrl(newUrl);
                player.sendMessage(Component.text("Set URL for display #" + display.getId() + " to " + newUrl, NamedTextColor.GREEN));
                break;

            case "devtools":
                if (args.length != 4) {
                    player.sendMessage(Component.text("Usage: /mb modify " + display.getId() + " devtools <on|off>", NamedTextColor.RED));
                    return;
                }
                boolean open = args[3].equalsIgnoreCase("on");
                display.getBrowser().setDevTools(open);
                player.sendMessage(Component.text("Set DevTools for display #" + display.getId() + " to " + (open ? "ON" : "OFF"), NamedTextColor.GREEN));
                break;

            case "pos":
                if (args.length != 6) {
                    player.sendMessage(Component.text("Usage: /mb modify " + display.getId() + " pos <x> <y> <z>", NamedTextColor.RED));
                    return;
                }
                try {
                    double x = Double.parseDouble(args[3]);
                    double y = Double.parseDouble(args[4]);
                    double z = Double.parseDouble(args[5]);
                    player.sendMessage(Component.text("Moving display #" + display.getId() + "...", NamedTextColor.YELLOW));
                    displayService.moveDisplay(player, display, new Location(player.getWorld(), x, y, z));
                    player.sendMessage(Component.text("Display #" + display.getId() + " moved successfully.", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid coordinates provided.", NamedTextColor.RED));
                }
                break;

            case "size":
                if (args.length != 5) {
                    player.sendMessage(Component.text("Usage: /mb modify " + display.getId() + " size <width> <height>", NamedTextColor.RED));
                    return;
                }
                try {
                    int newWidth = Integer.parseInt(args[3]);
                    int newHeight = Integer.parseInt(args[4]);
                    if (newWidth <= 0 || newHeight <= 0) {
                        player.sendMessage(Component.text("Width and height must be positive.", NamedTextColor.RED));
                        return;
                    }
                    player.sendMessage(Component.text("Resizing display #" + display.getId() + "...", NamedTextColor.YELLOW));
                    displayService.resizeDisplay(player, display, newWidth, newHeight);
                    player.sendMessage(Component.text("Display #" + display.getId() + " resized successfully.", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid width or height.", NamedTextColor.RED));
                }
                break;

            default:
                player.sendMessage(Component.text("Unknown property. Use: url, devtools, pos, size.", NamedTextColor.RED));
        }
    }

    private void handleNearCommand(Player player) {
        if (!player.hasPermission("mapbrowser.command.near")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }
        final Location playerLoc = player.getLocation();

        Optional<MapBrowserDisplay> nearestDisplay = plugin.getActiveDisplays().values().stream()
                .filter(display -> display.getLocation().getWorld().equals(playerLoc.getWorld()))
                .min(Comparator.comparingDouble(display -> display.getLocation().distanceSquared(playerLoc)));

        if (nearestDisplay.isPresent()) {
            MapBrowserDisplay display = nearestDisplay.get();
            player.sendMessage(Component.text("Nearest display is #", NamedTextColor.GREEN)
                    .append(Component.text(display.getId(), NamedTextColor.WHITE))
                    .append(Component.text(String.format(" (%.1f blocks away)", Math.sqrt(display.getLocation().distanceSquared(playerLoc))), NamedTextColor.GRAY))
            );
        } else {
            player.sendMessage(Component.text("No displays found in your world.", NamedTextColor.YELLOW));
        }
    }


    private void handleInputCommand(Player player, String[] args) {
        if (!player.hasPermission("mapbrowser.command.input")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /mb input <id> <text...>", NamedTextColor.RED));
            return;
        }

        MapBrowserDisplay display = getDisplayById(args[1], player);
        if (display == null) return;

        if (display.getBrowser() == null) {
            player.sendMessage(Component.text("The browser for display #" + display.getId() + " is not available.", NamedTextColor.RED));
            return;
        }

        String textToInput = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        display.getBrowser().inputText(textToInput);
        player.sendMessage(Component.text("Sent input to display #" + display.getId(), NamedTextColor.GREEN));
    }

    private void handleKeysCommand(Player player, String[] args) {
        if (!player.hasPermission("mapbrowser.command.keys")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }
        if (args.length != 4) {
            player.sendMessage(Component.text("Usage: /mb keys <id> <keyName> <pressDown|pressUp|click>", NamedTextColor.RED));
            return;
        }

        MapBrowserDisplay display = getDisplayById(args[1], player);
        if (display == null || display.getBrowser() == null) return;

        String keyName = args[2];
        Integer keyCode = KeyMap.getKeyCode(keyName);
        if (keyCode == null) {
            player.sendMessage(Component.text("Unknown key: " + keyName, NamedTextColor.RED));
            player.sendMessage(Component.text("Use Tab-complete to see available keys.", NamedTextColor.YELLOW));
            return;
        }

        String action = args[3].toLowerCase();
        if (!Arrays.asList("pressdown", "pressup", "click").contains(action)) {
            player.sendMessage(Component.text("Invalid action. Use: pressDown, pressUp, or click.", NamedTextColor.RED));
            return;
        }
        display.getBrowser().handleKeyAction(keyCode, action);
        player.sendMessage(Component.text("Sent key '" + keyName.toUpperCase() + "' (" + action + ") to display #" + display.getId(), NamedTextColor.GREEN));
    }

    /**
     * Handles the 'executeJs' subcommand to run JavaScript on a display.
     */
    private void handleExecuteJsCommand(Player player, String[] args) {
        if (!player.hasPermission("mapbrowser.command.executejs")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /mb executeJs <id> <javascript_code>", NamedTextColor.RED));
            return;
        }

        MapBrowserDisplay display = getDisplayById(args[1], player);
        if (display == null) return; // Message is sent by getDisplayById

        MapBrowserInstance browser = display.getBrowser();
        if (browser == null) {
            player.sendMessage(Component.text("The browser for display #" + display.getId() + " is not available.", NamedTextColor.RED));
            return;
        }

        // Combine all arguments after the ID into a single script string
        String scriptToExecute = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        // Execute the JavaScript. The URL and line number can be null/0 for simple execution.
        browser.executeJavaScript(scriptToExecute, null, 0);

        player.sendMessage(Component.text("Executed JavaScript on display #" + display.getId(), NamedTextColor.GREEN));
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("--- MapBrowser Help ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/mb create <x> <y> <z> <url> [w] [h]", NamedTextColor.AQUA).append(Component.text(" - Create a browser display.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb list", NamedTextColor.AQUA).append(Component.text(" - List all active displays.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb remove <id>", NamedTextColor.AQUA).append(Component.text(" - Remove a display by its ID.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb modify <id> <prop> [val]", NamedTextColor.AQUA).append(Component.text(" - Modify a display (url, devtools...).", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb input <id> <text>", NamedTextColor.AQUA).append(Component.text(" - Send text input to a display.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb keys <id> <key> <action>", NamedTextColor.AQUA).append(Component.text(" - Send a key event to a display.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb near", NamedTextColor.AQUA).append(Component.text(" - Find the nearest display.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb executeJs <id> <script>", NamedTextColor.AQUA).append(Component.text(" - Execute JavaScript on a display.", NamedTextColor.WHITE)));
    }

    private MapBrowserDisplay getDisplayById(String idString, Player player) {
        try {
            int id = Integer.parseInt(idString);
            MapBrowserDisplay display = plugin.getActiveDisplays().get(id);
            if (display == null) {
                player.sendMessage(Component.text("Map display with ID #" + id + " not found.", NamedTextColor.RED));
                return null;
            }
            return display;
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid ID format. It must be a number.", NamedTextColor.RED));
            return null;
        }
    }
}