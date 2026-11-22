package dev.digdown.client;

import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;

/** Điểm khởi tạo chung. Chỉ cần gọi DigDownCore.init() một lần trong onInitializeClient(). */
public final class DigDownCore {
    private static class_304 openGuiKey;

    private DigDownCore() {
    }

    public static void init() {
        BuiltinModules.registerAll();
        DigDownHud.register();

        class_304.class_11900 category = class_304.class_11900.method_74698(
                class_2960.method_60655("digdown", "main"));

        // Mặc định Right Shift. Trên điện thoại: gán nút điều khiển của Zalith vào phím này,
        // hoặc đổi phím trong Options > Controls > Key Binds > DigDown Client.
        openGuiKey = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.digdown.open_gui",
                class_3675.class_307.field_1668,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(DigDownCore::onTick);
    }

    private static void onTick(class_310 mc) {
        while (openGuiKey.method_1436()) {
            if (mc.field_1755 == null) {
                mc.method_1507(new ClickGuiScreen());
            }
        }
        if (mc.field_1724 != null) {
            ModuleManager.tick(mc);
        }
    }
}
