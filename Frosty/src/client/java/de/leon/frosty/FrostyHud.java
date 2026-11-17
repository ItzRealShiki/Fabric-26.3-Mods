package de.leon.frosty;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

public final class FrostyHud {
    private static long lastBlockingChange;
    private static boolean wasBlocking;

    private FrostyHud() {}

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("frosty", "shield_status"), (g, delta) -> {
            if (!FrostyConfig.shieldStatus) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.gui.hud.isHidden()) return;

            var player = mc.player;
            var main = player.getMainHandItem();
            var off = player.getOffhandItem();
            boolean hasShield = main.is(Items.SHIELD) || off.is(Items.SHIELD);
            boolean blocking = player.isBlocking();
            long now = System.currentTimeMillis();
            if (blocking != wasBlocking) { lastBlockingChange = now; wasBlocking = blocking; }

            String label;
            int color;
            if (!hasShield) {
                label = "NO SHIELD";
                color = FrostyConfig.color(FrostyConfig.brokenColor, 0xFFFF667A);
            } else if (blocking && now - lastBlockingChange < 180L) {
                label = "BLOCKING";
                color = FrostyConfig.color(FrostyConfig.blockingColor, 0xFFFFFFFF);
            } else if (blocking) {
                label = "BLOCKED";
                color = FrostyConfig.color(FrostyConfig.blockedColor, 0xFF58D7FF);
            } else {
                label = "NOT BLOCKING";
                color = FrostyConfig.color(FrostyConfig.notBlockingColor, 0xFFA8C7D8);
            }

            float scale = (float) FrostyConfig.hudScale;
            g.pose().pushMatrix();
            g.pose().translate(FrostyConfig.shieldX, FrostyConfig.shieldY);
            g.pose().scale(scale, scale);
            int width = mc.font.width(label) + 18;
            g.fill(0, 0, width, 18, 0xCC071A2A);
            g.fill(0, 0, 2, 18, 0xFF58D7FF);
            g.text(mc.font, Component.literal(label), 9, 5, color);
            g.pose().popMatrix();
        });
    }
}
