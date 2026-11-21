package dev.digdown.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Click GUI của DigDown. Vẽ hoàn toàn bằng GuiGraphics (không cần thư viện ngoài),
 * nút to và chỉ dùng click trái nên dùng được bằng cảm ứng trên Zalith/Pojav.
 */
public class ClickGuiScreen extends Screen {
    private static final int BG = 0xF0150F22;
    private static final int CARD = 0xFF261B3B;
    private static final int CARD_HOVER = 0xFF33264F;
    private static final int LINE = 0xFF3A2D55;
    private static final int ACCENT = 0xFFFF5FB8;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFF9A8FB0;
    private static final int SWITCH_OFF = 0xFF4A4060;

    private static final int HEADER = 26;
    private static final int SIDEBAR = 84;
    private static final int CARD_H = 30;
    private static final int GAP = 6;

    private enum Tab {
        MODULES("MODULES"),
        HUD("HUD");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /** Một vùng bấm được, xây lại mỗi khung hình trong render(). */
    private record Zone(int x1, int y1, int x2, int y2, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x1 && x < x2 && y >= y1 && y < y2;
        }
    }

    /** Một thẻ có công tắc: dùng chung cho module và tùy chọn HUD. */
    private record Card(String name, String desc, BooleanSupplier state, Runnable toggle) {
    }

    // static để lần sau mở lại vẫn nhớ tab/danh mục đang chọn
    private static Tab currentTab = Tab.MODULES;
    private static Module.Category currentCategory = Module.Category.MOVEMENT;

    private final List<Zone> zones = new ArrayList<>();

    public ClickGuiScreen() {
        super(Component.literal("DigDown"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        zones.clear();
        Font font = Minecraft.getInstance().font;

        int w = Math.min(width - 16, 400);
        int h = Math.min(height - 16, 240);
        int x = (width - w) / 2;
        int y = (height - h) / 2;

        // Khung cửa sổ
        g.fill(x, y, x + w, y + h, BG);
        outline(g, x, y, x + w, y + h, ACCENT);

        // Thanh trên: tên + tab
        g.drawString(font, "DIGDOWN", x + 10, y + 9, ACCENT);
        int tx = x + 12 + font.width("DIGDOWN") + 12;
        for (Tab tab : Tab.values()) {
            int tw = font.width(tab.label) + 14;
            boolean active = tab == currentTab;
            boolean hover = inside(mouseX, mouseY, tx, y + 4, tx + tw, y + HEADER - 3);
            if (active || hover) {
                g.fill(tx, y + 4, tx + tw, y + HEADER - 3, active ? CARD_HOVER : CARD);
            }
            if (active) {
                g.fill(tx, y + HEADER - 4, tx + tw, y + HEADER - 3, ACCENT);
            }
            g.drawString(font, tab.label, tx + 7, y + 9, active ? TEXT : DIM);
            final Tab target = tab;
            zones.add(new Zone(tx, y + 4, tx + tw, y + HEADER - 3, () -> currentTab = target));
            tx += tw + 4;
        }

        // Nút đóng
        int closeX = x + w - 18;
        g.drawString(font, "x", closeX + 4, y + 9, DIM);
        zones.add(new Zone(closeX, y + 4, closeX + 14, y + HEADER - 3, this::onClose));

        g.fill(x + 1, y + HEADER, x + w - 1, y + HEADER + 1, LINE);

        // Thân: thanh danh mục bên trái (tab Modules) + các thẻ
        int bodyTop = y + HEADER + 1;
        int contentLeft;
        List<Card> cards = new ArrayList<>();

        if (currentTab == Tab.MODULES) {
            List<Module.Category> cats = new ArrayList<>();
            for (Module.Category c : Module.Category.values()) {
                if (!ModuleManager.byCategory(c).isEmpty()) {
                    cats.add(c);
                }
            }
            if (!cats.isEmpty() && !cats.contains(currentCategory)) {
                currentCategory = cats.get(0);
            }

            int cy = bodyTop + 8;
            int rx1 = x + 6;
            int rx2 = x + SIDEBAR - 4;
            for (Module.Category c : cats) {
                boolean selected = c == currentCategory;
                boolean hover = inside(mouseX, mouseY, rx1, cy, rx2, cy + 20);
                if (selected || hover) {
                    g.fill(rx1, cy, rx2, cy + 20, selected ? CARD_HOVER : CARD);
                }
                if (selected) {
                    g.fill(rx1, cy, rx1 + 2, cy + 20, ACCENT);
                }
                g.drawString(font, c.label().toUpperCase(Locale.ROOT), rx1 + 9, cy + 6, selected ? TEXT : DIM);
                final Module.Category target = c;
                zones.add(new Zone(rx1, cy, rx2, cy + 20, () -> currentCategory = target));
                cy += 24;
            }
            g.fill(x + SIDEBAR, bodyTop, x + SIDEBAR + 1, y + h - 1, LINE);

            contentLeft = x + SIDEBAR + 8;
            for (Module m : ModuleManager.byCategory(currentCategory)) {
                cards.add(new Card(m.getName(), m.getDescription(), m::isEnabled, m::toggle));
            }
        } else {
            contentLeft = x + 8;
            cards.addAll(hudCards());
        }

        // Lưới thẻ 2 cột (1 cột nếu màn hình hẹp)
        int contentRight = x + w - 8;
        int top = bodyTop + 8;
        int bottom = y + h - 6;
        int cols = (contentRight - contentLeft) >= 240 ? 2 : 1;
        int cardW = (contentRight - contentLeft - GAP * (cols - 1)) / cols;

        for (int i = 0; i < cards.size(); i++) {
            int cardX = contentLeft + (i % cols) * (cardW + GAP);
            int cardY = top + (i / cols) * (CARD_H + GAP);
            if (cardY + CARD_H > bottom) {
                break;
            }
            drawCard(g, font, cards.get(i), cardX, cardY, cardW, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawCard(GuiGraphics g, Font font, Card card, int x, int y, int w, int mouseX, int mouseY) {
        boolean on = card.state().getAsBoolean();
        boolean hover = inside(mouseX, mouseY, x, y, x + w, y + CARD_H);

        g.fill(x, y, x + w, y + CARD_H, hover ? CARD_HOVER : CARD);
        if (on) {
            g.fill(x, y, x + 2, y + CARD_H, ACCENT);
        }

        g.drawString(font, card.name(), x + 8, y + 6, TEXT);
        g.drawString(font, font.plainSubstrByWidth(card.desc(), w - 52), x + 8, y + 18, DIM);

        // Công tắc
        int sx = x + w - 30;
        int sy = y + 10;
        g.fill(sx, sy, sx + 22, sy + 10, on ? ACCENT : SWITCH_OFF);
        int knobX = on ? sx + 13 : sx + 1;
        g.fill(knobX, sy + 1, knobX + 8, sy + 9, TEXT);

        zones.add(new Zone(x, y, x + w, y + CARD_H, card.toggle()));
    }

    private static List<Card> hudCards() {
        List<Card> list = new ArrayList<>();
        list.add(new Card("HUD", "Show or hide the whole HUD",
                () -> DigDownHud.enabled, () -> DigDownHud.enabled = !DigDownHud.enabled));
        list.add(new Card("Client name", "Show the DigDown label",
                () -> DigDownHud.showName, () -> DigDownHud.showName = !DigDownHud.showName));
        list.add(new Card("FPS", "Show frames per second",
                () -> DigDownHud.showFps, () -> DigDownHud.showFps = !DigDownHud.showFps));
        list.add(new Card("Coordinates", "Show your XYZ position",
                () -> DigDownHud.showCoords, () -> DigDownHud.showCoords = !DigDownHud.showCoords));
        return list;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            for (Zone zone : zones) {
                if (zone.contains(mx, my)) {
                    zone.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(int px, int py, int x1, int y1, int x2, int y2) {
        return px >= x1 && px < x2 && py >= y1 && py < y2;
    }

    private static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }
}
