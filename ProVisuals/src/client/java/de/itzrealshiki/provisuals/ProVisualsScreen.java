package de.itzrealshiki.provisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class ProVisualsScreen extends Screen {
    private record FeatureRow(FeatureRegistry.Feature feature, int x, int y, int width, int height) {}
    private record NavigationRow(String label, String category, String subcategory, int x, int y, int width, int height) {}
    private record SectionHeader(String title, String subtitle, int y) {}
    private record SubcategoryTab(String title, int x, int y, int width, int height) {}
    private record ActionRow(String title, String value, String description,
                             int x, int y, int width, int height, boolean secondary) {}
    private record AccentSwatch(String palette, int x, int y, int width, int height, int color) {}
    private record FriendRow(String name, int color, boolean online,
                             int x, int y, int width, int height) {}

    private String page = "Overview";
    private String selectedSubcategory = "Combat Visuals";
    private String status = "All systems ready";
    private EditBox friendNameInput;

    private final List<FeatureRow> featureRows = new ArrayList<>();
    private final List<NavigationRow> navigationRows = new ArrayList<>();
    private final List<SectionHeader> sectionHeaders = new ArrayList<>();
    private final List<ActionRow> drawActionRows = new ArrayList<>();
    private final List<AccentSwatch> accentSwatches = new ArrayList<>();
    private final List<FriendRow> friendRows = new ArrayList<>();

    public ProVisualsScreen() {
        super(ProVisualsUi.component("ProVisuals"));
    }

    @Override
    protected void init() {
        clearWidgets();
        featureRows.clear();
        navigationRows.clear();
        sectionHeaders.clear();
        drawActionRows.clear();
        accentSwatches.clear();
        friendRows.clear();
        friendNameInput = null;

        int contentX = panelX() + 164;
        int contentWidth = Math.max(220, Math.min(520, width - contentX - 28));
        int py = panelY();
        buildNavigation(py);

        switch (page) {
            case "Overview" -> buildOverview(contentX, py, contentWidth);
            case "Music" -> buildMusicPage(contentX, py, contentWidth);
            case "Tools" -> buildToolsPage(contentX, py, contentWidth);
            case "Settings" -> buildSettingsPage(contentX, py, contentWidth);
            default -> buildFeaturePage(contentX, py, contentWidth);
        }
    }

    private void buildNavigation(int py) {
        int x = panelX() + 18;
        int y = py + 68;
        int width = 122;

        for (String category : FeatureRegistry.CATEGORIES) {
            addNavigationRow(navigationLabel(category), category, null, x, y, width, 21);
            y += 23;
            if (category.equals(page)) {
                for (String subcategory : categorySubcategories(category)) {
                    addNavigationRow("  " + subcategoryLabel(subcategory), category,
                            subcategory, x + 5, y, width - 5, 18);
                    y += 19;
                }
                y += 3;
            }
        }
    }

    private void addNavigationRow(String label, String category, String subcategory,
                                  int x, int y, int width, int height) {
        navigationRows.add(new NavigationRow(label, category, subcategory, x, y, width, height));
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), button -> {
            page = category;
            if (subcategory == null) {
                List<String> subcategories = categorySubcategories(category);
                if (!subcategories.isEmpty()) selectedSubcategory = subcategories.get(0);
                status = "Page: " + navigationLabel(category);
            } else {
                selectedSubcategory = subcategory;
                if ("World".equals(category) && "Sky Lyrics".equals(subcategory)) {
                    selectedSubcategory = "World Visuals";
                    if (!MusicLyricsBridge.openNativeSkySettings(this)) {
                        status = "SkyLyrics is not ready — try again in a moment";
                        rebuildWidgets();
                    }
                    return;
                }
                status = subcategoryLabel(subcategory);
            }
            rebuildWidgets();
        }).bounds(x, y, width, height).build());
    }

    private static String navigationLabel(String category) {
        return "Overview".equals(category) ? "Home" : category;
    }

    private void renderNavigation(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (NavigationRow row : navigationRows) {
            boolean selected = row.subcategory() == null
                    ? row.category().equals(page)
                    : row.category().equals(page) && row.subcategory().equals(selectedSubcategory);
            boolean hovered = inside(row.x(), row.y(), row.width(), row.height(), mouseX, mouseY);
            int background = selected ? ProVisualsUi.accentDeep() : hovered ? 0xFF262334 : 0x001D1B29;
            if ((background >>> 24) != 0) {
                ProVisualsUi.fill(graphics, row.x(), row.y(), row.width(), row.height(), 7, background);
            }
            int color = selected ? ProVisualsUi.accentLight()
                    : row.subcategory() != null ? 0xFFAAA4BA : 0xFFD1CADF;
            ProVisualsUi.text(graphics, font, row.label(), row.x() + 8, row.y() + 6,
                    color, selected);
        }
    }

    private void buildOverview(int x, int py, int contentWidth) {
        sectionHeaders.add(new SectionHeader("Welcome",
                "Choose a category from the main navigation", py + 76));

        int gap = 7;
        int swatchWidth = Math.max(28, (contentWidth - gap * 5) / 6);
        int swatchY = py + 206;
        int swatchHeight = 35;
        for (int i = 0; i < ProVisualsConfig.ACCENT_PALETTES.size(); i++) {
            String palette = ProVisualsConfig.ACCENT_PALETTES.get(i);
            int swatchX = x + i * (swatchWidth + gap);
            accentSwatches.add(new AccentSwatch(palette, swatchX, swatchY,
                    swatchWidth, swatchHeight, ProVisualsUi.accentSwatch(palette)));
            addRenderableWidget(Button.builder(ProVisualsUi.component(""), button -> {
                ProVisualsConfig.accentPalette = palette;
                ProVisualsConfig.save();
                status = "Accent color: " + palette;
                rebuildWidgets();
            }).bounds(swatchX, swatchY, swatchWidth, swatchHeight).build());
        }
    }

    private void buildFeaturePage(int x, int py, int contentWidth) {
        if ("PvP".equals(page) && "Player Visuals".equals(selectedSubcategory)) {
            buildFriendsPage(x, py, contentWidth);
            return;
        }
        if ("World".equals(page) && "Sky Lyrics".equals(selectedSubcategory)) {
            buildSkyLyricsPage(x, py, contentWidth);
            return;
        }
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
                boolean enabled = ProVisualsConfig.isEnabled(feature.key());
                if ("custom_sky".equals(feature.key())) {
                    boolean applied = CustomSkyBridge.setEnabled(enabled);
                    status = applied
                            ? CustomSkyBridge.statusText(enabled)
                            : "Nuit is missing or could not be controlled — check the mod installation";
                } else {
                    status = feature.rendererConnected()
                            ? feature.title() + " " + onOff(enabled)
                            : "Preference saved — visual renderer not connected yet";
                }
                rebuildWidgets();
            }).bounds(x, y, contentWidth, rowHeight).build());
            y += rowHeight + rowGap;
        }

        if ("PvP".equals(page) && "Combat Visuals".equals(selectedSubcategory)) {
            addSettingRow("Only on Mace Hit", onOff(ProVisualsConfig.onlyOnMaceHit),
                    "Only trigger the enabled combat visuals when the held weapon is a Mace",
                    x, y + 2, contentWidth,
                    button -> {
                        ProVisualsConfig.onlyOnMaceHit = !ProVisualsConfig.onlyOnMaceHit;
                        ProVisualsConfig.save();
                        status = "Only on Mace Hit " + onOff(ProVisualsConfig.onlyOnMaceHit);
                        rebuildWidgets();
                    });
            addSettingRow("Kill Effect Style", killEffectLabel(ProVisualsConfig.killEffectStyle),
                    "Choose the effect for normal player kills; friends use their own-color lightning",
                    x, y + 43, contentWidth,
                    button -> {
                        ProVisualsConfig.killEffectStyle = nextKillEffectStyle(ProVisualsConfig.killEffectStyle);
                        ProVisualsConfig.save();
                        status = "Kill Effect: " + killEffectLabel(ProVisualsConfig.killEffectStyle);
                        rebuildWidgets();
                    });
        }
    }

    private void buildFriendsPage(int x, int py, int contentWidth) {
        sectionHeaders.add(new SectionHeader("Visual Friends",
                "Personal accent colors, local glow and current-server presence", py + 72));

        addSettingRow("Friend outlines", onOff(ProVisualsConfig.isEnabled("visual_friends")),
                "Use each friend's personal color for their player and armor glow", x, py + 108, contentWidth,
                b -> {
                    ProVisualsConfig.toggle("visual_friends");
                    status = "Friend outlines " + onOff(ProVisualsConfig.isEnabled("visual_friends"));
                    rebuildWidgets();
                });

        int addButtonWidth = 82;
        int gap = 8;
        friendNameInput = new EditBox(font, x, py + 151, contentWidth - addButtonWidth - gap,
                31, ProVisualsUi.component("Minecraft username"));
        friendNameInput.setMaxLength(16);
        addRenderableWidget(friendNameInput);

        addRenderableWidget(Button.builder(ProVisualsUi.component("Add"), button -> {
            String name = friendNameInput == null ? "" : friendNameInput.getValue();
            if (FriendManager.add(name)) {
                status = "Added friend: " + name.trim();
            } else {
                status = "Enter a valid, non-duplicate Minecraft username";
            }
            rebuildWidgets();
        }).bounds(x + contentWidth - addButtonWidth, py + 151, addButtonWidth, 31).build());

        int y = py + 194;
        List<String> friends = FriendManager.friends();
        if (friends.isEmpty()) {
            drawActionRows.add(new ActionRow("No friends added", "",
                    "Add a Minecraft username above", x, y, contentWidth, 34, false));
            return;
        }

        int removeWidth = 62;
        int rowGap = 7;
        for (String friend : friends.stream().limit(4).toList()) {
            int rowWidth = contentWidth - removeWidth - rowGap;
            friendRows.add(new FriendRow(friend, FriendManager.colorForName(friend),
                    FriendManager.isOnline(friend), x, y, rowWidth, 31));
            addRenderableWidget(Button.builder(ProVisualsUi.component(""), button -> {
                int color = FriendManager.cycleColor(friend);
                status = friend + " color: " + FriendManager.colorNameFor(friend);
                rebuildWidgets();
            }).bounds(x, y, rowWidth, 31).build());

            addRenderableWidget(Button.builder(ProVisualsUi.component(""), button -> {
                FriendManager.remove(friend);
                status = "Removed friend: " + friend;
                rebuildWidgets();
            }).bounds(x + rowWidth + rowGap, y, removeWidth, 31).build());

            drawActionRows.add(new ActionRow("Remove", "×",
                    "Remove " + friend + " from your local friends list",
                    x + rowWidth + rowGap, y, removeWidth, 31, true));
            y += 36;
        }

        drawActionRows.add(new ActionRow("Presence", "Current server only",
                "Shows when a listed friend appears in this server's player list",
                x, py + 345, contentWidth, 30, false));
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
            case "Lyrics" -> buildSkyLyricsPage(x, py, contentWidth);
            default -> {
                sectionHeaders.add(new SectionHeader("Music player",
                        "Playback information and media controls", py + 76));
                addSettingRow("SkyLyrics Music Island", onOff(MusicLyricsBridge.booleanSetting("showHud", true)),
                        "Show the original SkyLyrics player overlay", x, py + 116, contentWidth,
                        b -> {
                            boolean enabled = !MusicLyricsBridge.booleanSetting("showHud", true);
                            if (MusicLyricsBridge.setNowPlayingHudEnabled(enabled)) {
                                status = "SkyLyrics Music Island " + onOff(enabled);
                            } else {
                                status = "SkyLyrics is not ready — try again in a moment";
                            }
                            rebuildWidgets();
                        });
            }
        }
    }

    private void buildSkyLyricsPage(int x, int py, int contentWidth) {
        sectionHeaders.add(new SectionHeader("Sky Lyrics",
                "Placement, style, colors and animation for lyrics in the world", py + 72));
        if (!MusicLyricsBridge.isPresent()) {
            addLyricsOption("SkyLyrics module", "Unavailable",
                    "The bundled lyrics module could not be found", x, py + 112, contentWidth,
                    () -> status = "SkyLyrics module is not loaded");
            return;
        }

        int gap = 10;
        int half = (contentWidth - gap) / 2;
        int y = py + 104;
        int row = 34;

        addLyricsOption("Lyrics overlay", onOff(MusicLyricsBridge.isLyricsEnabled()),
                "Show or hide synced lyrics", x, y, contentWidth,
                () -> status = "Lyrics " + onOff(MusicLyricsBridge.toggleLyrics()));
        y += row;

        addLyricsOption("Placement", lyricsLabel(MusicLyricsBridge.stringSetting("mode", "sky")),
                "Sky or around the player", x, y, half,
                () -> cycleLyricsChoice("mode", "sky", "around"));
        addLyricsOption("Style", lyricsLabel(MusicLyricsBridge.stringSetting("style", "elegant")),
                "Animation and line layout", x + half + gap, y, half,
                () -> cycleLyricsChoice("style", "elegant", "natural", "flow", "classic"));
        y += row;

        addLyricsOption("Font", lyricsLabel(MusicLyricsBridge.stringSetting("font", "sans")),
                "SkyLyrics text font", x, y, half,
                () -> cycleLyricsChoice("font", "sans", "script", "minecraft"));
        addLyricsOption("Palette", lyricsLabel(MusicLyricsBridge.stringSetting("palette", "standard")),
                "Text and glow colors", x + half + gap, y, half,
                () -> cycleLyricsChoice("palette", "standard", "rose", "lavender", "auto",
                        "aurora", "sunset", "gold", "sakura", "ocean", "white"));
        y += row;

        addLyricsOption("Text size", String.format(java.util.Locale.ROOT, "%.1fx",
                        MusicLyricsBridge.numberSetting("scale", 1.0f)),
                "Cycle lyric text scale", x, y, half,
                () -> cycleLyricsNumber("scale", 0.7f, 0.85f, 1.0f, 1.15f, 1.3f, 1.5f));
        addLyricsOption("Opacity", Math.round(MusicLyricsBridge.numberSetting("opacity", 0.82f) * 100) + "%",
                "Cycle lyric opacity", x + half + gap, y, half,
                () -> cycleLyricsNumber("opacity", 0.4f, 0.55f, 0.7f, 0.82f, 0.9f, 1.0f));
        y += row;

        addLyricsOption("Behind terrain", onOff(MusicLyricsBridge.booleanSetting("skyBehindTerrain", true)),
                "Mountains and blocks hide sky lyrics", x, y, half,
                () -> toggleLyricsBoolean("skyBehindTerrain", true));
        addLyricsOption("Follow view", onOff(MusicLyricsBridge.booleanSetting("followView", true)),
                "Place new lines along your view", x + half + gap, y, half,
                () -> toggleLyricsBoolean("followView", true));
        y += row;

        addLyricsOption("Beat bounce", onOff(MusicLyricsBridge.booleanSetting("beatBounce", false)),
                "Lyrics react to the beat", x, y, half,
                () -> toggleLyricsBoolean("beatBounce", false));
        addLyricsOption("Title card", onOff(MusicLyricsBridge.booleanSetting("showTitleCard", true)),
                "Show title and artist for new tracks", x + half + gap, y, half,
                () -> toggleLyricsBoolean("showTitleCard", true));
        y += row;

        addLyricsOption("Follow along", onOff(MusicLyricsBridge.booleanSetting("followAlong", false)),
                "Lyrics move with your camera", x + half + gap, y, half,
                () -> toggleLyricsBoolean("followAlong", false));
    }

    private void addLyricsOption(String title, String value, String description,
                                 int x, int y, int width, Runnable action) {
        addRenderableWidget(Button.builder(ProVisualsUi.component(""), button -> {
            action.run();
            rebuildWidgets();
        }).bounds(x, y, width, 31).build());
        drawActionRows.add(new ActionRow(title, value, description, x, y, width, 31, false));
    }

    private void cycleLyricsChoice(String field, String... values) {
        String next = MusicLyricsBridge.cycleStringSetting(field, values);
        status = "SkyLyrics " + field + ": " + lyricsLabel(next);
    }

    private void cycleLyricsNumber(String field, float... values) {
        float next = MusicLyricsBridge.cycleNumberSetting(field, values);
        status = "SkyLyrics " + field + ": " + String.format(java.util.Locale.ROOT, "%.2f", next);
    }

    private void toggleLyricsBoolean(String field, boolean fallback) {
        boolean enabled = MusicLyricsBridge.toggleBooleanSetting(field, fallback);
        status = "SkyLyrics " + field + " " + onOff(enabled);
    }

    private static String lyricsLabel(String value) {
        return switch (value) {
            case "sky" -> "Sky";
            case "around" -> "Around";
            case "elegant" -> "Elegant";
            case "natural" -> "Natural";
            case "flow" -> "Flow";
            case "classic" -> "Classic";
            case "sans" -> "Poppins";
            case "script" -> "Kalam";
            case "minecraft" -> "Minecraft";
            case "standard" -> "Standard";
            case "rose" -> "Rose";
            case "lavender" -> "Lavender";
            case "auto" -> "Auto";
            case "aurora" -> "Aurora";
            case "sunset" -> "Sunset";
            case "gold" -> "Gold";
            case "sakura" -> "Sakura";
            case "ocean" -> "Ocean";
            case "white" -> "White";
            default -> value;
        };
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
            case "Held Items" -> {
                sectionHeaders.add(new SectionHeader("Held item glow",
                        "Use sampled item-model texture colors for hand outlines", py + 76));
                addSettingRow("Texture-matched color", onOff(ProVisualsConfig.heldItemTextureColor),
                        "Sample the visible item texture and tint layers", x, py + 116, contentWidth,
                        b -> {
                            ProVisualsConfig.heldItemTextureColor = !ProVisualsConfig.heldItemTextureColor;
                            ProVisualsConfig.save();
                            status = "Texture-matched held glow " + onOff(ProVisualsConfig.heldItemTextureColor);
                            rebuildWidgets();
                        });
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
            case "Settings" -> List.of("Reflections", "Ore Glow", "Held Items", "Interface");
            case "Tools" -> List.of("Error Center");
            case "World" -> List.of("World Visuals", "Sky Lyrics");
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
            case "Sky Lyrics" -> "Sky Lyrics";
            case "Ore Glow" -> "Ore glow";
            case "Held Items" -> "Held Items";
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

    private static String nextKillEffectStyle(String current) {
        List<String> styles = List.of("SKULL_RISE", "LIGHTNING", "SOUL_BURST",
                "ENDER_BURST", "FIREWORKS", "TOTEM_POP");
        int index = styles.indexOf(current);
        return styles.get((index + 1) % styles.size());
    }

    private static String killEffectLabel(String value) {
        return switch (value) {
            case "LIGHTNING" -> "Lightning";
            case "SOUL_BURST" -> "Soul Burst";
            case "ENDER_BURST" -> "Ender Burst";
            case "FIREWORKS" -> "Fireworks";
            case "TOTEM_POP" -> "Totem Pop";
            default -> "Skull Rise";
        };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xF2090911, 0xF2171027);
        int px = panelX();
        int py = panelY();
        ProVisualsUi.fill(graphics, px, py, Math.min(732, width - 24), 392, 18, 0xF0151520);
        ProVisualsUi.fill(graphics, px, py, 3, 392, 2, ProVisualsUi.accentPrimary());
        ProVisualsUi.fill(graphics, px + 16, py + 54, Math.min(700, width - 56), 1, 1, 0x555E536F);
        ProVisualsUi.fill(graphics, px + 148, py + 64, 1, 308, 1, 0x554E455D);


    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (ProVisualsConfig.roundedCorners) {
            for (var child : children()) {
                if (child instanceof Button button) {
                    ProVisualsUi.roundWidgetCorners(graphics, button.getX(), button.getY(),
                            button.getWidth(), button.getHeight(), 9, 0xF0151520);
                }
            }
        }

        int x = panelX() + 20;
        int y = panelY() + 17;
        ProVisualsUi.text(graphics, font, "ProVisuals", x, y, 0xFFFFFFFF, true);
        ProVisualsUi.text(graphics, font, "Visuals + SkyLyrics  /  Right Shift", x, y + 16, ProVisualsUi.accentSoft(), true);
        renderNavigation(graphics, mouseX, mouseY);

        switch (page) {
            case "Overview" -> renderOverview(graphics, mouseX, mouseY);
            case "Music" -> renderMusic(graphics, mouseX, mouseY);
            case "Tools" -> renderTools(graphics, mouseX, mouseY);
            case "Settings" -> renderSettings(graphics, mouseX, mouseY);
            default -> renderFeaturePage(graphics, mouseX, mouseY);
        }

        ProVisualsUi.text(graphics, font, status, panelX() + 20, panelY() + 372, ProVisualsUi.accentSoft(), true);
    }

    private void renderOverview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
        int contentX = panelX() + 164;
        ProVisualsUi.text(graphics, font, "ProVisuals", contentX,
                panelY() + 126, 0xFFF1EDFA, true);
        ProVisualsUi.text(graphics, font, "Select a category from the left navigation panel.",
                contentX, panelY() + 145, 0xFFAAA4BA, false);
        ProVisualsUi.text(graphics, font, "Global accent color", contentX,
                panelY() + 171, 0xFFEDE7FA, true);
        ProVisualsUi.text(graphics, font, "Changes highlights across the menu, Music Island and visual effects",
                contentX, panelY() + 186, 0xFF9F96B0, false);

        for (AccentSwatch swatch : accentSwatches) {
            boolean selected = ProVisualsConfig.accentPalette.equals(swatch.palette());
            int border = selected ? ProVisualsUi.accentSoft() : 0xFF45404F;
            ProVisualsUi.fill(graphics, swatch.x(), swatch.y(), swatch.width(),
                    swatch.height(), 8, border);
            ProVisualsUi.fill(graphics, swatch.x() + 2, swatch.y() + 2,
                    swatch.width() - 4, swatch.height() - 4, 6, swatch.color());
            graphics.centeredText(font, ProVisualsUi.component(swatch.palette()),
                    swatch.x() + swatch.width() / 2, swatch.y() + swatch.height() + 8,
                    selected ? ProVisualsUi.accentLight() : 0xFFAAA4BA);
        }
    }

    private void renderFeaturePage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderSectionHeaders(graphics);
        if ("PvP".equals(page) && "Player Visuals".equals(selectedSubcategory)) {
            for (FriendRow row : friendRows) renderFriendRow(graphics, row, mouseX, mouseY);
            for (ActionRow row : drawActionRows) renderActionRow(graphics, row, mouseX, mouseY);
            return;
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
                    ProVisualsConfig.isEnabled(feature.key()) ? ProVisualsUi.accentButton() : 0xFF302B37);
            graphics.centeredText(font, ProVisualsUi.component(state), pillX + pillWidth / 2,
                    row.y() + 14, feature.rendererConnected() ? ProVisualsUi.accentLight() : 0xFFB8AFC2);
        }

        for (ActionRow row : drawActionRows) renderActionRow(graphics, row, mouseX, mouseY);

        if (featureRows.stream().anyMatch(row -> !row.feature().rendererConnected())) {
            ProVisualsUi.text(graphics, font,
                    "Some modules only save preferences until their visual renderer is connected.",
                    panelX() + 164, panelY() + 347, 0xFFE8C987, false);
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

        int listX = panelX() + 164;
        int listY = panelY() + 164;
        int listWidth = Math.min(520, width - listX - 28);
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
            ProVisualsUi.text(graphics, font, header.title(), panelX() + 164, header.y(),
                    0xFFEDE7FA, true);
            ProVisualsUi.text(graphics, font, header.subtitle(), panelX() + 164, header.y() + 15,
                    0xFF9F96B0, false);
        }
    }

    private void renderFriendRow(GuiGraphicsExtractor graphics, FriendRow row, int mouseX, int mouseY) {
        boolean hovered = inside(row.x(), row.y(), row.width(), row.height(), mouseX, mouseY);
        ProVisualsUi.fill(graphics, row.x(), row.y(), row.width(), row.height(), 10,
                hovered ? 0xFF282338 : 0xFF1D1B29);
        ProVisualsUi.fill(graphics, row.x() + 9, row.y() + 8, 15, 15, 7, row.color());
        ProVisualsUi.text(graphics, font, row.name(), row.x() + 32, row.y() + 5,
                0xFFF1EDFA, true);
        ProVisualsUi.text(graphics, font, FriendManager.colorNameFor(row.name()),
                row.x() + 32, row.y() + 18, 0xFFAAA4BA, false);
        String presence = row.online() ? "ONLINE" : "OFFLINE";
        int presenceColor = row.online() ? 0xFF78D69A : 0xFF8D8798;
        ProVisualsUi.text(graphics, font, presence,
                row.x() + row.width() - ProVisualsUi.width(font, presence) - 10,
                row.y() + 10, presenceColor, true);
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
                row.secondary() ? ProVisualsUi.accentSoft() : ProVisualsUi.accentLight(), true);
    }

    private static boolean inside(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
