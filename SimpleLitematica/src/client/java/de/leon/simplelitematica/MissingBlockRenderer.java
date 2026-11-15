package de.leon.simplelitematica;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final RenderPipeline SCHEMATIC_OUTLINES = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES)
            .withLocation(Identifier.fromNamespaceAndPath("simplelitematica", "pipeline/schematic_outlines"))
            .withDepthStencilState(Optional.empty())
            .build()
    );
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    // 8,192 blocks × 24 line vertices fit in this shared staging buffer.
    private static final StagedVertexBuffer BUFFER =
        new StagedVertexBuffer(() -> "SimpleLitematica overlays", 4 * 1024 * 1024);
    private static volatile List<BlockPos> missing = List.of();
    private static BlockModelResolver blockModelResolver;
    private static net.minecraft.client.resources.model.ModelManager cachedModelManager;
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Map<BlockState, BlockModelRenderState> MODEL_STATES = new HashMap<>();
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
            BlockPos schematicOrigin = SchematicManager.resolveOrigin(client.player.blockPosition());
            preview = SchematicManager.previewBlocks(schematicOrigin, 8192, 128.0);
            if (!SchematicManager.missingBlocksEnabled()) {
                missing = List.of();
                scanCursor = 0;
                scanResults.clear();
                scannedSchematic = active;
                return;
            }
            BlockPos playerPosition = SchematicManager.resolveOrigin(client.player.blockPosition());
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
                client.level, playerPosition, scanCursor, 10_000,
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

        if (cachedModelManager != client.getModelManager()) {
            cachedModelManager = client.getModelManager();
            blockModelResolver = new BlockModelResolver(cachedModelManager);
            MODEL_STATES.clear();
        }
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        for (SchematicManager.PreviewBlock block : blocks) {
            BlockPos pos = block.position();
            matrices.pushPose();
            matrices.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            // Resolve and cache the actual vanilla block model for each state.
            // White tint preserves the model's original texture colors; the
            // alpha keeps the preview visually distinct from placed blocks.
            BlockModelRenderState model = MODEL_STATES.computeIfAbsent(block.state(), state -> {
                BlockModelRenderState renderState = new BlockModelRenderState();
                blockModelResolver.update(renderState, state, BLOCK_DISPLAY_CONTEXT);
                return renderState;
            });
            model.submit(matrices, context.submitNodeCollector(), 0xF000F0,
                OverlayTexture.NO_OVERLAY, 0xA0FFFFFF);
            matrices.popPose();
        }
    }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        List<SchematicManager.PreviewBlock> schematicBlocks = preview;
        if (!schematicBlocks.isEmpty()) {
            // Outline every schematic block, including blocks that already match
            // the world. This keeps the target cell visible while building.
            java.util.ArrayList<BlockPos> outlinePositions = new java.util.ArrayList<>(schematicBlocks.size());
            for (SchematicManager.PreviewBlock block : schematicBlocks) {
                outlinePositions.add(block.position());
            }
            renderOutlines(context, outlinePositions, SCHEMATIC_OUTLINES, 0.38f, 0.88f, 1.0f, 0.95f);
        }

        List<BlockPos> missingPositions = missing;
        if (!missingPositions.isEmpty()) {
            renderBoxes(context, missingPositions, THROUGH_WALLS, 1.0f, 0.12f, 0.08f, 0.30f);
        }
    }

    private static void renderOutlines(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context,
                                       List<BlockPos> positions, RenderPipeline pipeline,
                                       float red, float green, float blue, float alpha) {
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null || positions.isEmpty()) return;
        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = BUFFER.appendDraw(format, primitive, null);
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer builder = BUFFER.getVertexBuilder(draw);
        Matrix4f pose = matrices.last().pose();
        for (BlockPos pos : positions) {
            // Slightly expand the wireframe so it stays visible over the
            // translucent block model and does not z-fight with block faces.
            addWireframeBox(pose, builder,
                pos.getX() - 0.002f, pos.getY() - 0.002f, pos.getZ() - 0.002f,
                pos.getX() + 1.002f, pos.getY() + 1.002f, pos.getZ() + 1.002f,
                red, green, blue, alpha);
        }
        matrices.popPose();
        BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = BUFFER.getExecuteInfo(draw);
        if (info != null) draw(Minecraft.getInstance(), info, pipeline);
        BUFFER.endFrame();
    }

    private static void addWireframeBox(Matrix4f m, VertexConsumer b,
                                        float x1, float y1, float z1, float x2, float y2, float z2,
                                        float r, float g, float blue, float a) {
        // Bottom face
        line(m, b, x1, y1, z1, x2, y1, z1, r, g, blue, a);
        line(m, b, x2, y1, z1, x2, y1, z2, r, g, blue, a);
        line(m, b, x2, y1, z2, x1, y1, z2, r, g, blue, a);
        line(m, b, x1, y1, z2, x1, y1, z1, r, g, blue, a);
        // Top face
        line(m, b, x1, y2, z1, x2, y2, z1, r, g, blue, a);
        line(m, b, x2, y2, z1, x2, y2, z2, r, g, blue, a);
        line(m, b, x2, y2, z2, x1, y2, z2, r, g, blue, a);
        line(m, b, x1, y2, z2, x1, y2, z1, r, g, blue, a);
        // Four vertical edges
        line(m, b, x1, y1, z1, x1, y2, z1, r, g, blue, a);
        line(m, b, x2, y1, z1, x2, y2, z1, r, g, blue, a);
        line(m, b, x2, y1, z2, x2, y2, z2, r, g, blue, a);
        line(m, b, x1, y1, z2, x1, y2, z2, r, g, blue, a);
    }

    private static void line(Matrix4f m, VertexConsumer b,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float blue, float a) {
        b.addVertex(m, x1, y1, z1).setColor(r, g, blue, a);
        b.addVertex(m, x2, y2, z2).setColor(r, g, blue, a);
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
