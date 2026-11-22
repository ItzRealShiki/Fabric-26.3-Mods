package de.itzrealshiki.provisuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Purely visual local hit wave; it does not change damage, knockback or entity state. */
public final class EnergyWaveRenderer {
    private record Wave(Vec3 position, long startedAt) {}
    private static final List<Wave> WAVES = new ArrayList<>();
    private static final long DURATION_MS = 620L;
    private static boolean registered;

    private EnergyWaveRenderer() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            Minecraft client = Minecraft.getInstance();
            if (level.isClientSide() && player == client.player && entity != player
                    && ProVisualsConfig.isEnabled("hits_energy_wave")) {
                addWave(entity.position());
            }
            return InteractionResult.PASS;
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(EnergyWaveRenderer::submitWaves);
    }

    private static synchronized void addWave(Vec3 position) {
        long now = System.currentTimeMillis();
        WAVES.add(new Wave(position, now));
        while (WAVES.size() > 16) WAVES.remove(0);
    }

    private static void submitWaves(LevelRenderContext context) {
        if (!ProVisualsConfig.isEnabled("hits_energy_wave")) {
            synchronized (WAVES) { WAVES.clear(); }
            return;
        }

        long now = System.currentTimeMillis();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        List<Wave> active = new ArrayList<>();
        synchronized (WAVES) {
            Iterator<Wave> iterator = WAVES.iterator();
            while (iterator.hasNext()) {
                Wave wave = iterator.next();
                if (now - wave.startedAt() > DURATION_MS) iterator.remove();
                else active.add(wave);
            }
        }

        for (Wave wave : active) {
            float progress = Math.max(0.0f, Math.min(1.0f, (now - wave.startedAt()) / (float) DURATION_MS));
            float radius = 0.18f + progress * 2.25f;
            int alpha = Math.max(0, Math.round((1.0f - progress) * 170.0f));
            int color = (alpha << 24) | 0x009A6BFF;
            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(wave.position().x - camera.x, wave.position().y - camera.y + 0.035,
                    wave.position().z - camera.z);
            context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(),
                    (pose, buffer) -> drawRing(pose, buffer, radius, 0.055f, color));
            poseStack.popPose();
        }
    }

    private static void drawRing(PoseStack.Pose pose, VertexConsumer out, float radius,
                                 float thickness, int color) {
        final int segments = 32;
        float inner = Math.max(0.025f, radius - thickness);
        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0 * i) / segments;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segments;
            float x0 = (float) Math.cos(a0);
            float z0 = (float) Math.sin(a0);
            float x1 = (float) Math.cos(a1);
            float z1 = (float) Math.sin(a1);

            out.addVertex(pose, x0 * inner, 0.0f, z0 * inner).setColor(color);
            out.addVertex(pose, x0 * radius, 0.0f, z0 * radius).setColor(color);
            out.addVertex(pose, x1 * radius, 0.0f, z1 * radius).setColor(color);
            out.addVertex(pose, x1 * inner, 0.0f, z1 * inner).setColor(color);
        }
    }
}
