package de.itzrealshiki.provisuals;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Persistent, local-only friend list used by the visual friend outline. */
public final class FriendManager {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("provisuals-friends.properties");
    private static final LinkedHashMap<String, String> FRIENDS = new LinkedHashMap<>();
    private static boolean loaded;

    private FriendManager() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        FRIENDS.clear();
        if (!Files.exists(FILE)) return;

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            int count = Math.max(0, Math.min(256, Integer.parseInt(properties.getProperty("count", "0"))));
            for (int i = 0; i < count; i++) {
                String name = properties.getProperty("friend." + i, "").trim();
                if (isValidName(name)) FRIENDS.put(name.toLowerCase(Locale.ROOT), name);
            }
        } catch (Exception exception) {
            Diagnostics.error("Could not load friend list", exception);
            FRIENDS.clear();
        }
    }

    public static synchronized boolean add(String rawName) {
        load();
        if (rawName == null) return false;
        String name = rawName.trim();
        if (!isValidName(name)) return false;

        String key = name.toLowerCase(Locale.ROOT);
        if (FRIENDS.containsKey(key)) return false;
        if (FRIENDS.size() >= 256) return false;
        FRIENDS.put(key, name);
        save();
        return true;
    }

    public static synchronized boolean remove(String rawName) {
        load();
        if (rawName == null) return false;
        String removed = FRIENDS.remove(rawName.trim().toLowerCase(Locale.ROOT));
        if (removed == null) return false;
        save();
        return true;
    }

    public static synchronized boolean isFriend(String rawName) {
        load();
        if (rawName == null || rawName.isBlank()) return false;
        return FRIENDS.containsKey(rawName.trim().toLowerCase(Locale.ROOT));
    }

    public static synchronized List<String> friends() {
        load();
        return List.copyOf(new ArrayList<>(FRIENDS.values()));
    }

    private static boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    private static synchronized void save() {
        Properties properties = new Properties();
        properties.setProperty("count", Integer.toString(FRIENDS.size()));
        int index = 0;
        for (String name : FRIENDS.values()) {
            properties.setProperty("friend." + index++, name);
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
