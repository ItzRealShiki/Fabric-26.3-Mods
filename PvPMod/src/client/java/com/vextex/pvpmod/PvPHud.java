package com.vextex.pvpmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public final class PvPHud {
    private PvPHud() {}

    public static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gui.hud.isHidden() || client.gui.screen() != null) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        if (PvPModConfig.isEnabled(PvPFeature.HEALTH_INDICATORS)
                && client.crosshairPickEntity instanceof LivingEntity target
                && target.isAlive()) {
            String health = String.format(java.util.Locale.ROOT, "%.1f / %.1f HP",
                    target.getHealth(), target.getMaxHealth());
            String label = target.getDisplayName().getString() + "  " + health;
            int x = (width - client.font.width(label)) / 2;
            graphics.fill(x - 5, 8, x + client.font.width(label) + 5, 22, 0x99000000);
            graphics.text(client.font, label, x, 11, 0xFFFFFFFF, true);
        }

        if (PvPModConfig.isEnabled(PvPFeature.KEYSTROKES)) {
            drawKeystrokes(graphics, client, height);
        }

        if (PvPModConfig.isEnabled(PvPFeature.ARMOR_HUD)) {
            drawArmor(graphics, client, width, height);
        }

        if (PvPModConfig.isEnabled(PvPFeature.POTION_EFFECTS)) {
            drawEffects(graphics, client, width);
        }
    }

    private static void drawKeystrokes(GuiGraphicsExtractor g, Minecraft mc, int height) {
        int x = 8;
        int y = height - 88;
        drawKey(g, mc, "W", x + 22, y, mc.options.keyUp.isDown());
        drawKey(g, mc, "A", x, y + 22, mc.options.keyLeft.isDown());
        drawKey(g, mc, "S", x + 22, y + 22, mc.options.keyDown.isDown());
        drawKey(g, mc, "D", x + 44, y + 22, mc.options.keyRight.isDown());
        drawKey(g, mc, "JUMP", x, y + 44, mc.options.keyJump.isDown(), 66);
        drawKey(g, mc, "SPRINT", x + 70, y + 44, mc.options.keySprint.isDown(), 66);
        drawKey(g, mc, "LMB", x, y + 66, mc.options.keyAttack.isDown(), 42);
        drawKey(g, mc, "RMB", x + 46, y + 66, mc.options.keyUse.isDown(), 42);
    }

    private static void drawKey(GuiGraphicsExtractor g, Minecraft mc, String text, int x, int y, boolean pressed) {
        drawKey(g, mc, text, x, y, pressed, 20);
    }

    private static void drawKey(GuiGraphicsExtractor g, Minecraft mc, String text, int x, int y, boolean pressed, int w) {
        g.fill(x, y, x + w, y + 20, pressed ? 0xCCFFFFFF : 0xAA000000);
        g.outline(x, y, w, 20, 0xFFAAAAAA);
        int tx = x + (w - mc.font.width(text)) / 2;
        g.text(mc.font, text, tx, y + 6, pressed ? 0xFF111111 : 0xFFFFFFFF, true);
    }

    private static void drawArmor(GuiGraphicsExtractor g, Minecraft mc, int width, int height) {
        int x = width - 84;
        int y = height - 72;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getItemBySlot(slots[i]);
            int px = x + i * 20;
            g.fill(px - 2, y - 2, px + 18, y + 20, 0x88000000);
            if (!stack.isEmpty()) {
                g.item(stack, px, y);
                int remaining = stack.isDamageableItem() && stack.getMaxDamage() > 0
                        ? Math.max(0, 100 - (stack.getDamageValue() * 100 / stack.getMaxDamage())) : -1;
                if (remaining >= 0) {
                    int color = remaining < 20 ? 0xFFFF5555 : remaining < 50 ? 0xFFFFAA33 : 0xFF55FF55;
                    String text = remaining + "%";
                    g.text(mc.font, text, px - 1, y + 20, color, true);
                }
            }
        }
    }

    private static void drawEffects(GuiGraphicsExtractor g, Minecraft mc, int width) {
        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        int y = 8;
        int shown = 0;
        for (MobEffectInstance effect : effects) {
            if (shown++ >= 6) break;
            String name = effect.getEffect().value().getDisplayName().getString();
            if (effect.getAmplifier() > 0) name += " " + (effect.getAmplifier() + 1);
            int seconds = Math.max(0, effect.getDuration() / 20);
            String time = String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
            String line = name + "  " + time;
            int x = width - mc.font.width(line) - 12;
            g.fill(x - 4, y - 2, width - 6, y + 12, 0x99000000);
            g.text(mc.font, line, x, y, 0xFFFFFFFF, true);
            y += 15;
        }
    }
}
