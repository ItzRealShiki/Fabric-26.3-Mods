package de.itzrealshiki.provisuals;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class MusicIslandHud implements HudElement {
    private record Rect(int x, int y, int width, int height) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
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
        // Minecraft suppresses HUD extraction itself while the HUD is hidden (F1).
        if (minecraft.level == null) return;

        // Compact layout: substantially less screen space than the original 210 x 66 panel.
        int panelWidth = Math.min(184, graphics.guiWidth() - 16);
        if (panelWidth < 156) return;
        int panelHeight = 50;
        int x = graphics.guiWidth() - panelWidth - 8;
        int y = 10;
        bounds = new Rect(x, y, panelWidth, panelHeight);

        ProVisualsUi.fill(graphics, x + 2, y + 2, panelWidth, panelHeight, 8, 0x55000000);
        ProVisualsUi.fill(graphics, x, y, panelWidth, panelHeight, 8, 0xE90D0D18);
        ProVisualsUi.fill(graphics, x + 1, y + 9, 2, panelHeight - 18, 2, 0xFF9A6BFF);

        drawIsland(graphics, x + 8, y + 13);
        MusicLyricsBridge.Track track = MusicLyricsBridge.currentTrack();
        Font font = minecraft.font;
        String title = track == null ? "No music detected" : track.title();
        String artist = track == null ? "Start music on your device" : track.artist();
        int textWidth = panelWidth - 50;
        graphics.text(font, Component.literal(trim(font, title, textWidth)), x + 42, y + 6, 0xFFFFFFFF, true);
        graphics.text(font, Component.literal(trim(font, artist, textWidth)), x + 42, y + 17, 0xFFB7B2C5, false);

        int progressX = x + 42;
        int progressY = y + 28;
        int progressWidth = panelWidth - 50;
        ProVisualsUi.fill(graphics, progressX, progressY, progressWidth, 3, 2, 0xFF35313F);
        if (track != null && track.durationMs() > 0) {
            float fraction = Math.max(0f, Math.min(1f, track.positionMs() / (float) track.durationMs()));
            int filledWidth = Math.round(progressWidth * fraction);
            if (filledWidth > 0) {
                ProVisualsUi.fill(graphics, progressX, progressY, filledWidth, 3, 2, 0xFF9A6BFF);
            }
        }

        int buttonY = y + 34;
        previousButton = new Rect(x + 42, buttonY, 18, 12);
        playButton = new Rect(x + 64, buttonY, 22, 12);
        nextButton = new Rect(x + 90, buttonY, 18, 12);
        drawControl(graphics, previousButton, "<");
        drawControl(graphics, playButton, track != null && track.playing() ? "||" : ">");
        drawControl(graphics, nextButton, ">");
        String state = track == null ? "IDLE" : (track.playing() ? "PLAYING" : "PAUSED");
        graphics.text(font, Component.literal(state), x + 114, buttonY + 2,
                track != null && track.playing() ? 0xFFBCA4FF : 0xFF9F92B8, false);
    }

    private static void clearHitboxes() {
        bounds = null;
        previousButton = null;
        playButton = null;
        nextButton = null;
    }

    private static void drawIsland(GuiGraphicsExtractor graphics, int x, int y) {
        ProVisualsUi.fill(graphics, x, y, 24, 24, 6, 0xFF211B30);
        ProVisualsUi.fill(graphics, x + 4, y + 10, 3, 7, 2, 0xFF9A6BFF);
        ProVisualsUi.fill(graphics, x + 9, y + 6, 3, 15, 2, 0xFFBCA4FF);
        ProVisualsUi.fill(graphics, x + 14, y + 3, 3, 19, 2, 0xFF8D78D8);
        ProVisualsUi.fill(graphics, x + 19, y + 9, 2, 9, 1, 0xFF9A6BFF);
    }

    private static void drawControl(GuiGraphicsExtractor graphics, Rect rect, String label) {
        ProVisualsUi.fill(graphics, rect.x(), rect.y(), rect.width(), rect.height(), 4, 0xFF282334);
        ProVisualsUi.fill(graphics, rect.x() + 4, rect.y(), rect.width() - 8, 1, 1, 0xFF7555B6);
        graphics.centeredText(Minecraft.getInstance().font, Component.literal(label),
                rect.x() + rect.width() / 2, rect.y() + 2, 0xFFFFFFFF);
    }

    private static String trim(Font font, String value, int maxWidth) {
        String text = value == null || value.isBlank() ? "Unknown" : value;
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 2 && font.width(text + "...") > maxWidth) text = text.substring(0, text.length() - 1);
        return text + "...";
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
