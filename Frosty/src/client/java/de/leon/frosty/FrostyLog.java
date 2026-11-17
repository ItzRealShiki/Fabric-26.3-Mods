package de.leon.frosty;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.config.Property;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class FrostyLog {
    public enum LevelType { ERROR, WARNING, INFO }
    public record Entry(String time, LevelType level, String message) {}
    private static final int LIMIT = 150;
    private static final ArrayDeque<Entry> ENTRIES = new ArrayDeque<>();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static boolean installed;
    private FrostyLog() {}

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        try {
            LoggerContext context = LoggerContext.getContext(false);
            Configuration config = context.getConfiguration();
            AbstractAppender appender = new AbstractAppender("FrostyStatusAppender", null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY) {
                @Override public void append(LogEvent event) {
                    Level level = event.getLevel();
                    LevelType type = level.isMoreSpecificThan(Level.ERROR) ? LevelType.ERROR :
                        level.isMoreSpecificThan(Level.WARN) ? LevelType.WARNING : LevelType.INFO;
                    StringBuilder message = new StringBuilder(event.getLoggerName()).append(": ")
                        .append(event.getMessage().getFormattedMessage());
                    if (event.getThrown() != null) {
                        StringWriter trace = new StringWriter();
                        event.getThrown().printStackTrace(new PrintWriter(trace));
                        message.append("\n").append(trace);
                    }
                    add(type, message.toString());
                }
            };
            appender.start();
            config.addAppender(appender);
            LoggerConfig root = config.getRootLogger();
            root.addAppender(appender, Level.INFO, null);
            context.updateLoggers();
            add(LevelType.INFO, "Frosty status monitor started; Minecraft logs are being captured.");
        } catch (Exception ex) {
            add(LevelType.ERROR, "Could not attach log monitor: " + ex);
        }
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            add(LevelType.ERROR, "Uncaught exception on " + thread.getName() + ": " + trace);
        });
    }

    public static synchronized void add(LevelType level, String message) {
        ENTRIES.addFirst(new Entry(LocalTime.now().format(TIME), level, message));
        while (ENTRIES.size() > LIMIT) ENTRIES.removeLast();
    }
    public static synchronized List<Entry> recent(int limit) { return new ArrayList<>(ENTRIES).stream().limit(limit).toList(); }
    public static synchronized int count(LevelType level) { return (int) ENTRIES.stream().filter(e -> e.level() == level).count(); }
    public static synchronized void clear() { ENTRIES.clear(); add(LevelType.INFO, "Log cleared."); }
    public static void openLogFolder() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb = os.contains("win") ? new ProcessBuilder("explorer", FabricLoader.getInstance().getGameDir().resolve("logs").toString()) :
                os.contains("mac") ? new ProcessBuilder("open", FabricLoader.getInstance().getGameDir().resolve("logs").toString()) :
                new ProcessBuilder("xdg-open", FabricLoader.getInstance().getGameDir().resolve("logs").toString());
            pb.start();
        } catch (Exception ex) { add(LevelType.WARNING, "Could not open Minecraft logs folder: " + ex.getMessage()); }
    }
}
