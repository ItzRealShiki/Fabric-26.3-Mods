package de.itzrealshiki.provisuals;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class ProVisualsUi {
    private ProVisualsUi() {}

    /** Accent colors shared by the menu, Music Island and visual effects. */
    public static int accentPrimary() {
        return switch (ProVisualsConfig.accentPalette) {
            case "BLUE" -> 0xFF579BFF;
            case "CYAN" -> 0xFF35C9D0;
            case "PINK" -> 0xFFF36EA9;
            case "GOLD" -> 0xFFE6AD4E;
            case "GREEN" -> 0xFF58C98D;
            default -> 0xFF9A6BFF;
        };
    }

    public static int accentSoft() {
        return switch (ProVisualsConfig.accentPalette) {
            case "BLUE" -> 0xFF9EC6FF;
            case "CYAN" -> 0xFF83E4E5;
            case "PINK" -> 0xFFFFABD0;
            case "GOLD" -> 0xFFF3CF88;
            case "GREEN" -> 0xFF9DE6BB;
            default -> 0xFFBCA4FF;
        };
    }

    public static int accentLight() {
        return switch (ProVisualsConfig.accentPalette) {
            case "BLUE" -> 0xFFC7DEFF;
            case "CYAN" -> 0xFFB8F1F0;
            case "PINK" -> 0xFFFFD0E5;
            case "GOLD" -> 0xFFFFE6B8;
            case "GREEN" -> 0xFFC9F5D9;
            default -> 0xFFD0BFFF;
        };
    }

    public static int accentDeep() {
        return switch (ProVisualsConfig.accentPalette) {
            case "BLUE" -> 0xFF203B62;
            case "CYAN" -> 0xFF1B4A50;
            case "PINK" -> 0xFF582840;
            case "GOLD" -> 0xFF594323;
            case "GREEN" -> 0xFF214B37;
            default -> 0xFF392A56;
        };
    }

    public static int accentButton() {
        return switch (ProVisualsConfig.accentPalette) {
            case "BLUE" -> 0xFF294C7E;
            case "CYAN" -> 0xFF24545B;
            case "PINK" -> 0xFF62304A;
            case "GOLD" -> 0xFF654E28;
            case "GREEN" -> 0xFF2E5E45;
            default -> 0xFF3C3154;
        };
    }

    public static int accentWithAlpha(int color, int alpha) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        return (safeAlpha << 24) | (color & 0x00FFFFFF);
    }

    public static int accentSwatch(String palette) {
        return switch (palette) {
            case "BLUE" -> 0xFF579BFF;
            case "CYAN" -> 0xFF35C9D0;
            case "PINK" -> 0xFFF36EA9;
            case "GOLD" -> 0xFFE6AD4E;
            case "GREEN" -> 0xFF58C98D;
            default -> 0xFF9A6BFF;
        };
    }

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
        return Component.literal(displayText(value));
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
        graphics.fill(x + r, y, x + width - r, y + height, color);
        graphics.fill(x, y + r, x + width, y + height - r, color);
        for (int corner = 0; corner < 4; corner++) {
            boolean right = (corner & 1) != 0;
            boolean bottom = (corner & 2) != 0;
            float centerX = right ? 0.0f : r;
            float centerY = bottom ? 0.0f : r;
            int originX = right ? x + width - r : x;
            int originY = bottom ? y + height - r : y;
            for (int row = 0; row < r; row++) {
                for (int col = 0; col < r; col++) {
                    float dx = col + 0.5f - centerX;
                    float dy = row + 0.5f - centerY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float coverage = clamp01(r + 0.5f - distance);
                    if (coverage <= 0.0f) continue;
                    graphics.fill(originX + col, originY + row,
                            originX + col + 1, originY + row + 1, withOpacity(color, coverage));
                }
            }
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int withOpacity(int color, float opacity) {
        int alpha = color >>> 24;
        int adjusted = Math.max(0, Math.min(255, Math.round(alpha * opacity)));
        return (adjusted << 24) | (color & 0x00FFFFFF);
    }

    public static void roundWidgetCorners(GuiGraphicsExtractor graphics, int x, int y,
                                          int width, int height, int radius, int backgroundColor) {
        if (!ProVisualsConfig.roundedCorners || width <= 0 || height <= 0) return;
        int r = Math.min(radius, Math.min(width / 2, height / 2));
        if (r <= 0) return;
        for (int corner = 0; corner < 4; corner++) {
            boolean right = (corner & 1) != 0;
            boolean bottom = (corner & 2) != 0;
            float centerX = right ? 0.0f : r;
            float centerY = bottom ? 0.0f : r;
            int originX = right ? x + width - r : x;
            int originY = bottom ? y + height - r : y;
            for (int row = 0; row < r; row++) {
                for (int col = 0; col < r; col++) {
                    float dx = col + 0.5f - centerX;
                    float dy = row + 0.5f - centerY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float insideCoverage = clamp01(r + 0.5f - distance);
                    float outsideCoverage = 1.0f - insideCoverage;
                    if (outsideCoverage <= 0.0f) continue;
                    graphics.fill(originX + col, originY + row,
                            originX + col + 1, originY + row + 1,
                            withOpacity(backgroundColor, outsideCoverage));
                }
            }
        }
    }

}
