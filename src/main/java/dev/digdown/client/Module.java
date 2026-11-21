package dev.digdown.client;

import net.minecraft.client.Minecraft;

/** Lớp cơ sở cho mọi chức năng. Muốn thêm chức năng mới: kế thừa lớp này. */
public abstract class Module {

    public enum Category {
        MOVEMENT("Movement"),
        RENDER("Render"),
        MISC("Misc");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    /** Được gọi một lần khi bật. */
    protected void onEnable() {
    }

    /** Được gọi một lần khi tắt. */
    protected void onDisable() {
    }

    /** Được gọi mỗi tick (20 lần/giây) khi module đang bật và đang trong thế giới. */
    public void onTick(Minecraft mc) {
    }
}
