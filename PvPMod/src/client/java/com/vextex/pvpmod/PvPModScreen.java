package com.vextex.pvpmod;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PvPModScreen extends Screen {
    private final boolean mainMenu;

    public PvPModScreen(boolean mainMenu) {
        super(Component.literal(mainMenu ? "PvP Mod" : "PvP Mod Modules"));
        this.mainMenu = mainMenu;
    }

    @Override
    protected void init() {
        int columns = mainMenu ? 2 : 2;
        int gap = 8;
        int buttonWidth = Math.min(220, (this.width - 48 - gap) / columns);
        int buttonHeight = 28;
        int startX = (this.width - (buttonWidth * columns + gap)) / 2;
        int startY = 52;

        PvPFeature[] features = PvPFeature.values();
        for (int i = 0; i < features.length; i++) {
            PvPFeature feature = features[i];
            int column = i % columns;
            int row = i / columns;
            int x = startX + column * (buttonWidth + gap);
            int y = startY + row * (buttonHeight + gap);

            this.addRenderableWidget(Button.builder(featureLabel(feature), button -> {
                PvPModConfig.setEnabled(feature, !PvPModConfig.isEnabled(feature));
                this.minecraft.gui.setScreen(new PvPModScreen(mainMenu));
            }).bounds(x, y, buttonWidth, buttonHeight).build());
        }

        int footerY = Math.min(this.height - 42, startY + ((features.length + columns - 1) / columns) * (buttonHeight + gap) + 8);
        if (mainMenu) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("HAND SUPPORT: " + PvPModConfig.handMode().label().toUpperCase(java.util.Locale.ROOT)),
                    button -> {
                        PvPModConfig.cycleHandMode();
                        this.minecraft.gui.setScreen(new PvPModScreen(true));
                    }).bounds(startX, footerY, buttonWidth * 2 + gap, buttonHeight).build());
        } else {
            this.addRenderableWidget(Button.builder(Component.literal("MAIN MENU"), button ->
                    this.minecraft.gui.setScreen(new PvPModScreen(true)))
                    .bounds(startX, footerY, buttonWidth * 2 + gap, buttonHeight).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("DONE"), button -> this.onClose())
                .bounds((this.width - 100) / 2, this.height - 28, 100, 20).build());
    }

    private static Component featureLabel(PvPFeature feature) {
        boolean enabled = PvPModConfig.isEnabled(feature);
        return Component.literal((enabled ? "[ON] " : "[OFF] ") + feature.title());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
