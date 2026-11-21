package de.leon.playerstatushud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Locale;

public final class PlayerStatusHudClient implements ClientModInitializer {
    private static final double MAX_DISTANCE_SQUARED = 100.0 * 100.0;
    private static final int BOX_WIDTH = 132;
    private static final int BOX_HEIGHT = 64;
    private static final int BACKGROUND = 0xE6101014;
    private static final int BORDER = 0xFF35353D;
    private static final int HEALTH_BACKGROUND = 0xFF3B2024;
    private static final int HEALTH_COLOR = 0xFFE34B55;
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("playerstatushud", "player_status");

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(HUD_ID, (graphics, deltaTracker) -> renderPlayers(graphics));
    }

    private static void renderPlayers(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.options.hideGui) return;

        var localPlayer = client.player;
        var camera = client.gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.getPosition();
        Quaternionf cameraRotation = new Quaternionf(camera.rotation()).conjugate();
        var projection = new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());

        for (Player target : client.level.players()) {
            if (target == localPlayer || target.isRemoved() || target.isSpectator()) continue;
            if (localPlayer.distanceToSqr(target) > MAX_DISTANCE_SQUARED) continue;
            if (!hasLineOfSight(client, localPlayer, target)) continue;

            Vec3 anchor = target.position().add(0.0, target.getBbHeight() + 0.42, 0.0);
            float[] screen = project(anchor, cameraPosition, cameraRotation, projection,
                graphics.guiWidth(), graphics.guiHeight());
            if (screen == null) continue;

            int x = Math.round(screen[0] - BOX_WIDTH / 2.0f);
            int y = Math.round(screen[1] - BOX_HEIGHT);
            if (x + BOX_WIDTH < 0 || x > graphics.guiWidth()
                || y + BOX_HEIGHT < 0 || y > graphics.guiHeight()) continue;

            drawPlayerBox(graphics, client, target, x, y);
        }
    }

    private static boolean hasLineOfSight(Minecraft client, Player viewer, Player target) {
        Vec3 start = viewer.getEyePosition();
        Vec3 end = target.getEyePosition();
        BlockHitResult hit = client.level.clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static float[] project(Vec3 worldPosition, Vec3 cameraPosition, Quaternionf inverseRotation,
                                   org.joml.Matrix4f projection, int screenWidth, int screenHeight) {
        Vec3 relative = worldPosition.subtract(cameraPosition);
        Vector3f view = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z)
            .rotate(inverseRotation);
        if (view.z >= -0.05f) return null;

        Vector4f clip = new Vector4f(view.x, view.y, view.z, 1.0f);
        projection.transform(clip);
        if (clip.w <= 0.0f) return null;

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        if (ndcX < -1.25f || ndcX > 1.25f || ndcY < -1.25f || ndcY > 1.25f) return null;

        float screenX = (ndcX * 0.5f + 0.5f) * screenWidth;
        float screenY = (0.5f - ndcY * 0.5f) * screenHeight;
        return new float[]{screenX, screenY};
    }

    private static void drawPlayerBox(GuiGraphics graphics, Minecraft client, Player player, int x, int y) {
        drawRoundedPanel(graphics, x, y, BOX_WIDTH, BOX_HEIGHT);

        // Player face, including the outer skin layer.
        Identifier skin = player.getSkin().texture();
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x + 7, y + 7, 18, 18,
            8.0f, 8.0f, 8, 8, 64, 64);
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x + 7, y + 7, 18, 18,
            40.0f, 8.0f, 8, 8, 64, 64);

        String name = player.getName().getString();
        graphics.text(client.font, name, x + 31, y + 7, 0xFFFFFFFF, true);

        float maxHealth = Math.max(1.0f, player.getMaxHealth());
        float health = Mth.clamp(player.getHealth(), 0.0f, maxHealth);
        float healthFraction = health / maxHealth;
        String hearts = formatHearts(health / 2.0f);
        graphics.text(client.font, hearts + " ♥", x + 31, y + 19, 0xFFFFD5D8, true);

        int barX = x + 31;
        int barY = y + 32;
        int barWidth = 91;
        graphics.fill(barX, barY, barX + barWidth, barY + 5, HEALTH_BACKGROUND);
        int filled = Math.round(barWidth * healthFraction);
        if (filled > 0) graphics.fill(barX, barY, barX + filled, barY + 5, HEALTH_COLOR);

        drawArmor(graphics, player, x + 7, y + 42);
    }

    private static String formatHearts(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void drawArmor(GuiGraphics graphics, Player player, int startX, int y) {
        List<EquipmentSlot> slots = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        );
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = player.getItemBySlot(slots.get(i));
            int slotX = startX + i * 29;
            if (stack.isEmpty()) continue;

            graphics.renderItem(stack, slotX, y);
            if (!stack.isDamageableItem()) continue;

            float remaining = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
            remaining = Mth.clamp(remaining, 0.0f, 1.0f);
            int color = 0xFF000000 | Mth.hsvToRgb(remaining / 3.0f, 1.0f, 1.0f);
            graphics.fill(slotX + 1, y + 17, slotX + 14, y + 19, 0xFF202020);
            int width = Math.round(13 * remaining);
            if (width > 0) graphics.fill(slotX + 1, y + 17, slotX + 1 + width, y + 19, color);
        }
    }

    private static void drawRoundedPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 3, y, x + width - 3, y + height, BACKGROUND);
        graphics.fill(x + 1, y + 2, x + width - 1, y + height - 2, BACKGROUND);
        graphics.fill(x, y + 4, x + width, y + height - 4, BACKGROUND);

        graphics.fill(x + 4, y, x + width - 4, y + 1, BORDER);
        graphics.fill(x + 2, y + 1, x + width - 2, y + 2, BORDER);
        graphics.fill(x + 1, y + 2, x + width - 1, y + 3, BORDER);
        graphics.fill(x, y + 4, x + 1, y + height - 4, BORDER);
        graphics.fill(x + width - 1, y + 4, x + width, y + height - 4, BORDER);
        graphics.fill(x + 1, y + height - 3, x + width - 1, y + height - 2, BORDER);
        graphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, BORDER);
    }
}
