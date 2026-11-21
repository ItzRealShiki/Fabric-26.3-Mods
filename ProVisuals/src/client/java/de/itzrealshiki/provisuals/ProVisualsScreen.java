package de.itzrealshiki.provisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ProVisualsScreen extends Screen {
    private String page = "Overview";
    private String status = "ALL SYSTEMS READY";

    public ProVisualsScreen() {
        super(Component.literal("ProVisuals"));
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelX = Math.max(14, width / 2 - 360);
        int panelY = Math.max(18, height / 2 - 190);
        int sideX = panelX + 10;
        int contentX = panelX + 174;
        int contentWidth = Math.min(520, width - contentX - 20);

        int navY = panelY + 42;
        for (String category : FeatureRegistry.CATEGORIES) {
            final String selected = category;
            addRenderableWidget(Button.builder(Component.literal(navLabel(category)), button -> {
                page = selected;
                status = "PAGE: " + selected.toUpperCase();
                rebuildWidgets();
            }).bounds(sideX, navY, 150, 22).build());
            navY += 26;
        }

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
    }

    private void addTextButton(String label, int x, int y, int w, String destination) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            page = destination;
            status = "PAGE: " + destination.toUpperCase();
            rebuildWidgets();
        }).bounds(x, y, w, 22).build());
    }

    private static String navLabel(String category) {
        return category.equals("Errors") ? "!  ERROR CENTER" : category.toUpperCase();
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
        graphics.fill(width / 2 - 366, height / 2 - 196, width / 2 + 366, height / 2 + 196, 0xF0151520);
        graphics.fill(width / 2 - 366, height / 2 - 196, width / 2 - 362, height / 2 + 196, 0xFF9A6BFF);
        graphics.fill(width / 2 - 350, height / 2 - 144, width / 2 - 190, height / 2 - 143, 0xFF7650D8);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
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
            graphics.text(font, "Synced line timing, automatic song palettes and multiple display looks.", width / 2 - 172, y + 56, 0xFFC4C0D0, true);
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
            graphics.text(font, "Reflection and ore glow settings are saved locally.", width / 2 - 172, y + 211, 0xFFFFD78B, true);
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
