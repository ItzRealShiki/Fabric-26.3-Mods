package de.leon.frosty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import java.util.ArrayList;
import java.util.List;

public final class FrostyScreen extends Screen {
    private enum Page { HOME, MODULES, PACKS, SETTINGS, STATUS }
    private Page page = Page.HOME;
    private final List<EditBox> fields = new ArrayList<>();
    private final List<Button> styledButtons = new ArrayList<>();
    private String status = "READY";
    private int logScroll, packPage;
    private Pack deleteCandidate;
    private int panelX, panelY, panelW, panelH, contentX, contentW;

    private static final int BG = 0xF20A111B;
    private static final int SURFACE = 0xFF111E2B;
    private static final int CARD = 0xFF172737;
    private static final int CARD_HOVER = 0xFF20384A;
    private static final int BORDER = 0xFF294356;
    private static final int MUTED = 0xFF91A9BA;
    private static final int TEXT = 0xFFF1F7FB;
    private static final int ACCENT = 0xFF68DDF7;
    private static final int GREEN = 0xFF73DDB8;
    private static final int RED = 0xFFFF788C;
    private static final int AMBER = 0xFFFFD17D;

    public FrostyScreen() { super(Component.literal("Frosty")); }
    private Component c(String en, String de) { return Component.literal(FrostyText.tr(en, de)); }

    @Override protected void init() {
        clearWidgets();
        styledButtons.clear();
        fields.clear();
        panelW = Math.min(940, width - 28);
        panelH = Math.min(590, height - 24);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        contentX = panelX + 28;
        contentW = panelW - 56;

        int navY = panelY + 72;
        int gap = 8;
        int navW = (contentW - gap * 4) / 5;
        Page[] pages = Page.values();
        String[][] labels = {{"HOME","START"}, {"MODULES","MODULE"}, {"TEXTURE PACKS","TEXTUREPACKS"}, {"SETTINGS","EINSTELLUNGEN"}, {"STATUS","STATUS"}};
        for (int i = 0; i < pages.length; i++) {
            final Page target = pages[i];
            addStyled(c(labels[i][0], labels[i][1]), contentX + i * (navW + gap), navY, navW, 34,
                () -> open(target), true);
        }

        if (page == Page.HOME) buildHome();
        else if (page == Page.MODULES) buildModules();
        else if (page == Page.PACKS) buildPacks();
        else if (page == Page.SETTINGS) buildSettings();
        else buildStatus();
    }

    private void addStyled(Component label, int x, int y, int w, int h, Runnable action, boolean nav) {
        Button button = Button.builder(label, b -> action.run()).bounds(x, y, w, h).build();
        styledButtons.add(button);
        addRenderableWidget(button);
    }

    private void addStyled(String label, int x, int y, int w, int h, Runnable action) {
        addStyled(Component.literal(FrostyText.sc(label)), x, y, w, h, action, false);
    }

    private void open(Page next) {
        page = next;
        deleteCandidate = null;
        status = "READY";
        rebuildWidgets();
    }

    private int top() { return panelY + 153; }

    private void buildHome() {
        int y = top() + 8;
        addStyled(c("OPEN MODULES", "MODULE ÖFFNEN"), contentX + contentW - 184, y + 94, 168, 34,
            () -> open(Page.MODULES), false);
        addStyled(c("MANAGE TEXTURE PACKS", "TEXTUREPACKS VERWALTEN"), contentX + 18, y + 184, 244, 34,
            () -> open(Page.PACKS), false);
        addStyled(c("VIEW STATUS", "STATUS ANSEHEN"), contentX + 274, y + 184, 160, 34,
            () -> open(Page.STATUS), false);
    }

    private void buildModules() {
        int y = top() + 10, gap = 14, cw = (contentW - gap) / 2;
        moduleToggle("SMALL TOTEM POPS", "KLEINE TOTEM-POPS", FrostyConfig.smallTotemPops, contentX, y, cw, 94, 0);
        moduleToggle("FIXED SHIELDS", "FIXIERTE SCHILDE", FrostyConfig.fixedShields, contentX + cw + gap, y, cw, 94, 1);
        moduleToggle("SHIELD STATUS", "SCHILD-STATUS", FrostyConfig.shieldStatus, contentX, y + 108, cw, 94, 2);
        addStyled(c("CUSTOMIZE SHIELD STATUS", "SCHILD-STATUS ANPASSEN"),
            contentX + cw + gap, y + 140, cw, 34, () -> open(Page.SETTINGS), false);
    }

    private void moduleToggle(String en, String de, boolean enabled, int x, int y, int w, int h, int index) {
        addStyled(c(enabled ? "ENABLED  ✓" : "DISABLED  ○", enabled ? "AKTIV  ✓" : "INAKTIV  ○"),
            x + w - 142, y + 48, 126, 30, () -> {
                switch (index) {
                    case 0 -> FrostyConfig.smallTotemPops = !FrostyConfig.smallTotemPops;
                    case 1 -> FrostyConfig.fixedShields = !FrostyConfig.fixedShields;
                    default -> FrostyConfig.shieldStatus = !FrostyConfig.shieldStatus;
                }
                FrostyConfig.save();
                rebuildWidgets();
            }, false);
    }

    private void buildPacks() {
        List<Pack> packs = FrostyPackManager.packs();
        int pageSize = 6;
        int pages = Math.max(1, (packs.size() + pageSize - 1) / pageSize);
        packPage = Math.min(packPage, pages - 1);
        int start = packPage * pageSize;
        int end = Math.min(packs.size(), start + pageSize);
        int y = top() + 6;

        addStyled(c("OPEN PACK FOLDER", "PACK-ORDNER ÖFFNEN"), contentX + contentW - 190, y - 2, 174, 30,
            () -> {
                try { com.mojang.blaze3d.Blaze3D.openPath(Minecraft.getInstance().getResourcePackDirectory()); }
                catch (Exception ex) { status = "COULD NOT OPEN FOLDER"; }
            }, false);

        for (int i = start; i < end; i++) {
            Pack pack = packs.get(i);
            int rowY = y + 42 + (i - start) * 47;
            int actionX = contentX + contentW - 350;
            addStyled(c(FrostyPackManager.enabled(pack) ? "DISABLE" : "ENABLE",
                    FrostyPackManager.enabled(pack) ? "DEAKTIVIEREN" : "AKTIVIEREN"),
                actionX, rowY + 6, 102, 29, () -> { FrostyPackManager.toggle(pack); rebuildWidgets(); }, false);
            addStyled(c("UP", "HOCH"), actionX + 110, rowY + 6, 54, 29,
                () -> { FrostyPackManager.move(pack, 1); rebuildWidgets(); }, false);
            addStyled(c("DOWN", "RUNTER"), actionX + 170, rowY + 6, 68, 29,
                () -> { FrostyPackManager.move(pack, -1); rebuildWidgets(); }, false);
            if (FrostyPackManager.canDelete(pack)) {
                addStyled(c("DELETE", "LÖSCHEN"), actionX + 244, rowY + 6, 90, 29,
                    () -> { deleteCandidate = pack; rebuildWidgets(); }, false);
            }
        }

        addStyled(c("PREVIOUS", "ZURÜCK"), contentX, y + 42 + pageSize * 47, 112, 29,
            () -> { packPage = Math.max(0, packPage - 1); rebuildWidgets(); }, false);
        addStyled(c("NEXT", "WEITER"), contentX + 120, y + 42 + pageSize * 47, 112, 29,
            () -> { packPage = Math.min(pages - 1, packPage + 1); rebuildWidgets(); }, false);

        if (deleteCandidate != null) {
            addStyled(c("CONFIRM DELETE", "LÖSCHEN BESTÄTIGEN"), contentX + contentW - 310,
                y + 42 + pageSize * 47, 174, 29, () -> {
                    Pack target = deleteCandidate;
                    deleteCandidate = null;
                    status = "DELETING PACK...";
                    FrostyPackManager.delete(target, deleted -> { status = deleted ? "PACK DELETED" : "DELETE FAILED"; rebuildWidgets(); });
                    rebuildWidgets();
                }, false);
            addStyled(c("CANCEL", "ABBRECHEN"), contentX + contentW - 126,
                y + 42 + pageSize * 47, 110, 29, () -> { deleteCandidate = null; rebuildWidgets(); }, false);
        }
    }

    private void buildSettings() {
        int y = top() + 8;
        addStyled(c("LANGUAGE  ·  " + (FrostyConfig.language.equals("de") ? "GERMAN" : "ENGLISH"),
                "SPRACHE  ·  " + (FrostyConfig.language.equals("de") ? "DEUTSCH" : "ENGLISCH")),
            contentX, y, 210, 30, () -> {
                FrostyConfig.language = FrostyConfig.language.equals("de") ? "en" : "de";
                FrostyConfig.save(); rebuildWidgets();
            }, false);
        addRenderableWidget(new FrostySlider(contentX, y + 47, contentW, 24,
            c("TOTEM POP SIZE", "TOTEM-GRÖSSE").getString(), FrostyConfig.totemScale, .25, 1.5,
            v -> { FrostyConfig.totemScale = v; FrostyConfig.save(); }));
        addRenderableWidget(new FrostySlider(contentX, y + 83, contentW, 24,
            c("SHIELD HUD SCALE", "SCHILD-HUD-GRÖSSE").getString(), FrostyConfig.hudScale, .5, 2.0,
            v -> { FrostyConfig.hudScale = v; FrostyConfig.save(); }));

        String[] en = {"BLOCKED", "NOT BLOCKING", "BROKEN", "BLOCKING"};
        String[] de = {"BLOCKIERT", "BLOCKIERT NICHT", "ZERBROCHEN", "BLOCKT"};
        String[] values = {FrostyConfig.blockedColor, FrostyConfig.notBlockingColor, FrostyConfig.brokenColor, FrostyConfig.blockingColor};
        for (int i = 0; i < 4; i++) {
            final int index = i;
            int rowY = y + 140 + i * 34;
            EditBox field = new EditBox(font, contentX, rowY, Math.max(100, contentW - 128), 25,
                c(en[i] + " COLOR", de[i] + " FARBE"));
            field.setMaxLength(7);
            field.setValue(values[i]);
            addRenderableWidget(field);
            fields.add(field);
            addStyled(c("APPLY", "ANWENDEN"), contentX + contentW - 112, rowY - 1, 112, 27,
                () -> saveColor(index), false);
        }
        addStyled(c("RESET COLORS", "FARBEN ZURÜCKSETZEN"), contentX, y + 284, 170, 29, () -> {
            FrostyConfig.blockedColor = "#58D7FF";
            FrostyConfig.notBlockingColor = "#A8C7D8";
            FrostyConfig.brokenColor = "#FF667A";
            FrostyConfig.blockingColor = "#FFFFFF";
            FrostyConfig.save(); rebuildWidgets();
        }, false);
    }

    private void buildStatus() {
        int y = top() + 6;
        addStyled(c("CLEAR", "LEEREN"), contentX, y, 90, 29, () -> {
            FrostyLog.clear(); logScroll = 0; rebuildWidgets();
        }, false);
        addStyled(c("OPEN LOG FOLDER", "LOG-ORDNER ÖFFNEN"), contentX + 100, y, 170, 29,
            FrostyLog::openLogFolder, false);
        addStyled(c("REFRESH", "AKTUALISIEREN"), contentX + 280, y, 112, 29, this::rebuildWidgets, false);
        addStyled(c("OLDER", "ÄLTER"), contentX, y + 38, 90, 27, () -> {
            logScroll = Math.min(Math.max(0, FrostyLog.recent(150).size() - 1), logScroll + 8); rebuildWidgets();
        }, false);
        addStyled(c("NEWER", "NEUER"), contentX + 100, y + 38, 90, 27,
            () -> { logScroll = Math.max(0, logScroll - 8); rebuildWidgets(); }, false);
        addStyled(c("COPY ALL", "ALLE KOPIEREN"), contentX + 200, y + 38, 130, 27, () -> {
            String all = FrostyLog.recent(150).stream().map(e -> e.time() + " [" + e.level() + "] " + e.message())
                .reduce("", (a, v) -> a + v + "\n");
            Minecraft.getInstance().keyboardHandler.setClipboard(all);
            status = "LOG COPIED";
        }, false);
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
        g.fill(0, 0, width, height, 0xB800070D);
        rounded(g, panelX + 7, panelY + 10, panelW, panelH, 22, 0x70000000);
        rounded(g, panelX, panelY, panelW, panelH, 22, BG);
        rounded(g, panelX + 1, panelY + 1, panelW - 2, 62, 20, SURFACE);
        g.fill(panelX + 28, panelY + 63, panelX + panelW - 28, panelY + 64, BORDER);
        g.fill(panelX + 28, panelY + 2, panelX + panelW - 28, panelY + 4, ACCENT);

        g.text(font, Component.literal("FROSTY"), panelX + 28, panelY + 19, TEXT);
        g.text(font, Component.literal(FrostyText.sc("CLIENT CONTROL CENTER")), panelX + 105, panelY + 22, MUTED);
        g.text(font, Component.literal("0.1.4"), panelX + panelW - 64, panelY + 21, ACCENT);

        String heading = switch (page) {
            case HOME -> FrostyText.tr("WELCOME BACK", "WILLKOMMEN ZURÜCK");
            case MODULES -> FrostyText.tr("MODULE LIBRARY", "MODUL-BIBLIOTHEK");
            case PACKS -> FrostyText.tr("TEXTURE PACK MANAGER", "TEXTUREPACK-MANAGER");
            case SETTINGS -> FrostyText.tr("PERSONALIZATION", "ANPASSUNG");
            case STATUS -> FrostyText.tr("SYSTEM DIAGNOSTICS", "SYSTEM-DIAGNOSE");
        };
        String subtitle = switch (page) {
            case HOME -> FrostyText.tr("Everything you need, in one clean place.", "Alles Wichtige an einem übersichtlichen Ort.");
            case MODULES -> FrostyText.tr("Control your Frosty features.", "Steuere deine Frosty-Funktionen.");
            case PACKS -> FrostyText.tr("Enable, reorder and remove your installed resource packs.", "Aktiviere, sortiere und entferne deine installierten Texturepacks.");
            case SETTINGS -> FrostyText.tr("Fine-tune the look and feel.", "Passe Darstellung und Verhalten an.");
            case STATUS -> FrostyText.tr("Recent events and diagnostic information.", "Aktuelle Ereignisse und Diagnoseinformationen.");
        };
        g.text(font, Component.literal(FrostyText.sc(heading)), contentX, panelY + 116, TEXT);
        g.text(font, Component.literal(subtitle), contentX, panelY + 134, MUTED);

        if (page == Page.HOME) drawHome(g);
        else if (page == Page.MODULES) drawModules(g);
        else if (page == Page.PACKS) drawPacks(g);
        else if (page == Page.SETTINGS) drawSettings(g);
        else drawStatus(g);

        super.extractRenderState(g, mouseX, mouseY, delta);
        for (Button button : styledButtons) {
            int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
            rounded(g, x - 2, y - 2, w + 4, h + 4, 13, 0xFF0B151F);
            int color = button.isHoveredOrFocused() ? CARD_HOVER : CARD;
            rounded(g, x, y, w, h, 12, color);
            if (button.isHoveredOrFocused()) rounded(g, x + 9, y + h - 3, w - 18, 3, 2, ACCENT);
            g.centeredText(font, button.getMessage(), x + w / 2, y + (h - 9) / 2, button.isHoveredOrFocused() ? ACCENT : TEXT);
        }
        rounded(g, contentX, panelY + panelH - 37, contentW, 1, 1, BORDER);
        g.text(font, Component.literal(FrostyText.sc("RIGHT SHIFT  ·  HUD EDITOR")), contentX, panelY + panelH - 24, MUTED);
        g.text(font, Component.literal(FrostyText.sc(status)), panelX + panelW - 170, panelY + panelH - 24, ACCENT);
    }

    private void drawHome(GuiGraphicsExtractor g) {
        int y = top() + 8, half = (contentW - 14) / 2;
        card(g, contentX, y, contentW, 142);
        g.text(font, Component.literal(FrostyText.sc("YOUR MINECRAFT, YOUR STYLE")), contentX + 22, y + 22, ACCENT);
        g.text(font, Component.literal(FrostyText.tr("A refreshed control center for your client.", "Eine neue Steuerzentrale für deinen Client.")), contentX + 22, y + 50, TEXT);
        g.text(font, Component.literal(FrostyText.tr("Manage modules, resource packs and diagnostics.", "Verwalte Module, Texturepacks und Diagnosen.")), contentX + 22, y + 72, MUTED);
        int y2 = y + 158;
        statCard(g, contentX, y2, half, 86, "ACTIVE MODULES", "AKTIVE MODULE", activeModules() + " / 3", GREEN);
        statCard(g, contentX + half + 14, y2, half, 86, "TEXTURE PACKS", "TEXTUREPACKS",
            String.valueOf(FrostyPackManager.packs().size()), ACCENT);
    }

    private int activeModules() {
        return (FrostyConfig.smallTotemPops ? 1 : 0) + (FrostyConfig.fixedShields ? 1 : 0) + (FrostyConfig.shieldStatus ? 1 : 0);
    }

    private void drawModules(GuiGraphicsExtractor g) {
        int y = top() + 8, gap = 14, cw = (contentW - gap) / 2;
        moduleCard(g, contentX, y, cw, 94, "SMALL TOTEM POPS", "KLEINE TOTEM-POPS", FrostyConfig.smallTotemPops);
        moduleCard(g, contentX + cw + gap, y, cw, 94, "FIXED SHIELDS", "FIXIERTE SCHILDE", FrostyConfig.fixedShields);
        moduleCard(g, contentX, y + 108, cw, 94, "SHIELD STATUS", "SCHILD-STATUS", FrostyConfig.shieldStatus);
        card(g, contentX + cw + gap, y + 108, cw, 94);
        g.text(font, Component.literal(FrostyText.sc("SHIELD APPEARANCE")), contentX + cw + gap + 18, y + 128, ACCENT);
        g.text(font, Component.literal(FrostyText.tr("Change colors and HUD scale.", "Farben und HUD-Größe ändern.")), contentX + cw + gap + 18, y + 153, MUTED);
    }

    private void moduleCard(GuiGraphicsExtractor g, int x, int y, int w, int h, String en, String de, boolean enabled) {
        card(g, x, y, w, h);
        rounded(g, x + 16, y + 20, 5, 48, 2, enabled ? GREEN : MUTED);
        g.text(font, Component.literal(FrostyText.tr(en, de)), x + 34, y + 22, TEXT);
        g.text(font, Component.literal(FrostyText.tr("Feature toggle", "Funktion umschalten")), x + 34, y + 46, MUTED);
        g.text(font, Component.literal(enabled ? "●  ON" : "○  OFF"), x + w - 62, y + 68, enabled ? GREEN : MUTED);
    }

    private void drawPacks(GuiGraphicsExtractor g) {
        List<Pack> packs = FrostyPackManager.packs();
        int pageSize = 6, start = packPage * pageSize;
        int end = Math.min(packs.size(), start + pageSize);
        int y = top() + 8;
        card(g, contentX, y + 34, contentW, 42 + pageSize * 47);
        if (packs.isEmpty()) {
            g.centeredText(font, c("NO RESOURCE PACKS FOUND", "KEINE TEXTUREPACKS GEFUNDEN"),
                contentX + contentW / 2, y + 104, MUTED);
        }
        for (int i = start; i < end; i++) {
            Pack pack = packs.get(i);
            int rowY = y + 45 + (i - start) * 47;
            boolean selected = FrostyPackManager.enabled(pack);
            rounded(g, contentX + 12, rowY, contentW - 24, 39, 12, selected ? 0xFF1A3442 : 0xFF142331);
            g.text(font, pack.getTitle(), contentX + 26, rowY + 8, TEXT);
            String id = pack.getId();
            if (id.length() > 45) id = id.substring(0, 42) + "...";
            g.text(font, Component.literal(id), contentX + 26, rowY + 23, MUTED);
            g.text(font, Component.literal(selected ? "ACTIVE" : "INACTIVE"), contentX + contentW - 430, rowY + 14,
                selected ? GREEN : MUTED);
        }
        int pages = Math.max(1, (packs.size() + pageSize - 1) / pageSize);
        g.text(font, Component.literal("PAGE " + (packPage + 1) + " / " + pages), contentX + 246, y + 42 + pageSize * 47 + 10, MUTED);
        if (deleteCandidate != null) {
            rounded(g, contentX + 130, y + 105, contentW - 260, 92, 10, 0xFF38212A);
            g.centeredText(font, c("DELETE", "LÖSCHEN"), contentX + contentW / 2, y + 120, RED);
            g.centeredText(font, deleteCandidate.getTitle(), contentX + contentW / 2, y + 142, TEXT);
            g.centeredText(font, c("THIS CANNOT BE UNDONE", "DIES KANN NICHT RÜCKGÄNGIG GEMACHT WERDEN"),
                contentX + contentW / 2, y + 164, MUTED);
        }
    }

    private void drawSettings(GuiGraphicsExtractor g) {
        int y = top() + 8;
        card(g, contentX, y + 34, contentW, 326);
        g.text(font, Component.literal(FrostyText.sc("GENERAL & APPEARANCE")), contentX + 18, y + 50, ACCENT);
        g.text(font, Component.literal(FrostyText.tr("Language", "Sprache")), contentX + 18, y + 92, TEXT);
        g.text(font, Component.literal(FrostyText.tr("Totem size", "Totem-Größe")), contentX + 18, y + 139, TEXT);
        g.text(font, Component.literal(FrostyText.tr("Shield HUD scale", "Schild-HUD-Größe")), contentX + 18, y + 175, TEXT);
        g.text(font, Component.literal(FrostyText.sc("SHIELD STATUS COLORS  ·  HEX")), contentX + 18, y + 223, ACCENT);
        String[] labels = {"BLOCKED", "NOT BLOCKING", "BROKEN", "BLOCKING"};
        String[] values = {FrostyConfig.blockedColor, FrostyConfig.notBlockingColor, FrostyConfig.brokenColor, FrostyConfig.blockingColor};
        for (int i = 0; i < 4; i++) {
            int rowY = y + 245 + i * 34;
            rounded(g, contentX + 18, rowY + 4, 12, 12, 4, FrostyConfig.color(values[i], ACCENT));
            g.text(font, Component.literal(FrostyText.tr(labels[i], new String[]{"BLOCKIERT","BLOCKIERT NICHT","ZERBROCHEN","BLOCKT"}[i])), contentX + 40, rowY + 3, TEXT);
            g.text(font, Component.literal(values[i]), contentX + contentW - 82, rowY + 3, MUTED);
        }
    }

    private void drawStatus(GuiGraphicsExtractor g) {
        int y = top() + 8, errors = FrostyLog.count(FrostyLog.LevelType.ERROR);
        int warnings = FrostyLog.count(FrostyLog.LevelType.WARNING), entriesCount = FrostyLog.recent(150).size();
        int third = (contentW - 24) / 3;
        statCard(g, contentX, y, third, 66, "ERRORS", "FEHLER", String.valueOf(errors), errors > 0 ? RED : GREEN);
        statCard(g, contentX + third + 12, y, third, 66, "WARNINGS", "WARNUNGEN", String.valueOf(warnings), warnings > 0 ? AMBER : GREEN);
        statCard(g, contentX + (third + 12) * 2, y, third, 66, "ENTRIES", "EINTRÄGE", String.valueOf(entriesCount), ACCENT);
        card(g, contentX, y + 78, contentW, 250);
        List<FrostyLog.Entry> entries = FrostyLog.recent(150);
        int first = Math.min(logScroll, Math.max(0, entries.size() - 1));
        for (int i = first; i < Math.min(entries.size(), first + 11); i++) {
            FrostyLog.Entry e = entries.get(i);
            int color = e.level() == FrostyLog.LevelType.ERROR ? RED : e.level() == FrostyLog.LevelType.WARNING ? AMBER : ACCENT;
            String line = e.time() + "  [" + e.level() + "]  " + e.message().replace('\n', ' ');
            if (line.length() > 112) line = line.substring(0, 109) + "...";
            g.text(font, Component.literal(FrostyText.sc(line)), contentX + 14, y + 91 + (i - first) * 19, color);
        }
        if (entries.isEmpty()) g.text(font, c("NO LOG ENTRIES YET", "NOCH KEINE LOG-EINTRÄGE"), contentX + 14, y + 96, MUTED);
    }

    private void statCard(GuiGraphicsExtractor g, int x, int y, int w, int h, String en, String de, String value, int color) {
        card(g, x, y, w, h);
        g.text(font, Component.literal(FrostyText.tr(en, de)), x + 16, y + 14, MUTED);
        g.text(font, Component.literal(value), x + 16, y + 39, color);
    }

    private void card(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        rounded(g, x, y, w, h, 15, BORDER);
        rounded(g, x + 1, y + 1, w - 2, h - 2, 14, CARD);
    }

    private void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        int r = Math.min(radius, Math.min(w / 2, h / 2));
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < r) {
                double dy = r - row - 0.5;
                inset = r - (int)Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)));
            } else if (row >= h - r) {
                double dy = row - (h - r) + 0.5;
                inset = r - (int)Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)));
            }
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
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
