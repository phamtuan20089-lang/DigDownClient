package dev.digdown.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Font riêng của DigDown (Baloo + Noto, định nghĩa trong assets/digdown/font/ui.json).
 * Mọi chữ của Click GUI và HUD đều đi qua UiFont.text().
 */
public final class UiFont {
    /** Đặt false để quay về font mặc định của Minecraft (ví dụ nếu font bị lỗi trên máy). */
    public static boolean enabled = true;

    private static final Style STYLE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("digdown", "ui")));

    private UiFont() {
    }

    public static Component text(String s) {
        return enabled ? Component.literal(s).withStyle(STYLE) : Component.literal(s);
    }
}
