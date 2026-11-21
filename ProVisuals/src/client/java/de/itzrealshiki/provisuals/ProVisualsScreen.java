package de.itzrealshiki.provisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ProVisualsScreen extends Screen {
    private static final int NAV_BAR_HEIGHT = 54;
    private static final int NAV_BAR_GAP = 5;

    private String page = "Overview";
    private String status = "ALL SYSTEMS READY";
    private final List<Button> navigationButtons = new ArrayList<>();
    private float navigationProgress;
    private long lastNavigationFrameTime;

    public ProVisualsScreen() {
        super(Component.literal("ProVisuals"));
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelX = Math.max(14, width / 2 - 360);
        int panelY = Math.max(18, height / 2 - 190);
        int contentX = panelX + 28;
        int contentWidth = Math.max(180, Math.min(660, width - contentX - 28));

        if (page.equals("Overview")) {
            addTextButton("WORLD VISUALS", contentX, panelY + 68, contentWidth, "World Visuals");
            addTextButton("PLAYER VISUALS", contentX, panelY + 96, contentWidth, "Player Visuals");
            addTextButton("COMBAT VISUALS", contentX, panelY + 124, contentWidth, "Combat Visuals");
            addTextButton("HUD & INTERFACE", contentX, panelY + 152, contentWidth, "HUD & Interface");
            addTextButton("MUSIC & LYRICS", contentX, panelY + 180, contentWidth, "Music");
            addTextButton("ERROR CENTER", contentX, panelY + 208, contentWidth, "Errors");
        } else if (page.equals("Music")) {
            addRenderableWidget(Button.builder(Component.literal("LYRICS IN THE SKY: " + onOff(MusicLyricsBridge.isLyricsEnabled())),
                    b -> { boolean state = MusicLyricsBridge.toggleLyrics(); status = "LYRICS " + onOff(state); rebuildWidgets(); })
                    .bounds(contentX, panelY + 66, contentWidth, 22).build());
            addRenderableWidget(Button.builder(Component.literal("MUSIC ISLAND: " + onOff(ProVisualsConfig.musicIslandEnabled)),
                    b -> { ProVisualsConfig.musicIslandEnabled = !ProVisualsConfig.musicIslandEnabled; ProVisualsConfig.save(); status = "MUSIC ISLAND " + onOff(ProVisualsConfig.musicIslandEnabled); rebuildWidgets(); })
                    .bounds(contentX, panelY + 94, contentWidth, 22).build());
            addRenderableWidget(Button.builder(Component.literal("OPEN LYRICS SETTINGS"),
                    b -> MusicLyricsBridge.openSettings()).bounds(contentX, panelY + 122, contentWidth, 22).build());
        } else if (page.equals("Errors")) {
            addRenderableWidget(Button.builder(Component.literal("REFRESH DIAGNOSTICS"), b -> {
                Diagnostics.info("Diagnostics refreshed by user");
                status = "DIAGNOSTICS REFRESHED";
                rebuildWidgets();
            }).bounds(contentX, panelY + 64, contentWidth, 22).build());
            addRenderableWidget(Button.builder(Component.literal("COPY DIAGNOSTICS"), b -> {
                Minecraft.getInstance().keyboardHandler.setClipboard(String.join("\n", Diagnostics.recentEntries()));
                status = "DIAGNOSTICS COPIED";
            }).bounds(contentX, panelY + 91, contentWidth, 22).build());
        } else if (page.equals("Settings")) {
            addRenderableWidget(Button.builder(Component.literal("REFLECTION INTENSITY: " + percent(ProVisualsConfig.reflectionIntensity)),
                    b -> { ProVisualsConfig.reflectionIntensity = cycle(ProVisualsConfig.reflectionIntensity); ProVisualsConfig.save(); rebuildWidgets(); })
                    .bounds(contentX, panelY + 66, contentWidth, 22).build());
            addRenderableWidget(Button.builder(Component.literal("REFLECTION QUALITY: " + ProVisualsConfig.reflectionQuality),
                    b -> { ProVisualsConfig.reflectionQuality = nextQuality(ProVisualsConfig.reflectionQuality); ProVisualsConfig.save(); rebuildWidgets(); })
                    .bounds(contentX, panelY + 94, contentWidth, 22).build());
            addRenderableWidget(Button.builder(Component.literal("REFLECTIVE SURFACES: " + surfaceLabel(ProVisualsConfig.reflectiveSurfaces)),
                    b -> { ProVisualsConfig.reflectiveSurfaces = nextSurface(ProVisualsConfig.reflectiveSurfaces); ProVisualsConfig.save(); rebuildWidgets(); })
                    .bounds(contentX, panelY + 122, contentWidth, 22).build());
            addRenderableWidget(Button.builder(Component.literal("ORE INTERIOR GLOW: " + onOff(ProVisualsConfig.oreInteriorGlow)),
                    b -> { ProVisualsConfig.oreInteriorGlow = !ProVisualsConfig.oreInteriorGlow; ProVisualsConfig.save(); rebuildWidgets(); })
                    .bounds(contentX, panelY + 150, contentWidth, 22).build());
            addRenderableWidget(Button.builder(Component.literal("ORE OUTLINE GLOW: " + onOff(ProVisualsConfig.oreOutlineGlow)),
                    b -> { ProVisualsConfig.oreOutlineGlow = !ProVisualsConfig.oreOutlineGlow; ProVisualsConfig.save(); rebuildWidgets(); })
                    .bounds(contentX, panelY + 178, contentWidth, 22).build());
            addRenderableWidget(Button.builder(Component.literal("UI CORNERS: " + (ProVisualsConfig.roundedCorners ? "ROUNDED" : "SQUARE")),
                    b -> { ProVisualsConfig.roundedCorners = !ProVisualsConfig.roundedCorners; ProVisualsConfig.save(); rebuildWidgets(); })
                    .bounds(contentX, panelY + 206, contentWidth, 22).build());
        } else {
            List<FeatureRegistry.Feature> features = FeatureRegistry.inCategory(page);
            int y = panelY + 64;
            for (FeatureRegistry.Feature feature : features) {
                String label = (ProVisualsConfig.isEnabled(feature.key()) ? "[ON] " : "[OFF] ") + feature.title();
                addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    ProVisualsConfig.toggle(feature.key());
                    status = feature.rendererConnected()
                            ? feature.title().toUpperCase() + " " + onOff(ProVisualsConfig.isEnabled(feature.key()))
                            : "PREFERENCE SAVED — RENDERER NOT YET CONNECTED";
                    rebuildWidgets();
                }).bounds(contentX, y, contentWidth, 22).build());
                y += 29;
            }
        }
        addNavigationBar();
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
            Button button = Button.builder(Component.literal(navBarLabel(category)), b -> {
                page = category;
                status = "PAGE: " + category.toUpperCase();
                rebuildWidgets();
            }).bounds(startX + i * (buttonWidth + gap), height + 24, buttonWidth, 22).build();

            addRenderableWidget(button);
            navigationButtons.add(button);
        }
    }

    private void updateNavigationBar(int mouseY) {
        long now = System.nanoTime();
        if (lastNavigationFrameTime == 0L) {
            lastNavigationFrameTime = now;
        }
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
        for (Button button : navigationButtons) {
            button.setY(barTop + 16);
        }
    }

    private void addTextButton(String label, int x, int y, int w, String destination) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            page = destination;
            status = "PAGE: " + destination.toUpperCase();
            rebuildWidgets();
        }).bounds(x, y, w, 22).build());
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
        ProVisualsUi.fill(graphics, width / 2 - 366, height / 2 - 196, 732, 392, 14, 0xF0151520);
        ProVisualsUi.fill(graphics, width / 2 - 366, height / 2 - 196, 4, 392, 2, 0xFF9A6BFF);
        ProVisualsUi.fill(graphics, width / 2 - 350, height / 2 - 144, 160, 1, 1, 0xFF7650D8);

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
        int x = Math.max(14, width / 2 - 350);
        int y = Math.max(18, height / 2 - 180);
        graphics.text(font, "ProVisuals", x, y, 0xFFFFFFFF, true);
        graphics.text(font, "VISUAL CLIENT  /  MODERN VIOLET", x, y + 14, 0xFFBCA4FF, true);
        graphics.text(font, page.toUpperCase(), width / 2 - 172, y + 4, 0xFFE8DEFF, true);

        if (page.equals("Overview")) {
            graphics.text(font, "WELCOME BACK", width / 2 - 172, y + 36, 0xFFFFFFFF, true);
            graphics.text(font, "A clean control center for every visual module.", width / 2 - 172, y + 50, 0xFFC4C0D0, true);
            graphics.text(font, "12 visual modules registered", width / 2 - 172, y + 72, 0xFFBCA4FF, true);
            graphics.text(font, "M  /  Open ProVisuals", width / 2 - 172, y + 92, 0xFF9F92B8, true);
        } else if (page.equals("Music")) {
            graphics.text(font, "LYRICS IN THE SKY + MUSIC ISLAND", width / 2 - 172, y + 36, 0xFFFFFFFF, true);
            graphics.text(font, "Compact Music Island with track info, progress and media controls.", width / 2 - 172, y + 56, 0xFFC4C0D0, true);
            graphics.text(font, "Windows / Linux media controls are clickable while chat is open.", width / 2 - 172, y + 74, 0xFFC4C0D0, true);
            graphics.text(font, "Open Lyrics Settings for fonts, colours and lyric placement.", width / 2 - 172, y + 92, 0xFFBCA4FF, true);
        } else if (page.equals("Errors")) {
            graphics.text(font, "DIAGNOSTIC EVENTS", width / 2 - 172, y + 35, 0xFFFFC6D2, true);
            List<String> entries = Diagnostics.recentEntries();
            int lineY = y + 112;
            if (entries.isEmpty()) {
                graphics.text(font, "No ProVisuals errors recorded this session.", width / 2 - 172, y + 64, 0xFFC4C0D0, true);
            } else {
                for (String entry : entries.stream().limit(6).toList()) {
                    String compact = entry.length() > 76 ? entry.substring(0, 73) + "..." : entry;
                    graphics.text(font, compact, width / 2 - 172, lineY, 0xFFD6D2E0, true);
                    lineY += 13;
                }
            }
            graphics.text(font, "Saved to config/provisuals-diagnostics.log", width / 2 - 172, y + 207, 0xFF9F92B8, true);
        } else if (page.equals("Settings")) {
            graphics.text(font, "RENDER QUALITY & GENERAL OPTIONS", width / 2 - 172, y + 36, 0xFFFFFFFF, true);
            graphics.text(font, "Rounded is the default. Use UI CORNERS above to switch to square.", width / 2 - 172, y + 224, 0xFFFFD78B, true);
        } else {
            List<FeatureRegistry.Feature> features = FeatureRegistry.inCategory(page);
            graphics.text(font, features.size() + " MODULES  /  CLICK TO CHANGE SAVED STATE", width / 2 - 172, y + 36, 0xFFC4C0D0, true);
            if (features.stream().anyMatch(feature -> !feature.rendererConnected())) {
                graphics.text(font, "Preferences save locally; this visual renderer is not connected yet.", width / 2 - 172,
                        y + 211, 0xFFFFD78B, true);
            }
        }
        graphics.text(font, status, x, height / 2 + 174, 0xFFBCA4FF, true);
    }
}
