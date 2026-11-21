package de.itzrealshiki.provisuals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Optional integration with Nuit's runtime API.
 *
 * One loaded resource-pack skybox is treated as a universal skybox: dimension
 * and vanilla-skybox restrictions are removed, while biome, effects, weather,
 * coordinate ranges, fade and texture settings remain untouched.
 */
public final class CustomSkyBridge {
    private static final String NUIT_API_CLASS = "me.flashyreese.mods.nuit.api.NuitApi";
    private static final String ABSTRACT_SKYBOX_CLASS = "me.flashyreese.mods.nuit.skybox.AbstractSkybox";
    private static final String CONDITIONS_CLASS = "me.flashyreese.mods.nuit.components.Conditions";
    private static final String CONDITION_CLASS = "me.flashyreese.mods.nuit.components.Condition";

    private static final Set<Object> UNIVERSAL_SKYBOXES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static long lastCoverageCheck;

    private CustomSkyBridge() {
    }

    private static Object manager() throws ReflectiveOperationException {
        Class<?> api = Class.forName(NUIT_API_CLASS);
        return api.getMethod("getInstance").invoke(null);
    }

    private static Object invoke(Object target, String method, Class<?>... parameterTypes)
            throws ReflectiveOperationException {
        return target.getClass().getMethod(method, parameterTypes).invoke(target);
    }

    public static boolean setEnabled(boolean enabled) {
        try {
            Object nuit = manager();
            nuit.getClass().getMethod("setEnabled", boolean.class).invoke(nuit, enabled);
            Object actual = invoke(nuit, "isEnabled");
            boolean applied = actual instanceof Boolean value && value == enabled;
            if (applied && enabled) ensureAllWorldCoverage();
            return applied;
        } catch (ClassNotFoundException exception) {
            if (enabled) Diagnostics.warn("Custom Sky needs the optional Nuit mod.");
            return !enabled;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Diagnostics.warn("Could not control Nuit's skybox engine: " + exception.getMessage());
            return false;
        }
    }

    /** If exactly one skybox is loaded, remove only its dimension and vanilla-skybox filters. */
    public static void ensureAllWorldCoverage() {
        long now = System.currentTimeMillis();
        if (now - lastCoverageCheck < 1500L) return;
        lastCoverageCheck = now;

        try {
            Object nuit = manager();
            Object raw = invoke(nuit, "getSkyboxes");
            if (!(raw instanceof Map<?, ?> skyboxes) || skyboxes.size() != 1) return;

            Object skybox = skyboxes.values().iterator().next();
            if (UNIVERSAL_SKYBOXES.contains(skybox)) return;

            Class<?> abstractSkybox = Class.forName(ABSTRACT_SKYBOX_CLASS);
            if (!abstractSkybox.isInstance(skybox)) return;

            Field conditionsField = abstractSkybox.getDeclaredField("conditions");
            conditionsField.setAccessible(true);
            Object oldConditions = conditionsField.get(skybox);
            Class<?> condition = Class.forName(CONDITION_CLASS);
            Class<?> conditions = Class.forName(CONDITIONS_CLASS);
            Object empty = condition.getMethod("of").invoke(null);

            String[] methods = {"getBiomes", "getEffects", "getWeathers",
                    "getXRanges", "getYRanges", "getZRanges"};
            Object[] preserved = new Object[methods.length];
            for (int i = 0; i < methods.length; i++) {
                preserved[i] = oldConditions.getClass().getMethod(methods[i]).invoke(oldConditions);
            }

            Constructor<?> constructor = conditions.getConstructor(
                    condition, condition, condition, condition, condition, condition, condition, condition);
            Object universal = constructor.newInstance(
                    preserved[0], empty, empty, preserved[1], preserved[2], preserved[3], preserved[4], preserved[5]);
            conditionsField.set(skybox, universal);
            UNIVERSAL_SKYBOXES.add(skybox);
            Diagnostics.info("Single Nuit resource-pack skybox now covers every dimension.");
        } catch (ClassNotFoundException exception) {
            // Nuit is optional; without it there is no skybox engine to control.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Diagnostics.warn("Could not enable all-dimension sky coverage: " + exception.getMessage());
        }
    }

    public static String statusText(boolean enabled) {
        if (!enabled) return "Custom Sky OFF — vanilla sky restored";

        try {
            Object nuit = manager();
            Object engineEnabled = invoke(nuit, "isEnabled");
            if (!(engineEnabled instanceof Boolean value) || !value) {
                return "Nuit is installed, but its skybox engine is disabled";
            }

            Object raw = invoke(nuit, "getSkyboxes");
            int loaded = raw instanceof Map<?, ?> map ? map.size() : 0;
            Object active = invoke(nuit, "getActiveSkyboxes");
            int count = active instanceof List<?> list ? list.size() : 0;

            if (loaded == 1 && raw instanceof Map<?, ?> map
                    && UNIVERSAL_SKYBOXES.contains(map.values().iterator().next())) {
                return "Nuit ON — universal skybox for all dimensions; " + count + " active";
            }
            if (count == 0) {
                return loaded == 0
                        ? "Nuit ON — no skybox loaded from resource packs"
                        : "Nuit ON — no matching skybox active in this dimension";
            }
            return "Nuit ON — " + count + " active skybox(es); pack dimension rules preserved";
        } catch (ClassNotFoundException exception) {
            return "Nuit not found — install/enable Nuit for resource-pack skies";
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Diagnostics.warn("Could not read Nuit skybox status: " + exception.getMessage());
            return "Nuit skybox status unavailable — see Error Center";
        }
    }
}
