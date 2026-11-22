package de.itzrealshiki.provisuals;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/** Persistent local visual friends, per-friend accent colors and current-server presence. */
public final class FriendManager {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("provisuals-friends.properties");
    private static final LinkedHashMap<String, String> FRIENDS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Integer> FRIEND_COLORS = new LinkedHashMap<>();
    private static final Set<String> ONLINE_PLAYERS = new HashSet<>();

    private static final List<Integer> COLOR_PRESETS = List.of(
            0xFF9A6BFF, // Violet
            0xFF579BFF, // Blue
            0xFF35C9D0, // Cyan
            0xFF58C98D, // Green
            0xFFE6AD4E, // Gold
            0xFFF36EA9, // Pink
            0xFFFF6B6B, // Coral
            0xFFF2F2F2  // White
    );

    private static boolean loaded;
    private static Object trackedConnection;
    private static boolean presenceBaselineReady;

    private FriendManager() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        FRIENDS.clear();
        FRIEND_COLORS.clear();
        if (!Files.exists(FILE)) return;

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            int count = Math.max(0, Math.min(256, Integer.parseInt(properties.getProperty("count", "0"))));
            for (int i = 0; i < count; i++) {
                String name = properties.getProperty("friend." + i, "").trim();
                if (!isValidName(name)) continue;
                String key = key(name);
                FRIENDS.put(key, name);
                String colorValue = properties.getProperty("friend." + i + ".color");
                if (colorValue != null) {
                    try {
                        FRIEND_COLORS.put(key, (int) Long.parseLong(colorValue));
                    } catch (NumberFormatException ignored) {
                        // Older friend entries simply use the current global accent color.
                    }
                }
            }
        } catch (Exception exception) {
            Diagnostics.error("Could not load friend list", exception);
            FRIENDS.clear();
            FRIEND_COLORS.clear();
        }
    }

    public static synchronized boolean add(String rawName) {
        load();
        if (rawName == null) return false;
        String name = rawName.trim();
        if (!isValidName(name)) return false;

        String key = key(name);
        if (FRIENDS.containsKey(key) || FRIENDS.size() >= 256) return false;
        FRIENDS.put(key, name);
        save();
        return true;
    }

    public static synchronized boolean remove(String rawName) {
        load();
        if (rawName == null) return false;
        String key = key(rawName);
        String removed = FRIENDS.remove(key);
        if (removed == null) return false;
        FRIEND_COLORS.remove(key);
        ONLINE_PLAYERS.remove(key);
        save();
        return true;
    }

    public static synchronized boolean isFriend(String rawName) {
        load();
        if (rawName == null || rawName.isBlank()) return false;
        return FRIENDS.containsKey(key(rawName));
    }

    public static synchronized List<String> friends() {
        load();
        return List.copyOf(new ArrayList<>(FRIENDS.values()));
    }

    public static synchronized int colorForName(String rawName) {
        load();
        if (rawName == null) return ProVisualsUi.accentPrimary();
        return FRIEND_COLORS.getOrDefault(key(rawName), ProVisualsUi.accentPrimary());
    }

    public static synchronized String colorNameFor(String rawName) {
        int color = colorForName(rawName);
        int index = COLOR_PRESETS.indexOf(color);
        return switch (index) {
            case 1 -> "Blue";
            case 2 -> "Cyan";
            case 3 -> "Green";
            case 4 -> "Gold";
            case 5 -> "Pink";
            case 6 -> "Coral";
            case 7 -> "White";
            default -> "Violet";
        };
    }

    public static synchronized int cycleColor(String rawName) {
        load();
        if (rawName == null || !FRIENDS.containsKey(key(rawName))) {
            return ProVisualsUi.accentPrimary();
        }
        String key = key(rawName);
        int current = FRIEND_COLORS.getOrDefault(key, COLOR_PRESETS.get(0));
        int index = COLOR_PRESETS.indexOf(current);
        int next = COLOR_PRESETS.get((index + 1) % COLOR_PRESETS.size());
        FRIEND_COLORS.put(key, next);
        save();
        return next;
    }

    public static synchronized boolean isOnline(String rawName) {
        load();
        return rawName != null && ONLINE_PLAYERS.contains(key(rawName));
    }

    /**
     * Watches the tab-list data for the currently connected server.
     * The first complete snapshot is treated as a baseline, so joining a server
     * does not generate false "friend joined" alerts for people already there.
     */
    public static synchronized void tickPresence(Minecraft client) {
        load();
        if (client == null || client.player == null || client.level == null || client.getConnection() == null) {
            trackedConnection = null;
            presenceBaselineReady = false;
            ONLINE_PLAYERS.clear();
            return;
        }

        var connection = client.getConnection();
        if (connection != trackedConnection) {
            trackedConnection = connection;
            presenceBaselineReady = false;
            ONLINE_PLAYERS.clear();
        }

        Set<String> currentOnline = new HashSet<>();
        for (var info : connection.getOnlinePlayers()) {
            if (info != null && info.getProfile() != null) {
                currentOnline.add(key(info.getProfile().name()));
            }
        }

        // The local player appearing in the tab list means the initial player-list snapshot arrived.
        if (!presenceBaselineReady) {
            if (connection.getPlayerInfo(client.player.getUUID()) == null) return;
            ONLINE_PLAYERS.clear();
            ONLINE_PLAYERS.addAll(currentOnline);
            presenceBaselineReady = true;
            return;
        }

        for (String playerKey : currentOnline) {
            if (ONLINE_PLAYERS.contains(playerKey)) continue;
            String friendName = FRIENDS.get(playerKey);
            if (friendName == null) continue;

            String serverName = currentServerName(client);
            client.gui.setOverlayMessage(
                    Component.literal("Friend online: " + friendName + " joined " + serverName), false);
        }

        ONLINE_PLAYERS.clear();
        ONLINE_PLAYERS.addAll(currentOnline);
    }

    private static String currentServerName(Minecraft client) {
        var server = client.getCurrentServer();
        if (server != null) {
            if (server.name != null && !server.name.isBlank()) return server.name;
            if (server.ip != null && !server.ip.isBlank()) return server.ip;
        }
        return client.hasSingleplayerServer() ? "this world" : "this server";
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    private static synchronized void save() {
        Properties properties = new Properties();
        properties.setProperty("count", Integer.toString(FRIENDS.size()));
        int index = 0;
        for (Map.Entry<String, String> entry : FRIENDS.entrySet()) {
            properties.setProperty("friend." + index, entry.getValue());
            Integer color = FRIEND_COLORS.get(entry.getKey());
            if (color != null) {
                properties.setProperty("friend." + index + ".color", Integer.toUnsignedString(color));
            }
            index++;
        }
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                properties.store(writer, "ProVisuals local visual friends");
            }
        } catch (IOException exception) {
            Diagnostics.error("Could not save friend list", exception);
        }
    }
}
