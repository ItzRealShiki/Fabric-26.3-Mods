package de.itzrealshiki.provisuals;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Shared drawing and typography helpers for every ProVisuals surface.
 */
public final class ProVisualsUi {
    private ProVisualsUi() {}

    /**
     * Unicode small-cap substitutions used consistently by the complete ProVisuals UI.
     * Characters without a dedicated small-cap form (for example x) use their lowercase
     * glyph so that the text remains readable instead of showing a missing-glyph box.
     */
    public static String smallCaps(String value) {
        if (value == null || value.isEmpty()) return value == null ? "" : value;
        StringBuilder result = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            int lower = Character.toLowerCase(codePoint);
            result.append(switch (lower) {
                case 'a' -> "ᴀ";
                case 'b' -> "ʙ";
                case 'c' -> "ᴄ";
                case 'd' -> "ᴅ";
                case 'e' -> "ᴇ";
                case 'f' -> "ꜰ";
                case 'g' -> "ɢ";
                case 'h' -> "ʜ";
                case 'i' -> "ɪ";
                case 'j' -> "ᴊ";
                case 'k' -> "ᴋ";
                case 'l' -> "ʟ";
                case 'm' -> "ᴍ";
                case 'n' -> "ɴ";
                case 'o' -> "ᴏ";
                case 'p' -> "ᴘ";
                case 'q' -> "ꞯ";
                case 'r' -> "ʀ";
                case 's' -> "ꜱ";
                case 't' -> "ᴛ";
                case 'u' -> "ᴜ";
                case 'v' -> "ᴠ";
                case 'w' -> "ᴡ";
                case 'y' -> "ʏ";
                case 'z' -> "ᴢ";
                default -> new String(Character.toChars(lower));
            });
        });
        return result.toString();
    }

    public static Component component(String value) {
        return Component.literal(smallCaps(value));
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, String value,
                            int x, int y, int color, boolean shadow) {
        graphics.text(font, smallCaps(value), x, y, color, shadow);
    }

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
