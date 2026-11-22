package dev.digdown.client;

import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_310;

/** Nơi khai báo các chức năng có sẵn. Thêm chức năng mới: viết class mới rồi thêm vào registerAll(). */
public final class BuiltinModules {

    private BuiltinModules() {
    }

    /** Module HUD duy nhất: DigDownHud đọc trạng thái và các tùy chọn từ đây. */
    public static Hud hud;

    public static void registerAll() {
        ModuleManager.register(new Sprint());
        ModuleManager.register(new AutoWalk());
        ModuleManager.register(new Fullbright());

        hud = new Hud();
        hud.setEnabled(true); // HUD bật sẵn
        ModuleManager.register(hud);
    }

    /** Gom toàn bộ HUD thành một chức năng, các dòng hiển thị là tùy chọn con (click phải vào HUD để chỉnh). */
    public static final class Hud extends Module {
        public final BoolSetting showName = addSetting("Client name", "Show the DigDown label", true);
        public final BoolSetting showFps = addSetting("FPS", "Show frames per second", true);
        public final BoolSetting showCoords = addSetting("Coordinates", "Show your XYZ position", true);

        public Hud() {
            super("HUD", "Name, FPS and coordinates overlay", Category.CLIENT);
        }
    }

    /** Tự chạy nhanh khi đang đi tiến. */
    public static final class Sprint extends Module {
        public Sprint() {
            super("Sprint", "Always sprint while moving forward", Category.MOVEMENT);
        }

        @Override
        public void onTick(class_310 mc) {
            if (mc.field_1724 != null && mc.field_1690.field_1894.method_1434()) {
                mc.field_1690.field_1867.method_23481(true);
            }
        }

        @Override
        protected void onDisable() {
            class_310.method_1551().field_1690.field_1867.method_23481(false);
        }
    }

    /** Tự đi tiến, tiện khi cầm điện thoại bằng một tay. */
    public static final class AutoWalk extends Module {
        public AutoWalk() {
            super("Auto Walk", "Walk forward without holding a key", Category.MOVEMENT);
        }

        @Override
        public void onTick(class_310 mc) {
            if (mc.field_1724 != null) {
                mc.field_1690.field_1894.method_23481(true);
            }
        }

        @Override
        protected void onDisable() {
            class_310.method_1551().field_1690.field_1894.method_23481(false);
        }
    }

    /** Nhìn rõ trong bóng tối bằng hiệu ứng night vision phía client. */
    public static final class Fullbright extends Module {
        public Fullbright() {
            super("Fullbright", "See clearly in the dark", Category.RENDER);
        }

        @Override
        public void onTick(class_310 mc) {
            if (mc.field_1724 == null) {
                return;
            }
            class_1293 current = mc.field_1724.method_6112(class_1294.field_5925);
            if (current == null || current.method_5584() < 400) {
                mc.field_1724.method_6092(new class_1293(class_1294.field_5925, 100000, 0, false, false, false));
            }
        }

        @Override
        protected void onDisable() {
            class_310 mc = class_310.method_1551();
            if (mc.field_1724 != null) {
                mc.field_1724.method_6016(class_1294.field_5925);
            }
        }
    }
}
