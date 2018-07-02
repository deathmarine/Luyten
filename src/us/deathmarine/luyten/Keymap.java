package us.deathmarine.luyten;

import java.awt.event.InputEvent;

public final class Keymap {
    /**
     * Ctrl+click defaults to "context menu" in macOS, so META+click is used there.
     *
     * @return META_DOWN_MASK for macOS, CTRL_DOWN_MASK otherwise
     */
    public static int ctrlDownModifier() {
        return SystemInfo.IS_MAC ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
    }
}
