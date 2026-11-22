package de.itzrealshiki.provisuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Purely visual local hit wave and optional earthquake; world block states are never changed. */
public final class EnergyWaveRenderer {
    private record Wave(Vec3 position, long startedAt) {}
    private record ShapeBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}
    private record LiftBlock(int x, int y, int z, float distance, List<ShapeBox> boxes) {}
    private record Earthquake(Vec3 anchor, long startedAt, List<LiftBlock> blocks) {}

    private static final List<Wave> WAVES = new ArrayList<>();
    private static final long DURATION_MS = 620L;
    private static final int EARTHQUAKE_RADIUS = 20;
    private static final float BLOCK_ARRIVAL_MS = 38.0f;
    private static final float BLOCK_PULSE_MS = 360.0f;
    private static final long EARTHQUAKE_DURATION_MS =
            (long) (EARTHQUAKE_RADIUS * BLOCK_ARRIVAL_MS + BLOCK_PULSE_MS + 40.0f);
    private static final float MAX_BLOCK_LIFT = 0.34f;
    private static Earthquake activeEarthquake;
    private static boolean registered;

    private EnergyWaveRenderer() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            Minecraft client = Minecraft.getInstance();
            boolean waveEnabled = ProVisualsConfig.isEnabled("hits_energy_wave");
            boolean earthquakeEnabled = ProVisualsConfig.isEnabled("earthquake");
            boolean maceRequirementMet = !ProVisualsConfig.onlyOnMaceHit
                    || player.getItemInHand(hand).is(Items.MACE);
            if (level.isClientSide() && player == client.player && entity != player
                    && (waveEnabled || earthquakeEnabled) && maceRequirementMet) {
                if (waveEnabled) addWave(entity.position());
                if (earthquakeEnabled) addEarthquake(player, level);
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

    /**
     * Captures the exposed ground surface in a 20-block radius once per hit.
     * Only render-shape snapshots are stored; no block is moved or modified in the world.
     */
    private static synchronized void addEarthquake(Player player, BlockGetter level) {
        BlockPos origin = player.blockPosition();
        List<LiftBlock> blocks = new ArrayList<>();
        int centerX = origin.getX();
        int centerY = origin.getY();
        int centerZ = origin.getZ();
        double playerX = player.getX();
        double playerZ = player.getZ();

        for (int dx = -EARTHQUAKE_RADIUS; dx <= EARTHQUAKE_RADIUS; dx++) {
            for (int dz = -EARTHQUAKE_RADIUS; dz <= EARTHQUAKE_RADIUS; dz++) {
                double distance = Math.hypot(centerX + dx + 0.5 - playerX,
                        centerZ + dz + 0.5 - playerZ);
                if (distance > EARTHQUAKE_RADIUS) continue;

                // Find the first visible, sturdy ground surface in this column.
                for (int y = centerY + 2; y >= centerY - 10; y--) {
                    BlockPos pos = new BlockPos(centerX + dx, y, centerZ + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.isFaceSturdy(level, pos, Direction.UP)) continue;

                    BlockState above = level.getBlockState(pos.above());
                    if (!above.isAir() && above.getFluidState().isEmpty()) continue;

                    VoxelShape shape = state.getShape(level, pos);
                    if (shape.isEmpty()) continue;

                    List<ShapeBox> boxes = new ArrayList<>();
                    for (AABB box : shape.toAabbs()) {
                        boxes.add(new ShapeBox((float) box.minX, (float) box.minY,
                                (float) box.minZ, (float) box.maxX,
                                (float) box.maxY, (float) box.maxZ));
                    }
                    if (!boxes.isEmpty()) {
                        blocks.add(new LiftBlock(dx, y - centerY, dz,
                                (float) distance, List.copyOf(boxes)));
                    }
                    break;
                }
            }
        }

        if (!blocks.isEmpty()) {
            activeEarthquake = new Earthquake(
                    new Vec3(centerX, centerY, centerZ), System.currentTimeMillis(), List.copyOf(blocks));
        }
    }

    private static void submitWaves(LevelRenderContext context) {
        boolean waveEnabled = ProVisualsConfig.isEnabled("hits_energy_wave");
        boolean earthquakeEnabled = ProVisualsConfig.isEnabled("earthquake");
        if (!waveEnabled && !earthquakeEnabled) {
            synchronized (WAVES) { WAVES.clear(); }
            synchronized (EnergyWaveRenderer.class) { activeEarthquake = null; }
            return;
        }

        long now = System.currentTimeMillis();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        List<Wave> active = new ArrayList<>();
        synchronized (WAVES) {
            if (!waveEnabled) {
                WAVES.clear();
            } else {
                Iterator<Wave> iterator = WAVES.iterator();
                while (iterator.hasNext()) {
                    Wave wave = iterator.next();
                    if (now - wave.startedAt() > DURATION_MS) iterator.remove();
                    else active.add(wave);
                }
            }
        }

        for (Wave wave : active) {
            float progress = Math.max(0.0f, Math.min(1.0f, (now - wave.startedAt()) / (float) DURATION_MS));
            float radius = 0.18f + progress * 2.25f;
            int alpha = Math.max(0, Math.round((1.0f - progress) * 170.0f));
            int color = ProVisualsUi.accentWithAlpha(ProVisualsUi.accentPrimary(), alpha);
            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(wave.position().x - camera.x, wave.position().y - camera.y + 0.035,
                    wave.position().z - camera.z);
            context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(),
                    (pose, buffer) -> drawRing(pose, buffer, radius, 0.055f, color));
            poseStack.popPose();
        }

        if (earthquakeEnabled) {
            submitEarthquake(context, camera, now);
        } else {
            synchronized (EnergyWaveRenderer.class) { activeEarthquake = null; }
        }
    }

    private static void submitEarthquake(LevelRenderContext context, Vec3 camera, long now) {
        Earthquake earthquake;
        synchronized (EnergyWaveRenderer.class) {
            earthquake = activeEarthquake;
            if (earthquake == null) return;
            if (now - earthquake.startedAt() > EARTHQUAKE_DURATION_MS) {
                activeEarthquake = null;
                return;
            }
        }
        if (camera.distanceTo(earthquake.anchor()) > 64.0) return;

        float elapsed = now - earthquake.startedAt();
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(earthquake.anchor().x - camera.x,
                earthquake.anchor().y - camera.y, earthquake.anchor().z - camera.z);
        context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(),
                (pose, buffer) -> {
                    for (LiftBlock block : earthquake.blocks()) {
                        float localTime = elapsed - block.distance() * BLOCK_ARRIVAL_MS;
                        if (localTime < 0.0f || localTime > BLOCK_PULSE_MS) continue;

                        float pulse = (float) Math.sin(Math.PI * localTime / BLOCK_PULSE_MS);
                        if (pulse <= 0.015f) continue;
                        float lift = MAX_BLOCK_LIFT * pulse;
                        int topAlpha = Math.max(0, Math.min(180, Math.round(150.0f * pulse)));
                        int sideAlpha = Math.round(topAlpha * 0.62f);
                        int topColor = ProVisualsUi.accentWithAlpha(ProVisualsUi.accentLight(), topAlpha);
                        int sideColor = ProVisualsUi.accentWithAlpha(ProVisualsUi.accentPrimary(), sideAlpha);

                        for (ShapeBox box : block.boxes()) {
                            drawLiftedBox(pose, buffer, block.x() + box.minX(),
                                    block.y() + box.minY() + lift, block.z() + box.minZ(),
                                    block.x() + box.maxX(), block.y() + box.maxY() + lift,
                                    block.z() + box.maxZ(), topColor, sideColor);
                        }
                    }
                });
        poseStack.popPose();
    }

    private static void drawLiftedBox(PoseStack.Pose pose, VertexConsumer out,
                                      float x0, float y0, float z0,
                                      float x1, float y1, float z1,
                                      int topColor, int sideColor) {
        // Top face
        drawQuad(pose, out, topColor,
                x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        // Four sides; the empty gap beneath the lifted overlay makes the motion readable.
        drawQuad(pose, out, sideColor,
                x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        drawQuad(pose, out, sideColor,
                x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        drawQuad(pose, out, sideColor,
                x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
        drawQuad(pose, out, sideColor,
                x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
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
