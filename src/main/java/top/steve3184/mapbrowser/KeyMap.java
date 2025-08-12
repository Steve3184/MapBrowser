package top.steve3184.mapbrowser;

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class KeyMap {

    private static final Map<String, Integer> keyMap = new HashMap<>();

    static {
        for (char c = 'A'; c <= 'Z'; c++) {
            keyMap.put(String.valueOf(c), KeyEvent.getExtendedKeyCodeForChar(c));
        }
        for (char c = '0'; c <= '9'; c++) {
            keyMap.put(String.valueOf(c), KeyEvent.getExtendedKeyCodeForChar(c));
        }

        keyMap.put("F1", KeyEvent.VK_F1);
        keyMap.put("F2", KeyEvent.VK_F2);
        keyMap.put("F3", KeyEvent.VK_F3);
        keyMap.put("F4", KeyEvent.VK_F4);
        keyMap.put("F5", KeyEvent.VK_F5);
        keyMap.put("F6", KeyEvent.VK_F6);
        keyMap.put("F7", KeyEvent.VK_F7);
        keyMap.put("F8", KeyEvent.VK_F8);
        keyMap.put("F9", KeyEvent.VK_F9);
        keyMap.put("F10", KeyEvent.VK_F10);
        keyMap.put("F11", KeyEvent.VK_F11);
        keyMap.put("F12", KeyEvent.VK_F12);

        keyMap.put("ENTER", KeyEvent.VK_ENTER);
        keyMap.put("BACKSPACE", KeyEvent.VK_BACK_SPACE);
        keyMap.put("TAB", KeyEvent.VK_TAB);
        keyMap.put("SHIFT", KeyEvent.VK_SHIFT);
        keyMap.put("CONTROL", KeyEvent.VK_CONTROL);
        keyMap.put("ALT", KeyEvent.VK_ALT);
        keyMap.put("ESCAPE", KeyEvent.VK_ESCAPE);
        keyMap.put("DELETE", KeyEvent.VK_DELETE);
        keyMap.put("SPACE", KeyEvent.VK_SPACE);

        keyMap.put("UP", KeyEvent.VK_UP);
        keyMap.put("DOWN", KeyEvent.VK_DOWN);
        keyMap.put("LEFT", KeyEvent.VK_LEFT);
        keyMap.put("RIGHT", KeyEvent.VK_RIGHT);

        keyMap.put("HOME", KeyEvent.VK_HOME);
        keyMap.put("END", KeyEvent.VK_END);
        keyMap.put("PAGE_UP", KeyEvent.VK_PAGE_UP);
        keyMap.put("PAGE_DOWN", KeyEvent.VK_PAGE_DOWN);
        keyMap.put("INSERT", KeyEvent.VK_INSERT);
    }
    public static Integer getKeyCode(String keyName) {
        return keyMap.get(keyName.toUpperCase());
    }
    public static Set<String> getKeyNames() {
        return Collections.unmodifiableSet(keyMap.keySet());
    }
}