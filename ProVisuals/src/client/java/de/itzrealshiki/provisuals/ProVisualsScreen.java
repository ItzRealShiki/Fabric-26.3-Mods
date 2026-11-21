package de.itzrealshiki.provisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class ProVisualsScreen extends Screen {
    private record FeatureRow(FeatureRegistry.Feature feature, int x, int y, int width, int height) {}
    private record SectionHeader(String title, String subtitle, int y) {}
    private record SubcategoryTab(String title, int x, int y, int width, int height) {}
    private record ActionRow(String title, String value, String description,
                             int x, int y, int width, int height, boolean secondary) {}

    private String page = "Overview";
    private String selectedSubcategory = "Combat Visuals";
    private String status = "All systems ready";

    private final List<FeatureRow> featureRows = new ArrayList<>();
    private final List<SectionHeader> sectionHeaders = new ArrayList<>();
    private final List<ActionRow> drawActionRows = new ArrayList<>();

    public ProVisualsScreen() {
        super(ProVisualsUi.component("ProVisuals"));
    }

    @Override
    protected void init() {
        clearWidgets();
        featureRows.clear();
        sectionHeaders.clear();
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
    }

    private void buildOverview(int x, int py, int contentWidth) {
        sectionHeaders.add(new SectionHeader("Welcome",
                "Open a category from the Music Island", py + 76));
    }

    private void buildFeaturePage(int x, int py, int contentWidth) {
        List<String> subcategories = categorySubcategories(page);
        if (subcategories.isEmpty()) return;
        if (!subcategories.contains(selectedSubcategory)) selectedSubcategory = subcategories.get(0);

        String subtitle = switch (page) {
            case "PvP" -> "Combat and player-focused visual modules";
            case "Glowing" -> "Block, armor and item glow categories";
            case "World" -> "World appearance, lighting and environment";
            case "HUD" -> "Information panels and interface elements";
            default -> "Visual modules and their options";
        };
        int moduleCount = FeatureRegistry.inCategory(selectedSubcategory).size();
        sectionHeaders.add(new SectionHeader(subcategoryLabel(selectedSubcategory),
                subtitle + "  /  " + moduleCount + " modules", py + 72));

        int y = py + 112;
        int rowHeight = 37;
        int rowGap = 8;
        for (FeatureRegistry.Feature feature : FeatureRegistry.inCategory(selectedSubcategory)) {
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
        switch (selectedSubcategory) {
            case "Genres" -> {
                sectionHeaders.add(new SectionHeader("Genre recognition",
                        "Recognizes Hardtekk, Techno, Hardstyle and more", py + 76));
                addSettingRow("Genre lookup", onOff(ProVisualsConfig.genreRecognitionEnabled),
                        "Track title and artist only; no audio is sent",
                        x, py + 116, contentWidth,
                        b -> {
                            ProVisualsConfig.genreRecognitionEnabled = !ProVisualsConfig.genreRecognitionEnabled;
                            ProVisualsConfig.save();
                            status = "Genre recognition " + onOff(ProVisualsConfig.genreRecognitionEnabled);
                            rebuildWidgets();
                        });
            }
            case "Lyrics" -> {
                addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> {
                    boolean enabled = MusicLyricsBridge.toggleLyrics();
                    status = "Lyrics " + onOff(enabled);
                    rebuildWidgets();
                }).bounds(x, py + 100, contentWidth, 30).build());
                drawActionRows.add(new ActionRow("Lyrics", onOff(MusicLyricsBridge.isLyricsEnabled()),
                        "Synced lyrics overlay", x, py + 100, contentWidth, 30, false));

                addRenderableWidget(Button.builder(ProVisualsUi.component(""), b -> MusicLyricsBridge.openSettings())
                        .bounds(x, py + 136, contentWidth, 30).build());
                drawActionRows.add(new ActionRow("Lyrics settings", "›",
                        "Typography and placement", x, py + 136, contentWidth, 30, true));
            }
            default -> {
                sectionHeaders.add(new SectionHeader("Music player",
                        "Playback information and media controls", py + 76));
                addSettingRow("Music Island", onOff(ProVisualsConfig.musicIslandEnabled),
                        "Show the compact player overlay", x, py + 116, contentWidth,
                        b -> {
                            ProVisualsConfig.musicIslandEnabled = !ProVisualsConfig.musicIslandEnabled;
                            ProVisualsConfig.save();
                            status = "Music Island " + onOff(ProVisualsConfig.musicIslandEnabled);
                            rebuildWidgets();
                        });
            }
        }
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
        switch (selectedSubcategory) {
            case "Ore Glow" -> {
                sectionHeaders.add(new SectionHeader("Ore glow",
                        "Choose how selected ores are highlighted", py + 76));
                addSettingRow("Interior glow", onOff(ProVisualsConfig.oreInteriorGlow),
                        "Glow inside ore blocks", x, py + 116, half,
                        b -> { ProVisualsConfig.oreInteriorGlow = !ProVisualsConfig.oreInteriorGlow; ProVisualsConfig.save(); rebuildWidgets(); });
                addSettingRow("Outline glow", onOff(ProVisualsConfig.oreOutlineGlow),
                        "Glow around ore outlines", x + half + gap, py + 116, half,
                        b -> { ProVisualsConfig.oreOutlineGlow = !ProVisualsConfig.oreOutlineGlow; ProVisualsConfig.save(); rebuildWidgets(); });
            }
            case "Interface" -> {
                sectionHeaders.add(new SectionHeader("Interface",
                        "Appearance of the ProVisuals menu", py + 76));
                addSettingRow("Corner style", ProVisualsConfig.roundedCorners ? "Rounded" : "Square",
                        "Switch between rounded and square corners", x, py + 116, contentWidth,
                        b -> { ProVisualsConfig.roundedCorners = !ProVisualsConfig.roundedCorners; ProVisualsConfig.save(); rebuildWidgets(); });
            }
            default -> {
                sectionHeaders.add(new SectionHeader("Reflections",
                        "Quality and coverage of reflective surfaces", py + 76));
                addSettingRow("Intensity", percent(ProVisualsConfig.reflectionIntensity),
                        "Cycle reflection strength", x, py + 116, half,
                        b -> { ProVisualsConfig.reflectionIntensity = cycle(ProVisualsConfig.reflectionIntensity); ProVisualsConfig.save(); rebuildWidgets(); });
                addSettingRow("Quality", ProVisualsConfig.reflectionQuality,
                        "Cycle rendering quality", x + half + gap, py + 116, half,
                        b -> { ProVisualsConfig.reflectionQuality = nextQuality(ProVisualsConfig.reflectionQuality); ProVisualsConfig.save(); rebuildWidgets(); });
                addSettingRow("Surfaces", surfaceLabel(ProVisualsConfig.reflectiveSurfaces),
                        "Choose which surfaces reflect", x, py + 159, contentWidth,
                        b -> { ProVisualsConfig.reflectiveSurfaces = nextSurface(ProVisualsConfig.reflectiveSurfaces); ProVisualsConfig.save(); rebuildWidgets(); });
            }
        }
    }

    private void openPage(String destination) {
        page = destination;
        List<String> subcategories = categorySubcategories(destination);
        if (!subcategories.isEmpty()) selectedSubcategory = subcategories.get(0);
        status = "Page: " + destination;
        rebuildWidgets();
    }

    public static void openFromMusicIsland(String category, String subcategory) {
        ProVisualsScreen screen = new ProVisualsScreen();
        screen.page = category;
        if (subcategory != null && categorySubcategories(category).contains(subcategory)) {
            screen.selectedSubcategory = subcategory;
        }
        Minecraft.getInstance().gui.setScreen(screen);
    }

    private void selectSubcategory(String subcategory) {
        selectedSubcategory = subcategory;
        status = subcategoryLabel(subcategory);
        rebuildWidgets();
    }

    public static List<String> categorySubcategories(String category) {
        return switch (category) {
            case "Music" -> List.of("Player", "Genres", "Lyrics");
            case "Settings" -> List.of("Reflections", "Ore Glow", "Interface");
            case "Tools" -> List.of("Error Center");
            default -> FeatureRegistry.subcategories(category);
        };
    }

    private static String subcategoryLabel(String value) {
        return switch (value) {
            case "Combat Visuals" -> "Combat";
            case "Player Visuals" -> "Player";
            case "World Visuals" -> "Environment";
            case "HUD & Interface" -> "HUD elements";
            case "Block Glow" -> "Blocks";
            case "Armor Glow" -> "Armor";
            case "Item Glow" -> "Items";
            case "Genres" -> "Genres";
            case "Lyrics" -> "Lyrics";
            case "Ore Glow" -> "Ore glow";
            case "Error Center" -> "Error Center";
            default -> value;
        };
    }

    private int panelX() { return Math.max(12, width / 2 - 366); }
    private int panelY() { return Math.max(8, height / 2 - 196); }

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

        ProVisualsUi.text(graphics, font, status, panelX() + 20, panelY() + 372, 0xFFBCA4FF, true);
    }

    private void renderOverview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
        ProVisualsUi.text(graphics, font, "ProVisuals", panelX() + 28,
                panelY() + 126, 0xFFF1EDFA, true);
        ProVisualsUi.text(graphics, font, "Select a category on the right.",
                panelX() + 28, panelY() + 145, 0xFFAAA4BA, false);
    }

    private void renderFeaturePage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
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
        int valueWidth = ProVisualsUi.width(font, row.value());
        ProVisualsUi.text(graphics, font, row.value(),
                row.x() + row.width() - valueWidth - 14, row.y() + 12,
                row.secondary() ? 0xFFBCA4FF : 0xFFD0BFFF, true);
    }

    private static boolean inside(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
