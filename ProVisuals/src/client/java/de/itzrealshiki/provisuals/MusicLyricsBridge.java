package de.itzrealshiki.provisuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import java.lang.reflect.Field;

/** Reflection bridge to the nested Lyrics in the Sky module. */
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
    private static Object lyricsConfig() throws ReflectiveOperationException {
        Object controller = controller();
        if (controller == null) throw new IllegalStateException("SkyLyrics controller is not initialized");
        return controller.getClass().getMethod("config").invoke(controller);
    }
    private static Field configField(String name) throws ReflectiveOperationException {
        Field field = lyricsConfig().getClass().getField(name);
        field.setAccessible(true);
        return field;
    }

    public static Track currentTrack() {
        try {
            Object controller = controller();
            if (controller == null) return null;
            Object timeline = controller.getClass().getMethod("timeline", long.class)
                    .invoke(controller, System.currentTimeMillis());
            if (timeline == null) return null;
            Class<?> type = timeline.getClass();
            return new Track((String) type.getMethod("title").invoke(timeline),
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

    public static boolean isLyricsEnabled() { return booleanSetting("enabled", false); }
    public static boolean toggleLyrics() { return toggleBooleanSetting("enabled", false); }

    public static String stringSetting(String name, String fallback) {
        try {
            Object value = configField(name).get(lyricsConfig());
            return value instanceof String text ? text : fallback;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return fallback;
        }
    }

    public static String cycleStringSetting(String name, String... values) {
        if (values == null || values.length == 0) return stringSetting(name, "");
        try {
            Object config = lyricsConfig();
            Field field = config.getClass().getField(name);
            Object raw = field.get(config);
            String current = raw instanceof String text ? text : values[0];
            int index = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(current)) { index = i; break; }
            }
            String next = values[(index + 1) % values.length];
            field.set(config, next);
            config.getClass().getMethod("save").invoke(config);
            return next;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Diagnostics.warn("Could not change SkyLyrics option '" + name + "': " + exception.getMessage());
            return stringSetting(name, values[0]);
        }
    }

    public static boolean booleanSetting(String name, boolean fallback) {
        try {
            Object value = configField(name).get(lyricsConfig());
            return value instanceof Boolean bool ? bool : fallback;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return fallback;
        }
    }

    public static boolean toggleBooleanSetting(String name, boolean fallback) {
        try {
            Object config = lyricsConfig();
            Field field = config.getClass().getField(name);
            Object raw = field.get(config);
            boolean next = !(raw instanceof Boolean bool ? bool : fallback);
            field.set(config, next);
            config.getClass().getMethod("save").invoke(config);
            return next;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Diagnostics.warn("Could not toggle SkyLyrics option '" + name + "': " + exception.getMessage());
            return booleanSetting(name, fallback);
        }
    }

    public static float numberSetting(String name, float fallback) {
        try {
            Object value = configField(name).get(lyricsConfig());
            return value instanceof Number number ? number.floatValue() : fallback;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return fallback;
        }
    }

    public static float cycleNumberSetting(String name, float... values) {
        if (values == null || values.length == 0) return numberSetting(name, 0.0f);
        try {
            Object config = lyricsConfig();
            Field field = config.getClass().getField(name);
            Object raw = field.get(config);
            float current = raw instanceof Number number ? number.floatValue() : values[0];
            int index = 0;
            for (int i = 0; i < values.length; i++) {
                if (Math.abs(values[i] - current) < 0.001f) { index = i; break; }
            }
            float next = values[(index + 1) % values.length];
            field.set(config, next);
            config.getClass().getMethod("save").invoke(config);
            return next;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Diagnostics.warn("Could not change SkyLyrics number option '" + name + "': " + exception.getMessage());
            return numberSetting(name, values[0]);
        }
    }

    /** Leave Right Shift exclusively to the ProVisuals main menu. */
    public static boolean disableSeparateSettingsKey() {
        try {
            Class<?> client = clientClass();
            Field field = client.getDeclaredField("settingsKey");
            field.setAccessible(true);
            Object value = field.get(null);
            if (!(value instanceof KeyMapping keyMapping)) return false;
            keyMapping.setKey(InputConstants.UNKNOWN);
            try {
                var reset = KeyMapping.class.getDeclaredMethod("resetMapping");
                reset.setAccessible(true);
                reset.invoke(null);
            } catch (ReflectiveOperationException ignored) {
                // setKey() can rebuild the key map itself on some versions.
            }
            return true;
        } catch (ClassNotFoundException exception) {
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Diagnostics.warn("Could not unbind the separate SkyLyrics menu key: " + exception.getMessage());
            return true;
        }
    }

    /** Hides the nested SkyLyrics Now Playing HUD so Music Island is the only player overlay. */
    public static boolean disableSeparateNowPlayingHud() {
        try {
            Object config = lyricsConfig();
            Field field = config.getClass().getField("showHud");
            if (field.getBoolean(config)) {
                field.setBoolean(config, false);
                config.getClass().getMethod("save").invoke(config);
            }
            return true;
        } catch (ClassNotFoundException exception) {
            return true;
        } catch (IllegalStateException exception) {
            // SkyLyrics may initialize a few ticks after ProVisuals.
            return false;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Diagnostics.warn("Could not hide SkyLyrics' separate Now Playing HUD: " + exception.getMessage());
            return true;
        }
    }

    public static void openSettings() {
        ProVisualsScreen.openFromMusicIsland("Music", "Lyrics");
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
