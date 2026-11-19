package com.vextex.pvpmod;

public enum PvPFeature {
    FIXED_SHIELDS("Fixed Shields", "Shield behavior and feedback fixes"),
    SHIELD_STATUS("Shield Status", "Shows whether a shield is ready"),
    TOTEM_SIZE("Totem Size", "Adjusts held totem size"),
    PARTICLES("Particles", "Enables or disables selected particles"),
    SMALL_HELD_ITEMS("Small Held Items", "Reduces held item model size"),
    FULLBRIGHT("Fullbright", "Improves visibility in dark areas"),
    HEALTH_INDICATORS("Health Indicators", "Displays entity health information"),
    KEYSTROKES("Keystrokes", "Displays movement and action keys"),
    ARMOR_HUD("Armor HUD", "Shows armor durability and warnings"),
    POTION_EFFECTS("Potion Effects", "Displays active potion effects");

    private final String title;
    private final String description;

    PvPFeature(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String configKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
