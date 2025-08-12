package org.cef.browser;

import org.cef.CefClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A specialized off-screen CEF browser instance tailored for rendering onto Minecraft maps.
 * It captures browser frames as pixel data and forwards input events from the game.
 * It also manages a nested browser instance for showing developer tools.
 */
public class MapBrowserInstance extends CefBrowserOsr {
    // Lock to ensure thread-safe access to the pixel buffer.
    private final ReentrantLock bufferLock = new ReentrantLock();
    // Raw pixel data from CEF in BGRA format.
    private byte[] rawPixelData;
    private int pixelWidth;
    private int pixelHeight;
    // Flag to indicate if a new frame has been painted since the last update.
    private boolean hasNewFrame = false;

    // --- DevTools Fields ---
    // Lock for thread-safe operations on the DevTools instance.
    private final Object devToolsLock = new Object();
    // The nested browser instance for the developer tools.
    private MapBrowserInstance devToolsBrowser = null;
    // Flag indicating if the developer tools are currently open and should be rendered.
    private boolean isDevToolsOpen = false;

    /**
     * Public constructor for a main browser instance.
     * @param client The CefClient instance.
     * @param url The initial URL to load.
     */
    public MapBrowserInstance(CefClient client, String url) {
        super(client, url, false, null); // `isTransparent` is false.
    }

    /**
     * Private constructor used exclusively for creating a DevTools browser instance.
     * It uses reflection to set internal fields required for linking to a parent browser.
     */
    private MapBrowserInstance(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        super(client, url, transparent, context);
        // Use reflection to set private fields in the parent CefBrowser_N class.
        try {
            Field parentField = CefBrowser_N.class.getDeclaredField("parent_");
            parentField.setAccessible(true);
            parentField.set(this, parent);

            Field inspectAtField = CefBrowser_N.class.getDeclaredField("inspectAt_");
            inspectAtField.setAccessible(true);
            inspectAtField.set(this, inspectAt);
        } catch (Exception e) {
            // This should not happen in a normal environment.
            throw new RuntimeException("Failed to initialize DevTools via reflection", e);
        }
    }

    /**
     * Overridden to return our custom browser type for DevTools.
     */
    @Override
    protected MapBrowserInstance createDevToolsBrowser(CefClient client, String url, CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        return new MapBrowserInstance(client, url, false, context, parent, inspectAt);
    }

    /**
     * Called by JCEF when a new frame is ready to be painted.
     * This method copies the raw pixel data from the CEF buffer into a local byte array.
     */
    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        bufferLock.lock();
        try {
            // Re-initialize buffer if the browser size has changed.
            if (width != this.pixelWidth || height != this.pixelHeight) {
                this.pixelWidth = width;
                this.pixelHeight = height;
                this.rawPixelData = new byte[width * height * 4];
            }
            // Copy pixel data from the direct ByteBuffer to our heap byte array.
            buffer.get(this.rawPixelData);
            this.hasNewFrame = true;
        } finally {
            bufferLock.unlock();
        }
    }
    /**
     * Retrieves the latest frame as an ARGB pixel array, if available.
     * This method converts the raw BGRA data to the ARGB format used by Minecraft maps.
     * If DevTools is open, it returns the pixel data from the DevTools browser instead.
     * @return An integer array of ARGB pixels, or null if there is no new frame.
     */
    public int[] getAndUpdatePixelData() {
        // If DevTools is active, delegate the call to it.
        synchronized (devToolsLock) {
            if (isDevToolsOpen && devToolsBrowser != null) {
                return devToolsBrowser.getAndUpdatePixelData();
            }
        }

        // Return null if no new frame has been painted.
        if (!hasNewFrame) {
            return null;
        }

        int[] argbPixels;
        bufferLock.lock();
        try {
            // Double-check the flag after acquiring the lock.
            if (!hasNewFrame) return null;

            // Convert raw BGRA data to ARGB integers.
            argbPixels = new int[pixelWidth * pixelHeight];
            for (int i = 0; i < argbPixels.length; i++) {
                int bufferIndex = i * 4;
                int b = rawPixelData[bufferIndex] & 0xFF;
                int g = rawPixelData[bufferIndex + 1] & 0xFF;
                int r = rawPixelData[bufferIndex + 2] & 0xFF;
                int a = rawPixelData[bufferIndex + 3] & 0xFF;
                argbPixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            this.hasNewFrame = false; // Mark the frame as consumed.
        } finally {
            bufferLock.unlock();
        }
        return argbPixels;
    }

    /**
     * Resizes the off-screen browser.
     * Uses reflection to access and modify the private size field in the superclass.
     * @param width The new width in pixels.
     * @param height The new height in pixels.
     */
    public void resize(int width, int height) {
        try {
            // Reflection is needed as the size field is private in CefBrowserOsr.
            Field field = this.getClass().getSuperclass().getDeclaredField("browser_rect_");
            field.setAccessible(true);
            Rectangle rect = (Rectangle) field.get(this);
            rect.setBounds(0, 0, width, height);
            // Notify the native browser that it was resized.
            wasResized(width, height);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resize browser via reflection", e);
        }
    }

    public int getPixelWidth() {
        return pixelWidth;
    }

    public int getPixelHeight() {
        return pixelHeight;
    }

    /**
     * Opens or closes the developer tools for this browser instance.
     * @param open True to open DevTools, false to close them.
     */
    public void setDevTools(boolean open) {
        synchronized (devToolsLock) {
            if (open == isDevToolsOpen) return; // No change needed.

            this.isDevToolsOpen = open;
            if (open) {
                // Create and initialize the DevTools browser if it doesn't exist.
                if (devToolsBrowser == null) {
                    devToolsBrowser = (MapBrowserInstance) this.getDevTools(null);

                    // Initialization must happen on the AWT event dispatch thread.
                    SwingUtilities.invokeLater(() -> {
                        devToolsBrowser.createImmediately();
                        devToolsBrowser.resize(this.pixelWidth, this.pixelHeight);
                    });
                }
            } else {
                // Close and dispose of the DevTools browser.
                if (devToolsBrowser != null) {
                    devToolsBrowser.close(true);
                    devToolsBrowser = null;
                }
            }
        }
    }

    public boolean isDevToolsOpen() {
        return isDevToolsOpen;
    }

    /**
     * Sends a complete string of text to the browser by simulating key type events.
     * Delegates to DevTools if open.
     * @param text The text to input.
     */
    public void inputText(String text) {
        synchronized (devToolsLock) {
            if (isDevToolsOpen && devToolsBrowser != null) {
                devToolsBrowser.inputText(text);
                return;
            }
        }

        // Simulate a KEY_TYPED event for each character.
        for (char c : text.toCharArray()) {
            KeyEvent keyEvent = new KeyEvent(getUIComponent(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, c);
            sendKeyEvent(keyEvent);
        }
    }

    /**
     * Sends a specific key action (press, release, or click) to the browser.
     * Delegates to DevTools if open.
     * @param keyCode The java.awt.event.KeyEvent constant for the key.
     * @param action The action to perform: "pressDown", "pressUp", or "click".
     */
    public void handleKeyAction(int keyCode, String action) {
        synchronized (devToolsLock) {
            if (isDevToolsOpen && devToolsBrowser != null) {
                devToolsBrowser.handleKeyAction(keyCode, action);
                return;
            }
        }

        switch (action.toLowerCase()) {
            case "pressdown" -> {
                KeyEvent pressEvent = new KeyEvent(getUIComponent(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
                sendKeyEvent(pressEvent);
            }
            case "pressup" -> {
                KeyEvent releaseEvent = new KeyEvent(getUIComponent(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
                sendKeyEvent(releaseEvent);
            }
            case "click" -> {
                // Simulate a quick press and release.
                KeyEvent pressEvent = new KeyEvent(getUIComponent(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
                sendKeyEvent(pressEvent);
                KeyEvent releaseEvent = new KeyEvent(getUIComponent(), KeyEvent.KEY_RELEASED, System.currentTimeMillis() + 20, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
                sendKeyEvent(releaseEvent);
            }
        }
    }

    /**
     * Sends a mouse click (press and release) event to the browser at specific coordinates.
     * Delegates to DevTools if open.
     * @param x The x-coordinate of the click.
     * @param y The y-coordinate of the click.
     * @param awtButtonType The MouseEvent button type (e.g., MouseEvent.BUTTON1).
     */
    public void sendMouseClick(int x, int y, int awtButtonType) {
        synchronized (devToolsLock) {
            if (isDevToolsOpen && devToolsBrowser != null) {
                devToolsBrowser.sendMouseClick(x, y, awtButtonType);
                return;
            }
        }
        // Simulate a press followed by a release to form a click.
        MouseEvent move = new MouseEvent(getUIComponent(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false);
        sendMouseEvent(move);
        MouseEvent press = new MouseEvent(getUIComponent(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, 1, false, awtButtonType);
        sendMouseEvent(press);

        MouseEvent release = new MouseEvent(getUIComponent(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis() + 20, 0, x, y, 1, false, awtButtonType);
        sendMouseEvent(release);
    }

    @Override
    public void close(boolean force) {
        synchronized (devToolsLock) {
            if(isDevToolsOpen && devToolsBrowser != null) {
                devToolsBrowser.close(force);
            }
        }
        super.close(force);
    }
}