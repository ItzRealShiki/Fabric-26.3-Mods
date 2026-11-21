package de.itzrealshiki.provisuals;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class ProVisualsUi {
    private static final Identifier UI_FONT =
            Identifier.fromNamespaceAndPath("provisuals", "ui");

    private ProVisualsUi() {}

    /**
     * Replaces the sharp Unicode small-cap look with regular Minecraft glyphs.
     * All-uppercase UI labels are softened to title case; mixed-case text and
     * media metadata are left untouched.
     */
    public static String displayText(String value) {
        if (value == null || value.isEmpty()) return value == null ? "" : value;

        boolean hasLetters = value.codePoints().anyMatch(Character::isLetter);
        boolean hasLowercase = value.codePoints().anyMatch(Character::isLowerCase);
        if (!hasLetters || hasLowercase) return value;

        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(lower.length());
        boolean startWord = true;
        for (int offset = 0; offset < lower.length();) {
            int codePoint = lower.codePointAt(offset);
            if (Character.isLetter(codePoint)) {
                result.appendCodePoint(startWord ? Character.toUpperCase(codePoint) : codePoint);
                startWord = false;
            } else {
                result.appendCodePoint(codePoint);
                startWord = !Character.isDigit(codePoint);
            }
            offset += Character.charCount(codePoint);
        }

        return result.toString()
                .replace("Hud", "HUD")
                .replace("Pvp", "PvP")
                .replace("Ui", "UI")
                .replace("Fps", "FPS")
                .replace("Cpu", "CPU")
                .replace("Gpu", "GPU")
                .replace("On", "ON")
                .replace("Off", "OFF");
    }

    public static Component component(String value) {
        return Component.literal(displayText(value)).withStyle(style -> style.withFont(new FontDescription.Resource(UI_FONT)));
    }

    public static int width(Font font, String value) {
        return font.width(component(value));
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, String value,
                            int x, int y, int color, boolean shadow) {
        graphics.text(font, component(value), x, y, color, shadow);
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
