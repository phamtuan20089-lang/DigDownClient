package dev.digdown.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Nơi khai báo các chức năng có sẵn. Thêm chức năng mới: viết class mới rồi thêm vào registerAll(). */
public final class BuiltinModules {

    private BuiltinModules() {
    }

    public static void registerAll() {
        ModuleManager.register(new Sprint());
        ModuleManager.register(new AutoWalk());
        ModuleManager.register(new Fullbright());
    }

    /** Tự chạy nhanh khi đang đi tiến. */
    public static final class Sprint extends Module {
        public Sprint() {
            super("Sprint", "Always sprint while moving forward", Category.MOVEMENT);
        }

        @Override
        public void onTick(Minecraft mc) {
            if (mc.player != null && mc.options.keyUp.isDown()) {
                mc.options.keySprint.setDown(true);
            }
        }

        @Override
        protected void onDisable() {
            Minecraft.getInstance().options.keySprint.setDown(false);
        }
    }

    /** Tự đi tiến, tiện khi cầm điện thoại bằng một tay. */
    public static final class AutoWalk extends Module {
        public AutoWalk() {
            super("Auto Walk", "Walk forward without holding a key", Category.MOVEMENT);
        }

        @Override
        public void onTick(Minecraft mc) {
            if (mc.player != null) {
                mc.options.keyUp.setDown(true);
            }
        }

        @Override
        protected void onDisable() {
            Minecraft.getInstance().options.keyUp.setDown(false);
        }
    }

    /** Nhìn rõ trong bóng tối bằng hiệu ứng night vision phía client. */
    public static final class Fullbright extends Module {
        public Fullbright() {
            super("Fullbright", "See clearly in the dark", Category.RENDER);
        }

        @Override
        public void onTick(Minecraft mc) {
            if (mc.player == null) {
                return;
            }
            MobEffectInstance current = mc.player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 400) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100000, 0, false, false, false));
            }
        }

        @Override
        protected void onDisable() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
}
