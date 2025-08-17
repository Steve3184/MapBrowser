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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        // Player check is now handled within each specific command that requires it.

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        // A check to ensure only players can use commands that require a player instance
        if (!(sender instanceof Player) && Arrays.asList("create", "list", "remove", "modify", "near", "executeJs").contains(subCommand)) {
            sender.sendMessage(Component.text("This command can only be used by a player.", NamedTextColor.RED));
            return true;
        }

        switch (subCommand) {
            // Commands that require a player
            case "create" -> {
                assert sender instanceof Player;
                handleCreateCommand((Player) sender, args);
            }
            case "list" -> {
                assert sender instanceof Player;
                handleListCommand((Player) sender);
            }
            case "remove" -> {
                assert sender instanceof Player;
                handleRemoveCommand((Player) sender, args);
            }
            case "modify" -> {
                assert sender instanceof Player;
                handleModifyCommand((Player) sender, args);
            }
            case "near" -> {
                assert sender instanceof Player;
                handleNearCommand((Player) sender);
            }
            // Commands that can be run by console
            case "input" -> handleInputCommand(sender, args);
            case "keys" -> handleKeysCommand(sender, args);
            case "executejs" -> handleExecuteJsCommand(sender, args);
            default -> sendHelpMessage(sender);
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
                return StringUtil.copyPartialMatches(args[2], Arrays.asList("url", "devtools", "pos", "size", "refresh", "scale"), completions);
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
            if (args.length == 4 && args[2].equalsIgnoreCase("scale")) {
                return StringUtil.copyPartialMatches(args[3], List.of("newScale"), completions);
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
            case "refresh" -> {
                browser.reload();
                player.sendMessage(Component.text("Refreshed display #" + display.getId(), NamedTextColor.GREEN));
            }
            case "url" -> {
                if (args.length != 4) {
                    player.sendMessage(Component.text("Usage: /mb modify " + display.getId() + " url <new_url>", NamedTextColor.RED));
                    return;
                }
                String newUrl = args[3];
                display.getBrowser().loadURL(newUrl);
                display.setUrl(newUrl);
                player.sendMessage(Component.text("Set URL for display #" + display.getId() + " to " + newUrl, NamedTextColor.GREEN));
            }
            case "devtools" -> {
                if (args.length != 4) {
                    player.sendMessage(Component.text("Usage: /mb modify " + display.getId() + " devtools <on|off>", NamedTextColor.RED));
                    return;
                }
                boolean open = args[3].equalsIgnoreCase("on");
//                display.getBrowser().setDevTools(open);
//                player.sendMessage(Component.text("Set DevTools for display #" + display.getId() + " to " + (open ? "ON" : "OFF"), NamedTextColor.GREEN));
                player.sendMessage(Component.text("Devtools is not usable now, you can try to using remote debugging.", NamedTextColor.RED));
            }
            case "pos" -> {
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
            }
            case "size" -> {
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
            }
            case "scale" -> {
                if (args.length != 4) {
                    player.sendMessage(Component.text("Usage: /mb modify " + display.getId() + " scale <newScale>", NamedTextColor.RED));
                    return;
                }
                try {
                    int newScale = Integer.parseInt(args[3]);
                    if (newScale < 1) {
                        player.sendMessage(Component.text("Scale must >=1.", NamedTextColor.RED));
                        return;
                    }
                    player.sendMessage(Component.text("Setting scale of display #" + display.getId() + "...", NamedTextColor.YELLOW));
                    displayService.scaleDisplay(player, display, newScale);
                    player.sendMessage(Component.text("Display #" + display.getId() + " scaled successfully.", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid scale.", NamedTextColor.RED));
                }
            }
            default -> player.sendMessage(Component.text("Unknown property. Use: url, devtools, pos, size.", NamedTextColor.RED));
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


    private void handleInputCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mapbrowser.command.input")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /mb input <id> <text...>", NamedTextColor.RED));
            return;
        }

        MapBrowserDisplay display = getDisplayById(args[1], sender);
        if (display == null) return;

        if (display.getBrowser() == null) {
            sender.sendMessage(Component.text("The browser for display #" + display.getId() + " is not available.", NamedTextColor.RED));
            return;
        }

        String textToInput = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        display.getBrowser().inputText(textToInput);
        sender.sendMessage(Component.text("Sent input to display #" + display.getId(), NamedTextColor.GREEN));
    }

    private void handleKeysCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mapbrowser.command.keys")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }
        if (args.length != 4) {
            sender.sendMessage(Component.text("Usage: /mb keys <id> <keyName> <pressDown|pressUp|click>", NamedTextColor.RED));
            return;
        }

        MapBrowserDisplay display = getDisplayById(args[1], sender);
        if (display == null || display.getBrowser() == null) return;

        String keyName = args[2];
        Integer keyCode = KeyMap.getKeyCode(keyName);
        if (keyCode == null) {
            sender.sendMessage(Component.text("Unknown key: " + keyName, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use Tab-complete to see available keys.", NamedTextColor.YELLOW));
            return;
        }

        String action = args[3].toLowerCase();
        if (!Arrays.asList("pressdown", "pressup", "click").contains(action)) {
            sender.sendMessage(Component.text("Invalid action. Use: pressDown, pressUp, or click.", NamedTextColor.RED));
            return;
        }
        display.getBrowser().handleKeyAction(keyCode, action);
        sender.sendMessage(Component.text("Sent key '" + keyName.toUpperCase() + "' (" + action + ") to display #" + display.getId(), NamedTextColor.GREEN));
    }

    /**
     * Handles the 'executeJs' subcommand to run JavaScript on a display.
     */
    private void handleExecuteJsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mapbrowser.command.executejs")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /mb executeJs <id> <code|path.js>", NamedTextColor.RED));
            return;
        }

        MapBrowserDisplay display = getDisplayById(args[1], sender);
        if (display == null) return;

        MapBrowserInstance browser = display.getBrowser();
        if (browser == null) {
            sender.sendMessage(Component.text("The browser for display #" + display.getId() + " is not available.", NamedTextColor.RED));
            return;
        }

        String potentialPath = args[2];
        String scriptToExecute;

        // Check if the input is a script path
        if (potentialPath.endsWith(".js") && !potentialPath.startsWith(".")) {
            // Rejoin arguments in case the file path has spaces (though it's bad practice, this handles it)
            String fullPath = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            File scriptFile = new File(new File(plugin.getDataFolder(), "snippets"), fullPath);

            if (!scriptFile.exists()) {
                sender.sendMessage(Component.text("Error: Script file not found: " + fullPath, NamedTextColor.RED));
                return;
            }

            try {
                scriptToExecute = Files.readString(scriptFile.toPath());
                sender.sendMessage(Component.text("Executing script from file: " + fullPath, NamedTextColor.YELLOW));
            } catch (IOException e) {
                sender.sendMessage(Component.text("Error reading script file: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().severe("Could not read JS snippet: " + fullPath);
                e.printStackTrace();
                return;
            }
        } else {
            // Treat as raw JavaScript code
            scriptToExecute = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        }

        browser.executeJavaScript(scriptToExecute, null, 0);
        sender.sendMessage(Component.text("Executed JavaScript on display #" + display.getId(), NamedTextColor.GREEN));
    }

    private void sendHelpMessage(CommandSender player) {
        player.sendMessage(Component.text("--- MapBrowser Help ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/mb create <x> <y> <z> <url> [w] [h]", NamedTextColor.AQUA).append(Component.text(" - Create a browser display.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb list", NamedTextColor.AQUA).append(Component.text(" - List all active displays.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb remove <id>", NamedTextColor.AQUA).append(Component.text(" - Remove a display by its ID.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb modify <id> <prop> [val]", NamedTextColor.AQUA).append(Component.text(" - Modify a display (url, devtools...).", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb input <id> <text>", NamedTextColor.AQUA).append(Component.text(" - Send text input to a display.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb keys <id> <key> <action>", NamedTextColor.AQUA).append(Component.text(" - Send a key event to a display.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb near", NamedTextColor.AQUA).append(Component.text(" - Find the nearest display.", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/mb executeJs <id> <code|path.js>", NamedTextColor.AQUA).append(Component.text(" - Execute JavaScript on a display.", NamedTextColor.WHITE)));
    }

    private MapBrowserDisplay getDisplayById(String idString, CommandSender player) {
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