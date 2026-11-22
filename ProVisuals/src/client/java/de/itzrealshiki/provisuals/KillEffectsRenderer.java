package de.itzrealshiki.provisuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Non-graphic client-only player kill effects, triggered by confirmed server death messages. */
public final class KillEffectsRenderer {
    private record RecentTarget(UUID uuid, String name, Vec3 position, long hitAt) {}
    private record Effect(Vec3 position, long startedAt, String style, int color) {}

    private static final List<Effect> EFFECTS = new ArrayList<>();
    private static final long TARGET_VALID_MS = 8_000L;
    private static final long EFFECT_DURATION_MS = 1_900L;
    private static RecentTarget recentTarget;
    private static boolean registered;

    private KillEffectsRenderer() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide() && player == Minecraft.getInstance().player
                    && target instanceof Player targetPlayer && targetPlayer != player) {
                rememberTarget(targetPlayer);
            }
            return InteractionResult.PASS;
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(KillEffectsRenderer::submitEffects);
        Diagnostics.info("Kill effects renderer registered");
    }

    private static synchronized void rememberTarget(Player target) {
        recentTarget = new RecentTarget(target.getUUID(), target.getGameProfile().getName(),
                target.position(), System.currentTimeMillis());
    }

    /** Keep the saved victim position fresh while the attacked player remains loaded. */
    public static synchronized void tickTracking(Minecraft client) {
        if (recentTarget == null) return;
        if (client == null || client.player == null || client.level == null
                || System.currentTimeMillis() - recentTarget.hitAt() > TARGET_VALID_MS) {
            recentTarget = null;
            return;
        }
        Player target = client.level.getPlayerByUUID(recentTarget.uuid());
        if (target != null) {
            recentTarget = new RecentTarget(recentTarget.uuid(), recentTarget.name(),
                    target.position(), recentTarget.hitAt());
        }
    }

    /**
     * Called for incoming system chat on the client thread.
     * Only a death message containing both the recently attacked target and the local
     * player's name can trigger an effect; ordinary hits and unrelated deaths are ignored.
     */
    public static void onDeathMessage(Component component) {
        Minecraft client = Minecraft.getInstance();
        if (!ProVisualsConfig.isEnabled("kill_effects")
                || client.player == null || client.level == null || component == null) return;

        RecentTarget target;
        synchronized (KillEffectsRenderer.class) {
            target = recentTarget;
            recentTarget = null;
        }
        if (target == null || System.currentTimeMillis() - target.hitAt() > TARGET_VALID_MS) return;

        String message = component.getString();
        String localName = client.player.getName().getString();
        if (localName.isBlank() || target.name().isBlank()
                || !message.contains(localName) || !message.contains(target.name())) return;

        boolean friend = FriendManager.isFriend(target.name());
        String style = friend ? "LIGHTNING" : ProVisualsConfig.killEffectStyle;
        int color = friend ? FriendManager.colorForName(target.name()) : ProVisualsUi.accentPrimary();
        spawn(target.position(), style, color, friend);
    }

    private static synchronized void spawn(Vec3 position, String style, int color, boolean friend) {
        long now = System.currentTimeMillis();
        EFFECTS.add(new Effect(position, now, style, color));
        while (EFFECTS.size() > 8) EFFECTS.remove(0);

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        if ("LIGHTNING".equals(style)) {
            client.level.playLocalSound(position.x, position.y, position.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                    friend ? 0.62f : 0.42f, friend ? 1.08f : 1.18f, false);
            for (int i = 0; i < 16; i++) {
                double ox = (client.level.random.nextDouble() - 0.5) * 1.25;
                double oz = (client.level.random.nextDouble() - 0.5) * 1.25;
                client.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        position.x + ox, position.y + 0.2 + client.level.random.nextDouble() * 3.5,
                        position.z + oz, 0.0, -0.08, 0.0);
            }
        } else if ("SOUL_BURST".equals(style)) {
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2.0 * i / 12.0;
                client.level.addParticle(ParticleTypes.SOUL,
                        position.x + Math.cos(angle) * 0.45, position.y + 0.45,
                        position.z + Math.sin(angle) * 0.45,
                        Math.cos(angle) * 0.035, 0.06, Math.sin(angle) * 0.035);
            }
        } else if ("ENDER_BURST".equals(style)) {
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * 2.0 * i / 14.0;
                client.level.addParticle(ParticleTypes.PORTAL,
                        position.x + Math.cos(angle) * 0.45, position.y + 0.35,
                        position.z + Math.sin(angle) * 0.45,
                        Math.cos(angle) * 0.025, 0.045, Math.sin(angle) * 0.025);
            }
        } else if ("FIREWORKS".equals(style)) {
            client.level.playLocalSound(position.x, position.y, position.z,
                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.35f, 1.35f, false);
            for (int i = 0; i < 18; i++) {
                double angle = Math.PI * 2.0 * i / 18.0;
                client.level.addParticle(ParticleTypes.FIREWORK,
                        position.x, position.y + 0.85, position.z,
                        Math.cos(angle) * 0.12, 0.07 + (i % 3) * 0.025, Math.sin(angle) * 0.12);
            }
        } else if ("TOTEM_POP".equals(style)) {
            client.level.playLocalSound(position.x, position.y, position.z,
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.38f, 1.22f, false);
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2.0 * i / 12.0;
                client.level.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                        position.x + Math.cos(angle) * 0.5, position.y + 0.4,
                        position.z + Math.sin(angle) * 0.5,
                        Math.cos(angle) * 0.035, 0.09, Math.sin(angle) * 0.035);
            }
        } else {
            // Skull Rise is intentionally a clean, stylized icon with no graphic detail.
            client.level.addParticle(ParticleTypes.POOF,
                    position.x, position.y + 0.25, position.z, 0.0, 0.08, 0.0);
        }
    }

    private static void submitEffects(LevelRenderContext context) {
        if (!ProVisualsConfig.isEnabled("kill_effects")) {
            synchronized (EFFECTS) { EFFECTS.clear(); }
            return;
        }

        long now = System.currentTimeMillis();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        List<Effect> active = new ArrayList<>();
        synchronized (EFFECTS) {
            Iterator<Effect> iterator = EFFECTS.iterator();
            while (iterator.hasNext()) {
                Effect effect = iterator.next();
                if (now - effect.startedAt() > EFFECT_DURATION_MS) iterator.remove();
                else if (camera.distanceTo(effect.position()) <= 64.0) active.add(effect);
            }
        }

        for (Effect effect : active) {
            float progress = Math.max(0.0f,
                    Math.min(1.0f, (now - effect.startedAt()) / (float) EFFECT_DURATION_MS));
            PoseStack pose = context.poseStack();
            pose.pushPose();
            pose.translate(effect.position().x - camera.x,
                    effect.position().y - camera.y, effect.position().z - camera.z);
            context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugFilledBox(),
                    (matrix, buffer) -> drawEffect(matrix, buffer, effect, progress));
            pose.popPose();
        }
    }

    private static void drawEffect(PoseStack.Pose pose, VertexConsumer out,
                                   Effect effect, float progress) {
        int alpha = Math.max(0, Math.round((1.0f - progress) * 220.0f));
        int main = ProVisualsUi.accentWithAlpha(effect.color(), alpha);
        int light = ProVisualsUi.accentWithAlpha(lighten(effect.color(), 0.34f), alpha);
        int dark = ProVisualsUi.accentWithAlpha(0xFF151322, Math.min(alpha, 175));

        switch (effect.style()) {
            case "LIGHTNING" -> drawLightning(pose, out, progress, main, light);
            case "SOUL_BURST" -> drawSoulBurst(pose, out, progress, main, light);
            case "ENDER_BURST" -> drawEnderBurst(pose, out, progress, main, light);
            case "FIREWORKS" -> drawFireworks(pose, out, progress, main, light);
            case "TOTEM_POP" -> drawTotemPop(pose, out, progress, main, light);
            default -> drawSkullRise(pose, out, progress, main, light, dark);
        }
    }

    private static void drawSkullRise(PoseStack.Pose pose, VertexConsumer out, float progress,
                                      int main, int light, int dark) {
        float rise = progress * 1.15f;
        // Rounded-looking silhouette assembled from small cuboids; no gore or damage imagery.
        cuboid(pose, out, -0.17f, 0.23f + rise, -0.13f, 0.17f, 0.52f + rise, 0.13f, light);
        cuboid(pose, out, -0.13f, 0.07f + rise, -0.115f, 0.13f, 0.27f + rise, 0.115f, main);
        cuboid(pose, out, -0.095f, -0.015f + rise, -0.095f, 0.095f, 0.09f + rise, 0.095f, light);
        // Eye sockets and a tiny nose mark turn the floating shape into a readable skull icon.
        cuboid(pose, out, -0.12f, 0.31f + rise, -0.132f, -0.035f, 0.405f + rise, -0.119f, dark);
        cuboid(pose, out, 0.035f, 0.31f + rise, -0.132f, 0.12f, 0.405f + rise, -0.119f, dark);
        cuboid(pose, out, -0.025f, 0.22f + rise, -0.132f, 0.025f, 0.275f + rise, -0.119f, dark);
        for (int i = 0; i < 3; i++) {
            float x = -0.065f + i * 0.065f;
            cuboid(pose, out, x, 0.07f + rise, -0.119f, x + 0.027f,
                    0.15f + rise, -0.105f, main);
        }
    }

    private static void drawLightning(PoseStack.Pose pose, VertexConsumer out, float progress,
                                      int main, int light) {
        float fade = 1.0f - progress;
        float startY = 4.35f;
        float endY = 0.12f;
        float[] xs = {0.0f, 0.17f, -0.13f, 0.12f, -0.05f, 0.0f};
        for (int i = 0; i < xs.length - 1; i++) {
            float y0 = startY - (startY - endY) * i / (xs.length - 1);
            float y1 = startY - (startY - endY) * (i + 1) / (xs.length - 1);
            drawBeamSegment(pose, out, xs[i], y0, xs[i + 1], y1, 0.075f, light);
            drawBeamSegment(pose, out, xs[i] + 0.055f, y0, xs[i + 1] + 0.055f, y1,
                    0.022f, main);
        }
        // Small ground flash fades with the bolt.
        drawRing(pose, out, 0.28f + progress * 0.62f, 0.045f,
                ProVisualsUi.accentWithAlpha(lighten(main, 0.22f), Math.round(150 * fade)), 0.035f);
    }

    private static void drawSoulBurst(PoseStack.Pose pose, VertexConsumer out, float progress,
                                      int main, int light) {
        float radius = 0.18f + progress * 1.05f;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            float y = 0.32f + (i % 3) * 0.22f + progress * 0.52f;
            cuboid(pose, out, x - 0.045f, y - 0.08f, z - 0.045f,
                    x + 0.045f, y + 0.08f, z + 0.045f, i % 2 == 0 ? light : main);
        }
        drawRing(pose, out, radius, 0.045f, main, 0.05f + progress * 0.15f);
    }

    private static void drawEnderBurst(PoseStack.Pose pose, VertexConsumer out, float progress,
                                       int main, int light) {
        float radius = 0.2f + progress * 1.18f;
        drawRing(pose, out, radius, 0.05f, main, 0.22f);
        drawRing(pose, out, radius * 0.72f, 0.035f, light, 1.05f + progress * 0.28f);
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * i / 6.0 + progress * 2.4;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            float y = 0.25f + (i % 2) * 0.32f + progress * 0.5f;
            cuboid(pose, out, x - 0.035f, y - 0.035f, z - 0.035f,
                    x + 0.035f, y + 0.035f, z + 0.035f, light);
        }
    }

    private static void drawFireworks(PoseStack.Pose pose, VertexConsumer out, float progress,
                                      int main, int light) {
        float radius = 0.15f + progress * 1.15f;
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0 * i / 12.0;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            float y = 0.95f + (i % 3 - 1) * 0.16f + progress * 0.4f;
            drawBeamSegment(pose, out, 0.0f, 0.95f, x, y, 0.028f,
                    i % 2 == 0 ? light : main, z);
            cuboid(pose, out, x - 0.045f, y - 0.045f, z - 0.045f,
                    x + 0.045f, y + 0.045f, z + 0.045f, light);
        }
        drawRing(pose, out, radius * 0.72f, 0.035f, main, 0.95f);
    }

    private static void drawTotemPop(PoseStack.Pose pose, VertexConsumer out, float progress,
                                     int main, int light) {
        float radius = 0.14f + progress * 0.9f;
        cuboid(pose, out, -0.12f, 0.25f + progress * 0.6f, -0.06f,
                0.12f, 0.72f + progress * 0.6f, 0.06f, light);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            cuboid(pose, out, x - 0.03f, 0.35f + progress * 0.55f, z - 0.03f,
                    x + 0.03f, 0.41f + progress * 0.55f, z + 0.03f, i % 2 == 0 ? main : light);
        }
        drawRing(pose, out, radius, 0.04f, main, 0.03f);
    }

    private static void drawBeamSegment(PoseStack.Pose pose, VertexConsumer out,
                                        float x0, float y0, float x1, float y1,
                                        float width, int color) {
        drawBeamSegment(pose, out, x0, y0, x1, y1, width, color, 0.0f);
    }

    private static void drawBeamSegment(PoseStack.Pose pose, VertexConsumer out,
                                        float x0, float y0, float x1, float y1,
                                        float width, int color, float zOffset) {
        float z = zOffset;
        drawQuad(pose, out, color,
                x0 - width, y0, z, x0 + width, y0, z,
                x1 + width, y1, z, x1 - width, y1, z);
        drawQuad(pose, out, color,
                x0 - width, y0, z - width, x1 - width, y1, z - width,
                x1 + width, y1, z - width, x0 + width, y0, z - width);
    }

    private static void drawRing(PoseStack.Pose pose, VertexConsumer out,
                                 float radius, float thickness, int color, float y) {
        final int segments = 40;
        float inner = Math.max(0.025f, radius - thickness);
        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0 * i) / segments;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segments;
            float x0 = (float) Math.cos(a0);
            float z0 = (float) Math.sin(a0);
            float x1 = (float) Math.cos(a1);
            float z1 = (float) Math.sin(a1);
            out.addVertex(pose, x0 * inner, y, z0 * inner).setColor(color);
            out.addVertex(pose, x0 * radius, y, z0 * radius).setColor(color);
            out.addVertex(pose, x1 * radius, y, z1 * radius).setColor(color);
            out.addVertex(pose, x1 * inner, y, z1 * inner).setColor(color);
        }
    }

    private static void cuboid(PoseStack.Pose pose, VertexConsumer out,
                               float x0, float y0, float z0,
                               float x1, float y1, float z1, int color) {
        // Front and back
        drawQuad(pose, out, color, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        drawQuad(pose, out, color, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
        // Left and right
        drawQuad(pose, out, color, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
        drawQuad(pose, out, color, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        // Top and bottom
        drawQuad(pose, out, color, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        drawQuad(pose, out, color, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
    }

    private static void drawQuad(PoseStack.Pose pose, VertexConsumer out, int color,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3) {
        out.addVertex(pose, x0, y0, z0).setColor(color);
        out.addVertex(pose, x1, y1, z1).setColor(color);
        out.addVertex(pose, x2, y2, z2).setColor(color);
        out.addVertex(pose, x3, y3, z3).setColor(color);
    }

    private static int lighten(int color, float amount) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        red += Math.round((255 - red) * amount);
        green += Math.round((255 - green) * amount);
        blue += Math.round((255 - blue) * amount);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
}
