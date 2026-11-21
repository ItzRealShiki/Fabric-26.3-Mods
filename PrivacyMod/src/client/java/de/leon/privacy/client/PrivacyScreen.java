package de.leon.privacy.client;

import de.leon.privacy.api.PrivacyEntry;
import de.leon.privacy.api.PrivacyManager;
import de.leon.privacy.api.PrivacyVisibility;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import restudio.reglass.client.api.ReGlassApi;
import restudio.reglass.client.api.WidgetStyle;

import java.util.List;

/**
 * Lightweight privacy dashboard. Uses Minecraft's batched GUI fills and text renderer;
 * no per-frame blur, framebuffer copies, or custom shader passes are required.
 */
public final class PrivacyScreen extends Screen {
    private static final int PANEL_WIDTH = 390;
    private static final int PANEL_PADDING = 16;
    private static final int ROW_HEIGHT = 43;
    private static final WidgetStyle GLASS_STYLE = WidgetStyle.create()
        .tint(0x080808, 0.58f)
        .smoothing(0.05f)
        .shadow(22.0f, 0.24f, 0.0f, 4.0f)
        .shadowColor(0x000000, 0.82f);

    public PrivacyScreen() {
        super(Component.literal("Privacy"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        List<PrivacyEntry> entries = PrivacyManager.getEntries();
        int panelHeight = 64 + entries.size() * ROW_HEIGHT + 24;
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - panelHeight) / 2;

        graphics.fill(0, 0, width, height, 0x99000000);
        ReGlassApi.create(graphics)
            .dimensions(left, top, PANEL_WIDTH, panelHeight)
            .cornerRadius(12.0f)
            .style(GLASS_STYLE)
            .render();
        drawRoundedOutline(graphics, left, top, PANEL_WIDTH, panelHeight, 12, 0xFF777777);

        graphics.centeredText(font, title, width / 2, top + 13, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal("LOCAL PRIVACY OVERVIEW"), width / 2, top + 29, 0xFFAAAAAA);

        int rowY = top + 55;
        for (PrivacyEntry entry : entries) {
            int stateColor = color(entry.visibility());
            ReGlassApi.create(graphics)
                .dimensions(left + PANEL_PADDING, rowY + 2, PANEL_WIDTH - PANEL_PADDING * 2, ROW_HEIGHT - 5)
                .cornerRadius(8.0f)
                .style(GLASS_STYLE)
                .render();
            graphics.fill(left + PANEL_PADDING + 8, rowY + 2, left + PANEL_WIDTH - PANEL_PADDING - 8, rowY + 3, 0x553F3F3F);
            graphics.fill(left + PANEL_PADDING, rowY + 8, left + PANEL_PADDING + 3, rowY + 30, stateColor);
            graphics.text(font, Component.literal(entry.title()), left + PANEL_PADDING + 11, rowY + 5, 0xFFFFFFFF);
            graphics.text(font, Component.literal(label(entry.visibility())), left + PANEL_WIDTH - PANEL_PADDING - 96, rowY + 5, stateColor);
            String explanation = entry.explanation();
            if (explanation.length() > 62) explanation = explanation.substring(0, 59) + "...";
            graphics.text(font, Component.literal(explanation), left + PANEL_PADDING + 11, rowY + 19, 0xFFAAAAAA);
            rowY += ROW_HEIGHT;
        }

        graphics.centeredText(font, Component.literal("ESC  •  CLOSE     |     STATUS IS INFORMATIONAL"),
            width / 2, top + panelHeight - 18, 0xFF888888);
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

    private static void drawRoundedOutline(GuiGraphicsExtractor graphics, int x, int y,
                                            int width, int height, int radius, int color) {
        for (int row = 0; row < height; row++) {
            int edge = Math.min(row, height - 1 - row);
            int outerInset = edge >= radius ? 0 : radius - (int) Math.ceil(
                Math.sqrt(Math.max(0.0, radius * radius - Math.pow(radius - edge - 0.5, 2)))
            );
            int innerRadius = Math.max(1, radius - 1);
            int innerEdge = Math.min(row - 1, height - 2 - row);
            int innerInset = innerEdge >= innerRadius || innerEdge < 0 ? 0 : innerRadius - (int) Math.ceil(
                Math.sqrt(Math.max(0.0, innerRadius * innerRadius - Math.pow(innerRadius - innerEdge - 0.5, 2)))
            );
            graphics.fill(x + outerInset, y + row, x + outerInset + 1, y + row + 1, color);
            graphics.fill(x + width - outerInset - 1, y + row, x + width - outerInset, y + row + 1, color);
            if (innerInset > outerInset + 1) {
                graphics.fill(x + outerInset + 1, y + row, x + innerInset, y + row + 1, color);
                graphics.fill(x + width - innerInset, y + row, x + width - outerInset - 1, y + row + 1, color);
            }
        }
    }
}
