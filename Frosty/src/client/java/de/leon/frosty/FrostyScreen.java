package de.leon.frosty;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class FrostyScreen extends Screen {
    private enum Page { MODULES, SETTINGS }
    private Page page = Page.MODULES;
    private final List<EditBox> fields = new ArrayList<>();
    private String status = "FROSTY CLIENT  /  26.3";

    public FrostyScreen() { super(Component.literal("Frosty")); }

    @Override
    protected void init() {
        clearWidgets();
        fields.clear();
        int left = width / 2 - 175;
        int top = height / 2 - 105;

        addRenderableWidget(Button.builder(Component.literal("MODULES"), b -> { page = Page.MODULES; rebuildWidgets(); })
            .bounds(left - 105, top, 96, 22).build());
        addRenderableWidget(Button.builder(Component.literal("SETTINGS"), b -> { page = Page.SETTINGS; rebuildWidgets(); })
            .bounds(left - 105, top + 27, 96, 22).build());
        addRenderableWidget(Button.builder(Component.literal("DONE"), b -> onClose())
            .bounds(left - 105, top + 54, 96, 22).build());

        if (page == Page.MODULES) {
            moduleButton("SMALL TOTEM POPS", FrostyConfig.smallTotemPops, left, top + 4, () -> {
                FrostyConfig.smallTotemPops = !FrostyConfig.smallTotemPops; FrostyConfig.save(); rebuildWidgets();
            });
            moduleButton("FIXED SHIELDS", FrostyConfig.fixedShields, left, top + 43, () -> {
                FrostyConfig.fixedShields = !FrostyConfig.fixedShields; FrostyConfig.save(); rebuildWidgets();
            });
            moduleButton("SHIELD STATUS", FrostyConfig.shieldStatus, left, top + 82, () -> {
                FrostyConfig.shieldStatus = !FrostyConfig.shieldStatus; FrostyConfig.save(); rebuildWidgets();
            });
            addRenderableWidget(Button.builder(Component.literal("CONFIGURE SHIELD STATUS"), b -> {
                page = Page.SETTINGS; rebuildWidgets();
            }).bounds(left, top + 122, 350, 22).build());
        } else {
            addRenderableWidget(new FrostySlider(left, top + 5, 350, 20, "TOTEM SIZE", FrostyConfig.totemScale, 0.25, 1.50, v -> {
                FrostyConfig.totemScale = v; FrostyConfig.save();
            }));
            addRenderableWidget(new FrostySlider(left, top + 32, 350, 20, "HUD SIZE", FrostyConfig.hudScale, 0.50, 1.50, v -> {
                FrostyConfig.hudScale = v; FrostyConfig.save();
            }));
            String[] labels = {"BLOCKED COLOR", "NOT BLOCKING COLOR", "BROKEN COLOR", "BLOCKING COLOR"};
            String[] values = {FrostyConfig.blockedColor, FrostyConfig.notBlockingColor, FrostyConfig.brokenColor, FrostyConfig.blockingColor};
            for (int i = 0; i < labels.length; i++) {
                final int index = i;
                EditBox field = new EditBox(font, left, top + 62 + i * 25, 160, 20, Component.literal(labels[i]));
                field.setMaxLength(7); field.setValue(values[i]); addRenderableWidget(field); fields.add(field);
                addRenderableWidget(Button.builder(Component.literal("APPLY"), b -> saveColor(index))
                    .bounds(left + 168, top + 62 + i * 25, 80, 20).build());
            }
            addRenderableWidget(Button.builder(Component.literal("RESET COLORS"), b -> {
                FrostyConfig.blockedColor = "#58D7FF"; FrostyConfig.notBlockingColor = "#A8C7D8";
                FrostyConfig.brokenColor = "#FF667A"; FrostyConfig.blockingColor = "#FFFFFF";
                FrostyConfig.save(); rebuildWidgets();
            }).bounds(left + 255, top + 62, 95, 20).build());
            addRenderableWidget(Button.builder(Component.literal("BACK TO MODULES"), b -> {
                page = Page.MODULES; rebuildWidgets();
            }).bounds(left, top + 169, 170, 20).build());
        }
    }

    private void moduleButton(String name, boolean enabled, int x, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(name + "   " + (enabled ? "ON" : "OFF")), b -> action.run())
            .bounds(x, y, 350, 30).build());
    }

    private void saveColor(int index) {
        String value = fields.get(index).getValue().trim().toUpperCase();
        if (!value.matches("#[0-9A-F]{6}")) { status = "USE HEX FORMAT, E.G. #58D7FF"; return; }
        switch (index) {
            case 0 -> FrostyConfig.blockedColor = value;
            case 1 -> FrostyConfig.notBlockingColor = value;
            case 2 -> FrostyConfig.brokenColor = value;
            case 3 -> FrostyConfig.blockingColor = value;
        }
        FrostyConfig.save(); status = "COLOR SAVED";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xD905101C);
        int x = width / 2 - 175, y = height / 2 - 112;
        g.fill(x - 112, y - 9, x + 365, y + 205, 0xEE0A2032);
        g.fill(x - 112, y - 9, x + 365, y - 7, 0xFF58D7FF);
        g.fill(x - 101, y + 3, x - 99, y + 79, 0xFF58D7FF);
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.text(font, Component.literal("FROSTY"), x - 100, y - 1, 0xFFFFFFFF);
        g.text(font, Component.literal(page == Page.MODULES ? "CLIENT MODULES" : "PERSONALIZE YOUR HUD"), x, y - 1, 0xFF9DEBFF);
        g.text(font, Component.literal(status), x, y + 191, 0xFF78BBD2);
    }

    private static final class FrostySlider extends AbstractSliderButton {
        private final String label;
        private final double min, max;
        private final java.util.function.DoubleConsumer changed;
        FrostySlider(int x, int y, int w, int h, String label, double value, double min, double max,
                     java.util.function.DoubleConsumer changed) {
            super(x, y, w, h, Component.empty(), (value - min) / (max - min));
            this.label = label; this.min = min; this.max = max; this.changed = changed; updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Component.literal(label + "  " + Math.round((min + value * (max - min)) * 100) + "%"));
        }
        @Override protected void applyValue() { changed.accept(min + value * (max - min)); }
    }
}
