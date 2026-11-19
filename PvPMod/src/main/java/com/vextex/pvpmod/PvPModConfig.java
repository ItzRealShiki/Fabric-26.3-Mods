package com.vextex.pvpmod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

public final class PvPModConfig {
    public enum HandMode {
        BOTH("Both Hands"),
        MAIN_HAND("Main Hand"),
        OFF_HAND("Off Hand");

        private final String label;

        HandMode(String label) { this.label = label; }
        public String label() { return label; }

        public HandMode next() {
            HandMode[] values = values();
            return values()[(ordinal() + 1) % values.length];
        }
    }

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pvpmod.properties");
    private static final Map<PvPFeature, Boolean> FEATURES = new EnumMap<>(PvPFeature.class);
    private static HandMode handMode = HandMode.BOTH;

    static {
        for (PvPFeature feature : PvPFeature.values()) {
            FEATURES.put(feature, feature == PvPFeature.PARTICLES);
        }
        load();
    }

    private PvPModConfig() {}

    public static boolean isEnabled(PvPFeature feature) {
        return FEATURES.getOrDefault(feature, false);
    }

    public static void setEnabled(PvPFeature feature, boolean enabled) {
        FEATURES.put(feature, enabled);
        save();
    }

    public static HandMode handMode() { return handMode; }

    public static void cycleHandMode() {
        handMode = handMode.next();
        save();
    }

    private static void load() {
        if (!Files.isRegularFile(FILE)) return;

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(FILE)) {
            properties.load(input);
            for (PvPFeature feature : PvPFeature.values()) {
                String defaultValue = feature == PvPFeature.PARTICLES ? "true" : "false";
                FEATURES.put(feature, Boolean.parseBoolean(
                        properties.getProperty("feature." + feature.configKey(), defaultValue)));
            }
            try {
                handMode = HandMode.valueOf(properties.getProperty("hand_mode", "BOTH"));
            } catch (IllegalArgumentException ignored) {
                handMode = HandMode.BOTH;
            }
        } catch (IOException exception) {
            System.err.println("[PvP Mod] Could not load configuration: " + exception.getMessage());
        }
    }

    private static void save() {
        Properties properties = new Properties();
        for (Map.Entry<PvPFeature, Boolean> entry : FEATURES.entrySet()) {
            properties.setProperty("feature." + entry.getKey().configKey(), entry.getValue().toString());
        }
        properties.setProperty("hand_mode", handMode.name());

        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properties.store(output, "PvP Mod configuration");
            }
        } catch (IOException exception) {
            System.err.println("[PvP Mod] Could not save configuration: " + exception.getMessage());
        }
    }
}
