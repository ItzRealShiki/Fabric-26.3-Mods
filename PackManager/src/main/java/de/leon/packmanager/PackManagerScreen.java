package de.leon.packmanager;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class PackManagerScreen extends Screen {
    private final Screen parent;

    public PackManagerScreen(Screen parent) {
        super(Text.translatable("screen.packmanager.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.back"),
                button -> close()
        ).dimensions(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 24, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.packmanager.subtitle"),
                this.width / 2, this.height / 2 - 16, 0xA0A0A0);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.packmanager.update_info"),
                this.width / 2, this.height / 2 + 2, 0xA0A0A0);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.packmanager.update_help"),
                this.width / 2, this.height / 2 + 18, 0x808080);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
