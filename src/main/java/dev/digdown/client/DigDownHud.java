package dev.digdown.client;

import java.util.Locale;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_9779;

public final class DigDownHud {
    // Màu dạng ARGB, phải có 0xFF ở đầu (alpha), nếu không chữ sẽ trong suốt
    private static final int WHITE = 0xFFFFFFFF;
    private static final int ACCENT = 0xFFFF5FB8;

    private DigDownHud() {
    }

    /** Được gọi từ DigDownCore.init(), không cần gọi ở nơi khác. */
    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                class_2960.method_60655("digdown", "hud"),
                DigDownHud::render
        );
    }

    private static void render(class_332 graphics, class_9779 tickCounter) {
        class_310 mc = class_310.method_1551();
        BuiltinModules.Hud hud = BuiltinModules.hud;
        if (hud == null || !hud.isEnabled() || mc.field_1724 == null) {
            return;
        }

        int x = 4;
        int y = 4;
        int lineHeight = mc.field_1772.field_2000 + 2;

        if (hud.showName.get()) {
            graphics.method_27535(mc.field_1772, UiFont.text("DigDown Client"), x, y, ACCENT);
            y += lineHeight;
        }
        if (hud.showFps.get()) {
            graphics.method_27535(mc.field_1772, UiFont.text("FPS: " + mc.method_47599()), x, y, WHITE);
            y += lineHeight;
        }
        if (hud.showCoords.get()) {
            String coords = String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
                    mc.field_1724.method_23317(), mc.field_1724.method_23318(), mc.field_1724.method_23321());
            graphics.method_27535(mc.field_1772, UiFont.text(coords), x, y, WHITE);
        }
    }
}
