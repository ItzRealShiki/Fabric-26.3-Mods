package de.itzrealshiki.provisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class ProVisualsScreen extends Screen {
    private static final int NAV_BAR_HEIGHT = 54;
    private static final int NAV_BAR_GAP = 7;

    private record DashboardCard(String category, String description, String meta, String index,
                                 int x, int y, int width, int height) {}
    private record FeatureRow(FeatureRegistry.Feature feature, int x, int y, int width, int height) {}
    private record SectionHeader(String title, String subtitle, int y) {}
    private record SubcategoryTab(String title, int x, int y, int width, int height) {}
    private record ActionRow(String title, String value, String description,
                             int x, int y, int width, int height, boolean secondary) {}

    private String page = "Overview";
    private String selectedSubcategory = "Combat Visuals";
    private String status = "All systems ready";

    private final List<Button> navigationButtons = new ArrayList<>();
    private final List<DashboardCard> dashboardCards = new ArrayList<>();
    private final List<FeatureRow> featureRows = new ArrayList<>();
    private final List<SectionHeader> sectionHeaders = new ArrayList<>();
    private final List<SubcategoryTab> subcategoryTabs = new ArrayList<>();
    private final List<ActionRow> drawActionRows = new ArrayList<>();

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
        subcategoryTabs.clear();
        drawActionRows.clear();

        int contentX = panelX() + 28;
        int contentWidth = Math.max(180, Math.min(660, width - contentX - 28));
        int py = panelY();

        switch (page) {
            case "Overview" -> buildOverview(contentX, py, contentWidth);
            case "Music" -> buildMusicPage(contentX, py, contentWidth);
            case "Tools" -> buildToolsPage(contentX, py, contentWidth);
            case "Settings" -> buildSettingsPage(contentX, py, contentWidth);
            default -> buildFeaturePage(contentX, py, contentWidth);
        }
        addNavigationBar();
    }

    private void buildOverview(int x, int py, int contentWidth) {
        DashboardCard[] cards = {
                new DashboardCard("PvP", "Combat, armor and player visuals",
                        FeatureRegistry.countInTopCategory("PvP") + " modules", "01", 0, 0, 0, 0),
                new DashboardCard("Visuals", "Lighting, ores and world reflections",
                        FeatureRegistry.countInTopCategory("Visuals") + " modules", "02", 0, 0, 0, 0),
                new DashboardCard("HUD", "Armor, target, inventory and status panels",
                        FeatureRegistry.countInTopCategory("HUD") + " modules", "03", 0, 0, 0, 0),
                new DashboardCard("Music", "Music Island, genre and synced lyrics",
                        "3 features", "04", 0, 0, 0, 0),
                new DashboardCard("Settings", "Appearance and visual quality",
                        "Options", "05", 0, 0, 0, 0),
                new DashboardCard("Tools", "Diagnostics and recent error logs",
                        "Error Center", "06", 0, 0, 0, 0)
        };

        int gap = 10;
        int cardWidth = (contentWidth - gap) / 2;
        int cardHeight = 58;
        int startY = py + 91;
        int rowGap = 10;

        for (int i = 0; i < cards.length; i++) {
            DashboardCard card = cards[i];
            int col = i % 2;
            int row = i / 2;
            int cardX = x + col * (cardWidth + gap);
            int cardY = startY + row * (cardHeight + rowGap);
            DashboardCard positioned = new DashboardCard(card.category(), card.description(),
                    card.meta(), card.index(), cardX, cardY, cardWidth, cardHeight);
            dashboardCards.add(positioned);
            addRenderableWidget(Button.builder(ProVisualsUi.component(""),
                    b -> openPage(card.category())).bounds(cardX, cardY, cardWidth, cardHeight).build());
        }
    }

    private void buildFeaturePage(int x, int py, int contentWidth) {
        List<String> subcategories = FeatureRegistry.subcategories(page);
        if (subcategories.isEmpty()) return;
        if (!subcategories.contains(selectedSubcategory)) selectedSubcategory = subcategories.get(0);

        String subtitle = switch (page) {
            case "PvP" -> "Combat and player-focused visual modules";
            case "Visuals" -> "World appearance, lighting and environment";
            case "HUD" -> "Information panels and interface elements";
            default -> "Visual modules and their options";
        };
        int moduleCount = FeatureRegistry.inCategory(selectedSubcategory).size();
        sectionHeaders.add(new SectionHeader(page,
                subtitle + "  /  " + moduleCount + " modules", py + 72));

        int tabY = py + 105;
        int tabX = x;
        for (String subcategory : subcategories) {
            String label = subcategoryLabel(subcategory);
            int tabWidth = Math.max(90, font.width(ProVisualsUi.displayText(label)) + 30);
            SubcategoryTab tab = new SubcategoryTab(label, tabX, tabY, tabWidth, 25);
            subcategoryTabs.add(tab);
            addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
                selectedSubcategory = subcategory;
                rebuildWidgets();
            }).bounds(tabX, tabY, tabWidth, 25).build());
            tabX += tabWidth + 8;
        }

        List<FeatureRegistry.Feature> features = FeatureRegistry.inCategory(selectedSubcategory);
        int y = py + 145;
        int rowHeight = 37;
        int rowGap = 8;
        for (FeatureRegistry.Feature feature : features) {
            FeatureRow row = new FeatureRow(feature, x, y, contentWidth, rowHeight);
            featureRows.add(row);
            addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
                ProVisualsConfig.toggle(feature.key());
                status = feature.rendererConnected()
                        ? feature.title() + " " + onOff(ProVisualsConfig.isEnabled(feature.key()))
                        : "Preference saved — visual renderer not connected yet";
                rebuildWidgets();
            }).bounds(x, y, contentWidth, rowHeight).build());
            y += rowHeight + rowGap;
        }
    }

    private void buildMusicPage(int x, int py, int contentWidth) {
        sectionHeaders.add(new SectionHeader("Music player",
                "Playback information and media controls", py + 72));
        addSettingRow("Music Island", onOff(ProVisualsConfig.musicIslandEnabled),
                "Show the compact player overlay", x, py + 112, contentWidth,
                b -> {
                    ProVisualsConfig.musicIslandEnabled = !ProVisualsConfig.musicIslandEnabled;
                    ProVisualsConfig.save();
                    status = "Music Island " + onOff(ProVisualsConfig.musicIslandEnabled);
                    rebuildWidgets();
                });

        sectionHeaders.add(new SectionHeader("Genre recognition",
                "Recognizes styles such as Hardtekk, Techno, Hardstyle and more", py + 164));
        addSettingRow("Genre lookup", onOff(ProVisualsConfig.genreRecognitionEnabled),
                "Looks up current track title and artist; no audio is sent",
                x, py + 204, contentWidth,
                b -> {
                    ProVisualsConfig.genreRecognitionEnabled = !ProVisualsConfig.genreRecognitionEnabled;
                    ProVisualsConfig.save();
                    status = "Genre recognition " + onOff(ProVisualsConfig.genreRecognitionEnabled);
                    rebuildWidgets();
                });

        sectionHeaders.add(new SectionHeader("Lyrics in the Sky",
                "Synced lyrics, typography and placement", py + 246));
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
            boolean enabled = MusicLyricsBridge.toggleLyrics();
            status = "Lyrics " + onOff(enabled);
            rebuildWidgets();
        }).bounds(x, py + 286, contentWidth, 35).build());
        drawActionRows.add(new ActionRow("Lyrics display",
                onOff(MusicLyricsBridge.isLyricsEnabled()), "Toggle the lyrics overlay",
                x, py + 286, contentWidth, 35, false));

        addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> MusicLyricsBridge.openSettings())
                .bounds(x, py + 327, contentWidth, 35).build());
        drawActionRows.add(new ActionRow("Open lyrics settings", "›",
                "Fonts, colours and lyric placement", x, py + 327, contentWidth, 35, true));
    }

    private void buildToolsPage(int x, int py, int contentWidth) {
        sectionHeaders.add(new SectionHeader("Error Center",
                "Recent events and troubleshooting information", py + 72));
        int gap = 10;
        int half = (contentWidth - gap) / 2;

        addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
            Diagnostics.info("Diagnostics refreshed by user");
            status = "Diagnostics refreshed";
        }).bounds(x, py + 116, half, 35).build());
        drawActionRows.add(new ActionRow("Refresh log", "↻",
                "Update recent diagnostic events", x, py + 116, half, 35, false));

        addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(String.join("\n", Diagnostics.recentEntries()));
            status = "Diagnostics copied";
        }).bounds(x + half + gap, py + 116, half, 35).build());
        drawActionRows.add(new ActionRow("Copy log", "↗",
                "Copy all available entries", x + half + gap, py + 116, half, 35, true));
    }

    private void addSettingRow(String title, String value, String description,
                               int x, int y, int w, Button.OnPress action) {
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), action)
                .bounds(x, y, w, 35).build());
        drawActionRows.add(new ActionRow(title, value, description, x, y, w, 35, false));
    }

    private void buildSettingsPage(int x, int py, int contentWidth) {
        int gap = 10;
        int half = (contentWidth - gap) / 2;

        sectionHeaders.add(new SectionHeader("Reflections",
                "Quality and coverage of reflective surfaces", py + 72));
        addSettingRow("Intensity", percent(ProVisualsConfig.reflectionIntensity),
                "Cycle reflection strength", x, py + 112, half,
                b -> { ProVisualsConfig.reflectionIntensity = cycle(ProVisualsConfig.reflectionIntensity); ProVisualsConfig.save(); rebuildWidgets(); });
        addSettingRow("Quality", ProVisualsConfig.reflectionQuality,
                "Cycle rendering quality", x + half + gap, py + 112, half,
                b -> { ProVisualsConfig.reflectionQuality = nextQuality(ProVisualsConfig.reflectionQuality); ProVisualsConfig.save(); rebuildWidgets(); });
        addSettingRow("Surfaces", surfaceLabel(ProVisualsConfig.reflectiveSurfaces),
                "Choose which surfaces reflect", x, py + 155, contentWidth,
                b -> { ProVisualsConfig.reflectiveSurfaces = nextSurface(ProVisualsConfig.reflectiveSurfaces); ProVisualsConfig.save(); rebuildWidgets(); });

        sectionHeaders.add(new SectionHeader("Ore glow",
                "Choose how selected ores are highlighted", py + 207));
        addSettingRow("Interior glow", onOff(ProVisualsConfig.oreInteriorGlow),
                "Glow inside ore blocks", x, py + 247, half,
                b -> { ProVisualsConfig.oreInteriorGlow = !ProVisualsConfig.oreInteriorGlow; ProVisualsConfig.save(); rebuildWidgets(); });
        addSettingRow("Outline glow", onOff(ProVisualsConfig.oreOutlineGlow),
                "Glow around ore outlines", x + half + gap, py + 247, half,
                b -> { ProVisualsConfig.oreOutlineGlow = !ProVisualsConfig.oreOutlineGlow; ProVisualsConfig.save(); rebuildWidgets(); });

        sectionHeaders.add(new SectionHeader("Interface",
                "Appearance of the ProVisuals menu", py + 285));
        addSettingRow("Corner style", ProVisualsConfig.roundedCorners ? "Rounded" : "Square",
                "Switch between rounded and square corners", x, py + 325, contentWidth,
                b -> { ProVisualsConfig.roundedCorners = !ProVisualsConfig.roundedCorners; ProVisualsConfig.save(); rebuildWidgets(); });
    }

    private void openPage(String destination) {
        page = destination;
        status = "Page: " + destination;
        rebuildWidgets();
    }

    private static String subcategoryLabel(String value) {
        return switch (value) {
            case "Combat Visuals" -> "Combat";
            case "Player Visuals" -> "Player";
            case "World Visuals" -> "World";
            case "HUD & Interface" -> "HUD elements";
            default -> value;
        };
    }

    private int panelX() { return Math.max(12, width / 2 - 366); }
    private int panelY() { return Math.max(8, height / 2 - 196); }

    private void addNavigationBar() {
        navigationButtons.clear();
        int barWidth = Math.max(180, Math.min(720, width - 16));
        int gap = width < 540 ? 2 : NAV_BAR_GAP;
        int buttonWidth = Math.max(1, (barWidth - gap * (FeatureRegistry.CATEGORIES.size() - 1))
                / FeatureRegistry.CATEGORIES.size());
        int totalWidth = buttonWidth * FeatureRegistry.CATEGORIES.size()
                + gap * (FeatureRegistry.CATEGORIES.size() - 1);
        int startX = (width - totalWidth) / 2;

        for (String category : FeatureRegistry.CATEGORIES) {
            Button button = Button.builder(ProVisualsUi.component(navBarLabel(category)),
                    b -> openPage(category))
                    .bounds(startX, height + 24, buttonWidth, 22).build();
            addRenderableWidget(button);
            navigationButtons.add(button);
            startX += buttonWidth + gap;
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
        float step = elapsedSeconds / 0.22f;
        navigationProgress = hoveringDock
                ? Math.min(1.0f, navigationProgress + step)
                : Math.max(0.0f, navigationProgress - step);

        int barTop = height - NAV_BAR_HEIGHT
                + Math.round((1.0f - navigationProgress) * (NAV_BAR_HEIGHT + 22));
        for (Button button : navigationButtons) button.setY(barTop + 16);
    }

    private static String navBarLabel(String category) {
        return switch (category) {
            case "Overview" -> "Home";
            case "PvP" -> "PvP";
            case "Visuals" -> "Visuals";
            case "HUD" -> "HUD";
            case "Music" -> "Music";
            case "Settings" -> "Settings";
            case "Tools" -> "Tools";
            default -> category;
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
        return switch (value) { case "SELECTED" -> "Selected blocks"; case "WATER_ONLY" -> "Water only"; default -> "All blocks"; };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xF2090911, 0xF2171027);
        int px = panelX();
        int py = panelY();
        ProVisualsUi.fill(graphics, px, py, Math.min(732, width - 24), 392, 18, 0xF0151520);
        ProVisualsUi.fill(graphics, px, py, 3, 392, 2, 0xFF9A6BFF);
        ProVisualsUi.fill(graphics, px + 16, py + 54, Math.min(700, width - 56), 1, 1, 0x555E536F);

        updateNavigationBar(mouseY);
        int barWidth = Math.max(180, Math.min(720, width - 16));
        int barX = (width - barWidth) / 2;
        int barTop = height - NAV_BAR_HEIGHT
                + Math.round((1.0f - navigationProgress) * (NAV_BAR_HEIGHT + 22));
        ProVisualsUi.fill(graphics, barX - 8, barTop, barWidth + 16,
                Math.max(0, height - barTop), 14, 0xF0181726);
        ProVisualsUi.fill(graphics, barX - 8, barTop, barWidth + 16, 2, 1, 0xFF9A6BFF);

        int alpha = Math.round(220 * (1.0f - navigationProgress));
        if (alpha > 0) {
            ProVisualsUi.fill(graphics, width / 2 - 34, height - 5, 68, 3, 2,
                    (alpha << 24) | 0x009A6BFF);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (ProVisualsConfig.roundedCorners) {
            for (var child : children()) {
                if (child instanceof Button button) {
                    ProVisualsUi.roundWidgetCorners(graphics, button.getX(), button.getY(),
                            button.getWidth(), button.getHeight(), 6, 0xF0151520);
                }
            }
        }

        int x = panelX() + 20;
        int y = panelY() + 17;
        ProVisualsUi.text(graphics, font, "ProVisuals", x, y, 0xFFFFFFFF, true);
        ProVisualsUi.text(graphics, font, "Visual client  /  Modern violet", x, y + 16, 0xFFBCA4FF, true);

        switch (page) {
            case "Overview" -> renderOverview(graphics, mouseX, mouseY);
            case "Music" -> renderMusic(graphics, mouseX, mouseY);
            case "Tools" -> renderTools(graphics, mouseX, mouseY);
            case "Settings" -> renderSettings(graphics, mouseX, mouseY);
            default -> renderFeaturePage(graphics, mouseX, mouseY);
        }

        ProVisualsUi.text(graphics, font, status, panelX() + 20, height / 2 + 174, 0xFFBCA4FF, true);
    }

    private void renderOverview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = panelX() + 28;
        int y = panelY() + 65;
        ProVisualsUi.text(graphics, font, "Your visual workspace", x, y, 0xFFFFFFFF, true);
        ProVisualsUi.text(graphics, font, "Choose a top-level category to see its features.",
                x, y + 15, 0xFFAAA4BA, false);

        for (DashboardCard card : dashboardCards) {
            boolean hovered = inside(card.x(), card.y(), card.width(), card.height(), mouseX, mouseY);
            ProVisualsUi.fill(graphics, card.x(), card.y(), card.width(), card.height(), 13,
                    hovered ? 0xFF282338 : 0xFF1D1B29);
            ProVisualsUi.fill(graphics, card.x() + 1, card.y() + 10, 2, card.height() - 20, 1,
                    hovered ? 0xFFBCA4FF : 0xFF7653B8);
            ProVisualsUi.text(graphics, font, card.index(), card.x() + 13, card.y() + 10, 0xFF9A6BFF, false);
            ProVisualsUi.text(graphics, font, card.category(), card.x() + 42,
                    card.y() + 9, 0xFFF1EDFA, true);
            ProVisualsUi.text(graphics, font, card.description(), card.x() + 42,
                    card.y() + 25, 0xFFAAA4BA, false);
            ProVisualsUi.text(graphics, font, card.meta(),
                    card.x() + card.width() - font.width(ProVisualsUi.displayText(card.meta())) - 12,
                    card.y() + card.height() - 13, 0xFF9A86C3, false);
        }
    }

    private void renderFeaturePage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
        for (SubcategoryTab tab : subcategoryTabs) {
            boolean selected = tab.title().equals(subcategoryLabel(selectedSubcategory));
            boolean hovered = inside(tab.x(), tab.y(), tab.width(), tab.height(), mouseX, mouseY);
            ProVisualsUi.fill(graphics, tab.x(), tab.y(), tab.width(), tab.height(), 10,
                    selected ? 0xFF443263 : hovered ? 0xFF302B3E : 0xFF211F2D);
            ProVisualsUi.text(graphics, font, tab.title(), tab.x() + 13, tab.y() + 8,
                    selected ? 0xFFF5F1FF : 0xFFB7B0C5, true);
        }

        for (FeatureRow row : featureRows) {
            FeatureRegistry.Feature feature = row.feature();
            boolean hovered = inside(row.x(), row.y(), row.width(), row.height(), mouseX, mouseY);
            ProVisualsUi.fill(graphics, row.x(), row.y(), row.width(), row.height(), 11,
                    hovered ? 0xFF282338 : 0xFF1D1B29);
            ProVisualsUi.text(graphics, font, feature.title(),
                    row.x() + 14, row.y() + 7, 0xFFF1EDFA, true);
            ProVisualsUi.text(graphics, font, feature.description(),
                    row.x() + 14, row.y() + 21, 0xFFAAA4BA, false);

            String state = ProVisualsConfig.isEnabled(feature.key()) ? "ON" : "OFF";
            int pillWidth = 44;
            int pillX = row.x() + row.width() - pillWidth - 11;
            ProVisualsUi.fill(graphics, pillX, row.y() + 10, pillWidth, 17, 8,
                    ProVisualsConfig.isEnabled(feature.key()) ? 0xFF3C3154 : 0xFF302B37);
            graphics.centeredText(font, ProVisualsUi.component(state), pillX + pillWidth / 2,
                    row.y() + 14, feature.rendererConnected() ? 0xFFD0BFFF : 0xFFB8AFC2);
        }

        if (featureRows.stream().anyMatch(row -> !row.feature().rendererConnected())) {
            ProVisualsUi.text(graphics, font,
                    "Some modules only save preferences until their visual renderer is connected.",
                    panelX() + 28, panelY() + 347, 0xFFE8C987, false);
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

    private void renderTools(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
        for (ActionRow row : drawActionRows) renderActionRow(graphics, row, mouseX, mouseY);

        int listX = panelX() + 28;
        int listY = panelY() + 164;
        int listWidth = Math.min(660, width - listX - 28);
        ProVisualsUi.fill(graphics, listX, listY, listWidth, 150, 13, 0xFF1D1B29);
        ProVisualsUi.text(graphics, font, "Recent events", listX + 14, listY + 13, 0xFFD6C8F5, true);
        List<String> entries = Diagnostics.recentEntries();
        if (entries.isEmpty()) {
            ProVisualsUi.text(graphics, font, "No ProVisuals errors recorded this session.",
                    listX + 14, listY + 39, 0xFFAAA4BA, false);
        } else {
            int lineY = listY + 37;
            for (String entry : entries.stream().limit(7).toList()) {
                String compact = entry.length() > 84 ? entry.substring(0, 81) + "..." : entry;
                ProVisualsUi.text(graphics, font, compact, listX + 14, lineY, 0xFFD6D2E0, false);
                lineY += 15;
            }
        }
        ProVisualsUi.text(graphics, font, "Log file  /  config/provisuals-diagnostics.log",
                listX + 14, listY + 130, 0xFF8F86A3, false);
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
        ProVisualsUi.fill(graphics, row.x(), row.y(), row.width(), row.height(), 11,
                hovered ? 0xFF282338 : 0xFF1D1B29);
        ProVisualsUi.text(graphics, font, row.title(), row.x() + 14, row.y() + 7,
                0xFFF1EDFA, true);
        ProVisualsUi.text(graphics, font, row.description(), row.x() + 14, row.y() + 21,
                0xFFAAA4BA, false);
        int valueWidth = font.width(ProVisualsUi.displayText(row.value()));
        ProVisualsUi.text(graphics, font, row.value(),
                row.x() + row.width() - valueWidth - 14, row.y() + 12,
                row.secondary() ? 0xFFBCA4FF : 0xFFD0BFFF, true);
    }

    private static boolean inside(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
