package de.itzrealshiki.provisuals;

import java.lang.reflect.Method;

/** Reads the current SkyLyrics HUD bounds without linking against its nested mod at compile time. */
public final class BossBarLayoutBridge {
    private static final String HUD_CLASS = "de.henner.lyricsinthesky.render.NowPlayingHud";
    private static volatile Method lastBoundsMethod;
    private static volatile Method xMethod;
    private static volatile Method yMethod;
    private static volatile Method widthMethod;
    private static volatile Method heightMethod;
    private static volatile boolean lookupAttempted;

    private BossBarLayoutBridge() {
    }

    /** Vanilla starts at y=12; move it below a top-anchored SkyLyrics bar when one is visible. */
    public static int bossBarTop(int vanillaY) {
        Object bounds = currentBounds();
        if (bounds == null) return vanillaY;
        try {
            int x = ((Number) xMethod.invoke(bounds)).intValue();
            int y = ((Number) yMethod.invoke(bounds)).intValue();
            int width = ((Number) widthMethod.invoke(bounds)).intValue();
            int height = ((Number) heightMethod.invoke(bounds)).intValue();
            if (width <= 0 || height <= 0) return vanillaY;

            // A bottom-positioned or manually dragged bar should not force the boss bar off-screen.
            if (y > 90) return vanillaY;
            int barBottom = y + height;
            int candidate = barBottom + 14; // 5 px breathing room after the Island, then title above the boss bar.
            return Math.max(vanillaY, candidate);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return vanillaY;
        }
    }

    private static Object currentBounds() {
        try {
            resolveMethods();
            if (lastBoundsMethod == null) return null;
            return lastBoundsMethod.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static synchronized void resolveMethods() {
        if (lookupAttempted) return;
        lookupAttempted = true;
        try {
            Class<?> hud = Class.forName(HUD_CLASS, false, BossBarLayoutBridge.class.getClassLoader());
            lastBoundsMethod = hud.getMethod("lastBounds");
            Class<?> bounds = lastBoundsMethod.getReturnType();
            xMethod = bounds.getMethod("x");
            yMethod = bounds.getMethod("y");
            widthMethod = bounds.getMethod("width");
            heightMethod = bounds.getMethod("height");
            Diagnostics.info("SkyLyrics bounds hook found for boss bar layout");
        } catch (ReflectiveOperationException | LinkageError exception) {
            Diagnostics.warn("SkyLyrics bounds hook unavailable; boss bar keeps vanilla position");
        }
    }
}
