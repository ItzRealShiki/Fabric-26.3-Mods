package de.itzrealshiki.provisuals;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MusicIslandHud implements HudElement {
    private record Rect(int x, int y, int width, int height) {
        boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    private record NavigationHit(String value, Rect rect) {}

    private static volatile Rect previousButton;
    private static volatile Rect playButton;
    private static volatile Rect nextButton;
    private static volatile Rect bounds;
    private static volatile Rect navigationBounds;

    private static final List<NavigationHit> CATEGORY_HITS = new ArrayList<>();
    private static final List<NavigationHit> SUBCATEGORY_HITS = new ArrayList<>();

    private static final int NAV_ROW_HEIGHT = 15;
    private static final int NAV_MAIN_HEIGHT = NAV_ROW_HEIGHT * 2;
    private static final int NAV_SUB_HEIGHT = 17;
    private static final int NAV_GAP = 2;

    private static String activeCategory = "PvP";
    private static String activeSubcategory = "Combat Visuals";
    private static float navigationProgress;
    private static long lastNavigationFrameTime;
    private static boolean leftButtonWasDown;

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        clearMediaHitboxes();
        if (!ProVisualsConfig.musicIslandEnabled) {
            bounds = null;
            navigationBounds = null;
            navigationProgress = 0.0f;
            CATEGORY_HITS.clear();
            SUBCATEGORY_HITS.clear();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            bounds = null;
            navigationBounds = null;
            return;
        }

        int panelWidth = Math.min(278, graphics.guiWidth() - 20);
        if (panelWidth < 220) {
            bounds = null;
            navigationBounds = null;
            return;
        }

        int panelHeight = 84;
        int x = graphics.guiWidth() - panelWidth - 10;
        int y = 12;
        bounds = new Rect(x, y, panelWidth, panelHeight);

        MusicLyricsBridge.Track track = MusicLyricsBridge.currentTrack();
        MusicGenreDetector.update(track == null ? null : track.title(), track == null ? null : track.artist());
        boolean playing = track != null && track.playing();
        Font font = minecraft.font;

        ProVisualsUi.fill(graphics, x + 2, y + 4, panelWidth, panelHeight, 13, 0x55000000);
        ProVisualsUi.fill(graphics, x, y, panelWidth, panelHeight, 13, 0xEF0D0D17);
        ProVisualsUi.fill(graphics, x + 1, y + 1, panelWidth - 2, panelHeight - 2, 12, 0xF313121E);
        ProVisualsUi.fill(graphics, x + 1, y + 1, panelWidth - 2, 2, 1, 0xFF6E4CC7);
        ProVisualsUi.fill(graphics, x + 1, y + 9, 2, panelHeight - 18, 1, 0xFF9A6BFF);

        drawAlbumArt(graphics, x + 10, y + 20, playing);

        int contentX = x + 61;
        int controlsX = x + panelWidth - 76;
        int contentWidth = Math.max(64, controlsX - contentX - 8);

        ProVisualsUi.fill(graphics, contentX, y + 9, 5, 5, 3,
                playing ? 0xFFBCA4FF : 0xFF777184);
        ProVisualsUi.text(graphics, font, track == null ? "Waiting for audio" : "Now playing",
                contentX + 9, y + 7, 0xFFBCA4FF, false);

        // Playback controls stay in the title row of this single Music Island.
        previousButton = new Rect(controlsX, y + 5, 18, 18);
        playButton = new Rect(controlsX + 22, y + 5, 25, 18);
        nextButton = new Rect(controlsX + 51, y + 5, 18, 18);
        drawControl(graphics, previousButton, "‹", false);
        drawControl(graphics, playButton, playing ? "Ⅱ" : "›", true);
        drawControl(graphics, nextButton, "›", false);

        String title = track == null ? "No music detected" : track.title();
        String artist = track == null ? "Start music on your device" : track.artist();
        graphics.text(font, trim(font, title, contentWidth), contentX, y + 21, 0xFFF5F1FF, true);
        graphics.text(font, trim(font, artist, contentWidth), contentX, y + 34, 0xFFAAA4BA, false);

        String genre = MusicGenreDetector.currentGenre();
        int genreWidth = Math.min(contentWidth, Math.max(48, font.width(genre) + 12));
        ProVisualsUi.fill(graphics, contentX, y + 47, genreWidth, 12, 6, 0xFF29233A);
        graphics.text(font, trim(font, genre, genreWidth - 10), contentX + 6, y + 49,
                0xFFD0BFFF, false);

        int progressX = contentX;
        int progressY = y + 63;
        ProVisualsUi.fill(graphics, progressX, progressY, contentWidth, 3, 2, 0xFF34313F);
        if (track != null && track.durationMs() > 0) {
            float fraction = Math.max(0.0f,
                    Math.min(1.0f, track.positionMs() / (float) track.durationMs()));
            int filledWidth = Math.round(contentWidth * fraction);
            if (filledWidth > 0) {
                ProVisualsUi.fill(graphics, progressX, progressY, filledWidth, 3, 2, 0xFF9A6BFF);
                ProVisualsUi.fill(graphics, progressX + Math.max(0, filledWidth - 2),
                        progressY - 1, 3, 5, 2, 0xFFD7C7FF);
            }
        }

        String timing = track == null || track.durationMs() <= 0
                ? "0:00 / 0:00"
                : formatTime(track.positionMs()) + " / " + formatTime(track.durationMs());
        ProVisualsUi.text(graphics, font, timing, contentX, y + 70, 0xFF888296, false);

        updateNavigationVisibility(minecraft);
        drawNavigation(graphics, font);
    }

    /** Handles clicks in-world; screen clicks are routed through Fabric's ScreenMouseEvents hook. */
    public static void tickInput(Minecraft minecraft) {
        if (!ProVisualsConfig.musicIslandEnabled || minecraft.level == null) {
            leftButtonWasDown = minecraft.mouseHandler.isLeftPressed();
            return;
        }

        boolean leftDown = minecraft.mouseHandler.isLeftPressed();
        if (leftDown && !leftButtonWasDown && minecraft.gui.screen() == null
                && !minecraft.mouseHandler.isMouseGrabbed()) {
            double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
            double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
            handleMouseClick(mouseX, mouseY);
        }
        leftButtonWasDown = leftDown;
    }

    private static void updateNavigationVisibility(Minecraft minecraft) {
        long now = System.nanoTime();
        if (lastNavigationFrameTime == 0L) lastNavigationFrameTime = now;
        float elapsed = Math.min(0.05f,
                Math.max(0.0f, (now - lastNavigationFrameTime) / 1_000_000_000.0f));
        lastNavigationFrameTime = now;

        boolean cursorVisible = !minecraft.mouseHandler.isMouseGrabbed();
        double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

        boolean overIsland = bounds != null && bounds.contains(mouseX, mouseY);
        boolean overNavigation = navigationBounds != null
                && navigationBounds.contains(mouseX, mouseY)
                && navigationProgress > 0.001f;
        boolean hovered = cursorVisible && (overIsland || overNavigation);

        float amount = elapsed / 0.14f;
        navigationProgress = hovered
                ? Math.min(1.0f, navigationProgress + amount)
                : Math.max(0.0f, navigationProgress - amount);
    }

    /** Expands the category navigation from the bottom edge of the Music Island on hover. */
    private static void drawNavigation(GuiGraphicsExtractor graphics, Font font) {
        CATEGORY_HITS.clear();
        SUBCATEGORY_HITS.clear();
        navigationBounds = null;
        if (bounds == null || navigationProgress <= 0.015f) return;

        int x = bounds.x();
        int width = bounds.width();
        int baseY = bounds.y() + bounds.height() + NAV_GAP;
        int slide = Math.round((1.0f - navigationProgress) * 5.0f);

        List<String> subcategories = ProVisualsScreen.categorySubcategories(activeCategory);
        int totalHeight = NAV_MAIN_HEIGHT + (subcategories.isEmpty() ? 0 : NAV_SUB_HEIGHT);
        navigationBounds = new Rect(x, baseY - slide, width, totalHeight + slide);

        int background = withAlpha(0xF0181726, navigationProgress);
        int border = withAlpha(0xFF5E4A83, navigationProgress);
        ProVisualsUi.fill(graphics, x, baseY - slide, width, totalHeight, 8, background);
        ProVisualsUi.fill(graphics, x + 7, baseY - slide, width - 14, 1, 1, border);

        List<String> categories = FeatureRegistry.CATEGORIES;
        int cellWidth = width / 4;
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            int row = i / 4;
            int col = i % 4;
            int cellX = x + col * cellWidth;
            int cellY = baseY + row * NAV_ROW_HEIGHT - slide;
            int actualWidth = col == 3 ? width - col * cellWidth : cellWidth;
            Rect rect = new Rect(cellX, cellY, actualWidth, NAV_ROW_HEIGHT);
            CATEGORY_HITS.add(new NavigationHit(category, rect));

            boolean selected = category.equals(activeCategory);
            if (selected) {
                ProVisualsUi.fill(graphics, cellX + 5, cellY + 2, actualWidth - 10,
                        NAV_ROW_HEIGHT - 4, 4, withAlpha(0xFF3B2D56, navigationProgress));
            }
            int labelColor = selected ? withAlpha(0xFFF5F1FF, navigationProgress)
                    : withAlpha(0xFFB7B0C5, navigationProgress);

            if ("Settings".equals(category)) {
                String label = navLabel(category);
                int gearSize = 13;
                int gap = 3;
                int totalWidth = gearSize + gap + font.width(label);
                int startX = cellX + Math.max(2, (actualWidth - totalWidth) / 2);
                drawSettingsGear(graphics, startX, cellY + 1, gearSize,
                        selected ? 0xFFE9DDFF : 0xFFB7B0C5,
                        selected ? 0xFF3B2D56 : 0xFF262333);
                ProVisualsUi.text(graphics, font, label, startX + gearSize + gap,
                        cellY + 4, labelColor, false);
            } else {
                graphics.centeredText(font, ProVisualsUi.component(navLabel(category)),
                        cellX + actualWidth / 2, cellY + 4, labelColor);
            }
        }

        if (!subcategories.isEmpty()) {
            int subY = baseY + NAV_MAIN_HEIGHT - slide;
            ProVisualsUi.fill(graphics, x + 8, subY, width - 16, 1, 1,
                    withAlpha(0x445E536F, navigationProgress));
            int subCellWidth = width / subcategories.size();
            for (int i = 0; i < subcategories.size(); i++) {
                String subcategory = subcategories.get(i);
                int cellX = x + i * subCellWidth;
                int actualWidth = i == subcategories.size() - 1
                        ? width - i * subCellWidth : subCellWidth;
                Rect rect = new Rect(cellX, subY + 1, actualWidth, NAV_SUB_HEIGHT - 1);
                SUBCATEGORY_HITS.add(new NavigationHit(subcategory, rect));

                boolean selected = subcategory.equals(activeSubcategory);
                if (selected) {
                    ProVisualsUi.fill(graphics, cellX + 5, subY + 3, actualWidth - 10,
                            NAV_SUB_HEIGHT - 5, 4, withAlpha(0xFF332747, navigationProgress));
                }
                int labelColor = selected ? withAlpha(0xFFDCCEFF, navigationProgress)
                        : withAlpha(0xFFAAA4BA, navigationProgress);
                graphics.centeredText(font, ProVisualsUi.component(navSubcategoryLabel(subcategory)),
                        cellX + actualWidth / 2, subY + 5, labelColor);
            }
        }
    }

    public static boolean handleMouseClick(double mouseX, double mouseY) {
        if (navigationProgress >= 0.55f && navigationBounds != null
                && navigationBounds.contains(mouseX, mouseY)) {
            for (NavigationHit hit : CATEGORY_HITS) {
                if (!hit.rect().contains(mouseX, mouseY)) continue;
                activeCategory = hit.value();
                List<String> subcategories = ProVisualsScreen.categorySubcategories(activeCategory);
                if (subcategories.isEmpty()) {
                    ProVisualsScreen.openFromMusicIsland(activeCategory, null);
                } else {
                    activeSubcategory = subcategories.get(0);
                }
                return true;
            }

            for (NavigationHit hit : SUBCATEGORY_HITS) {
                if (hit.rect().contains(mouseX, mouseY)) {
                    activeSubcategory = hit.value();
                    ProVisualsScreen.openFromMusicIsland(activeCategory, activeSubcategory);
                    return true;
                }
            }
            return true;
        }

        if (bounds == null || !bounds.contains(mouseX, mouseY)) return false;
        if (previousButton != null && previousButton.contains(mouseX, mouseY)) {
            MusicMediaControls.send(MusicMediaControls.Action.PREVIOUS);
            return true;
        }
        if (playButton != null && playButton.contains(mouseX, mouseY)) {
            MusicMediaControls.send(MusicMediaControls.Action.PLAY_PAUSE);
            return true;
        }
        if (nextButton != null && nextButton.contains(mouseX, mouseY)) {
            MusicMediaControls.send(MusicMediaControls.Action.NEXT);
            return true;
        }
        return false;
    }

    public static boolean handleChatClick(double mouseX, double mouseY) {
        return handleMouseClick(mouseX, mouseY);
    }

    private static void drawSettingsGear(GuiGraphicsExtractor graphics, int x, int y,
                                         int size, int color, int holeColor) {
        int offset = size - 5;
        int shadow = 0x66302A3F;
        ProVisualsUi.fill(graphics, x + 4, y + 1, 5, 4, 2, shadow);
        ProVisualsUi.fill(graphics, x + 4, y + size - 5, 5, 4, 2, shadow);
        ProVisualsUi.fill(graphics, x + 1, y + 4, 4, 5, 2, shadow);
        ProVisualsUi.fill(graphics, x + size - 5, y + 4, 4, 5, 2, shadow);
        ProVisualsUi.fill(graphics, x + 2, y + 2, 4, 4, 2, shadow);
        ProVisualsUi.fill(graphics, x + offset - 1, y + 2, 4, 4, 2, shadow);
        ProVisualsUi.fill(graphics, x + 2, y + offset - 1, 4, 4, 2, shadow);
        ProVisualsUi.fill(graphics, x + offset - 1, y + offset - 1, 4, 4, 2, shadow);
        ProVisualsUi.fill(graphics, x + 4, y, 5, 4, 2, color);
        ProVisualsUi.fill(graphics, x + 4, y + size - 4, 5, 4, 2, color);
        ProVisualsUi.fill(graphics, x, y + 4, 4, 5, 2, color);
        ProVisualsUi.fill(graphics, x + size - 4, y + 4, 4, 5, 2, color);
        ProVisualsUi.fill(graphics, x + 2, y + 2, 4, 4, 2, color);
        ProVisualsUi.fill(graphics, x + offset - 1, y + 2, 4, 4, 2, color);
        ProVisualsUi.fill(graphics, x + 2, y + offset - 1, 4, 4, 2, color);
        ProVisualsUi.fill(graphics, x + offset - 1, y + offset - 1, 4, 4, 2, color);
        ProVisualsUi.fill(graphics, x + 2, y + 2, size - 4, size - 4, 5, color);
        ProVisualsUi.fill(graphics, x + 5, y + 5, 3, 3, 2, holeColor);
    }

    private static String navLabel(String category) {
        return switch (category) {
            case "Overview" -> "Home";
            case "PvP" -> "PvP";
            case "Glowing" -> "Glowing";
            case "World" -> "World";
            case "HUD" -> "HUD";
            case "Music" -> "Music";
            case "Settings" -> "Settings";
            case "Tools" -> "Tools";
            default -> category;
        };
    }

    private static String navSubcategoryLabel(String subcategory) {
        return switch (subcategory) {
            case "Combat Visuals" -> "Combat";
            case "Player Visuals" -> "Player";
            case "World Visuals" -> "Environment";
            case "HUD & Interface" -> "HUD";
            case "Block Glow" -> "Blocks";
            case "Armor Glow" -> "Armor";
            case "Item Glow" -> "Items";
            case "Genres" -> "Genres";
            case "Lyrics" -> "Lyrics";
            case "Ore Glow" -> "Ore Glow";
            case "Error Center" -> "Errors";
            default -> subcategory;
        };
    }

    private static int withAlpha(int color, float opacity) {
        int alpha = color >>> 24;
        int faded = Math.max(0, Math.min(255, Math.round(alpha * opacity)));
        return (faded << 24) | (color & 0x00FFFFFF);
    }

    private static void drawAlbumArt(GuiGraphicsExtractor graphics, int x, int y, boolean playing) {
        ProVisualsUi.fill(graphics, x + 1, y + 2, 43, 43, 10, 0xFF08070D);
        ProVisualsUi.fill(graphics, x, y, 43, 43, 10, 0xFF28203E);
        ProVisualsUi.fill(graphics, x + 1, y + 1, 41, 41, 9, 0xFF33264F);
        ProVisualsUi.fill(graphics, x + 4, y + 4, 35, 35, 7, 0xFF211B34);

        int[] baseHeights = {7, 13, 19, 14, 9, 16, 7};
        int phase = (int) (System.currentTimeMillis() / 135L);
        int centerY = y + 22;
        for (int i = 0; i < baseHeights.length; i++) {
            int variation = playing ? (int) Math.round(Math.sin((phase + i * 2) * 0.65) * 4) : 0;
            int height = Math.max(4, Math.min(21, baseHeights[i] + variation));
            int barX = x + 8 + i * 4;
            ProVisualsUi.fill(graphics, barX, centerY - height / 2, 2, height, 1,
                    i == 3 ? 0xFFE0D2FF : 0xFF9A6BFF);
        }
        ProVisualsUi.fill(graphics, x + 7, y + 35, 29, 1, 1, 0x557D65B5);
    }

    private static void drawControl(GuiGraphicsExtractor graphics, Rect rect, String symbol, boolean primary) {
        int background = primary ? 0xFF6745A8 : 0xFF252231;
        int foreground = primary ? 0xFFFFFFFF : 0xFFD5CEE5;
        ProVisualsUi.fill(graphics, rect.x(), rect.y(), rect.width(), rect.height(), 6, background);
        if (primary) {
            ProVisualsUi.fill(graphics, rect.x() + 4, rect.y(), rect.width() - 8, 1, 1, 0xFFBCA4FF);
        }
        graphics.centeredText(Minecraft.getInstance().font, ProVisualsUi.component(symbol),
                rect.x() + rect.width() / 2, rect.y() + 4, foreground);
    }

    private static void clearMediaHitboxes() {
        previousButton = null;
        playButton = null;
        nextButton = null;
    }

    private static String trim(Font font, String value, int maxWidth) {
        String text = value == null || value.isBlank() ? "Unknown" : value;
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        while (!text.isEmpty() && font.width(text + ellipsis) > maxWidth) {
            int end = text.offsetByCodePoints(text.length(), -1);
            text = text.substring(0, end);
        }
        return text.isEmpty() ? ellipsis : text + ellipsis;
    }

    private static String formatTime(long milliseconds) {
        long seconds = Math.max(0L, milliseconds) / 1000L;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }
}
