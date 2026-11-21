package dev.digdown.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Click GUI của DigDown: tab trên cùng, danh mục bên trái, thanh tìm kiếm, thẻ module có công tắc.
 * Click trái vào thẻ để bật/tắt module. Click phải vào thẻ có tùy chọn con để mở tùy chọn đó.
 * Góc bo tròn, âm thanh tick, hoạt ảnh mở menu. Vẽ bằng GuiGraphics.
 */
public class ClickGuiScreen extends Screen {
    private static final int BG = 0xF010091A;
    private static final int CARD = 0xFF1F1632;
    private static final int CARD_HOVER = 0xFF2B2043;
    private static final int LINE = 0xFF362A50;
    private static final int ACCENT = 0xFFFF5FB8;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFF9A8FB0;
    private static final int SWITCH_OFF = 0xFF4A4060;

    private static final int HEADER = 30;
    private static final int SIDEBAR = 96;
    private static final int CARD_H = 32;
    private static final int GAP = 6;
    private static final int BAR_H = 16;

    // Độ bo góc (pixel)
    private static final int WINDOW_R = 8;
    private static final int CARD_R = 4;

    // Thời gian hoạt ảnh mở menu (mili giây)
    private static final long ANIM_MS = 220L;

    private enum Tab {
        MODULES("MODULES"),
        SETTINGS("SETTINGS");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /** Một vùng bấm được, xây lại mỗi khung hình trong render(). Vùng thêm trước được ưu tiên. */
    private record Zone(int x1, int y1, int x2, int y2, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x1 && x < x2 && y >= y1 && y < y2;
        }
    }

    /** Một thẻ có công tắc. settingsOf != null thì click phải vào thẻ sẽ mở tùy chọn con của module đó. */
    private record Card(String name, String desc, BooleanSupplier state, Runnable toggle, Module settingsOf) {
    }

    // static để lần sau mở lại vẫn nhớ tab/danh mục đang chọn
    private static Tab currentTab = Tab.MODULES;
    private static Module.Category currentCategory = Module.Category.MOVEMENT;

    private final List<Zone> zones = new ArrayList<>();
    private final List<Zone> rightZones = new ArrayList<>(); // vùng click phải (mở tùy chọn con)
    private final long openedAt = System.currentTimeMillis();
    private float fade = 1.0F;
    private Module openedModule; // module đang xem tùy chọn con (null = xem danh sách)
    private EditBox searchBox;

    public ClickGuiScreen() {
        super(Component.literal("DigDown"));
        playClick(1.5F); // tiếng tick khi mở menu
    }

    @Override
    protected void init() {
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 10, Component.literal("Search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(24);
        searchBox.setHint(Component.literal("Search modules..."));
        searchBox.setValue(old);
        addWidget(searchBox);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean animationOn() {
        return ClientSettings.GUI_ANIMATION.get();
    }

    private boolean animating() {
        return animationOn() && System.currentTimeMillis() - openedAt < ANIM_MS;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        zones.clear();
        rightZones.clear();
        Font font = Minecraft.getInstance().font;

        // Tiến độ hoạt ảnh: 0 -> 1
        float t = animationOn() ? Math.min(1.0F, (System.currentTimeMillis() - openedAt) / (float) ANIM_MS) : 1.0F;
        fade = Math.min(1.0F, t * 1.6F);
        float scale = 0.85F + 0.15F * easeOutBack(t);

        int w = Math.min(width - 16, 420);
        int h = Math.min(height - 16, 250);
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

        // ---- Thanh trên: logo + tên + tab + nút đóng
        fillRounded(g, x + 10, y + 6, x + 28, y + 24, 5, mul(ACCENT));
        drawText(g, font, "D", x + 10 + (18 - textWidth(font, "D")) / 2, y + 11, mul(TEXT));
        drawText(g, font, "DIGDOWN", x + 34, y + 11, mul(TEXT));

        int tx = x + 34 + textWidth(font, "DIGDOWN") + 14;
        for (Tab tab : Tab.values()) {
            int tw = textWidth(font, tab.label) + 16;
            boolean active = tab == currentTab;
            boolean hover = inside(mouseX, mouseY, tx, y + 5, tx + tw, y + HEADER - 4);
            if (active || hover) {
                fillRounded(g, tx, y + 5, tx + tw, y + HEADER - 4, CARD_R, mul(active ? CARD_HOVER : CARD));
            }
            if (active) {
                strokeRounded(g, tx, y + 5, tx + tw, y + HEADER - 4, CARD_R, mul(ACCENT));
            }
            drawText(g, font, tab.label, tx + 8, y + 11, mul(active ? TEXT : DIM));
            final Tab target = tab;
            zones.add(new Zone(tx, y + 5, tx + tw, y + HEADER - 4, () -> {
                currentTab = target;
                openedModule = null;
                playClick(1.0F);
            }));
            tx += tw + 4;
        }

        int closeX = x + w - 22;
        fillRounded(g, closeX, y + 8, closeX + 14, y + 22, 4,
                mul(inside(mouseX, mouseY, closeX, y + 8, closeX + 14, y + 22) ? CARD_HOVER : CARD));
        drawText(g, font, "x", closeX + (14 - textWidth(font, "x")) / 2, y + 11, mul(DIM));
        zones.add(new Zone(closeX, y + 8, closeX + 14, y + 22, () -> {
            playClick(0.8F);
            onClose();
        }));

        g.fill(x + 6, y + HEADER, x + w - 6, y + HEADER + 1, mul(LINE));

        // ---- Thân
        int bodyTop = y + HEADER + 1;
        int bottom = y + h - 8;
        boolean modulesTab = currentTab == Tab.MODULES;
        int contentLeft = x + 10;
        int contentRight = x + w - 10;
        int top = bodyTop + 8;
        List<Card> cards = new ArrayList<>();
        boolean searchVisible = false;

        if (modulesTab) {
            // Thanh danh mục bên trái
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
            int rx2 = x + SIDEBAR - 6;
            for (Module.Category c : cats) {
                boolean selected = c == currentCategory && openedModule == null
                        && (searchBox == null || searchBox.getValue().isEmpty());
                boolean hover = inside(mouseX, mouseY, rx1, rowY, rx2, rowY + 22);
                if (selected || hover) {
                    fillRounded(g, rx1, rowY, rx2, rowY + 22, CARD_R, mul(selected ? CARD_HOVER : CARD));
                }
                if (selected) {
                    strokeRounded(g, rx1, rowY, rx2, rowY + 22, CARD_R, mul(ACCENT));
                }
                fillRounded(g, rx1 + 8, rowY + 7, rx1 + 16, rowY + 15, 2, mul(selected ? ACCENT : DIM));
                drawText(g, font, c.label().toUpperCase(Locale.ROOT), rx1 + 22, rowY + 7,
                        mul(selected ? TEXT : DIM));
                final Module.Category target = c;
                zones.add(new Zone(rx1, rowY, rx2, rowY + 22, () -> {
                    currentCategory = target;
                    openedModule = null;
                    if (searchBox != null) {
                        searchBox.setValue("");
                    }
                    playClick(1.0F);
                }));
                rowY += 26;
            }
            drawText(g, font, "v1.0.0", x + 14, y + h - 18, mul(DIM));
            g.fill(x + SIDEBAR, bodyTop + 4, x + SIDEBAR + 1, y + h - 8, mul(LINE));
            contentLeft = x + SIDEBAR + 10;

            // Thanh công cụ phía trên các thẻ: tìm kiếm / nút quay lại + bộ đếm
            String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            String rightLabel;
            if (openedModule != null) {
                for (Module.BoolSetting s : openedModule.getSettings()) {
                    cards.add(new Card(s.getName(), s.getDescription(), s::get, s::toggle, null));
                }
                rightLabel = openedModule.getName().toUpperCase(Locale.ROOT) + " SETTINGS";

                int bw = textWidth(font, "< BACK") + 16;
                boolean hover = inside(mouseX, mouseY, contentLeft, top, contentLeft + bw, top + BAR_H);
                fillRounded(g, contentLeft, top, contentLeft + bw, top + BAR_H, CARD_R,
                        mul(hover ? CARD_HOVER : CARD));
                drawText(g, font, "< BACK", contentLeft + 8, top + 4, mul(TEXT));
                zones.add(new Zone(contentLeft, top, contentLeft + bw, top + BAR_H, () -> {
                    openedModule = null;
                    playClick(0.9F);
                }));
            } else {
                List<Module> source = new ArrayList<>();
                if (query.isEmpty()) {
                    source.addAll(ModuleManager.byCategory(currentCategory));
                } else {
                    for (Module m : ModuleManager.all()) {
                        if (m.getName().toLowerCase(Locale.ROOT).contains(query)) {
                            source.add(m);
                        }
                    }
                }
                for (Module m : source) {
                    cards.add(new Card(m.getName(), m.getDescription(), m::isEnabled, m::toggle,
                            m.getSettings().isEmpty() ? null : m));
                }
                rightLabel = cards.size() + (cards.size() == 1 ? " MODULE" : " MODULES");

                // Thanh tìm kiếm
                int barW = Math.max(60, Math.min(160, contentRight - contentLeft - 90));
                boolean focused = searchBox != null && searchBox.isFocused();
                fillRounded(g, contentLeft, top, contentLeft + barW, top + BAR_H, CARD_R, mul(CARD));
                if (focused) {
                    strokeRounded(g, contentLeft, top, contentLeft + barW, top + BAR_H, CARD_R, mul(ACCENT));
                }
                if (searchBox != null) {
                    searchBox.setX(contentLeft + 8);
                    searchBox.setY(top + 4);
                    searchBox.setWidth(barW - 16);
                }
                searchVisible = true;
            }

            int rw = textWidth(font, rightLabel);
            drawText(g, font, rightLabel, contentRight - rw, top + 4, mul(DIM));
            top += BAR_H + 8;
        } else {
            for (Module.BoolSetting s : ClientSettings.all()) {
                cards.add(new Card(s.getName(), s.getDescription(), s::get, s::toggle, null));
            }
        }

        if (searchBox != null) {
            searchBox.visible = searchVisible;
            if (searchVisible) {
                searchBox.render(g, mouseX, mouseY, partialTick);
            }
        }

        // Logo mờ ở nền vùng nội dung
        Matrix3x2fStack marker = g.pose();
        int markWidth = textWidth(font, "DIGDOWN");
        marker.pushMatrix();
        marker.translate((contentLeft + contentRight) / 2.0F, (top + bottom) / 2.0F + 14);
        marker.scale(3.0F, 3.0F);
        g.drawString(font, UiFont.text("DIGDOWN"), -markWidth / 2, -4, mul(0x14FFFFFF));
        marker.popMatrix();

        // Lưới thẻ 2 cột (1 cột nếu màn hình hẹp)
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

        // Công tắc bo tròn
        int sx = x + w - 30;
        int sy = y + (CARD_H - 10) / 2;
        int textRight = sx - 6;

        drawText(g, font, card.name(), x + 8, y + 7, mul(TEXT));
        drawText(g, font, fit(font, card.desc(), textRight - (x + 8)), x + 8, y + 19, mul(DIM));

        fillRounded(g, sx, sy, sx + 22, sy + 10, 5, mul(on ? ACCENT : SWITCH_OFF));
        int knobX = on ? sx + 13 : sx + 1;
        fillRounded(g, knobX, sy + 1, knobX + 8, sy + 9, 4, mul(TEXT));

        // Bấm vào thẻ: đổi trạng thái rồi phát tiếng tick (cao khi bật, trầm khi tắt)
        zones.add(new Zone(x, y, x + w, y + CARD_H, () -> {
            card.toggle().run();
            playClick(card.state().getAsBoolean() ? 1.3F : 0.8F);
        }));

        // Click phải vào thẻ có tùy chọn con: mở tùy chọn đó
        if (card.settingsOf() != null) {
            final Module target = card.settingsOf();
            rightZones.add(new Zone(x, y, x + w, y + CARD_H, () -> {
                openedModule = target;
                playClick(1.1F);
            }));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Bỏ qua click trong lúc menu đang phóng to (khoảng 0,2 giây)
        if (animating()) {
            return true;
        }
        double mx = event.x();
        double my = event.y();
        if (event.button() == 0) {
            for (Zone zone : zones) {
                if (zone.contains(mx, my)) {
                    if (searchBox != null) {
                        searchBox.setFocused(false);
                    }
                    zone.action().run();
                    return true;
                }
            }
        } else if (event.button() == 1) {
            for (Zone zone : rightZones) {
                if (zone.contains(mx, my)) {
                    if (searchBox != null) {
                        searchBox.setFocused(false);
                    }
                    zone.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

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

    // ---------------------------------------------------------------- tiện ích

    /** Tiếng tick của giao diện. pitch cao = tiếng cao hơn. Tắt được trong tab SETTINGS. */
    private static void playClick(float pitch) {
        if (!ClientSettings.GUI_SOUND.get()) {
            return;
        }
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
