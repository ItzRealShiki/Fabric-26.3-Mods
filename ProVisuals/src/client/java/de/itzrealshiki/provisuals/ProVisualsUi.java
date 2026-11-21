package de.itzrealshiki.provisuals;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared drawing helpers for ProVisuals surfaces.
 * Keeps corner styling consistent and lets the user switch it globally.
 */
public final class ProVisualsUi {
    private ProVisualsUi() {}

    public static void fill(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                            int radius, int color) {
        if (width <= 0 || height <= 0) return;
        if (!ProVisualsConfig.roundedCorners || radius <= 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        int r = Math.min(radius, Math.min(width / 2, height / 2));
        if (r <= 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        graphics.fill(x, y + r, x + width, y + height - r, color);
        graphics.fill(x + r, y, x + width - r, y + height, color);

        for (int row = 0; row < r; row++) {
            int distanceFromCenter = r - row - 1;
            int inset = r - (int) Math.floor(Math.sqrt((double) r * r
                    - (double) distanceFromCenter * distanceFromCenter));
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            graphics.fill(x + inset, y + height - row - 1,
                    x + width - inset, y + height - row, color);
        }
    }

    /**
     * Clips the four square corners of a widget to the rounded-corner shape.
     * The caller supplies the panel colour so the cut-outs blend into the screen.
     */
    public static void roundWidgetCorners(GuiGraphicsExtractor graphics, int x, int y,
                                          int width, int height, int radius, int backgroundColor) {
        if (!ProVisualsConfig.roundedCorners || width <= 0 || height <= 0) return;
        int r = Math.min(radius, Math.min(width / 2, height / 2));
        for (int row = 0; row < r; row++) {
            int distanceFromCenter = r - row - 1;
            int inset = r - (int) Math.floor(Math.sqrt((double) r * r
                    - (double) distanceFromCenter * distanceFromCenter));
            graphics.fill(x, y + row, x + inset, y + row + 1, backgroundColor);
            graphics.fill(x + width - inset, y + row, x + width, y + row + 1, backgroundColor);
            graphics.fill(x, y + height - row - 1, x + inset, y + height - row, backgroundColor);
            graphics.fill(x + width - inset, y + height - row - 1,
                    x + width, y + height - row, backgroundColor);
        }
    }
}
