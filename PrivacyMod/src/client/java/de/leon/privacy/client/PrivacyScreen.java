package de.leon.privacy.client;

import de.leon.privacy.api.PrivacyEntry;
import de.leon.privacy.api.PrivacyManager;
import de.leon.privacy.api.PrivacyVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Privacy dashboard with a self-contained dark glass-like UI.
 * All surfaces are rendered directly by PrivacyMod; no external UI library is used.
 */
public final class PrivacyScreen extends Screen {
    private static final int PANEL_WIDTH = 390;
    private static final int PANEL_PADDING = 16;
    private static final int ROW_HEIGHT = 43;

    public PrivacyScreen() {
        super(Component.literal("Privacy"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        List<PrivacyEntry> entries = PrivacyManager.getEntries();
        int panelHeight = 132 + entries.size() * ROW_HEIGHT + 24;
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - panelHeight) / 2;

        graphics.fill(0, 0, width, height, 0x99000000);
        drawRoundedSurface(graphics, left, top, PANEL_WIDTH, panelHeight, 12,
            0xF20A0A0E, 0xFF3A3A44);

        graphics.centeredText(font, title, width / 2, top + 13, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal("LOCAL PRIVACY OVERVIEW"),
            width / 2, top + 29, 0xFF9999A3);

        int rowY = top + 55;
        for (PrivacyEntry entry : entries) {
            int stateColor = color(entry.visibility());
            drawRoundedSurface(graphics, left + PANEL_PADDING, rowY + 2,
                PANEL_WIDTH - PANEL_PADDING * 2, ROW_HEIGHT - 5, 7,
                0xD91A1A21, 0xFF303039);
            graphics.fill(left + PANEL_PADDING + 9, rowY + 8,
                left + PANEL_PADDING + 11, rowY + 29, stateColor);
            graphics.text(font, Component.literal(entry.title()),
                left + PANEL_PADDING + 17, rowY + 5, 0xFFF2F2F5);
            graphics.text(font, Component.literal(label(entry.visibility())),
                left + PANEL_WIDTH - PANEL_PADDING - 96, rowY + 5, stateColor);
            String explanation = entry.explanation();
            if (explanation.length() > 62) explanation = explanation.substring(0, 59) + "...";
            graphics.text(font, Component.literal(explanation),
                left + PANEL_PADDING + 17, rowY + 19, 0xFFAAAAB2);
            rowY += ROW_HEIGHT;
        }

        int settingsY = top + 59 + entries.size() * ROW_HEIGHT;
        PrivacyManager.HudController hud = PrivacyManager.getHudController();
        if (hud != null) {
            graphics.text(font, Component.literal("PLAYER STATUS HUD"),
                left + PANEL_PADDING, settingsY, 0xFFF2F2F5);
            int buttonY = settingsY + 16;
            drawButton(graphics, left + PANEL_PADDING, buttonY, 154, 22,
                hud.isEnabled() ? "HUD: ON" : "HUD: OFF",
                hud.isEnabled() ? 0xFF245C43 : 0xFF5A3033);
            graphics.text(font, Component.literal("SIZE"), left + 184, buttonY + 7, 0xFFBBBBC4);
            drawButton(graphics, left + 235, buttonY, 30, 22, "-", 0xFF292930);
            graphics.centeredText(font, Component.literal(hud.getSizePercent() + "%"),
                left + 298, buttonY + 7, 0xFFFFFFFF);
            drawButton(graphics, left + 331, buttonY, 30, 22, "+", 0xFF292930);
        }

        graphics.centeredText(font, Component.literal("H  •  CLOSE     |     STATUS IS INFORMATIONAL"),
            width / 2, top + panelHeight - 18, 0xFF777780);
    }

    private static void drawButton(GuiGraphicsExtractor graphics, int x, int y, int width,
                                   int height, String label, int background) {
        drawRoundedSurface(graphics, x, y, width, height, 5, background, 0xFF55555F);
        graphics.centeredText(Minecraft.getInstance().font,
            Component.literal(label), x + width / 2, y + 7, 0xFFFFFFFF);
    }

    private static void drawRoundedSurface(GuiGraphicsExtractor graphics, int x, int y,
                                           int width, int height, int radius,
                                           int fillColor, int borderColor) {
        for (int row = 0; row < height; row++) {
            int edge = Math.min(row, height - 1 - row);
            int inset = edge >= radius ? 0 : radius - (int) Math.ceil(
                Math.sqrt(Math.max(0.0, radius * radius - Math.pow(radius - edge - 0.5, 2))));
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, fillColor);
            graphics.fill(x + inset, y + row, x + inset + 1, y + row + 1, borderColor);
            graphics.fill(x + width - inset - 1, y + row,
                x + width - inset, y + row + 1, borderColor);
            if (row == 0 || row == height - 1) {
                graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, borderColor);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            double mouseX = event.x();
            double mouseY = event.y();
            List<PrivacyEntry> entries = PrivacyManager.getEntries();
            int panelHeight = 132 + entries.size() * ROW_HEIGHT + 24;
            int left = (width - PANEL_WIDTH) / 2;
            int top = (height - panelHeight) / 2;
            int settingsY = top + 59 + entries.size() * ROW_HEIGHT;
            int buttonY = settingsY + 16;
            PrivacyManager.HudController hud = PrivacyManager.getHudController();
            if (hud != null && mouseY >= buttonY && mouseY < buttonY + 22) {
                if (mouseX >= left + PANEL_PADDING && mouseX < left + PANEL_PADDING + 154) {
                    hud.setEnabled(!hud.isEnabled());
                    return true;
                }
                if (mouseX >= left + 235 && mouseX < left + 265) {
                    hud.setSizePercent(hud.getSizePercent() - 5);
                    return true;
                }
                if (mouseX >= left + 331 && mouseX < left + 361) {
                    hud.setSizePercent(hud.getSizePercent() + 5);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static int color(PrivacyVisibility visibility) {
        return switch (visibility) {
            case PRIVATE -> 0xFF70D69A;
            case PARTIALLY_EXPOSED -> 0xFFFFC76A;
            case EXPOSED -> 0xFFFF737A;
            case UNKNOWN -> 0xFFAAAAAA;
        };
    }

    private static String label(PrivacyVisibility visibility) {
        return switch (visibility) {
            case PRIVATE -> "PRIVATE";
            case PARTIALLY_EXPOSED -> "PARTIAL";
            case EXPOSED -> "EXPOSED";
            case UNKNOWN -> "UNKNOWN";
        };
    }
}
