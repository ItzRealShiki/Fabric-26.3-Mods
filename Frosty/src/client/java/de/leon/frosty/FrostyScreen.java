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

public final class FrostyScreen extends Screen {
    private enum Page { MODULES, SETTINGS, STATUS }
    private Page page = Page.MODULES;
    private final List<EditBox> fields = new ArrayList<>();
    private String status = "READY";
    private int logScroll;
    private int panelX, panelY, contentX, contentW;

    public FrostyScreen() { super(Component.literal("Frosty")); }
    private Component c(String en, String de) { return Component.literal(FrostyText.tr(en, de)); }

    @Override protected void init() {
        clearWidgets();
        fields.clear();
        int panelW = Math.min(660, width - 24);
        int panelH = Math.min(390, height - 24);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        contentX = panelX + 154;
        contentW = panelW - 178;
        int navX = panelX + 14;
        int navY = panelY + 76;

        navButton("HOME", "HOME", Page.MODULES, navX, navY);
        navButton("SETTINGS", "EINSTELLUNGEN", Page.SETTINGS, navX, navY + 38);
        navButton("LOGS", "PROTOKOLLE", Page.STATUS, navX, navY + 76);
        addRenderableWidget(Button.builder(c("CLOSE", "SCHLIESSEN"), b -> onClose())
            .bounds(navX, panelY + panelH - 42, 126, 25).build());

        if (page == Page.MODULES) buildModules();
        else if (page == Page.SETTINGS) buildSettings();
        else buildStatus();
    }

    private void navButton(String en, String de, Page target, int x, int y) {
        String label = (page == target ? "●  " : "   ") + FrostyText.tr(en, de);
        addRenderableWidget(Button.builder(Component.literal(label), b -> open(target))
            .bounds(x, y, 126, 29).build());
    }

    private void buildModules() {
        int y = panelY + 105;
        moduleCard("SMALL TOTEM POPS", "KLEINE TOTEM-POPS",
            "REDUCED TOTEM POPUP SIZE", "KLEINERE TOTEM-ANIMATION",
            FrostyConfig.smallTotemPops, contentX, y);
        moduleCard("FIXED SHIELDS", "FIXIERTE SCHILDE",
            "KEEP SHIELDS IN A CONSISTENT POSITION", "SCHILDE BLEIBEN AN EINER FESTEN POSITION",
            FrostyConfig.fixedShields, contentX, y + 76);
        moduleCard("SHIELD STATUS", "SCHILD-STATUS",
            "DISPLAY YOUR CURRENT SHIELD STATE", "ZEIGE DEN AKTUELLEN SCHILD-STATUS",
            FrostyConfig.shieldStatus, contentX, y + 152);
        addRenderableWidget(Button.builder(c("CONFIGURE SHIELD HUD", "SCHILD-HUD EINSTELLEN"),
            b -> open(Page.SETTINGS)).bounds(contentX, y + 230, contentW, 26).build());
    }

    private void moduleCard(String en, String de, String descEn, String descDe,
                            boolean enabled, int x, int y) {
        addRenderableWidget(Button.builder(c(enabled ? "ENABLED  ✓" : "DISABLED  ○",
                enabled ? "AKTIVIERT  ✓" : "DEAKTIVIERT  ○"), b -> {
            switch (en) {
                case "SMALL TOTEM POPS" -> FrostyConfig.smallTotemPops = !FrostyConfig.smallTotemPops;
                case "FIXED SHIELDS" -> FrostyConfig.fixedShields = !FrostyConfig.fixedShields;
                case "SHIELD STATUS" -> FrostyConfig.shieldStatus = !FrostyConfig.shieldStatus;
            }
            FrostyConfig.save();
            rebuildWidgets();
        }).bounds(x + contentW - 112, y + 18, 106, 25).build());
    }

    private void buildSettings() {
        int y = panelY + 91;
        addRenderableWidget(Button.builder(c("LANGUAGE  ·  " + (FrostyConfig.language.equals("de") ? "GERMAN" : "ENGLISH"),
                "SPRACHE  ·  " + (FrostyConfig.language.equals("de") ? "DEUTSCH" : "ENGLISCH")),
            b -> { FrostyConfig.language = FrostyConfig.language.equals("de") ? "en" : "de"; FrostyConfig.save(); rebuildWidgets(); })
            .bounds(contentX, y, contentW, 25).build());
        addRenderableWidget(new FrostySlider(contentX, y + 40, contentW, 22,
            c("TOTEM POP SIZE", "TOTEM-GROESSE").getString(), FrostyConfig.totemScale, .25, 1.5,
            v -> { FrostyConfig.totemScale = v; FrostyConfig.save(); }));
        addRenderableWidget(new FrostySlider(contentX, y + 70, contentW, 22,
            c("SHIELD HUD SCALE", "SCHILD-HUD-GROESSE").getString(), FrostyConfig.hudScale, .5, 1.5,
            v -> { FrostyConfig.hudScale = v; FrostyConfig.save(); }));

        String[] en = {"BLOCKED", "NOT BLOCKING", "BROKEN", "BLOCKING"};
        String[] de = {"BLOCKIERT", "BLOCKIERT NICHT", "ZERBROCHEN", "BLOCKT"};
        String[] values = {FrostyConfig.blockedColor, FrostyConfig.notBlockingColor,
            FrostyConfig.brokenColor, FrostyConfig.blockingColor};
        for (int i = 0; i < 4; i++) {
            final int index = i;
            int rowY = y + 105 + i * 29;
            EditBox field = new EditBox(font, contentX, rowY, Math.max(90, contentW - 100), 21,
                c(en[i] + " COLOR", de[i] + " FARBE"));
            field.setMaxLength(7);
            field.setValue(values[i]);
            addRenderableWidget(field);
            fields.add(field);
            addRenderableWidget(Button.builder(c("APPLY", "ANWENDEN"), b -> saveColor(index))
                .bounds(contentX + contentW - 91, rowY, 91, 21).build());
        }
        addRenderableWidget(Button.builder(c("RESET COLORS", "FARBEN ZURUECKSETZEN"), b -> {
            FrostyConfig.blockedColor = "#58D7FF";
            FrostyConfig.notBlockingColor = "#A8C7D8";
            FrostyConfig.brokenColor = "#FF667A";
            FrostyConfig.blockingColor = "#FFFFFF";
            FrostyConfig.save();
            rebuildWidgets();
        }).bounds(contentX, y + 225, contentW, 24).build());
    }

    private void buildStatus() {
        int y = panelY + 92;
        addRenderableWidget(Button.builder(c("CLEAR LOG", "LOG LEEREN"), b -> {
            FrostyLog.clear(); logScroll = 0; rebuildWidgets();
        }).bounds(contentX, y, 90, 24).build());
        addRenderableWidget(Button.builder(c("OPEN LOG FOLDER", "LOG-ORDNER OEFFNEN"), b -> FrostyLog.openLogFolder())
            .bounds(contentX + 96, y, 142, 24).build());
        addRenderableWidget(Button.builder(c("REFRESH", "AKTUALISIEREN"), b -> rebuildWidgets())
            .bounds(contentX + 244, y, Math.max(70, contentW - 244), 24).build());
        addRenderableWidget(Button.builder(c("OLDER", "AELTER"), b -> {
            logScroll = Math.min(Math.max(0, FrostyLog.recent(150).size() - 1), logScroll + 7); rebuildWidgets();
        }).bounds(contentX, y + 31, 82, 22).build());
        addRenderableWidget(Button.builder(c("NEWER", "NEUER"), b -> {
            logScroll = Math.max(0, logScroll - 7); rebuildWidgets();
        }).bounds(contentX + 88, y + 31, 82, 22).build());
        addRenderableWidget(Button.builder(c("COPY ALL", "ALLE KOPIEREN"), b -> {
            String all = FrostyLog.recent(150).stream().map(e -> e.time() + " [" + e.level() + "] " + e.message())
                .reduce("", (a, v) -> a + v + "\n");
            Minecraft.getInstance().keyboardHandler.setClipboard(all);
            status = "LOG COPIED";
        }).bounds(contentX + 176, y + 31, 110, 22).build());
        int errors = FrostyLog.count(FrostyLog.LevelType.ERROR);
        int warnings = FrostyLog.count(FrostyLog.LevelType.WARNING);
        status = "ERRORS: " + errors + "   WARNINGS: " + warnings + "   ENTRIES: " + FrostyLog.recent(150).size();
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

    private void open(Page next) { page = next; logScroll = 0; status = "READY"; rebuildWidgets(); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xB900070E);
        int w = Math.min(660, width - 24), h = Math.min(390, height - 24);
        int x = (width - w) / 2, y = (height - h) / 2;
        g.fill(x + 5, y + 7, x + w + 5, y + h + 7, 0x55000000);
        g.fill(x, y, x + w, y + h, 0xF20D1722);
        g.fill(x, y, x + 146, y + h, 0xFF101D2A);
        g.fill(x, y, x + w, y + 2, 0xFF61D9FF);
        g.fill(x + 145, y + 1, x + 146, y + h - 1, 0xFF263B4A);
        g.text(font, Component.literal("FROSTY"), x + 19, y + 22, 0xFFFFFFFF);
        g.text(font, Component.literal("CLIENT"), x + 20, y + 39, 0xFF6DCFEF);
        g.fill(x + 15, y + 58, x + 131, y + 59, 0xFF2B4658);
        String heading = switch (page) {
            case MODULES -> FrostyText.tr("OVERVIEW", "UEBERSICHT");
            case SETTINGS -> FrostyText.tr("PREFERENCES", "EINSTELLUNGEN");
            case STATUS -> FrostyText.tr("SYSTEM LOGS", "SYSTEM-LOGS");
        };
        g.text(font, Component.literal(heading), x + 164, y + 23, 0xFFF1F7FA);
        g.text(font, Component.literal(FrostyText.sc("MINECRAFT CLIENT MOD")), x + 164, y + 41, 0xFF8298A8);

        int cx = x + 164, cy = y + 78, cw = w - 184;
        if (page == Page.MODULES) {
            card(g, cx, cy, cw, 66);
            card(g, cx, cy + 76, cw, 66);
            card(g, cx, cy + 152, cw, 66);
            drawModuleText(g, cx, cy + 18, "SMALL TOTEM POPS", "KLEINE TOTEM-POPS",
                "REDUCED TOTEM POPUP SIZE", "KLEINERE TOTEM-ANIMATION");
            drawModuleText(g, cx, cy + 94, "FIXED SHIELDS", "FIXIERTE SCHILDE",
                "CONSISTENT SHIELD POSITION", "KONSTANTE SCHILD-POSITION");
            drawModuleText(g, cx, cy + 170, "SHIELD STATUS", "SCHILD-STATUS",
                "LIVE SHIELD STATE INDICATOR", "LIVE-SCHILDSTATUS-ANZEIGE");
            g.text(font, Component.literal(FrostyText.sc("ACTIVE MODULES")), cx, cy - 15, 0xFF8EA9B9);
        } else if (page == Page.SETTINGS) {
            g.text(font, Component.literal(FrostyText.sc("GENERAL & APPEARANCE")), cx, cy - 15, 0xFF8EA9B9);
            card(g, cx, cy, cw, 257);
        } else {
            g.text(font, Component.literal(FrostyText.sc("RECENT GAME EVENTS")), cx, cy - 15, 0xFF8EA9B9);
            card(g, cx, cy, cw, 257);
            List<FrostyLog.Entry> entries = FrostyLog.recent(150);
            int first = Math.min(logScroll, Math.max(0, entries.size() - 1));
            for (int i = first; i < Math.min(entries.size(), first + 10); i++) {
                FrostyLog.Entry e = entries.get(i);
                int color = e.level() == FrostyLog.LevelType.ERROR ? 0xFFFF7185 :
                    e.level() == FrostyLog.LevelType.WARNING ? 0xFFFFD27A : 0xFF9DEBFF;
                String line = e.time() + "  [" + e.level() + "]  " + e.message().replace('\n', ' ');
                if (line.length() > 75) line = line.substring(0, 72) + "...";
                g.text(font, Component.literal(FrostyText.sc(line)), cx + 12, cy + 12 + (i - first) * 18, color);
            }
            if (entries.isEmpty()) g.text(font, c("NO LOG ENTRIES YET", "NOCH KEINE LOG-EINTRAEGE"), cx + 12, cy + 16, 0xFF9DEBFF);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.text(font, Component.literal(FrostyText.sc(status)), x + 164, y + h - 20, 0xFF6F9DB2);
        g.text(font, Component.literal("FROSTY  /  0.1.0"), x + 18, y + h - 20, 0xFF637D8D);
    }

    private void card(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF142432);
        g.fill(x, y, x + w, y + 1, 0xFF294354);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF203747);
    }

    private void drawModuleText(GuiGraphicsExtractor g, int x, int y, String en, String de,
                                String descEn, String descDe) {
        String title = FrostyText.tr(en, de);
        String desc = FrostyText.tr(descEn, descDe);
        g.text(font, Component.literal(title), x + 13, y, 0xFFEAF5FA);
        g.text(font, Component.literal(desc), x + 13, y + 17, 0xFF89A5B5);
        boolean enabled = switch (en) {
            case "SMALL TOTEM POPS" -> FrostyConfig.smallTotemPops;
            case "FIXED SHIELDS" -> FrostyConfig.fixedShields;
            default -> FrostyConfig.shieldStatus;
        };
        int dot = enabled ? 0xFF70DDB8 : 0xFF718391;
        g.fill(x + contentW - 97, y + 7, x + contentW - 89, y + 15, dot);
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
