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
        if (!ProVisualsConfig.musicIslandEnabled) { bounds = null; return; }
        Minecraft minecraft = Minecraft.getInstance();
        // Minecraft suppresses HUD extraction itself while the HUD is hidden (F1).
        if (minecraft.level == null) { bounds = null; return; }

        int panelWidth = Math.min(210, graphics.guiWidth() - 12);
        if (panelWidth < 150) return;
        int panelHeight = 66;
        int x = graphics.guiWidth() - panelWidth - 8;
        int y = 10;
        bounds = new Rect(x, y, panelWidth, panelHeight);

        graphics.fill(x + 2, y + 2, x + panelWidth + 2, y + panelHeight + 2, 0x55000000);
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xE90D0D18);
        graphics.fill(x, y, x + 2, y + panelHeight, 0xFF9A6BFF);
        graphics.fill(x + 8, y + panelHeight - 2, x + panelWidth - 8, y + panelHeight - 1, 0x557F5DCE);

        drawIsland(graphics, x + 8, y + 9);
        MusicLyricsBridge.Track track = MusicLyricsBridge.currentTrack();
        Font font = minecraft.font;
        String title = track == null ? "No music detected" : track.title();
        String artist = track == null ? "Start music on your device" : track.artist();
        graphics.text(font, Component.literal("MUSIC ISLAND"), x + 48, y + 6, 0xFFBCA4FF, true);
        graphics.text(font, Component.literal(trim(font, title, panelWidth - 58)), x + 48, y + 19, 0xFFFFFFFF, true);
        graphics.text(font, Component.literal(trim(font, artist, panelWidth - 58)), x + 48, y + 31, 0xFFB7B2C5, false);

        int progressX = x + 48;
        int progressY = y + 43;
        int progressWidth = panelWidth - 58;
        graphics.fill(progressX, progressY, progressX + progressWidth, progressY + 2, 0xFF35313F);
        if (track != null && track.durationMs() > 0) {
            float fraction = Math.max(0f, Math.min(1f, track.positionMs() / (float) track.durationMs()));
            graphics.fill(progressX, progressY, progressX + Math.round(progressWidth * fraction), progressY + 2, 0xFF9A6BFF);
        }

        int buttonY = y + 50;
        previousButton = new Rect(x + 48, buttonY, 25, 13);
        playButton = new Rect(x + 77, buttonY, 42, 13);
        nextButton = new Rect(x + 123, buttonY, 25, 13);
        drawControl(graphics, previousButton, "<<");
        drawControl(graphics, playButton, track != null && track.playing() ? "||" : ">");
        drawControl(graphics, nextButton, ">>");
        graphics.text(font, Component.literal(track == null ? "IDLE" : (track.playing() ? "PLAYING" : "PAUSED")),
                x + 155, buttonY + 3, 0xFF9F92B8, false);
    }

    private static void drawIsland(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x + 4, y, x + 29, y + 3, 0x667D5DCE);
        graphics.fill(x + 8, y + 3, x + 25, y + 7, 0xFF8D78D8);
        graphics.fill(x + 11, y + 7, x + 22, y + 11, 0xFF7757B8);
        graphics.fill(x + 14, y + 11, x + 19, y + 15, 0xFF5B4289);
        graphics.fill(x + 7, y + 3, x + 26, y + 5, 0xFF8CA66B);
        graphics.fill(x + 15, y - 3, x + 18, y, 0xFFBCA4FF);
        graphics.fill(x + 11, y - 1, x + 14, y + 1, 0x667F5DCE);
        graphics.fill(x + 20, y - 2, x + 23, y, 0x667F5DCE);
    }

    private static void drawControl(GuiGraphicsExtractor graphics, Rect rect, String label) {
        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), 0xFF282334);
        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + 1, 0xFF7555B6);
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
