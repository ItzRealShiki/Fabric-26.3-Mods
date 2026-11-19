package de.leon.packmanager;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class PackManagerScreen extends Screen {
    private final Screen parent;

    public PackManagerScreen(Screen parent) {
        super(Text.translatable("screen.packmanager.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Resource-pack actions are added here while preserving vanilla widget styling.
        addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                Text.translatable("gui.back"),
                button -> close()
        ).dimensions(this.width / 2 - 100, this.height -  [... ELLIPSIZATION ...]n context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.packmanager.subtitle"), this.width / 2, this.height / 2 - 8, 0xA0A0A0);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.packmanager.update_info"), this.width / 2, this.height / 2 + 12, 0xA0A0A0);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
