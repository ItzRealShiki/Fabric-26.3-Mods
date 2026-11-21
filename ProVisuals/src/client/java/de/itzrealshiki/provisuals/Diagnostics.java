package de.itzrealshiki.provisuals;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;

public final class Diagnostics {
    private static final int MAX_ENTRIES = 100;
    private static final ArrayDeque<String> ENTRIES = new ArrayDeque<>();
    private Diagnostics() {}

    public static synchronized void info(String message) { add("INFO", message, null); }
    public static synchronized void warn(String message) { add("WARN", message, null); }
    public static synchronized void error(String message, Throwable throwable) { add("ERROR", message, throwable); }

    private static void add(String level, String message, Throwable throwable) {
        String entry = Instant.now() + " [" + level + "] " + message;
        if (throwable != null) entry += " (" + throwable.getClass().getSimpleName() + ": " + String.valueOf(throwable.getMessage()) + ")";
        ENTRIES.addFirst(entry);
        while (ENTRIES.size() > MAX_ENTRIES) ENTRIES.removeLast();
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("provisuals-diagnostics.log");
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging must never crash the client while reporting another error.
        }
    }

    public static synchronized List<String> recentEntries() { return List.copyOf(ENTRIES); }

    public static synchronized void loadPreviousEntries() {
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("provisuals-diagnostics.log");
            if (!Files.exists(file)) return;
            List<String> lines = Files.readAllLines(file);
            int start = Math.max(0, lines.size() - MAX_ENTRIES);
            for (int i = lines.size() - 1; i >= start; i--) ENTRIES.addLast(lines.get(i));
        } catch (IOException exception) {
            add("WARN", "Could not read diagnostics history", exception);
        }
    }
}
