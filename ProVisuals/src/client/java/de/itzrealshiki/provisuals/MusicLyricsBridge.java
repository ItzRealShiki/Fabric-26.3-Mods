package de.itzrealshiki.provisuals;

import net.minecraft.client.Minecraft;
import java.lang.reflect.Field;
import java.lang.reflect.ReflectiveOperationException;

/** Bridge to the separately versioned nested Lyrics in the Sky Fabric module. */
public final class MusicLyricsBridge {
    private static final String CLIENT_CLASS = "de.henner.lyricsinthesky.LyricsInTheSkyClient";
    public record Track(String title, String artist, long positionMs, long durationMs, boolean playing) {}
    private MusicLyricsBridge() {}

    private static Class<?> clientClass() throws ReflectiveOperationException {
        return Class.forName(CLIENT_CLASS);
    }

    private static Object controller() throws ReflectiveOperationException {
        return clientClass().getMethod("controller").invoke(null);
    }

    public static Track currentTrack() {
        try {
            Object controller = controller();
            if (controller == null) return null;
            Object timeline = controller.getClass().getMethod("timeline", long.class)
                    .invoke(controller, System.currentTimeMillis());
            if (timeline == null) return null;
            Class<?> type = timeline.getClass();
            return new Track(
                    (String) type.getMethod("title").invoke(timeline),
                    (String) type.getMethod("artist").invoke(timeline),
                    ((Number) type.getMethod("positionMs").invoke(timeline)).longValue(),
                    ((Number) type.getMethod("durationMs").invoke(timeline)).longValue(),
                    (Boolean) type.getMethod("playing").invoke(timeline));
        } catch (ClassNotFoundException exception) {
            return null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Diagnostics.warn("Could not read media metadata: " + exception.getMessage());
            return null;
        }
    }

    public static boolean isLyricsEnabled() {
        try {
            Object controller = controller();
            if (controller == null) return false;
            Object config = controller.getClass().getMethod("config").invoke(controller);
            return config.getClass().getField("enabled").getBoolean(config);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    public static boolean toggleLyrics() {
        try {
            Object controller = controller();
            if (controller == null) return false;
            Object config = controller.getClass().getMethod("config").invoke(controller);
            Field enabled = config.getClass().getField("enabled");
            enabled.setBoolean(config, !enabled.getBoolean(config));
            config.getClass().getMethod("save").invoke(config);
            return enabled.getBoolean(config);
        } catch (ReflectiveOperationException exception) {
            Diagnostics.warn("Lyrics module is not ready: " + exception.getMessage());
            return false;
        }
    }

    public static void openSettings() {
        try {
            clientClass().getMethod("openSettings", Minecraft.class).invoke(null, Minecraft.getInstance());
        } catch (ReflectiveOperationException exception) {
            Diagnostics.error("Could not open Lyrics in the Sky settings", exception);
        }
    }

    public static boolean isPresent() {
        try {
            clientClass();
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
