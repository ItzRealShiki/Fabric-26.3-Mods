package com.vextex.pvpmod;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class PvPModScreen extends Screen {
    private static final int PANEL = 0xF0181D29;
    private static final int CARD = 0xFF252D3B;
    private static final int CARD_HOVER = 0xFF303B4D;
    private static final int ACCENT = 0xFF55C8FF;
    private static final int MUTED = 0xFF9AA8BA;
    private static final int ENABLED = 0xFF55D69E;
    private static final int DISABLED = 0xFFFF7B83;

    private final boolean mainMenu;

    public PvPModScreen(boolean mainMenu) {
        super(Text.literal(mainMenu ? "PvP Mod" : "PvP Mod Modules"));
        this.mainMenu = mainMenu;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xB9000000);
        int panelWidth = Math.min(width - 32, 620);
        int panelHeight = Math.min(height - 32, 430);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        context.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        context.fill(left, top, left + panelWidth, top + 3, ACCENT);

        context.drawTextWithShadow(textRenderer, Text.literal(mainMenu ? "PVP MOD" : "MODULE GRID"),
                left + 22, top + 18, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.literal(mainMenu ? "Manage your PvP modules and preferences" : "Select a module to toggle it"),
                left + 22, top + 36, MUTED);

        if (mainMenu) {
            renderMainMenu(context, mouseX, mouseY, left, top, panelWidth, panelHeight);
        } else {
            renderGrid(context, mouseX, mouseY, left, top, panelWidth, panelHeight);
        }

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("RIGHT SHIFT  •  ESC TO CLOSE"), width / 2, top + panelHeight - 20, MUTED);
    }

    private void renderGrid(DrawContext context, int mouseX, int mouseY, int left, int top,
                            int panelWidth, int panelHeight) {
        int gap = 9;
        int cardWidth = Math.min(124, (panelWidth - 52 - gap * 3) / 4);
        int cardHeight = Math.min(68, (panelHeight - 112 - gap * 2) / 3);
        int totalWidth = cardWidth * 4 + gap * 3;
        int totalHeight = cardHeight * 3 + gap * 2;
        int startX = left + (panelWidth - totalWidth) / 2;
        int startY = top + 68 + (panelHeight - 112 - totalHeight) / 2;

        List<int[]> positions = gridPositions();
        PvPFeature[] features = PvPFeature.values();
        for (int i = 0; i < features.length; i++) {
            int[] pos = positions.get(i);
            int x = startX + pos[0] * (cardWidth + gap);
            int y = startY + pos[1] * (cardHeight + gap);
            drawFeatureCard(context, features[i], x, y, cardWidth, cardHeight, mouseX, mouseY);
        }

        int mainX = startX + cardWidth + gap;
        int mainY = startY + cardHeight + gap;
        int mainWidth = cardWidth * 2 + gap;
        boolean hovered = inside(mouseX, mouseY, mainX, mainY, mainWidth, cardHeight);
        context.fill(mainX, mainY, mainX + mainWidth, mainY + cardHeight,
                hovered ? CARD_HOVER : 0xFF202A38);
        context.fill(mainX, mainY, mainX + 3, mainY + cardHeight, ACCENT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("MAIN MENU"),
                mainX + mainWidth / 2, mainY + cardHeight / 2 - 5, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Settings & overview"),
                mainX + mainWidth / 2, mainY + cardHeight / 2 + 10, MUTED);
    }

    private void renderMainMenu(DrawContext context, int mouseX, int mouseY, int left, int top,
                                int panelWidth, int panelHeight) {
        int gap = 8;
        int cardWidth = (panelWidth - 58) / 2;
        int cardHeight = Math.min(43, (panelHeight - 166) / 5);
        int startX = left + 21;
        int startY = top + 68;

        PvPFeature[] features = PvPFeature.values();
        for (int i = 0; i < features.length; i++) {
            int column = i % 2;
            int row = i / 2;
            int x = startX + column * (cardWidth + gap);
            int y = startY + row * (cardHeight + gap);
            drawFeatureCard(context, features[i], x, y, cardWidth, cardHeight, mouseX, mouseY);
        }

        int handY = startY + 5 * (cardHeight + gap) + 2;
        int handWidth = panelWidth - 42;
        boolean hovered = inside(mouseX, mouseY, startX, handY, handWidth, 32);
        context.fill(startX, handY, startX + handWidth, handY + 32, hovered ? CARD_HOVER : CARD);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("HAND SUPPORT: " + PvPModConfig.handMode().label().toUpperCase(java.util.Locale.ROOT)
                        + "  •  CLICK TO CHANGE"),
                startX + handWidth / 2, handY + 11, ACCENT);

        int backY = top + panelHeight - 48;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("BACK TO MODULE GRID"),
                width / 2, backY, ACCENT);
    }

    private void drawFeatureCard(DrawContext context, PvPFeature feature, int x, int y,
                                 int cardWidth, int cardHeight, int mouseX, int mouseY) {
        boolean enabled = PvPModConfig.isEnabled(feature);
        boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);
        context.fill(x, y, x + cardWidth, y + cardHeight, hovered ? CARD_HOVER : CARD);
        context.fill(x, y, x + 3, y + cardHeight, enabled ? ENABLED : DISABLED);
        context.drawTextWithShadow(textRenderer, Text.literal(feature.title()),
                x + 11, y + 9, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal(enabled ? "ENABLED" : "DISABLED"),
                x + 11, y + cardHeight - 17, enabled ? ENABLED : MUTED);
        context.drawTextWithShadow(textRenderer, Text.literal(enabled ? "ON" : "OFF"),
                x + cardWidth - 30, y + 9, enabled ? ENABLED : DISABLED);
    }

    private static List<int[]> gridPositions() {
        List<int[]> positions = new ArrayList<>();
        positions.add(new int[]{0, 0});
        positions.add(new int[]{1, 0});
        positions.add(new int[]{2, 0});
        positions.add(new int[]{3, 0});
        positions.add(new int[]{0, 1});
        positions.add(new int[]{3, 1});
        positions.add(new int[]{0, 2});
        positions.add(new int[]{1, 2});
        positions.add(new int[]{2, 2});
        positions.add(new int[]{3, 2});
        return positions;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int panelWidth = Math.min(width - 32, 620);
        int panelHeight = Math.min(height - 32, 430);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        if (!mainMenu) {
            int gap = 9;
            int cardWidth = Math.min(124, (panelWidth - 52 - gap * 3) / 4);
            int cardHeight = Math.min(68, (panelHeight - 112 - gap * 2) / 3);
            int totalHeight = cardHeight * 3 + gap * 2;
            int startX = left + (panelWidth - (cardWidth * 4 + gap * 3)) / 2;
            int startY = top + 68 + (panelHeight - 112 - totalHeight) / 2;

            int mainX = startX + cardWidth + gap;
            int mainY = startY + cardHeight + gap;
            if (inside(mouseX, mouseY, mainX, mainY, cardWidth * 2 + gap, cardHeight)) {
                client.setScreen(new PvPModScreen(true));
                return true;
            }

            PvPFeature[] features = PvPFeature.values();
            List<int[]> positions = gridPositions();
            for (int i = 0; i < features.length; i++) {
                int[] pos = positions.get(i);
                int x = startX + pos[0] * (cardWidth + gap);
                int y = startY + pos[1] * (cardHeight + gap);
                if (inside(mouseX, mouseY, x, y, cardWidth, cardHeight)) {
                    PvPModConfig.setEnabled(features[i], !PvPModConfig.isEnabled(features[i]));
                    return true;
                }
            }
        } else {
            int gap = 8;
            int cardWidth = (panelWidth - 58) / 2;
            int cardHeight = Math.min(43, (panelHeight - 166) / 5);
            int startX = left + 21;
            int startY = top + 68;

            PvPFeature[] features = PvPFeature.values();
            for (int i = 0; i < features.length; i++) {
                int column = i % 2;
                int row = i / 2;
                int x = startX + column * (cardWidth + gap);
                int y = startY + row * (cardHeight + gap);
                if (inside(mouseX, mouseY, x, y, cardWidth, cardHeight)) {
                    PvPModConfig.setEnabled(features[i], !PvPModConfig.isEnabled(features[i]));
                    return true;
                }
            }

            int handY = startY + 5 * (cardHeight + gap) + 2;
            if (inside(mouseX, mouseY, startX, handY, panelWidth - 42, 32)) {
                PvPModConfig.cycleHandMode();
                return true;
            }

            int backY = top + panelHeight - 48;
            if (mouseY >= backY - 5 && mouseY <= backY + 18) {
                client.setScreen(new PvPModScreen(false));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
