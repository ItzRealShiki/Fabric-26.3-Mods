package de.itzrealshiki.provisuals;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class ProVisualsConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("provisuals.properties");
    private static final Map<String, Boolean> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, Boolean> MODULES = new LinkedHashMap<>();

    public static float reflectionIntensity = 0.65f;
    public static String reflectionQuality = "MEDIUM";
    public static String reflectiveSurfaces = "ALL";
    public static boolean oreInteriorGlow = true;
    public static boolean oreOutlineGlow = true;
    public static boolean musicIslandEnabled = true;
    public static boolean genreRecognitionEnabled = true;
    public static boolean roundedCorners = true;

    static {
        DEFAULTS.put("glowing_ores", false);
        DEFAULTS.put("glowing_armor_trims", false);
        DEFAULTS.put("fullbright", false);
        DEFAULTS.put("inventory_hud", false);
        DEFAULTS.put("effects_hud", false);
        DEFAULTS.put("armor_hud", false);
        DEFAULTS.put("target_hud", false);
        DEFAULTS.put("visual_friends", false);
        DEFAULTS.put("armor_glow", false);
        DEFAULTS.put("item_outline_glow", false);
        DEFAULTS.put("hits_energy_wave", false);
        DEFAULTS.put("ground_reflections", false);
        DEFAULTS.put("custom_sky", true);
        MODULES.putAll(DEFAULTS);
    }

    private ProVisualsConfig() {}

    public static synchronized void load() {
        MODULES.clear();
        MODULES.putAll(DEFAULTS);
        if (!Files.exists(FILE)) { save(); return; }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            for (String key : DEFAULTS.keySet()) {
                MODULES.put(key, Boolean.parseBoolean(properties.getProperty("module." + key, String.valueOf(DEFAULTS.get(key)))));
            }
            reflectionIntensity = number(properties.getProperty("reflection.intensity"), 0.65f);
            reflectionQuality = allowed(properties.getProperty("reflection.quality"), "LOW", "MEDIUM", "HIGH", "MEDIUM");
            reflectiveSurfaces = allowed(properties.getProperty("reflection.surfaces"), "ALL", "SELECTED", "WATER_ONLY", "ALL");
            oreInteriorGlow = Boolean.parseBoolean(properties.getProperty("ores.interior", "true"));
            oreOutlineGlow = Boolean.parseBoolean(properties.getProperty("ores.outline", "true"));
            musicIslandEnabled = Boolean.parseBoolean(properties.getProperty("music.island.enabled", "true"));
            genreRecognitionEnabled = Boolean.parseBoolean(properties.getProperty("music.genre_recognition.enabled", "true"));
            roundedCorners = Boolean.parseBoolean(properties.getProperty("ui.rounded_corners", "true"));
            Diagnostics.info("Configuration loaded");
        } catch (Exception exception) {
            Diagnostics.error("Could not load configuration; defaults restored", exception);
            MODULES.clear();
            MODULES.putAll(DEFAULTS);
            roundedCorners = true;
            genreRecognitionEnabled = true;
        }
    }

    private static float number(String raw, float fallback) {
        try { return Math.max(0f, Math.min(1f, Float.parseFloat(raw))); }
        catch (RuntimeException exception) { return fallback; }
    }

    private static String allowed(String value, String a, String b, String c, String fallback) {
        if (a.equalsIgnoreCase(value)) return a;
        if (b.equalsIgnoreCase(value)) return b;
        if (c.equalsIgnoreCase(value)) return c;
        return fallback;
    }

    public static synchronized boolean isEnabled(String key) { return MODULES.getOrDefault(key, false); }
    public static synchronized void setEnabled(String key, boolean enabled) {
        if (!MODULES.containsKey(key)) return;
        MODULES.put(key, enabled);
        save();
    }
    public static synchronized void toggle(String key) { setEnabled(key, !isEnabled(key)); }

    public static synchronized void save() {
        Properties properties = new Properties();
        for (Map.Entry<String, Boolean> entry : MODULES.entrySet()) {
            properties.setProperty("module." + entry.getKey(), String.valueOf(entry.getValue()));
        }
        properties.setProperty("reflection.intensity", String.valueOf(reflectionIntensity));
        properties.setProperty("reflection.quality", reflectionQuality);
        properties.setProperty("reflection.surfaces", reflectiveSurfaces);
        properties.setProperty("ores.interior", String.valueOf(oreInteriorGlow));
        properties.setProperty("ores.outline", String.valueOf(oreOutlineGlow));
        properties.setProperty("music.island.enabled", String.valueOf(musicIslandEnabled));
        properties.setProperty("music.genre_recognition.enabled", String.valueOf(genreRecognitionEnabled));
        properties.setProperty("ui.rounded_corners", String.valueOf(roundedCorners));
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) { properties.store(writer, "ProVisuals client configuration"); }
        } catch (IOException exception) {
            Diagnostics.error("Could not save configuration", exception);
        }
    }
}
