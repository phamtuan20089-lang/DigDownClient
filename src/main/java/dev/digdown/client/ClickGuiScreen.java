package dev.digdown.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Click GUI của DigDown: góc bo tròn, âm thanh tick khi bấm, hoạt ảnh phóng to khi mở.
 * Vẽ hoàn toàn bằng GuiGraphics, chỉ dùng click trái nên dùng được bằng cảm ứng trên Zalith/Pojav.
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

    // Độ bo góc (pixel). Tăng/giảm để bo nhiều/ít hơn.
    private static final int WINDOW_R = 8;
    private static final int CARD_R = 4;

    // Thời gian hoạt ảnh mở menu (mili giây)
    private static final long ANIM_MS = 220L;

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
    private final long openedAt = System.currentTimeMillis();
    private float fade = 1.0F;

    public ClickGuiScreen() {
        super(Component.literal("DigDown"));
        playClick(1.5F); // tiếng tick khi mở menu
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        zones.clear();
        Font font = Minecraft.getInstance().font;

        // Tiến độ hoạt ảnh: 0 -> 1
        float t = Math.min(1.0F, (System.currentTimeMillis() - openedAt) / (float) ANIM_MS);
        fade = Math.min(1.0F, t * 1.6F);
        float scale = 0.85F + 0.15F * easeOutBack(t);

        int w = Math.min(width - 16, 400);
        int h = Math.min(height - 16, 240);
        int x = (width - w) / 2;
        int y = (height - h) / 2;

        // Phóng to từ giữa cửa sổ
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        float pivotX = x + w / 2.0F;
        float pivotY = y + h / 2.0F;
        pose.translate(pivotX, pivotY);
        pose.scale(scale, scale);
        pose.translate(-pivotX, -pivotY);

        // Khung cửa sổ bo góc
        fillRounded(g, x, y, x + w, y + h, WINDOW_R, mul(BG));
        strokeRounded(g, x, y, x + w, y + h, WINDOW_R, mul(ACCENT));

        // Thanh trên: tên + tab
        drawText(g, font, "DIGDOWN", x + 12, y + 9, mul(ACCENT));
        int tx = x + 14 + textWidth(font, "DIGDOWN") + 12;
        for (Tab tab : Tab.values()) {
            int tw = textWidth(font, tab.label) + 14;
            boolean active = tab == currentTab;
            boolean hover = inside(mouseX, mouseY, tx, y + 4, tx + tw, y + HEADER - 3);
            if (active || hover) {
                fillRounded(g, tx, y + 4, tx + tw, y + HEADER - 3, CARD_R, mul(active ? CARD_HOVER : CARD));
            }
            if (active) {
                strokeRounded(g, tx, y + 4, tx + tw, y + HEADER - 3, CARD_R, mul(ACCENT));
            }
            drawText(g, font, tab.label, tx + 7, y + 9, mul(active ? TEXT : DIM));
            final Tab target = tab;
            zones.add(new Zone(tx, y + 4, tx + tw, y + HEADER - 3, () -> {
                currentTab = target;
                playClick(1.0F);
            }));
            tx += tw + 4;
        }

        // Nút đóng
        int closeX = x + w - 20;
        drawText(g, font, "x", closeX + 4, y + 9, mul(DIM));
        zones.add(new Zone(closeX, y + 4, closeX + 14, y + HEADER - 3, () -> {
            playClick(0.8F);
            onClose();
        }));

        g.fill(x + 6, y + HEADER, x + w - 6, y + HEADER + 1, mul(LINE));

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

            int rowY = bodyTop + 8;
            int rx1 = x + 8;
            int rx2 = x + SIDEBAR - 4;
            for (Module.Category c : cats) {
                boolean selected = c == currentCategory;
                boolean hover = inside(mouseX, mouseY, rx1, rowY, rx2, rowY + 20);
                if (selected || hover) {
                    fillRounded(g, rx1, rowY, rx2, rowY + 20, CARD_R, mul(selected ? CARD_HOVER : CARD));
                }
                if (selected) {
                    strokeRounded(g, rx1, rowY, rx2, rowY + 20, CARD_R, mul(ACCENT));
                }
                drawText(g, font, c.label().toUpperCase(Locale.ROOT), rx1 + 9, rowY + 6,
                        mul(selected ? TEXT : DIM));
                final Module.Category target = c;
                zones.add(new Zone(rx1, rowY, rx2, rowY + 20, () -> {
                    currentCategory = target;
                    playClick(1.0F);
                }));
                rowY += 24;
            }
            g.fill(x + SIDEBAR, bodyTop + 4, x + SIDEBAR + 1, y + h - 8, mul(LINE));

            contentLeft = x + SIDEBAR + 8;
            for (Module m : ModuleManager.byCategory(currentCategory)) {
                cards.add(new Card(m.getName(), m.getDescription(), m::isEnabled, m::toggle));
            }
        } else {
            contentLeft = x + 10;
            cards.addAll(hudCards());
        }

        // Lưới thẻ 2 cột (1 cột nếu màn hình hẹp)
        int contentRight = x + w - 10;
        int top = bodyTop + 8;
        int bottom = y + h - 8;
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

        pose.popMatrix();
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawCard(GuiGraphics g, Font font, Card card, int x, int y, int w, int mouseX, int mouseY) {
        boolean on = card.state().getAsBoolean();
        boolean hover = inside(mouseX, mouseY, x, y, x + w, y + CARD_H);

        fillRounded(g, x, y, x + w, y + CARD_H, CARD_R, mul(hover ? CARD_HOVER : CARD));
        if (on) {
            strokeRounded(g, x, y, x + w, y + CARD_H, CARD_R, mul(ACCENT));
        }

        drawText(g, font, card.name(), x + 8, y + 6, mul(TEXT));
        drawText(g, font, fit(font, card.desc(), w - 52), x + 8, y + 18, mul(DIM));

        // Công tắc bo tròn
        int sx = x + w - 30;
        int sy = y + 10;
        fillRounded(g, sx, sy, sx + 22, sy + 10, 5, mul(on ? ACCENT : SWITCH_OFF));
        int knobX = on ? sx + 13 : sx + 1;
        fillRounded(g, knobX, sy + 1, knobX + 8, sy + 9, 4, mul(TEXT));

        // Bấm vào thẻ: đổi trạng thái rồi phát tiếng tick (cao khi bật, trầm khi tắt)
        zones.add(new Zone(x, y, x + w, y + CARD_H, () -> {
            card.toggle().run();
            playClick(card.state().getAsBoolean() ? 1.3F : 0.8F);
        }));
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
        // Bỏ qua click trong lúc menu đang phóng to (khoảng 0,2 giây)
        if (System.currentTimeMillis() - openedAt < ANIM_MS) {
            return true;
        }
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

    // ---------------------------------------------------------------- tiện ích

    // ---------------------------------------------------------------- chữ (font DigDown)

    private static void drawText(GuiGraphics g, Font font, String s, int x, int y, int color) {
        g.drawString(font, UiFont.text(s), x, y, color);
    }

    private static int textWidth(Font font, String s) {
        return font.width(UiFont.text(s));
    }

    /** Cắt chuỗi và thêm "..." nếu quá rộng. */
    private static String fit(Font font, String s, int maxWidth) {
        if (textWidth(font, s) <= maxWidth) {
            return s;
        }
        String cut = s;
        while (!cut.isEmpty() && textWidth(font, cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    /** Tiếng tick của giao diện. pitch cao = tiếng cao hơn. Muốn đổi âm thanh: thay SoundEvents.UI_BUTTON_CLICK. */
    private static void playClick(float pitch) {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    /** Nhân độ trong suốt của màu với hệ số mờ dần của hoạt ảnh. */
    private int mul(int argb) {
        int a = (int) ((argb >>> 24) * fade);
        if (a < 8) {
            a = 8; // alpha quá thấp có thể bị game vẽ sai
        }
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Chuyển động "bật" nhẹ: vượt qua 1 một chút rồi về 1. */
    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float p = t - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
    }

    private static boolean inside(int px, int py, int x1, int y1, int x2, int y2) {
        return px >= x1 && px < x2 && py >= y1 && py < y2;
    }

    /** Khoảng thụt vào của hàng thứ i (tính từ mép trên) để tạo góc bo bán kính r. */
    private static int inset(int r, int i) {
        double dy = r - i - 0.5;
        return r - (int) Math.round(Math.sqrt(r * r - dy * dy));
    }

    /** Hình chữ nhật đặc bo góc. */
    private static void fillRounded(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color) {
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            g.fill(x1, y1, x2, y2, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            g.fill(x1 + in, y1 + i, x2 - in, y1 + i + 1, color);
            g.fill(x1 + in, y2 - i - 1, x2 - in, y2 - i, color);
        }
        g.fill(x1, y1 + r, x2, y2 - r, color);
    }

    /** Viền 1 pixel bo góc (không tô phần bên trong). */
    private static void strokeRounded(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color) {
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            g.fill(x1, y1, x2, y1 + 1, color);
            g.fill(x1, y2 - 1, x2, y2, color);
            g.fill(x1, y1, x1 + 1, y2, color);
            g.fill(x2 - 1, y1, x2, y2, color);
            return;
        }
        int edge = inset(r, 0);
        g.fill(x1 + edge, y1, x2 - edge, y1 + 1, color);
        g.fill(x1 + edge, y2 - 1, x2 - edge, y2, color);
        for (int i = 1; i < r; i++) {
            int cur = inset(r, i);
            int wid = Math.max(1, inset(r, i - 1) - cur);
            // góc trên
            g.fill(x1 + cur, y1 + i, x1 + cur + wid, y1 + i + 1, color);
            g.fill(x2 - cur - wid, y1 + i, x2 - cur, y1 + i + 1, color);
            // góc dưới
            g.fill(x1 + cur, y2 - i - 1, x1 + cur + wid, y2 - i, color);
            g.fill(x2 - cur - wid, y2 - i - 1, x2 - cur, y2 - i, color);
        }
        g.fill(x1, y1 + r, x1 + 1, y2 - r, color);
        g.fill(x2 - 1, y1 + r, x2, y2 - r, color);
    }
}
