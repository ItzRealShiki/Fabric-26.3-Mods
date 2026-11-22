package de.itzrealshiki.provisuals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Client-only bridge that installs a permanent, all-dimension Nuit skybox and
 * plays the bundled original ambience while the feature is active.
 * Nuit is optional at runtime, so all of its API access stays reflective.
 */
public final class BlackHoleSkyController {
    private static final String NUIT_API = "me.flashyreese.mods.nuit.api.NuitApi";
    private static final String SKYBOX = "me.flashyreese.mods.nuit.api.skyboxes.Skybox";
    private static final Identifier SKY_ID =
            Identifier.fromNamespaceAndPath("provisuals", "black_hole_sky");
    private static final Identifier MUSIC_ID =
            Identifier.fromNamespaceAndPath("provisuals", "black_hole_ambience");

    private static SimpleSoundInstance ambience;
    private static boolean installed;

    private BlackHoleSkyController() {
    }

    private static Object manager() throws ReflectiveOperationException {
        Class<?> api = Class.forName(NUIT_API);
        return api.getMethod("getInstance").invoke(null);
    }

    public static boolean setEnabled(boolean enabled) {
        try {
            Object nuit = manager();
            if (enabled) {
                nuit.getClass().getMethod("setEnabled", boolean.class).invoke(nuit, true);
                installPermanentSkybox(nuit);
                return true;
            }

            nuit.getClass().getMethod("removePermanentSkybox", Identifier.class).invoke(nuit, SKY_ID);

            installed = false;
            stopMusic(Minecraft.getInstance());
            return true;
        } catch (ClassNotFoundException exception) {
            if (enabled) Diagnostics.warn("Black Hole Sky requires the optional Nuit mod.");
            return !enabled;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Diagnostics.error("Could not update Black Hole Sky", exception);
            return false;
        }
    }

    private static void installPermanentSkybox(Object nuit) throws ReflectiveOperationException {
        if (installed) return;
        String jsonText = """
                {
                  "schemaVersion": 1,
                  "type": "square-textured",
                  "properties": {
                    "layer": 10000,
                    "visibleUnderwater": true,
                    "rotation": {
                      "skyboxRotation": true,
                      "axis": { "0": [0, 1, 0] },
                      "speed": 0.16
                    },
                    "blend": "normal"
                  },
                  "conditions": {},
                  "texture": "provisuals:textures/sky/black_hole.png"
                }
                """;
        JsonObject skyJson = JsonParser.parseString(jsonText).getAsJsonObject();
        Class<?> skyboxType = Class.forName(SKYBOX);
        Method parse = nuit.getClass().getMethod("parseSkybox", Identifier.class, JsonObject.class);
        Method addPermanent = nuit.getClass().getMethod("addPermanentSkybox", Identifier.class, skyboxType);
        Optional<?> skybox = (Optional<?>) parse.invoke(nuit, SKY_ID, skyJson);
        if (skybox.isEmpty()) {
            throw new IllegalStateException("Nuit rejected the Black Hole Sky definition.");
        }
        addPermanent.invoke(nuit, SKY_ID, skybox.get());
        installed = true;
        Diagnostics.info("Permanent Black Hole Sky installed for every dimension.");
    }

    public static void tick(Minecraft client) {
        boolean shouldPlay = ProVisualsConfig.isEnabled("black_hole_sky")
                && ProVisualsConfig.blackHoleMusicEnabled
                && client.level != null
                && client.player != null;
        if (!shouldPlay) {
            stopMusic(client);
            return;
        }

        if (ambience == null || !client.getSoundManager().isActive(ambience)) {
            ambience = new SimpleSoundInstance(
                    MUSIC_ID,
                    SoundSource.MUSIC,
                    ProVisualsConfig.blackHoleMusicVolume,
                    1.0f,
                    RandomSource.create(),
                    true,
                    0,
                    SoundInstance.Attenuation.NONE,
                    0.0,
                    0.0,
                    0.0,
                    true
            );
            client.getSoundManager().play(ambience);
        }
    }

    public static void refreshMusicSettings() {
        stopMusic(Minecraft.getInstance());
    }

    public static void stopMusic(Minecraft client) {
        if (ambience != null) {
            client.getSoundManager().stop(ambience);
            ambience = null;
        }
    }
}
