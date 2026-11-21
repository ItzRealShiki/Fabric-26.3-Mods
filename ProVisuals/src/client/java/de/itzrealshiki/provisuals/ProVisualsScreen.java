package de.itzrealshiki.provisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class ProVisualsScreen extends Screen {
    private static final int NAV_BAR_HEIGHT = 54;
    private static final int NAV_BAR_GAP = 5;

    private record DashboardCard(String category, String description, String meta,
                                 String index, int x, int y, int width, int height) {}
    private record FeatureRow(FeatureRegistry.Feature feature, int x, int y, int width, int height) {}
    private record SectionHeader(String title, String subtitle, int y) {}

    private String page = "Overview";
    private String status = "ALL SYSTEMS READY";
    private final List<Button> navigationButtons = new ArrayList<>();
    private final List<DashboardCard> dashboardCards = new ArrayList<>();
    private final List<FeatureRow> featureRows = new ArrayList<>();
    private final List<SectionHeader> sectionHeaders = new ArrayList<>();
    private float navigationProgress;
    private long lastNavigationFrameTime;

    public ProVisualsScreen() {
        super(ProVisualsUi.component("ProVisuals"));
    }

    @Override
    protected void init() {
        clearWidgets();
        dashboardCards.clear();
        featureRows.clear();
        sectionHeaders.clear();
        drawActionRows.clear();

        int panelX = panelX();
        int panelY = panelY();
        int contentX = panelX + 28;
        int contentWidth = Math.max(180, Math.min(660, width - contentX - 28));

        switch (page) {
            case "Overview" -> buildOverview(contentX, panelY, contentWidth);
            case "Music" -> buildMusicPage(contentX, panelY, contentWidth);
            case "Errors" -> buildErrorsPage(contentX, panelY, contentWidth);
            case "Settings" -> buildSettingsPage(contentX, panelY, contentWidth);
            default -> buildFeaturePage(contentX, panelY, contentWidth);
        }
        addNavigationBar();
    }

    private void buildOverview(int x, int panelY, int contentWidth) {
        DashboardCard[] cards = {
                new DashboardCard("World Visuals", "Lighting, ores and world reflections", "3 MODULES", "01", 0, 0, 0, 0),
                new DashboardCard("Player Visuals", "Armor effects, trims and visual friends", "3 MODULES", "02", 0, 0, 0, 0),
                new DashboardCard("Combat Visuals", "Hit effects and item highlights", "2 MODULES", "03", 0, 0, 0, 0),
                new DashboardCard("HUD & Interface", "Armor, target, inventory and effects", "4 MODULES", "04", 0, 0, 0, 0),
                new DashboardCard("Music", "Music Island and synced lyrics", "2 FEATURES", "05", 0, 0, 0, 0),
                new DashboardCard("Settings", "Visual quality and interface preferences", "6 OPTIONS", "06", 0, 0, 0, 0)
        };

        int gap = 10;
        int cardWidth = (contentWidth - gap) / 2;
        int cardHeight = 54;
        int startY = panelY + 91;
        int rowGap = 9;
        for (int i = 0; i < cards.length; i++) {
            DashboardCard card = cards[i];
            int column = i % 2;
            int row = i / 2;
            int cardX = x + column * (cardWidth + gap);
            int cardY = startY + row * (cardHeight + rowGap);
            DashboardCard positioned = new DashboardCard(card.category(), card.description(),
                    card.meta(), card.index(), cardX, cardY, cardWidth, cardHeight);
            dashboardCards.add(positioned);
            addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> openPage(card.category()))
                    .bounds(cardX, cardY, cardWidth, cardHeight).build());
        }

        int errorY = startY + 3 * (cardHeight + rowGap);
        DashboardCard error = new DashboardCard("Error Center",
                "View diagnostics and copy the latest log", "DIAGNOSTICS", "07",
                x, errorY, contentWidth, 42);
        dashboardCards.add(error);
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> openPage("Errors"))
                .bounds(error.x(), error.y(), error.width(), error.height()).build());
    }

    private void buildFeaturePage(int x, int panelY, int contentWidth) {
        String subtitle = switch (page) {
            case "World Visuals" -> "Lighting, block appearance and environment effects";
            case "Player Visuals" -> "Personal visual details for players and equipment";
            case "Combat Visuals" -> "Lightweight visual feedback for combat";
            case "HUD & Interface" -> "Compact information panels on your screen";
            default -> "Visual modules and their options";
        };

        List<FeatureRegistry.Feature> features = FeatureRegistry.inCategory(page);
        sectionHeaders.add(new SectionHeader(page.toUpperCase(),
                subtitle + "  /  " + features.size() + " MODULES", panelY + 72));

        int y = panelY + 122;
        int rowHeight = 35;
        int rowGap = 8;
        for (FeatureRegistry.Feature feature : features) {
            FeatureRow row = new FeatureRow(feature, x, y, contentWidth, rowHeight);
            featureRows.add(row);
            addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
                ProVisualsConfig.toggle(feature.key());
                status = feature.rendererConnected()
                        ? feature.title().toUpperCase() + " " + onOff(ProVisualsConfig.isEnabled(feature.key()))
                        : "PREFERENCE SAVED — VISUAL RENDERER NOT CONNECTED YET";
                rebuildWidgets();
            }).bounds(x, y, contentWidth, rowHeight).build());
            y += rowHeight + rowGap;
        }
    }

    private void buildMusicPage(int x, int panelY, int contentWidth) {
        sectionHeaders.add(new SectionHeader("MUSIC PLAYER",
                "On-screen playback information and media controls", panelY + 72));
        addSettingRow("MUSIC ISLAND", onOff(ProVisualsConfig.musicIslandEnabled),
                "Compact player overlay with progress and playback controls",
                x, panelY + 112, contentWidth,
                b -> {
                    ProVisualsConfig.musicIslandEnabled = !ProVisualsConfig.musicIslandEnabled;
                    ProVisualsConfig.save();
                    status = "MUSIC ISLAND " + onOff(ProVisualsConfig.musicIslandEnabled);
                    rebuildWidgets();
                });

        sectionHeaders.add(new SectionHeader("LYRICS IN THE SKY",
                "Synced lyrics, typography and placement settings", panelY + 184));
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
            boolean enabled = MusicLyricsBridge.toggleLyrics();
            status = "LYRICS " + onOff(enabled);
            rebuildWidgets();
        }).bounds(x, panelY + 224, contentWidth, 35).build());
        drawActionRows.add(new ActionRow("LYRICS DISPLAY",
                onOff(MusicLyricsBridge.isLyricsEnabled()), "Toggle the lyrics overlay",
                x, panelY + 224, contentWidth, 35, false));
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> MusicLyricsBridge.openSettings())
                .bounds(x, panelY + 267, contentWidth, 35).build());
        drawActionRows.add(new ActionRow("OPEN LYRICS SETTINGS", "›",
                "Fonts, colours and lyric placement", x, panelY + 267, contentWidth, 35, true));
    }

    private void buildErrorsPage(int x, int panelY, int contentWidth) {
        sectionHeaders.add(new SectionHeader("DIAGNOSTIC CENTER",
                "Recent events and troubleshooting information", panelY + 72));
        int gap = 10;
        int half = (contentWidth - gap) / 2;
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
            Diagnostics.info("Diagnostics refreshed by user");
            status = "DIAGNOSTICS REFRESHED";
        }).bounds(x, panelY + 116, half, 35).build());
        drawActionRows.add(new ActionRow("REFRESH LOG", "↻",
                "Update recent diagnostic events", x, panelY + 116, half, 35, false));
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(String.join("\n", Diagnostics.recentEntries()));
            status = "DIAGNOSTICS COPIED";
        }).bounds(x + half + gap, panelY + 116, half, 35).build());
        drawActionRows.add(new ActionRow("COPY LOG", "↗",
                "Copy all available entries", x + half + gap, panelY + 116, half, 35, true));
    }

    private record ActionRow(String title, String value, String description,
                             int x, int y, int width, int height, boolean secondary) {}
    private final List<ActionRow> drawActionRows = new ArrayList<>();

    private void addSettingRow(String title, String value, String description,
                               int x, int y, int w, Button.OnPress action) {
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), action)
                .bounds(x, y, w, 35).build());
        drawActionRows.add(new ActionRow(title, value, description, x, y, w, 35, false));
    }

    private void buildSettingsPage(int x, int panelY, int contentWidth) {
        int gap = 10;
        int half = (contentWidth - gap) / 2;

        sectionHeaders.add(new SectionHeader("REFLECTIONS",
                "Quality and coverage of reflective surfaces", panelY + 72));
        addSettingRow("INTENSITY", percent(ProVisualsConfig.reflectionIntensity),
                "Cycle reflection strength", x, panelY + 112, half,
                b -> { ProVisualsConfig.reflectionIntensity = cycle(ProVisualsConfig.reflectionIntensity); ProVisualsConfig.save(); rebuildWidgets(); });
        addSettingRow("QUALITY", ProVisualsConfig.reflectionQuality,
                "Cycle rendering quality", x + half + gap, panelY + 112, half,
                b -> { ProVisualsConfig.reflectionQuality = nextQuality(ProVisualsConfig.reflectionQuality); ProVisualsConfig.save(); rebuildWidgets(); });
        addSettingRow("SURFACES", surfaceLabel(ProVisualsConfig.reflectiveSurfaces),
                "Choose which surfaces reflect", x, panelY + 155, contentWidth,
                b -> { ProVisualsConfig.reflectiveSurfaces = nextSurface(ProVisualsConfig.reflectiveSurfaces); ProVisualsConfig.save(); rebuildWidgets(); });

        sectionHeaders.add(new SectionHeader("ORE GLOW",
                "Choose how selected ores are highlighted", panelY + 207));
        addSettingRow("INTERIOR GLOW", onOff(ProVisualsConfig.oreInteriorGlow),
                "Glow inside ore blocks", x, panelY + 247, half,
                b -> { ProVisualsConfig.oreInteriorGlow = !ProVisualsConfig.oreInteriorGlow; ProVisualsConfig.save(); rebuildWidgets(); });
        addSettingRow("OUTLINE GLOW", onOff(ProVisualsConfig.oreOutlineGlow),
                "Glow around ore outlines", x + half + gap, panelY + 247, half,
                b -> { ProVisualsConfig.oreOutlineGlow = !ProVisualsConfig.oreOutlineGlow; ProVisualsConfig.save(); rebuildWidgets(); });

        sectionHeaders.add(new SectionHeader("INTERFACE",
                "Appearance of the ProVisuals menu", panelY + 285));
        addSettingRow("CORNER STYLE", ProVisualsConfig.roundedCorners ? "ROUNDED" : "SQUARE",
                "Switch between rounded and square corners", x, panelY + 325, contentWidth,
                b -> { ProVisualsConfig.roundedCorners = !ProVisualsConfig.roundedCorners; ProVisualsConfig.save(); rebuildWidgets(); });
    }

    private void openPage(String destination) {
        page = destination;
        status = "PAGE: " + destination.toUpperCase();
        rebuildWidgets();
    }

    private int panelX() {
        return Math.max(12, width / 2 - 366);
    }

    private int panelY() {
        return Math.max(8, height / 2 - 196);
    }

    private void addNavigationBar() {
        navigationButtons.clear();
        int barWidth = Math.max(180, Math.min(720, width - 16));
        int gap = width < 540 ? 2 : NAV_BAR_GAP;
        int buttonWidth = Math.max(1, (barWidth - gap * (FeatureRegistry.CATEGORIES.size() - 1))
                / FeatureRegistry.CATEGORIES.size());
        int totalWidth = buttonWidth * FeatureRegistry.CATEGORIES.size()
                + gap * (FeatureRegistry.CATEGORIES.size() - 1);
        int startX = (width - totalWidth) / 2;

        for (int i = 0; i < FeatureRegistry.CATEGORIES.size(); i++) {
            String category = FeatureRegistry.CATEGORIES.get(i);
            Button button = Button.builder(ProVisualsUi.component(navBarLabel(category)), b -> openPage(category))
                    .bounds(startX + i * (buttonWidth + gap), height + 24, buttonWidth, 22).build();
            addRenderableWidget(button);
            navigationButtons.add(button);
        }
    }

    private void updateNavigationBar(int mouseY) {
        long now = System.nanoTime();
        if (lastNavigationFrameTime == 0L) lastNavigationFrameTime = now;
        float elapsedSeconds = Math.min(0.05f,
                Math.max(0.0f, (now - lastNavigationFrameTime) / 1_000_000_000.0f));
        lastNavigationFrameTime = now;

        boolean hoveringDock = navigationProgress > 0.001f
                ? mouseY >= height - NAV_BAR_HEIGHT - 12 && mouseY <= height
                : mouseY >= height - 8 && mouseY <= height;
        float animationStep = elapsedSeconds / 0.22f;
        navigationProgress = hoveringDock
                ? Math.min(1.0f, navigationProgress + animationStep)
                : Math.max(0.0f, navigationProgress - animationStep);

        int barTop = height - NAV_BAR_HEIGHT
                + Math.round((1.0f - navigationProgress) * (NAV_BAR_HEIGHT + 22));
        for (Button button : navigationButtons) button.setY(barTop + 16);
    }

    private static String navBarLabel(String category) {
        return switch (category) {
            case "Overview" -> "HOME";
            case "World Visuals" -> "WORLD";
            case "Player Visuals" -> "PLAYER";
            case "Combat Visuals" -> "COMBAT";
            case "HUD & Interface" -> "HUD";
            case "Music" -> "MUSIC";
            case "Settings" -> "SETTINGS";
            case "Errors" -> "ERRORS";
            default -> category.toUpperCase();
        };
    }

    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }
    private static String percent(float value) { return Math.round(value * 100) + "%"; }
    private static float cycle(float value) { return value >= 0.95f ? 0.25f : Math.min(1.0f, value + 0.25f); }
    private static String nextQuality(String value) {
        return switch (value) { case "LOW" -> "MEDIUM"; case "MEDIUM" -> "HIGH"; default -> "LOW"; };
    }
    private static String nextSurface(String value) {
        return switch (value) { case "ALL" -> "SELECTED"; case "SELECTED" -> "WATER_ONLY"; default -> "ALL"; };
    }
    private static String surfaceLabel(String value) {
        return switch (value) { case "SELECTED" -> "SELECTED BLOCKS"; case "WATER_ONLY" -> "WATER ONLY"; default -> "ALL BLOCKS"; };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xF2090911, 0xF2171027);
        int px = panelX();
        int py = panelY();
        ProVisualsUi.fill(graphics, px, py, Math.min(732, width - 24), 392, 14, 0xF0151520);
        ProVisualsUi.fill(graphics, px, py, 4, 392, 2, 0xFF9A6BFF);
        ProVisualsUi.fill(graphics, px + 16, py + 54, Math.min(700, width - 56), 1, 1, 0x555E536F);

        updateNavigationBar(mouseY);
        int barWidth = Math.max(180, Math.min(720, width - 16));
        int barX = (width - barWidth) / 2;
        int barTop = height - NAV_BAR_HEIGHT
                + Math.round((1.0f - navigationProgress) * (NAV_BAR_HEIGHT + 22));
        ProVisualsUi.fill(graphics, barX - 8, barTop, barWidth + 16,
                Math.max(0, height - barTop), 10, 0xF0181726);
        ProVisualsUi.fill(graphics, barX - 8, barTop, barWidth + 16, 2, 1, 0xFF9A6BFF);

        int handleAlpha = Math.round(220 * (1.0f - navigationProgress));
        if (handleAlpha > 0) {
            ProVisualsUi.fill(graphics, width / 2 - 34, height - 5, 68, 3, 2,
                    (handleAlpha << 24) | 0x009A6BFF);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (ProVisualsConfig.roundedCorners) {
            for (var child : children()) {
                if (child instanceof Button button) {
                    ProVisualsUi.roundWidgetCorners(graphics, button.getX(), button.getY(),
                            button.getWidth(), button.getHeight(), 5, 0xF0151520);
                }
            }
        }

        int x = panelX() + 20;
        int y = panelY() + 17;
        ProVisualsUi.text(graphics, font, "PROVISUALS", x, y, 0xFFFFFFFF, true);
        ProVisualsUi.text(graphics, font, "VISUAL CLIENT  /  MODERN VIOLET", x, y + 16, 0xFFBCA4FF, true);

        switch (page) {
            case "Overview" -> renderOverview(graphics, mouseX, mouseY);
            case "Music" -> renderMusic(graphics, mouseX, mouseY);
            case "Errors" -> renderErrors(graphics, mouseX, mouseY);
            case "Settings" -> renderSettings(graphics, mouseX, mouseY);
            default -> renderFeaturePage(graphics, mouseX, mouseY);
        }

        ProVisualsUi.text(graphics, font, status, panelX() + 20, height / 2 + 174, 0xFFBCA4FF, true);
    }

    private void renderOverview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = panelX() + 28;
        int y = panelY() + 65;
        ProVisualsUi.text(graphics, font, "YOUR VISUAL WORKSPACE", x, y, 0xFFFFFFFF, true);
        ProVisualsUi.text(graphics, font, "Choose a category to find related features and settings.", x, y + 15, 0xFFAAA4BA, false);

        for (DashboardCard card : dashboardCards) {
            boolean hovered = inside(card.x(), card.y(), card.width(), card.height(), mouseX, mouseY);
            int base = hovered ? 0xFF282338 : 0xFF1D1B29;
            ProVisualsUi.fill(graphics, card.x(), card.y(), card.width(), card.height(), 9, base);
            ProVisualsUi.fill(graphics, card.x() + 1, card.y() + 8, 2, card.height() - 16, 1,
                    hovered ? 0xFFBCA4FF : 0xFF7653B8);
            ProVisualsUi.text(graphics, font, card.index(), card.x() + 12, card.y() + 9, 0xFF9A6BFF, false);
            ProVisualsUi.text(graphics, font, card.category().toUpperCase(), card.x() + 39,
                    card.y() + 8, 0xFFF1EDFA, true);
            ProVisualsUi.text(graphics, font, card.description(), card.x() + 39,
                    card.y() + 22, 0xFFAAA4BA, false);
            ProVisualsUi.text(graphics, font, card.meta(), card.x() + card.width() - font.width(ProVisualsUi.smallCaps(card.meta())) - 10,
                    card.y() + card.height() - 13, 0xFF9A86C3, false);
        }
    }

    private void renderFeaturePage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
        for (FeatureRow row : featureRows) {
            FeatureRegistry.Feature feature = row.feature();
            boolean hovered = inside(row.x(), row.y(), row.width(), row.height(), mouseX, mouseY);
            ProVisualsUi.fill(graphics, row.x(), row.y(), row.width(), row.height(), 8,
                    hovered ? 0xFF282338 : 0xFF1D1B29);
            ProVisualsUi.text(graphics, font, feature.title().toUpperCase(),
                    row.x() + 13, row.y() + 6, 0xFFF1EDFA, true);
            ProVisualsUi.text(graphics, font, feature.description(),
                    row.x() + 13, row.y() + 20, 0xFFAAA4BA, false);
            String state = ProVisualsConfig.isEnabled(feature.key()) ? "ON" : "OFF";
            int pillWidth = 44;
            int pillX = row.x() + row.width() - pillWidth - 10;
            ProVisualsUi.fill(graphics, pillX, row.y() + 9, pillWidth, 17, 6,
                    feature.rendererConnected() ? 0xFF342A4C : 0xFF302B37);
            graphics.centeredText(font, ProVisualsUi.component(state), pillX + pillWidth / 2,
                    row.y() + 13, feature.rendererConnected() ? 0xFFD0BFFF : 0xFFB8AFC2);
        }
        if (featureRows.stream().anyMatch(row -> !row.feature().rendererConnected())) {
            ProVisualsUi.text(graphics, font, "Some modules only save their preferences until their visual renderer is connected.",
                    panelX() + 28, panelY() + 341, 0xFFE8C987, false);
        }
    }

    private void renderMusic(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
        for (ActionRow row : drawActionRows) renderActionRow(graphics, row, mouseX, mouseY);
    }

    private void renderSettings(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
        for (ActionRow row : drawActionRows) renderActionRow(graphics, row, mouseX, mouseY);
    }

    private void renderErrors(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
        for (ActionRow row : drawActionRows) renderActionRow(graphics, row, mouseX, mouseY);

        int listX = panelX() + 28;
        int listY = panelY() + 164;
        int listWidth = Math.min(660, width - listX - 28);
        ProVisualsUi.fill(graphics, listX, listY, listWidth, 150, 9, 0xFF1D1B29);
        ProVisualsUi.text(graphics, font, "RECENT EVENTS", listX + 13, listY + 12, 0xFFD6C8F5, true);
        List<String> entries = Diagnostics.recentEntries();
        if (entries.isEmpty()) {
            ProVisualsUi.text(graphics, font, "No ProVisuals errors recorded this session.",
                    listX + 13, listY + 38, 0xFFAAA4BA, false);
        } else {
            int lineY = listY + 36;
            for (String entry : entries.stream().limit(7).toList()) {
                String compact = entry.length() > 84 ? entry.substring(0, 81) + "..." : entry;
                ProVisualsUi.text(graphics, font, compact, listX + 13, lineY, 0xFFD6D2E0, false);
                lineY += 15;
            }
        }
        ProVisualsUi.text(graphics, font, "LOG FILE  /  config/provisuals-diagnostics.log",
                listX + 13, listY + 130, 0xFF8F86A3, false);
    }

    private void renderSectionHeaders(GuiGraphicsExtractor graphics) {
        for (SectionHeader header : sectionHeaders) {
            ProVisualsUi.text(graphics, font, header.title(), panelX() + 28, header.y(),
                    0xFFEDE7FA, true);
            ProVisualsUi.text(graphics, font, header.subtitle(), panelX() + 28, header.y() + 15,
                    0xFF9F96B0, false);
        }
    }

    private void renderActionRow(GuiGraphicsExtractor graphics, ActionRow row, int mouseX, int mouseY) {
        boolean hovered = inside(row.x(), row.y(), row.width(), row.height(), mouseX, mouseY);
        ProVisualsUi.fill(graphics, row.x(), row.y(), row.width(), row.height(), 8,
                hovered ? 0xFF282338 : 0xFF1D1B29);
        ProVisualsUi.text(graphics, font, row.title(), row.x() + 13, row.y() + 6,
                0xFFF1EDFA, true);
        ProVisualsUi.text(graphics, font, row.description(), row.x() + 13, row.y() + 20,
                0xFFAAA4BA, false);
        String value = row.value();
        int valueWidth = font.width(ProVisualsUi.smallCaps(value));
        ProVisualsUi.text(graphics, font, value, row.x() + row.width() - valueWidth - 14,
                row.y() + 12, row.secondary() ? 0xFFBCA4FF : 0xFFD0BFFF, true);
    }

    private static boolean inside(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
