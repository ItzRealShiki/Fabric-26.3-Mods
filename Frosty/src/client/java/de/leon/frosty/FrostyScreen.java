package de.leon.frosty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Frosty's in-game control center.
 * Layout borrows OneClient's spacious top navigation, quiet surfaces and clear hierarchy,
 * adapted to Minecraft's screen/widget system.
 */
public final class FrostyScreen extends Screen {
    private enum Page { HOME, MODULES, SETTINGS, STATUS }
    private Page page = Page.HOME;
    private final List<EditBox> fields = new ArrayList<>();
    private String status = "READY";
    private int logScroll;
    private int panelX, panelY, panelW, panelH, contentX, contentW;

    private static final int BG = 0xF20B111B;
    private static final int SURFACE = 0xFF121E2B;
    private static final int CARD = 0xFF172635;
    private static final int CARD_ALT = 0xFF1B2D3D;
    private static final int BORDER = 0xFF2A4051;
    private static final int MUTED = 0xFF8EA5B5;
    private static final int TEXT = 0xFFF0F6FA;
    private static final int ACCENT = 0xFF65D8F5;
    private static final int GREEN = 0xFF75DDB8;
    private static final int RED = 0xFFFF7488;
    private static final int AMBER = 0xFFFFD17D;

    public FrostyScreen() { super(Component.literal("Frosty")); }
    private Component c(String en, String de) { return Component.literal(FrostyText.tr(en, de)); }

    @Override protected void init() {
        clearWidgets();
        fields.clear();
        panelW = Math.min(790, width - 28);
        panelH = Math.min(490, height - 24);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        contentX = panelX + 28;
        contentW = panelW - 56;

        int navY = panelY + 58;
        int navW = 116;
        int gap = 7;
        navButton("OVERVIEW", "ÜBERSICHT", Page.HOME, contentX, navY, navW);
        navButton("MODULES", "MODULE", Page.MODULES, contentX + navW + gap, navY, navW);
        navButton("SETTINGS", "EINSTELLUNGEN", Page.SETTINGS, contentX + (navW + gap) * 2, navY, navW);
        navButton("STATUS", "STATUS", Page.STATUS, contentX + (navW + gap) * 3, navY, navW);

        if (page == Page.HOME) buildHome();
        else if (page == Page.MODULES) buildModules();
        else if (page == Page.SETTINGS) buildSettings();
        else buildStatus();
    }

    private void navButton(String en, String de, Page target, int x, int y, int w) {
        String label = (page == target ? "•  " : "") + FrostyText.tr(en, de);
        addRenderableWidget(Button.builder(Component.literal(label), b -> open(target))
            .bounds(x, y, w, 25).build());
    }

    private void open(Page next) { page = next; logScroll = 0; status = "READY"; rebuildWidgets(); }

    private void buildHome() {
        int y = panelY + 231;
        addRenderableWidget(Button.builder(c("OPEN MODULES", "MODULE ÖFFNEN"), b -> open(Page.MODULES))
            .bounds(contentX + contentW - 150, y + 8, 150, 28).build());
        addRenderableWidget(Button.builder(c("CONFIGURE HUD", "HUD EINSTELLEN"), b -> open(Page.SETTINGS))
            .bounds(contentX, y + 185, 176, 27).build());
        addRenderableWidget(Button.builder(c("VIEW STATUS", "STATUS ANSEHEN"), b -> open(Page.STATUS))
            .bounds(contentX + 184, y + 185, 150, 27).build());
    }

    private void buildModules() {
        int y = panelY + 151;
        int gap = 12;
        int cardW = (contentW - gap) / 2;
        moduleToggle("SMALL TOTEM POPS", "KLEINE TOTEM-POPS",
            "REDUCED TOTEM POPUP SIZE", "KLEINERE TOTEM-ANIMATION",
            FrostyConfig.smallTotemPops, contentX, y, cardW, 90, 0);
        moduleToggle("FIXED SHIELDS", "FIXIERTE SCHILDE",
            "KEEP SHIELDS IN A CONSISTENT POSITION", "SCHILDE BLEIBEN AN FESTER POSITION",
            FrostyConfig.fixedShields, contentX + cardW + gap, y, cardW, 90, 1);
        moduleToggle("SHIELD STATUS", "SCHILD-STATUS",
            "LIVE SHIELD STATE INDICATOR", "LIVE-SCHILDSTATUS-ANZEIGE",
            FrostyConfig.shieldStatus, contentX, y + 102, cardW, 90, 2);
        addRenderableWidget(Button.builder(c("CUSTOMIZE SHIELD STATUS", "SCHILD-STATUS ANPASSEN"),
            b -> open(Page.SETTINGS)).bounds(contentX + cardW + gap, y + 132, cardW, 28).build());
    }

    private void moduleToggle(String en, String de, String descEn, String descDe,
                              boolean enabled, int x, int y, int w, int h, int index) {
        addRenderableWidget(Button.builder(c(enabled ? "ON  ✓" : "OFF  ○", enabled ? "AN  ✓" : "AUS  ○"),
            b -> {
                switch (index) {
                    case 0 -> FrostyConfig.smallTotemPops = !FrostyConfig.smallTotemPops;
                    case 1 -> FrostyConfig.fixedShields = !FrostyConfig.fixedShields;
                    default -> FrostyConfig.shieldStatus = !FrostyConfig.shieldStatus;
                }
                FrostyConfig.save();
                rebuildWidgets();
            }).bounds(x + w - 88, y + 48, 78, 25).build());
    }

    private void buildSettings() {
        int y = panelY + 151;
        addRenderableWidget(Button.builder(c("LANGUAGE  ·  " + (FrostyConfig.language.equals("de") ? "GERMAN" : "ENGLISH"),
                "SPRACHE  ·  " + (FrostyConfig.language.equals("de") ? "DEUTSCH" : "ENGLISCH")),
            b -> { FrostyConfig.language = FrostyConfig.language.equals("de") ? "en" : "de"; FrostyConfig.save(); rebuildWidgets(); })
            .bounds(contentX, y, 190, 25).build());
        addRenderableWidget(new FrostySlider(contentX, y + 42, contentW, 22,
            c("TOTEM POP SIZE", "TOTEM-GRÖSSE").getString(), FrostyConfig.totemScale, .25, 1.5,
            v -> { FrostyConfig.totemScale = v; FrostyConfig.save(); }));
        addRenderableWidget(new FrostySlider(contentX, y + 74, contentW, 22,
            c("SHIELD HUD SCALE", "SCHILD-HUD-GRÖSSE").getString(), FrostyConfig.hudScale, .5, 1.5,
            v -> { FrostyConfig.hudScale = v; FrostyConfig.save(); }));

        String[] en = {"BLOCKED", "NOT BLOCKING", "BROKEN", "BLOCKING"};
        String[] de = {"BLOCKIERT", "BLOCKIERT NICHT", "ZERBROCHEN", "BLOCKT"};
        String[] values = {FrostyConfig.blockedColor, FrostyConfig.notBlockingColor,
            FrostyConfig.brokenColor, FrostyConfig.blockingColor};
        for (int i = 0; i < 4; i++) {
            final int index = i;
            int rowY = y + 177 + i * 19;
            EditBox field = new EditBox(font, contentX, rowY, Math.max(90, contentW - 105), 21,
                c(en[i] + " COLOR", de[i] + " FARBE"));
            field.setMaxLength(7);
            field.setValue(values[i]);
            addRenderableWidget(field);
            fields.add(field);
            addRenderableWidget(Button.builder(c("APPLY", "ANWENDEN"), b -> saveColor(index))
                .bounds(contentX + contentW - 96, rowY, 96, 21).build());
        }
        addRenderableWidget(Button.builder(c("RESET COLORS", "FARBEN ZURÜCKSETZEN"), b -> {
            FrostyConfig.blockedColor = "#58D7FF";
            FrostyConfig.notBlockingColor = "#A8C7D8";
            FrostyConfig.brokenColor = "#FF667A";
            FrostyConfig.blockingColor = "#FFFFFF";
            FrostyConfig.save();
            rebuildWidgets();
        }).bounds(contentX, y + 260, 160, 23).build());
    }

    private void buildStatus() {
        int y = panelY + 123;
        addRenderableWidget(Button.builder(c("CLEAR", "LEEREN"), b -> {
            FrostyLog.clear(); logScroll = 0; rebuildWidgets();
        }).bounds(contentX, y, 82, 23).build());
        addRenderableWidget(Button.builder(c("OPEN LOG FOLDER", "LOG-ORDNER ÖFFNEN"), b -> FrostyLog.openLogFolder())
            .bounds(contentX + 88, y, 150, 23).build());
        addRenderableWidget(Button.builder(c("REFRESH", "AKTUALISIEREN"), b -> rebuildWidgets())
            .bounds(contentX + 244, y, 105, 23).build());
        addRenderableWidget(Button.builder(c("OLDER", "ÄLTER"), b -> {
            logScroll = Math.min(Math.max(0, FrostyLog.recent(150).size() - 1), logScroll + 8); rebuildWidgets();
        }).bounds(contentX, y + 30, 82, 22).build());
        addRenderableWidget(Button.builder(c("NEWER", "NEUER"), b -> {
            logScroll = Math.max(0, logScroll - 8); rebuildWidgets();
        }).bounds(contentX + 88, y + 30, 82, 22).build());
        addRenderableWidget(Button.builder(c("COPY ALL", "ALLE KOPIEREN"), b -> {
            String all = FrostyLog.recent(150).stream().map(e -> e.time() + " [" + e.level() + "] " + e.message())
                .reduce("", (a, v) -> a + v + "\n");
            Minecraft.getInstance().keyboardHandler.setClipboard(all);
            status = "LOG COPIED";
        }).bounds(contentX + 176, y + 30, 110, 22).build());
    }

    private void saveColor(int i) {
        String value = fields.get(i).getValue().trim().toUpperCase();
        if (!value.matches("#[0-9A-F]{6}")) { status = "USE HEX FORMAT: #58D7FF"; return; }
        switch (i) {
            case 0 -> FrostyConfig.blockedColor = value;
            case 1 -> FrostyConfig.notBlockingColor = value;
            case 2 -> FrostyConfig.brokenColor = value;
            case 3 -> FrostyConfig.blockingColor = value;
        }
        FrostyConfig.save();
        status = "COLOR SAVED";
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xB500080F);
        int x = panelX, y = panelY, w = panelW, h = panelH;
        g.fill(x + 5, y + 7, x + w + 5, y + h + 7, 0x65000000);
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w, y + 2, ACCENT);
        g.fill(x + 1, y + 1, x + w - 1, y + 53, SURFACE);
        g.fill(x + 28, y + 52, x + w - 28, y + 53, BORDER);

        // Header / brand block
        g.text(font, Component.literal("FROSTY"), x + 28, y + 17, TEXT);
        g.text(font, Component.literal(FrostyText.sc("MINECRAFT CLIENT")), x + 103, y + 20, MUTED);
        g.text(font, Component.literal("0.1.2"), x + w - 62, y + 20, ACCENT);

        int navX = contentX;
        int navY = y + 58;
        for (int i = 0; i < 4; i++) {
            int bx = navX + i * 123;
            g.fill(bx, navY, bx + 116, navY + 25, page.ordinal() == i ? 0xFF1C3B4C : SURFACE);
            if (page.ordinal() == i) g.fill(bx, navY + 23, bx + 116, navY + 25, ACCENT);
        }

        String heading = switch (page) {
            case HOME -> FrostyText.tr("WELCOME BACK", "WILLKOMMEN ZURÜCK");
            case MODULES -> FrostyText.tr("YOUR MODULES", "DEINE MODULE");
            case SETTINGS -> FrostyText.tr("PREFERENCES", "EINSTELLUNGEN");
            case STATUS -> FrostyText.tr("SYSTEM STATUS", "SYSTEMSTATUS");
        };
        String subtitle = switch (page) {
            case HOME -> FrostyText.tr("A clean control center for your Minecraft experience.", "Deine zentrale Steuerung für Minecraft.");
            case MODULES -> FrostyText.tr("Enable the features you want to use.", "Aktiviere die Funktionen, die du nutzen möchtest.");
            case SETTINGS -> FrostyText.tr("Personalize the Frosty experience.", "Passe Frosty an deine Wünsche an.");
            case STATUS -> FrostyText.tr("Recent events captured by the Frosty monitor.", "Aktuelle Ereignisse des Frosty-Monitors.");
        };
        int top = y + 108;
        g.text(font, Component.literal(FrostyText.sc(heading)), contentX, top, TEXT);
        g.text(font, Component.literal(subtitle), contentX, top + 17, MUTED);

        if (page == Page.HOME) drawHome(g, contentX, top + 43, contentW);
        else if (page == Page.MODULES) drawModules(g, contentX, top + 43, contentW);
        else if (page == Page.SETTINGS) drawSettings(g, contentX, top + 43, contentW);
        else drawStatus(g, contentX, top + 43, contentW);

        g.fill(contentX, y + h - 34, x + w - 28, y + h - 33, BORDER);
        g.text(font, Component.literal(FrostyText.sc("RIGHT SHIFT  ·  OPEN MENU")), contentX, y + h - 22, MUTED);
        g.text(font, Component.literal(FrostyText.sc(status)), x + w - 180, y + h - 22, ACCENT);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawHome(GuiGraphicsExtractor g, int x, int y, int w) {
        int half = (w - 12) / 2;
        card(g, x, y, w, 118);
        g.text(font, Component.literal(FrostyText.sc("FROSTY CONTROL CENTER")), x + 18, y + 17, ACCENT);
        g.text(font, Component.literal(FrostyText.tr("Manage modules, appearance and diagnostics.", "Verwalte Module, Design und Fehlerdiagnose.")), x + 18, y + 39, TEXT);
        g.text(font, Component.literal(FrostyText.tr("Your settings are saved automatically.", "Deine Einstellungen werden automatisch gespeichert.")), x + 18, y + 58, MUTED);
        int y2 = y + 132;
        statCard(g, x, y2, half, 78, "ACTIVE MODULES", "AKTIVE MODULE", activeModules() + " / 3", GREEN);
        statCard(g, x + half + 12, y2, half, 78, "LOG ENTRIES", "LOG-EINTRÄGE",
            String.valueOf(FrostyLog.recent(150).size()), ACCENT);
    }

    private int activeModules() {
        return (FrostyConfig.smallTotemPops ? 1 : 0) + (FrostyConfig.fixedShields ? 1 : 0) + (FrostyConfig.shieldStatus ? 1 : 0);
    }

    private void drawModules(GuiGraphicsExtractor g, int x, int y, int w) {
        int gap = 12, cw = (w - gap) / 2;
        moduleCard(g, x, y, cw, 90, "SMALL TOTEM POPS", "KLEINE TOTEM-POPS",
            "REDUCED TOTEM POPUP SIZE", "KLEINERE TOTEM-ANIMATION", FrostyConfig.smallTotemPops);
        moduleCard(g, x + cw + gap, y, cw, 90, "FIXED SHIELDS", "FIXIERTE SCHILDE",
            "CONSISTENT SHIELD POSITION", "KONSTANTE SCHILD-POSITION", FrostyConfig.fixedShields);
        moduleCard(g, x, y + 102, cw, 90, "SHIELD STATUS", "SCHILD-STATUS",
            "LIVE SHIELD STATE INDICATOR", "LIVE-SCHILDSTATUS-ANZEIGE", FrostyConfig.shieldStatus);
        card(g, x + cw + gap, y + 102, cw, 90);
        g.text(font, Component.literal(FrostyText.sc("SHIELD APPEARANCE")), x + cw + gap + 14, y + 120, TEXT);
        g.text(font, Component.literal(FrostyText.tr("Change colors and HUD scale", "Farben und HUD-Größe ändern")), x + cw + gap + 14, y + 141, MUTED);
    }

    private void moduleCard(GuiGraphicsExtractor g, int x, int y, int w, int h,
                            String en, String de, String descEn, String descDe, boolean enabled) {
        card(g, x, y, w, h);
        g.fill(x + 12, y + 15, x + 15, y + 39, enabled ? GREEN : MUTED);
        g.text(font, Component.literal(FrostyText.tr(en, de)), x + 25, y + 15, TEXT);
        g.text(font, Component.literal(FrostyText.tr(descEn, descDe)), x + 25, y + 35, MUTED);
        g.text(font, Component.literal(enabled ? "●  ON" : "○  OFF"), x + w - 54, y + 62, enabled ? GREEN : MUTED);
    }

    private void drawSettings(GuiGraphicsExtractor g, int x, int y, int w) {
        card(g, x, y, w, 257);
        g.text(font, Component.literal(FrostyText.sc("GENERAL & APPEARANCE")), x + 14, y + 13, ACCENT);
        g.text(font, Component.literal(FrostyText.tr("Language", "Sprache")), x + 14, y + 45, TEXT);
        g.text(font, Component.literal(FrostyText.tr("Totem size", "Totem-Größe")), x + 14, y + 88, TEXT);
        g.text(font, Component.literal(FrostyText.tr("Shield HUD scale", "Schild-HUD-Größe")), x + 14, y + 120, TEXT);
        g.text(font, Component.literal(FrostyText.sc("SHIELD STATUS COLORS  ·  HEX")), x + 14, y + 157, ACCENT);
        String[] labels = {"BLOCKED", "NOT BLOCKING", "BROKEN", "BLOCKING"};
        String[] values = {FrostyConfig.blockedColor, FrostyConfig.notBlockingColor, FrostyConfig.brokenColor, FrostyConfig.blockingColor};
        for (int i = 0; i < 4; i++) {
            int rowY = y + 177 + i * 19;
            g.fill(x + 14, rowY + 3, x + 23, rowY + 12, FrostyConfig.color(values[i], ACCENT));
            g.text(font, Component.literal(FrostyText.tr(labels[i], new String[]{"BLOCKIERT","BLOCKIERT NICHT","ZERBROCHEN","BLOCKT"}[i])), x + 31, rowY, TEXT);
            g.text(font, Component.literal(values[i]), x + w - 68, rowY, MUTED);
        }
    }

    private void drawStatus(GuiGraphicsExtractor g, int x, int y, int w) {
        int errors = FrostyLog.count(FrostyLog.LevelType.ERROR);
        int warnings = FrostyLog.count(FrostyLog.LevelType.WARNING);
        int entriesCount = FrostyLog.recent(150).size();
        int half = (w - 16) / 3;
        statCard(g, x, y, half, 54, "ERRORS", "FEHLER", String.valueOf(errors), errors > 0 ? RED : GREEN);
        statCard(g, x + half + 8, y, half, 54, "WARNINGS", "WARNUNGEN", String.valueOf(warnings), warnings > 0 ? AMBER : GREEN);
        statCard(g, x + (half + 8) * 2, y, half, 54, "ENTRIES", "EINTRÄGE", String.valueOf(entriesCount), ACCENT);
        card(g, x, y + 64, w, 178);
        List<FrostyLog.Entry> entries = FrostyLog.recent(150);
        int first = Math.min(logScroll, Math.max(0, entries.size() - 1));
        for (int i = first; i < Math.min(entries.size(), first + 8); i++) {
            FrostyLog.Entry e = entries.get(i);
            int color = e.level() == FrostyLog.LevelType.ERROR ? RED :
                e.level() == FrostyLog.LevelType.WARNING ? AMBER : ACCENT;
            String line = e.time() + "  [" + e.level() + "]  " + e.message().replace('\n', ' ');
            if (line.length() > 100) line = line.substring(0, 97) + "...";
            g.text(font, Component.literal(FrostyText.sc(line)), x + 12, y + 76 + (i - first) * 18, color);
        }
        if (entries.isEmpty()) g.text(font, c("NO LOG ENTRIES YET", "NOCH KEINE LOG-EINTRÄGE"), x + 12, y + 80, MUTED);
    }

    private void statCard(GuiGraphicsExtractor g, int x, int y, int w, int h,
                          String en, String de, String value, int color) {
        card(g, x, y, w, h);
        g.text(font, Component.literal(FrostyText.tr(en, de)), x + 12, y + 12, MUTED);
        g.text(font, Component.literal(value), x + 12, y + 34, color);
    }

    private void card(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, CARD);
        g.fill(x, y, x + w, y + 1, BORDER);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);
        g.fill(x, y, x + 1, y + h, BORDER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    private static final class FrostySlider extends AbstractSliderButton {
        private final String label;
        private final double min, max;
        private final java.util.function.DoubleConsumer changed;
        FrostySlider(int x, int y, int w, int h, String label, double value, double min, double max,
                     java.util.function.DoubleConsumer changed) {
            super(x, y, w, h, Component.empty(), (value - min) / (max - min));
            this.label = label; this.min = min; this.max = max; this.changed = changed; updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Component.literal(FrostyText.sc(label + "  " + Math.round((min + value * (max - min)) * 100) + "%")));
        }
        @Override protected void applyValue() { changed.accept(min + value * (max - min)); }
    }
}
