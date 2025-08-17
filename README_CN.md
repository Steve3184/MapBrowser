# MapBrowser

![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20+-green.svg)
![Status](https://img.shields.io/badge/Status-beta-orange.svg)

[English](README.md) | 简体中文

一个强大的 Spigot/Paper 插件，通过使用地图和 Java Chromium 内嵌框架 (JCEF)，将现代网络浏览器的功能带入 Minecraft。

> ![MapBrowser Preview](images/image1.jpg)

---

## 目录

- [功能](#功能)
- [依赖](#依赖)
- [安装](#安装)
- [配置](#配置)
- [命令与权限](#命令与权限)
- [从源码构建](#从源码构建)
- [许可证](#许可证)
- [致谢](#致谢)

## 功能

*   **动态浏览器创建**: 在游戏世界的任何位置创建任意大小的交互式浏览器屏幕。
*   **实时网页渲染**: 基于 Chromium 引擎，能够渲染现代网页，包括复杂的 CSS 和 JavaScript。
*   **完全交互式**:
    *   通过鼠标点击与网页元素交互。
    *   通过命令输入文本到表单和搜索框。
    *   通过命令发送特定按键（如 Enter, Shift, Backspace）以进行高级控制。
*   **开发者工具**: 实时开启/关闭 Chromium 开发者工具，直接在游戏内调试网页布局和性能。
*   **高度可配置**:
    *   通过 `config.yml` 启用远程调试，使用 Chrome DevTools 连接。
    *   支持指定自定义的 JCEF Bundle 路径。
    *   通过 `config.yml`，还可配置 `user-agent`、用户数据路径以及自定义 CEF 参数。

## 依赖

*   **[MapEngine](https://modrinth.com/plugin/mapengine) (必需)**: 本插件依赖于 MapEngine API 来渲染地图。请确保已在服务器上安装并启用 MapEngine。

## 安装

1.  从 [Releases 页面](https://github.com/Steve3184/MapBrowser/releases) 下载最新的 `MapBrowser-*-all.jar` 文件。
2.  确保你已经安装了 [MapEngine](https://modrinth.com/plugin/mapengine) 插件。
3.  将下载的 `MapBrowser-*-all.jar` 文件放入你服务器的 `plugins` 目录。
4.  启动或重启服务器。
5.  插件首次启动时，`jcef-maven` 将自动下载并安装适用于你服务器操作系统的自定义版本的 CEF，目前仅支持 `Windows amd64/i386` 和 `Linux amd64/arm64`。如果我们的自定义版本不可用，则会自动禁用插件。这个过程可能需要几分钟，并需要较好的网络连接。请耐心等待，并观察控制台日志。

## 配置

插件首次加载时，会在 `plugins/MapBrowser/config.yml` 创建一个配置文件。

```yaml
# --------------------------------------------------- #
#             MapBrowser 配置文件                      #
# --------------------------------------------------- #

jcef:
  # 你的自定义 JCEF 二进制文件路径（留空则禁用）
  # 默认: (plugins)/MapBrowser/jcef-bundle
  custom-install-path: ""
  # 自定义 jcef-maven 下载镜像（用于自定义构建）
  mirror: "https://github.com/Steve3184/mb_jcefbuild/releases/download/v1/"
  # jcef-maven 是否应在启动时检查 JCEF 二进制文件？
  skip-download: false

browser:
  # 是否启用声音（默认为 false）
  enable-sound: false
  # CEF 的用户数据目录
  # 默认: (plugins)/MapBrowser/userdata
  user-data-dir: "userdata"
  # CEF 的远程调试
  remote-debugging:
    enabled: false
    # 是否启用？（默认为 false）
    port: 9222
    # 远程调试端口（默认为 9222）
  # CEF 的 User-Agent
  user-agent: ""
  # CEF 日志应输出到何处
  # 'file' -> 写入到 (plugins)/MapBrowser/cef.log
  # 'console' -> 写入到控制台
  chrome-log-output: "file"
  # 额外的 CEF 启动参数
  custom-chrome-args:
    - "--disable-gpu-compositing"
    - "--disable-gpu-vsync"
```

## 命令与权限

| 命令              | 描述                              | 用法                                              | 权限                             |
|-----------------|---------------------------------|-------------------------------------------------|--------------------------------|
| `/mb create`    | 在指定位置创建一个新的浏览器屏幕。               | `/mb create <x> <y> <z> <url> [width] [height]` | `mapbrowser.command.create`    |
| `/mb list`      | 列出所有当前活动的浏览器屏幕。                 | `/mb list`                                      | `mapbrowser.command.list`      |
| `/mb remove`    | 移除一个指定的浏览器屏幕。                   | `/mb remove <id>`                               | `mapbrowser.command.remove`    |
| `/mb modify`    | 修改一个现有屏幕的属性。                    | `/mb modify <id> <prop> [values...]`            | `mapbrowser.command.modify`    |
| `├ url`         | 更改屏幕加载的网址。                      | `/mb modify <id> url <new_url>`                 |                                |
| `├ devtools`    | 开启或关闭开发者工具。                     | `/mb modify <id> devtools <on\|off>`            |                                |
| `├ pos`         | 移动浏览器屏幕。                        | `/mb modify <id> pos <x> <y> <z>`               |                                |
| `├ size`        | 更改浏览器屏幕的大小。                     | `/mb modify <id> size <width> <height>`         |                                |
| `└ refresh`     | 刷新浏览器页面。                        | `/mb modify <id> refresh`                       |                                |
| `└ scale`       | 设置浏览器页面的缩放。                     | `/mb modify <id> scale <newScale>`              |                                |
| `/mb input`     | 向指定的屏幕发送文本输入。                   | `/mb input <id> <text...>`                      | `mapbrowser.command.input`     |
| `/mb keys`      | 向指定的屏幕发送一个按键事件。                 | `/mb keys <id> <key> <action>`                  | `mapbrowser.command.keys`      |
| `/mb executeJs` | 在指定的浏览器屏幕中执行自定义 JS 代码或 JS 片段文件。 | `/mb executeJs <id> <jsCode\|snippet.js>`       | `mapbrowser.command.executejs` |
| `/mb near`      | 列出最近的浏览器屏幕的 ID。                 | `/mb near`                                      | `mapbrowser.command.near`      |

**`/mb keys` 的按键动作:**
*   `pressDown`: 模拟按下按键。
*   `pressUp`: 模拟松开按键。
*   `click`: 模拟一次完整的按下并松开。

## 从源码构建

如果你想自己修改或构建此插件：

1.  **环境准备**:
    *   Java Development Kit (JDK) 21 或更高版本。
    *   Git

2.  **构建步骤**:
    ```bash
    # 克隆仓库
    git clone https://github.com/Steve3184/MapBrowser.git
    cd MapBrowser

    # 使用 Gradle 构建 Jar
    ./gradlew shadowJar
    ```

3.  构建成功后，你可以在 `build/libs/` 目录下找到 `MapBrowser-*-all.jar`。

## 许可证

本项目使用 **GNU AGPL v3** 许可证。

你可以在此查看许可证的完整内容： [LICENSE](LICENSE)

### 依赖项许可证

*   **MapEngine**: `AGPL-3.0-only`
*   **JCEF-maven**: `Apache-2.0`

## 致谢

*   **[pianoman911](https://github.com/Pianoman911)** - 感谢他创造了功能强大的 [MapEngine](https://github.com/FroglightNET/MapEngine) 库。
*   **[CinemaMod](https://github.com/CinemaMod)** - 感谢他们提供了便捷的 [JCEF](https://github.com/CinemaMod/java-cef) 预构建二进制文件，极大地简化了对于支持 h264 等编码的 CEF 的集成。