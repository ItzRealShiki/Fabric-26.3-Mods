package de.leon.playerstatushud;

import de.leon.privacy.api.PrivacyEntry;
import de.leon.privacy.api.PrivacyManager;
import de.leon.privacy.api.PrivacyVisibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public final class PlayerStatusHudClient implements ClientModInitializer {
    private static final double MAX_DISTANCE_SQUARED = 100.0 * 100.0;
    private static final int BOX_WIDTH = 132;
    private static final int BOX_HEIGHT = 64;
    private static final int PANEL_BACKGROUND = 0xFF000000;
    private static final int PANEL_BORDER = 0xFF777777;
    private static final int PANEL_RADIUS = 9;
    private static final int LOW_HEALTH_TEXT = 0xFFFF5963;
    private static final int HEALTH_BACKGROUND = 0xFF3B2024;
    private static final int HEALTH_COLOR = 0xFFE34B55;
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("playerstatushud", "player_status");
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("playerstatushud", "main")
    );
    private static KeyMapping toggleHudKey;
    private static boolean hudEnabled = true;

    @Override
    public void onInitializeClient() {
        // Register this mod with the shared PrivacyMod dashboard.
        PrivacyManager.registerProvider(() -> List.of(
            new PrivacyEntry(
                "playerstatushud.local-overlay",
                "Player Status HUD rendering",
                "The overlay is rendered locally. PlayerStatusHUD adds no custom network packets, telemetry, or data upload.",
                PrivacyVisibility.PRIVATE
            )
        ));

        toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.playerstatushud.toggle_hud",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_H,
            KEY_CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.consumeClick()) {
                hudEnabled = !hudEnabled;
            }
        });
        HudElementRegistry.addLast(HUD_ID, (graphics, deltaTracker) -> renderPlayers(graphics));
    }

    private static void renderPlayers(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!hudEnabled || client.level == null || client.player == null || client.gui.hud.isHidden()) return;

        var localPlayer = client.player;

        for (Player target : client.level.players()) {
            if (target == localPlayer || target.isRemoved() || target.isSpectator()) continue;
            if (localPlayer.distanceToSqr(target) > MAX_DISTANCE_SQUARED) continue;
            if (!hasLineOfSight(client, localPlayer, target)) continue;

            Vec3 anchor = target.position().add(0.0, target.getBbHeight() + 0.42, 0.0);
            Vec3 projected = client.gameRenderer.projectPointToScreen(anchor);
            if (projected.z < -1.0 || projected.z > 1.0) continue;
            float screenX = (float) ((projected.x * 0.5 + 0.5) * graphics.guiWidth());
            float screenY = (float) ((0.5 - projected.y * 0.5) * graphics.guiHeight());

            int x = Math.round(screenX - BOX_WIDTH / 2.0f);
            int y = Math.round(screenY - BOX_HEIGHT);

            // Draw only complete panels that fit inside the visible GUI area.
            if (x < 0 || y < 0 || x + BOX_WIDTH > graphics.guiWidth()
                || y + BOX_HEIGHT > graphics.guiHeight()) continue;

            if (target instanceof AbstractClientPlayer clientPlayer) {
                drawPlayerBox(graphics, client, clientPlayer, x, y);
            }
        }
    }

    private static boolean hasLineOfSight(Minecraft client, Player viewer, Player target) {
        Vec3 start = viewer.getEyePosition();
        Vec3 end = target.getEyePosition();
        BlockHitResult hit = client.level.clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static void drawPlayerBox(GuiGraphicsExtractor graphics, Minecraft client, AbstractClientPlayer player, int x, int y) {
        float maxHealth = Math.max(1.0f, player.getMaxHealth());
        float health = Mth.clamp(player.getHealth(), 0.0f, maxHealth);
        float healthFraction = health / maxHealth;
        boolean lowHealth = health < 6.0f;
        drawRoundedPanel(graphics, x, y, BOX_WIDTH, BOX_HEIGHT, lowHealth);

        // Player face, including the outer skin layer.
        Identifier skin = player.getSkin().body().texturePath();
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x + 7, y + 7,
            8.0f, 8.0f, 18, 18, 8, 8, 64, 64);
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x + 7, y + 7,
            40.0f, 8.0f, 18, 18, 8, 8, 64, 64);

        String name = player.getName().getString();
        graphics.text(client.font, net.minecraft.network.chat.Component.literal(name), x + 31, y + 7,
            lowHealth ? LOW_HEALTH_TEXT : 0xFFFFFFFF);

        String hearts = formatHearts(health / 2.0f);
        graphics.text(client.font, net.minecraft.network.chat.Component.literal(hearts + " ♥"), x + 31, y + 19,
            lowHealth ? LOW_HEALTH_TEXT : 0xFFFFD5D8);

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

    private static void drawArmor(GuiGraphicsExtractor graphics, Player player, int startX, int y) {
        List<EquipmentSlot> slots = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        );
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = player.getItemBySlot(slots.get(i));
            int slotX = startX + i * 29;
            if (stack.isEmpty()) continue;

            graphics.item(stack, slotX, y);
            if (!stack.isDamageableItem()) continue;

            float remaining = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
            remaining = Mth.clamp(remaining, 0.0f, 1.0f);
            int color = 0xFF000000 | Mth.hsvToRgb(remaining / 3.0f, 1.0f, 1.0f);
            graphics.fill(slotX + 1, y + 17, slotX + 14, y + 19, 0xFF202020);
            int width = Math.round(13 * remaining);
            if (width > 0) graphics.fill(slotX + 1, y + 17, slotX + 1 + width, y + 19, color);
        }
    }

    private static void drawRoundedPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean lowHealth) {
        // A custom rounded rectangle: one-pixel grey outline and a solid black interior.
        // Low health is indicated by the text, not by changing the panel's shape or border.
        drawRoundedRect(graphics, x, y, width, height, PANEL_RADIUS, PANEL_BORDER);
        drawRoundedRect(graphics, x + 1, y + 1, width - 2, height - 2,
            PANEL_RADIUS - 1, PANEL_BACKGROUND);
    }

    private static void drawRoundedRect(GuiGraphicsExtractor graphics, int x, int y,
                                        int width, int height, int radius, int color) {
        for (int row = 0; row < height; row++) {
            int distanceFromTop = row;
            int distanceFromBottom = height - 1 - row;
            int inset = 0;

            if (distanceFromTop < radius) {
                inset = roundedCornerInset(radius, distanceFromTop);
            } else if (distanceFromBottom < radius) {
                inset = roundedCornerInset(radius, distanceFromBottom);
            }

            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }

    private static int roundedCornerInset(int radius, int row) {
        double vertical = radius - row - 0.5;
        double horizontal = Math.sqrt(Math.max(0.0, radius * radius - vertical * vertical));
        return Math.max(0, radius - (int) Math.ceil(horizontal));
    }
}
