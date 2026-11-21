package de.itzrealshiki.provisuals;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

public final class MusicIslandHud implements HudElement {
    private record Rect(int x, int y, int width, int height) {
        boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    private static volatile Rect previousButton;
    private static volatile Rect playButton;
    private static volatile Rect nextButton;
    private static volatile Rect bounds;

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        clearHitboxes();
        if (!ProVisualsConfig.musicIslandEnabled) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        int panelWidth = Math.min(278, graphics.guiWidth() - 20);
        if (panelWidth < 220) return;

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

        previousButton = new Rect(controlsX, y + 25, 18, 18);
        playButton = new Rect(controlsX + 22, y + 25, 25, 18);
        nextButton = new Rect(controlsX + 51, y + 25, 18, 18);
        drawControl(graphics, previousButton, "‹", false);
        drawControl(graphics, playButton, playing ? "Ⅱ" : "›", true);
        drawControl(graphics, nextButton, "›", false);

        int stateColor = playing ? 0xFFBCA4FF : 0xFF9A93A9;
        ProVisualsUi.fill(graphics, controlsX, y + 52, 69, 12, 5, 0xFF211D2D);
        String state = track == null ? "Idle" : (playing ? "Playing" : "Paused");
        graphics.centeredText(font, ProVisualsUi.component(state),
                controlsX + 34, y + 54, stateColor);
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

    private static void drawControl(GuiGraphicsExtractor graphics, Rect rect,
                                    String symbol, boolean primary) {
        int background = primary ? 0xFF6745A8 : 0xFF252231;
        int foreground = primary ? 0xFFFFFFFF : 0xFFD5CEE5;
        ProVisualsUi.fill(graphics, rect.x(), rect.y(), rect.width(), rect.height(), 6, background);
        if (primary) {
            ProVisualsUi.fill(graphics, rect.x() + 4, rect.y(), rect.width() - 8, 1, 1, 0xFFBCA4FF);
        }
        graphics.centeredText(Minecraft.getInstance().font, ProVisualsUi.component(symbol),
                rect.x() + rect.width() / 2, rect.y() + 4, foreground);
    }

    private static void clearHitboxes() {
        bounds = null;
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

    public static boolean handleChatClick(double mouseX, double mouseY) {
        if (bounds == null) return false;
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
}
