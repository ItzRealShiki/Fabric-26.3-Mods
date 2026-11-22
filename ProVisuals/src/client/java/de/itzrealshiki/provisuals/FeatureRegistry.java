package de.itzrealshiki.provisuals;

import java.util.List;

public final class FeatureRegistry {
    public record Feature(String key, String title, String description, String category, boolean rendererConnected) {}

    public static final List<String> CATEGORIES = List.of(
            "Overview", "PvP", "Glowing", "World", "HUD", "Music", "Settings", "Tools"
    );

    public static final List<Feature> FEATURES = List.of(
            new Feature("hits_energy_wave", "Energy Wave", "A short violet shockwave on hit.", "Combat Visuals", true),
            new Feature("earthquake", "Earthquake", "A separate 20-block circular ground-lift visual on hit.", "Combat Visuals", true),
            new Feature("kill_effects", "Kill Effects", "Play a custom visual effect when you eliminate another player.", "Combat Visuals", true),
            new Feature("visual_friends", "Visual Friends", "Add local friends and outline their visible name-tagged entities.", "Player Visuals", true),
            new Feature("glowing_ores", "Glowing Ores", "Soft fill and layered outline for the ore block under your crosshair.", "Block Glow", true),
            new Feature("armor_glow", "Armor Piece Glow", "Outline each equipped armor piece separately.", "Armor Glow", true),
            new Feature("glowing_armor_trims", "Glowing Armor Trims", "Glow the trim layer independently from the armor base.", "Armor Glow", true),
            new Feature("item_outline_glow", "Dropped Item Glow", "Outline item entities dropped in the world.", "Item Glow", true),
            new Feature("held_item_glow", "Held Item Glow", "Outline items in both hands using their model texture color.", "Item Glow", true),
            new Feature("custom_sky", "Custom Sky", "Nuit skyboxes from resource packs; preserves the source texture resolution (GPU limit applies).", "World Visuals", true),
            new Feature("fullbright", "Fullbright", "Client-side brightness control.", "World Visuals", true),
            new Feature("armor_hud", "Armor HUD", "Armor icons with durability bars and low-durability warnings.", "HUD & Interface", false),
            new Feature("target_hud", "Target HUD", "Compact target name, ping, health and armor display.", "HUD & Interface", false),
            new Feature("inventory_hud", "Inventory HUD", "Selected item counters with low-stock warnings.", "HUD & Interface", false),
            new Feature("effects_hud", "Effects HUD", "Active effects with end-of-effect warnings.", "HUD & Interface", false)
    );

    private FeatureRegistry() {}

    public static List<String> subcategories(String category) {
        return switch (category) {
            case "PvP" -> List.of("Combat Visuals", "Player Visuals");
            case "Glowing" -> List.of("Block Glow", "Armor Glow", "Item Glow");
            case "World" -> List.of("World Visuals");
            case "HUD" -> List.of("HUD & Interface");
            default -> List.of();
        };
    }

    public static List<Feature> inCategory(String category) {
        return FEATURES.stream().filter(feature -> feature.category().equals(category)).toList();
    }

    public static int countInTopCategory(String category) {
        return switch (category) {
            case "PvP" -> inCategory("Combat Visuals").size() + inCategory("Player Visuals").size();
            case "Glowing" -> inCategory("Block Glow").size()
                    + inCategory("Armor Glow").size() + inCategory("Item Glow").size();
            case "World" -> inCategory("World Visuals").size();
            case "HUD" -> inCategory("HUD & Interface").size();
            default -> 0;
        };
    }
}