package dev.digdown.client;

import java.util.Locale;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public final class DigDownHud {
    // Màu dạng ARGB, phải có 0xFF ở đầu (alpha), nếu không chữ sẽ trong suốt
    private static final int WHITE = 0xFFFFFFFF;
    private static final int ACCENT = 0xFFFF5FB8;

    // Các công tắc này được Click GUI (tab HUD) bật/tắt
    public static boolean enabled = true;
    public static boolean showName = true;
    public static boolean showFps = true;
    public static boolean showCoords = true;

    private DigDownHud() {
    }

    /** Được gọi từ DigDownCore.init(), không cần gọi ở nơi khác. */
    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("digdown", "hud"),
                DigDownHud::render
        );
    }

    private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null) {
            return;
        }

        int x = 4;
        int y = 4;
        int lineHeight = mc.font.lineHeight + 2;

        if (showName) {
            graphics.drawString(mc.font, UiFont.text("DigDown Client"), x, y, ACCENT);
            y += lineHeight;
        }
        if (showFps) {
            graphics.drawString(mc.font, UiFont.text("FPS: " + mc.getFps()), x, y, WHITE);
            y += lineHeight;
        }
        if (showCoords) {
            String coords = String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            graphics.drawString(mc.font, UiFont.text(coords), x, y, WHITE);
        }
    }
}
