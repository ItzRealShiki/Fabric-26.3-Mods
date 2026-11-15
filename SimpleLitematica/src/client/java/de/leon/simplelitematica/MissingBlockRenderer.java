package de.leon.simplelitematica;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricOrderedSubmitNodeCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Schematic preview and missing-block overlays. Direct GPU work runs at END_MAIN. */
public final class MissingBlockRenderer {
    private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("simplelitematica", "pipeline/missing_blocks"))
            .withDepthStencilState(Optional.empty())
            .build()
    );
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer BUFFER =
        new StagedVertexBuffer(() -> "SimpleLitematica overlays", RenderType.SMALL_BUFFER_SIZE);
    private static volatile List<BlockPos> missing = List.of();
    private static volatile List<SchematicManager.PreviewBlock> preview = List.of();
    private static int ticks;
    private static int scanCursor;
    private static SchematicData scannedSchematic;
    private static int scannedLayer = Integer.MIN_VALUE;
    private static int scannedOffsetX = Integer.MIN_VALUE;
    private static int scannedOffsetY = Integer.MIN_VALUE;
    private static int scannedOffsetZ = Integer.MIN_VALUE;
    private static int scannedRotation = Integer.MIN_VALUE;
    private static BlockPos scannedPlayerPosition;
    private static final java.util.ArrayList<BlockPos> scanResults = new java.util.ArrayList<>();

    private MissingBlockRenderer() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++ticks % 5 != 0) return;
            SchematicData active = SchematicManager.active();
            if (active == null || client.level == null || client.player == null) {
                missing = List.of();
                preview = List.of();
                scanCursor = 0;
                scanResults.clear();
                scannedSchematic = active;
                return;
            }
            preview = SchematicManager.previewBlocks(client.player.blockPosition(), 4096, 128.0);
            if (!SchematicManager.missingBlocksEnabled()) {
                missing = List.of();
                scanCursor = 0;
                scanResults.clear();
                scannedSchematic = active;
                return;
            }
            BlockPos playerPosition = client.player.blockPosition();
            boolean scanContextChanged = active != scannedSchematic
                || scannedLayer != SchematicManager.visibleLayer()
                || scannedOffsetX != SchematicManager.offsetX()
                || scannedOffsetY != SchematicManager.offsetY()
                || scannedOffsetZ != SchematicManager.offsetZ()
                || scannedRotation != SchematicManager.rotation()
                || !playerPosition.equals(scannedPlayerPosition);
            if (scanContextChanged) {
                scannedSchematic = active;
                scannedLayer = SchematicManager.visibleLayer();
                scannedOffsetX = SchematicManager.offsetX();
                scannedOffsetY = SchematicManager.offsetY();
                scannedOffsetZ = SchematicManager.offsetZ();
                scannedRotation = SchematicManager.rotation();
                scannedPlayerPosition = playerPosition;
                scanCursor = 0;
                scanResults.clear();
                missing = List.of();
            }
            int count = SchematicManager.blockEntryCount();
            scanResults.addAll(SchematicManager.missingPositionsBatch(
                client.level, client.player.blockPosition(), scanCursor, 10_000,
                Math.max(0, 4096 - scanResults.size())));
            scanCursor += 10_000;
            if (scanCursor >= count) {
                missing = List.copyOf(scanResults);
                scanCursor = 0;
                scanResults.clear();
            }
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(MissingBlockRenderer::submitPreview);
        LevelRenderEvents.END_MAIN.register(MissingBlockRenderer::render);
    }

    private static void submitPreview(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        List<SchematicManager.PreviewBlock> blocks = preview;
        Minecraft client = Minecraft.getInstance();
        if (blocks.isEmpty() || client.level == null) return;

        FabricOrderedSubmitNodeCollector collector =
            (FabricOrderedSubmitNodeCollector) context.submitNodeCollector();
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        for (SchematicManager.PreviewBlock block : blocks) {
            BlockPos pos = block.position();
            matrices.pushPose();
            matrices.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            collector.submitBlock(
                matrices,
                block.state(),
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                0xFF62B8FF,
                client.level,
                pos
            );
            matrices.popPose();
        }
    }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        List<BlockPos> missingPositions = missing;
        if (!missingPositions.isEmpty()) renderBoxes(context, missingPositions, THROUGH_WALLS, 1.0f, 0.12f, 0.08f, 0.30f);
    }

    private static void renderBoxes(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context,
                                    List<BlockPos> positions, RenderPipeline pipeline,
                                    float red, float green, float blue, float alpha) {
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = BUFFER.appendDraw(format, primitive,
            primitive == PrimitiveTopology.QUADS
                ? RenderSystem.getProjectionType().vertexSorting() : null);
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer builder = BUFFER.getVertexBuilder(draw);
        Matrix4f pose = matrices.last().pose();
        for (BlockPos pos : positions) {
            addBox(pose, builder, pos.getX()+0.025f, pos.getY()+0.025f, pos.getZ()+0.025f,
                pos.getX()+0.975f, pos.getY()+0.975f, pos.getZ()+0.975f, red, green, blue, alpha);
        }
        matrices.popPose();
        BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = BUFFER.getExecuteInfo(draw);
        if (info != null) draw(Minecraft.getInstance(), info, pipeline);
        BUFFER.endFrame();
    }

    private static void addBox(Matrix4f m, VertexConsumer b, float x1, float y1, float z1,
                               float x2, float y2, float z2, float r, float g, float blue, float a) {
        b.addVertex(m,x1,y1,z2).setColor(r,g,blue,a); b.addVertex(m,x2,y1,z2).setColor(r,g,blue,a);
        b.addVertex(m,x2,y2,z2).setColor(r,g,blue,a); b.addVertex(m,x1,y2,z2).setColor(r,g,blue,a);
        b.addVertex(m,x2,y1,z1).setColor(r,g,blue,a); b.addVertex(m,x1,y1,z1).setColor(r,g,blue,a);
        b.addVertex(m,x1,y2,z1).setColor(r,g,blue,a); b.addVertex(m,x2,y2,z1).setColor(r,g,blue,a);
        b.addVertex(m,x1,y1,z1).setColor(r,g,blue,a); b.addVertex(m,x1,y1,z2).setColor(r,g,blue,a);
        b.addVertex(m,x1,y2,z2).setColor(r,g,blue,a); b.addVertex(m,x1,y2,z1).setColor(r,g,blue,a);
        b.addVertex(m,x2,y1,z2).setColor(r,g,blue,a); b.addVertex(m,x2,y1,z1).setColor(r,g,blue,a);
        b.addVertex(m,x2,y2,z1).setColor(r,g,blue,a); b.addVertex(m,x2,y2,z2).setColor(r,g,blue,a);
        b.addVertex(m,x1,y2,z2).setColor(r,g,blue,a); b.addVertex(m,x2,y2,z2).setColor(r,g,blue,a);
        b.addVertex(m,x2,y2,z1).setColor(r,g,blue,a); b.addVertex(m,x1,y2,z1).setColor(r,g,blue,a);
        b.addVertex(m,x1,y1,z1).setColor(r,g,blue,a); b.addVertex(m,x2,y1,z1).setColor(r,g,blue,a);
        b.addVertex(m,x2,y1,z2).setColor(r,g,blue,a); b.addVertex(m,x1,y1,z2).setColor(r,g,blue,a);
    }

    private static void draw(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        var target = client.gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        if (color == null) return;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "SimpleLitematica overlay", color, Optional.empty(),
            target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }
}
