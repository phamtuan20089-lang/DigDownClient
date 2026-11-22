package dev.digdown.client;

import net.minecraft.class_11719;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;

/**
 * Font riêng của DigDown (Baloo + Noto, định nghĩa trong assets/digdown/font/ui.json).
 * Mọi chữ của Click GUI và HUD đều đi qua UiFont.text().
 * Có thể tắt trong Click GUI: tab SETTINGS, mục "Custom font".
 */
public final class UiFont {
    private static final class_2583 STYLE = class_2583.field_24360.method_27704(
            new class_11719.class_11721(class_2960.method_60655("digdown", "ui")));

    private UiFont() {
    }

    public static class_2561 text(String s) {
        return ClientSettings.CUSTOM_FONT.get() ? class_2561.method_43470(s).method_27696(STYLE) : class_2561.method_43470(s);
    }
}
