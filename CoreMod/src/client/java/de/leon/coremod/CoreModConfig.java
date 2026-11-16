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
    public static boolean customScoreboardEnabled = false;
    public static String scoreboardTitle = "DONUT SMP";
    public static String[] scoreboardLines = {"PLAYER: %player%", "BALANCE: %balance%", "PING: %ping%"};

    private CoreModConfig() {}

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(FILE), Data.class);
            if (data == null) return;
            if (data.quickCommands != null && data.quickCommands.length == 4) quickCommands = data.quickCommands;
            customScoreboardEnabled = data.customScoreboardEnabled;
            if (data.scoreboardTitle != null) scoreboardTitle = data.scoreboardTitle;
            if (data.scoreboardLines != null) scoreboardLines = data.scoreboardLines;
        } catch (Exception ignored) {
            CoreModClient.message("Could not read coremod.json. Defaults are active.");
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data data = new Data();
            data.quickCommands = quickCommands;
            data.customScoreboardEnabled = customScoreboardEnabled;
            data.scoreboardTitle = scoreboardTitle;
            data.scoreboardLines = scoreboardLines;
            Files.writeString(FILE, GSON.toJson(data));
        } catch (IOException ignored) {
            CoreModClient.message("Could not save coremod.json.");
        }
    }

    private static final class Data {
        String[] quickCommands;
        boolean customScoreboardEnabled;
        String scoreboardTitle;
        String[] scoreboardLines;
    }
}
