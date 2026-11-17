package de.leon.frosty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FrostyHudEditor extends Screen {
    private static final int PANEL = 0xE0101010, CARD = 0xFF555555, BORDER = 0xFF202020;
    private static final int TEXT = 0xFFFFFFFF, MUTED = 0xFFCCCCCC, ACCENT = 0xFFFFFFFF;
    private boolean dragging;
    private int dragOffsetX, dragOffsetY, panelX, panelY, panelW, panelH, previewW, previewH;

    public FrostyHudEditor() { super(Component.literal("Frosty HUD Editor")); }

    @Override protected void init() {
        panelW = Math.min(560, width - 32);
        panelH = 190;
        panelX = (width - panelW) / 2;
        panelY = height - panelH - 22;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x52030A11);
        rounded(g, panelX + 4, panelY + 6, panelW, panelH, 20, 0x66000000);
        rounded(g, panelX, panelY, panelW, panelH, 20, PANEL);
        rounded(g, panelX + 1, panelY + 1, panelW - 2, 3, 2, ACCENT);
        g.text(font, Component.literal("FROSTY  /  HUD EDITOR"), panelX + 22, panelY + 18, TEXT);
        g.text(font, Component.literal(FrostyText.tr("Drag the HUD element to move it", "Ziehe das HUD-Element, um es zu verschieben")),
            panelX + 22, panelY + 39, MUTED);

        String label = FrostyText.sc(FrostyText.tr("NOT BLOCKING", "BLOCKIERT NICHT"));
        previewW = font.width(label) + 22;
        previewH = 26;
        float scale = (float) FrostyConfig.hudScale;
        int w = Math.round(previewW * scale), h = Math.round(previewH * scale);
        int px = FrostyConfig.shieldX, py = FrostyConfig.shieldY;
        rounded(g, px - 3, py - 3, w + 6, h + 6, 10, 0xAA68DDF7);
        rounded(g, px, py, w, h, 8, 0xF20A1A29);
        rounded(g, px, py, 3, h, 2, ACCENT);
        g.text(font, Component.literal(label), px + 11, py + 8, 0xFFA8C7D8);

        int controlsY = panelY + 76;
        g.text(font, Component.literal(FrostyText.tr("SIZE", "GRÖSSE")), panelX + 22, controlsY + 9, MUTED);
        drawButton(g, panelX + 82, controlsY, 34, 30, "−", mouseX, mouseY);
        drawButton(g, panelX + 122, controlsY, 34, 30, "+", mouseX, mouseY);
        g.text(font, Component.literal(Math.round(FrostyConfig.hudScale * 100) + "%"), panelX + 166, controlsY + 10, ACCENT);
        drawButton(g, panelX + 216, controlsY, 104, 30, FrostyText.tr("RESET", "ZURÜCKSETZEN"), mouseX, mouseY);
        drawButton(g, panelX + (panelW - 204) / 2, panelY + 122, 204, 40,
            FrostyText.tr("OPEN MAIN MENU", "HAUPTMENÜ ÖFFNEN"), mouseX, mouseY, true);
        g.text(font, Component.literal(FrostyText.tr("Right Shift  ·  HUD editor", "Rechte Shift  ·  HUD-Editor")),
            panelX + 22, panelY + panelH - 17, MUTED);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, int mx, int my) {
        drawButton(g, x, y, w, h, text, mx, my, false);
    }
    private void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, int mx, int my, boolean primary) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        rounded(g, x, y, w, h, 11, primary ? (hover ? 0xFF777777 : 0xFF555555) : (hover ? 0xFF777777 : CARD));
        rounded(g, x, y, w, 2, 1, primary ? 0xFF9CEBFF : BORDER);
        g.centeredText(font, Component.literal(text), x + w / 2, y + (h - 9) / 2, primary || hover ? TEXT : MUTED);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int px = FrostyConfig.shieldX, py = FrostyConfig.shieldY;
        int w = Math.round(previewW * (float) FrostyConfig.hudScale), h = Math.round(previewH * (float) FrostyConfig.hudScale);
        if (mouseX >= px - 5 && mouseX <= px + w + 5 && mouseY >= py - 5 && mouseY <= py + h + 5) {
            dragging = true; dragOffsetX = (int) mouseX - px; dragOffsetY = (int) mouseY - py; return true;
        }
        int controlsY = panelY + 76;
        if (inside(mouseX, mouseY, panelX + 82, controlsY, 34, 30)) {
            FrostyConfig.hudScale = FrostyConfig.clamp(FrostyConfig.hudScale - 0.1, 0.5, 2.0); FrostyConfig.save(); return true;
        }
        if (inside(mouseX, mouseY, panelX + 122, controlsY, 34, 30)) {
            FrostyConfig.hudScale = FrostyConfig.clamp(FrostyConfig.hudScale + 0.1, 0.5, 2.0); FrostyConfig.save(); return true;
        }
        if (inside(mouseX, mouseY, panelX + 216, controlsY, 104, 30)) {
            FrostyConfig.shieldX = 12; FrostyConfig.shieldY = 12; FrostyConfig.hudScale = 1.0; FrostyConfig.save(); return true;
        }
        int mainX = panelX + (panelW - 204) / 2;
        if (inside(mouseX, mouseY, mainX, panelY + 122, 204, 40)) {
            Minecraft.getInstance().gui.setScreen(new FrostyScreen()); return true;
        }
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x(), mouseY = event.y();
        if (!dragging || event.button() != 0) return super.mouseDragged(event, dragX, dragY);
        FrostyConfig.shieldX = Math.max(0, Math.min(width - 50, (int) mouseX - dragOffsetX));
        FrostyConfig.shieldY = Math.max(0, Math.min(height - 30, (int) mouseY - dragOffsetY));
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && dragging) { dragging = false; FrostyConfig.save(); return true; }
        return super.mouseReleased(event);
    }

    @Override public boolean isPauseScreen() { return false; }
    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    private void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        g.fill(x, y, x + w, y + h, color);
    }
}
