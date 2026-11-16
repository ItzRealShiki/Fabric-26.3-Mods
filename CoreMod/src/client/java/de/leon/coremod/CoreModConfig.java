package de.leon.coremod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CoreModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("coremod.json");
    public static String[] quickCommands = {"/home", "/sell", "/ec", "/spawn"};
    public static String quickButtonSide = "RIGHT";
    public static String apiKey = "";
    public static String marketSummary = "NO MARKET DATA YET";
    public static String marketUpdated = "NEVER";
    public static double balance = 0;
    public static double farmBudget = 0;
    public static double farmCost = 0;
    public static double farmRevenue = 0;
    public static boolean customScoreboardEnabled = false;
    public static String scoreboardTitle = "DONUT SMP";
    public static String[] scoreboardLines = {"PLAYER: %player%", "BALANCE: %balance%", "PING: %ping%"};
    public static String[] schematicEntries = {};

    private CoreModConfig() {}
    public static void load() {
        if (!Files.exists(FILE)) { save(); return; }
        try {
            Data d = GSON.fromJson(Files.readString(FILE), Data.class);
            if (d == null) return;
            if (d.quickCommands != null && d.quickCommands.length == 4) quickCommands = d.quickCommands;
            if (d.quickButtonSide != null) quickButtonSide = d.quickButtonSide;
            if (d.apiKey != null) apiKey = d.apiKey;
            if (d.marketSummary != null) marketSummary = d.marketSummary;
            if (d.marketUpdated != null) marketUpdated = d.marketUpdated;
            balance = d.balance; farmBudget = d.farmBudget; farmCost = d.farmCost; farmRevenue = d.farmRevenue;
            customScoreboardEnabled = d.customScoreboardEnabled;
            if (d.scoreboardTitle != null) scoreboardTitle = d.scoreboardTitle;
            if (d.scoreboardLines != null) scoreboardLines = d.scoreboardLines;
            if (d.schematicEntries != null) schematicEntries = d.schematicEntries;
        } catch (Exception ignored) { }
    }
    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data d = new Data();
            d.quickCommands = quickCommands; d.quickButtonSide = quickButtonSide; d.apiKey = apiKey;
            d.marketSummary = marketSummary; d.marketUpdated = marketUpdated;
            d.balance = balance; d.farmBudget = farmBudget; d.farmCost = farmCost; d.farmRevenue = farmRevenue;
            d.customScoreboardEnabled = customScoreboardEnabled; d.scoreboardTitle = scoreboardTitle;
            d.scoreboardLines = scoreboardLines; d.schematicEntries = schematicEntries;
            Files.writeString(FILE, GSON.toJson(d));
        } catch (IOException ignored) { }
    }
    private static final class Data {
        String[] quickCommands; String quickButtonSide; String apiKey; String marketSummary; String marketUpdated;
        double balance; double farmBudget; double farmCost; double farmRevenue;
        boolean customScoreboardEnabled; String scoreboardTitle; String[] scoreboardLines; String[] schematicEntries;
    }
}
