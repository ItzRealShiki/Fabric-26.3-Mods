package de.itzrealshiki.provisuals;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Optional integration with Nuit's public runtime API.
 *
 * ProVisuals deliberately does not bundle a second sky renderer: Nuit owns
 * resource-pack discovery, skybox conditions, texture loading and rendering.
 */
public final class CustomSkyBridge {
    private static final String NUIT_API_CLASS = "me.flashyreese.mods.nuit.api.NuitApi";

    private CustomSkyBridge() {
    }

    private static Object manager() throws ReflectiveOperationException {
        Class<?> api = Class.forName(NUIT_API_CLASS);
        Method getInstance = api.getMethod("getInstance");
        return getInstance.invoke(null);
    }

    private static Object invoke(Object target, String method, Class<?>... parameterTypes)
            throws ReflectiveOperationException {
        return target.getClass().getMethod(method, parameterTypes).invoke(target);
    }

    /**
     * Enables or disables Nuit's skybox renderer without making Nuit a hard
     * dependency of ProVisuals.
     */
    public static boolean setEnabled(boolean enabled) {
        try {
            Object nuit = manager();
            Method setter = nuit.getClass().getMethod("setEnabled", boolean.class);
            setter.invoke(nuit, enabled);
            Object actual = invoke(nuit, "isEnabled");
            return actual instanceof Boolean value && value == enabled;
        } catch (ClassNotFoundException exception) {
            if (enabled) {
                Diagnostics.warn("Custom Sky needs the optional Nuit mod.");
            }
            return !enabled;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Diagnostics.warn("Could not control Nuit's skybox engine: " + exception.getMessage());
            return false;
        }
    }

    /**
     * Returns a useful UI status and makes clear when a pack has no matching
     * skybox for the current dimension/conditions.
     */
    public static String statusText(boolean enabled) {
        if (!enabled) {
            return "Custom Sky OFF — vanilla sky restored";
        }

        try {
            Object nuit = manager();
            Object engineEnabled = invoke(nuit, "isEnabled");
            if (!(engineEnabled instanceof Boolean value) || !value) {
                return "Nuit is installed, but its skybox engine is disabled";
            }

            Object hasRenderableSkybox = invoke(nuit, "hasActiveRenderableSkyboxes");
            if (!(hasRenderableSkybox instanceof Boolean hasSkybox) || !hasSkybox) {
                return "Nuit ON — no matching resource-pack skybox in this dimension";
            }

            Object activeSkyboxes = invoke(nuit, "getActiveSkyboxes");
            int count = activeSkyboxes instanceof List<?> list ? list.size() : 1;
            return "Nuit ON — " + count
                    + " active skybox(es); source texture resolution preserved (GPU limit applies)";
        } catch (ClassNotFoundException exception) {
            return "Nuit not found — install/enable Nuit for resource-pack skies";
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Diagnostics.warn("Could not read Nuit skybox status: " + exception.getMessage());
            return "Nuit skybox status unavailable — see Error Center";
        }
    }
}
