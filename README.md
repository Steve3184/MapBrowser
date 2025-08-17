# MapBrowser

![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20+-green.svg)
![Status](https://img.shields.io/badge/Status-beta-orange.svg)

English | [简体中文](README_CN.md)

A powerful Spigot/Paper plugin that brings the power of a modern web browser into Minecraft using maps and the Java Chromium Embedded Framework (JCEF).

> ![MapBrowser Preview](images/image1.jpg)

---

## Table of Contents

- [Features](#features)
- [Dependencies](#dependencies)
- [Installation](#installation)
- [Configuration](#configuration)
- [Commands and Permissions](#commands-and-permissions)
- [Building from Source](#building-from-source)
- [License](#license)
- [Acknowledgements](#acknowledgements)

## Features

*   **Dynamic Browser Creation**: Create interactive browser screens of any size, anywhere in the game world.
*   **Real-time Web Rendering**: Based on the Chromium engine, it can render modern web pages, including complex CSS and JavaScript.
*   **Fully Interactive**:
    *   Interact with web elements via mouse clicks.
    *   Input text into forms and search boxes using commands.
    *   Send specific key presses (like Enter, Shift, Backspace) for advanced control using commands.
*   **Developer Tools**: Toggle Chromium Developer Tools in real-time to debug web layouts and performance directly in-game.
*   **Highly Configurable**:
    *   Enable remote debugging via `config.yml` to connect with Chrome DevTools.
    *   Support for specifying a custom JCEF Bundle path.
    *   Configure `user-agent`, user data path, and custom CEF arguments through `config.yml`.

## Dependencies

*   **[MapEngine](https://modrinth.com/plugin/mapengine) (Required)**: This plugin depends on the MapEngine API for map rendering. Please ensure you have installed and enabled MapEngine on your server.

## Installation

1.  Download the latest `MapBrowser-*-all.jar` file from the [Releases Page](https://github.com/Steve3184/MapBrowser/releases).
2.  Ensure you have already installed the [MapEngine](https://modrinth.com/plugin/mapengine) plugin.
3.  Place the downloaded `MapBrowser-*-all.jar` file into your server's `plugins` directory.
4.  Start or restart your server.
5.  On the first launch, `jcef-maven` will automatically download and install a custom version of CEF suitable for your server's operating system. Currently, only `Windows amd64/i386` and `Linux amd64/arm64` are supported. If our custom version is not available, MapBrowser will be disabled automatically. This process may take a few minutes and requires a good network connection. Please be patient and monitor the console logs.

## Configuration

When the plugin loads for the first time, it will create a configuration file at `plugins/MapBrowser/config.yml`.

```yaml
# --------------------------------------------------- #
#             MapBrowser Configuration                #
# --------------------------------------------------- #

jcef:
  # Your custom JCEF binaries path (leave blank to disable)
  # Default: (plugins)/MapBrowser/jcef-bundle
  custom-install-path: ""
  # Custom jcef-maven download mirror (for custom-builds)
  mirror: "https://github.com/Steve3184/mb_jcefbuild/releases/download/v1/"
  # Should jcef-maven check the JCEF binaries at startup?
  skip-download: false

browser:
  # Should we enable the sound (default is false)
  enable-sound: false
  # CEF's user data directory
  # Default: (plugins)/MapBrowser/userdata
  user-data-dir: "userdata"
  # CEF's remote debugging
  remote-debugging:
    enabled: false
    # Should we enable it? (default is false)
    port: 9222
    # Remote Debugging Port (default is 9222)
  # CEF's User-Agent
  user-agent: ""
  # Where should CEF output the log
  # 'file' -> write to (plugins)/MapBrowser/cef.log
  # 'console' -> write to console
  chrome-log-output: "file"
  # Extra CEF startup args
  custom-chrome-args:
    - "--disable-gpu-compositing"
    - "--disable-gpu-vsync"
```

## Commands and Permissions

| Command         | Description                                                              | Usage                                           | Permission                     |
|-----------------|--------------------------------------------------------------------------|-------------------------------------------------|--------------------------------|
| `/mb create`    | Creates a new browser screen at the specified location.                  | `/mb create <x> <y> <z> <url> [width] [height]` | `mapbrowser.command.create`    |
| `/mb list`      | Lists all currently active browser screens.                              | `/mb list`                                      | `mapbrowser.command.list`      |
| `/mb remove`    | Removes a specified browser screen.                                      | `/mb remove <id>`                               | `mapbrowser.command.remove`    |
| `/mb modify`    | Modifies the properties of an existing screen.                           | `/mb modify <id> <prop> [values...]`            | `mapbrowser.command.modify`    |
| `├ url`         | Changes the URL loaded by the screen.                                    | `/mb modify <id> url <new_url>`                 |                                |
| `├ devtools`    | Toggles the developer tools on or off.                                   | `/mb modify <id> devtools <on\|off>`            |                                |
| `├ pos`         | Moves the browser screen.                                                | `/mb modify <id> pos <x> <y> <z>`               |                                |
| `├ size`        | Changes the size of the browser screen.                                  | `/mb modify <id> size <width> <height>`         |                                |
| `└ refresh`     | Refreshes the browser page.                                              | `/mb modify <id> refresh`                       |                                |
| `└ scale`       | Setting the scale of browser page.                                       | `/mb modify <id> scale <newScale>`              |                                |
| `/mb input`     | Sends text input to the specified screen.                                | `/mb input <id> <text...>`                      | `mapbrowser.command.input`     |
| `/mb keys`      | Sends a key event to the specified screen.                               | `/mb keys <id> <key> <action>`                  | `mapbrowser.command.keys`      |
| `/mb executeJs` | Executes custom JS code or a JS snippet in the specified browser screen. | `/mb executeJs <id> <jsCode\|snippet.js>`       | `mapbrowser.command.executejs` |
| `/mb near`      | Lists the ID of the nearest browser screen.                              | `/mb near`                                      | `mapbrowser.command.near`      |

**Key Actions for `/mb keys`:**
*   `pressDown`: Simulates pressing a key down.
*   `pressUp`: Simulates releasing a key.
*   `click`: Simulates a full press and release.

## Building from Source

If you want to modify or build this plugin yourself:

1.  **Prerequisites**:
    *   Java Development Kit (JDK) 21 or higher.
    *   Git

2.  **Build Steps**:
    ```bash
    # Clone the repository
    git clone https://github.com/Steve3184/MapBrowser.git
    cd MapBrowser

    # Build the Jar using Gradle
    ./gradlew shadowJar
    ```

3.  After a successful build, you can find `MapBrowser-*-all.jar` in the `build/libs/` directory.

## License

This project is licensed under the **GNU AGPL v3**.

You can view the full license here: [LICENSE](LICENSE)

### Dependency Licenses

*   **MapEngine**: `AGPL-3.0-only`
*   **JCEF-maven**: `Apache-2.0`

## Acknowledgements

*   **[pianoman911](https://github.com/Pianoman911)** - For creating the powerful [MapEngine](https://github.com/FroglightNET/MapEngine) library.
*   **[CinemaMod](https://github.com/CinemaMod)** - For providing convenient [JCEF](https://github.com/CinemaMod/java-cef) pre-built binaries, which greatly simplifies the integration of CEF with support for codecs like h264.
