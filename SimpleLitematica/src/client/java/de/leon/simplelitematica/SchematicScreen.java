package de.leon.simplelitematica;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.List;

public final class SchematicScreen extends Screen {
    private int selected = 0;
    private List<Path> files = List.of();

    public SchematicScreen() {
        super(Component.literal("SimpleLitematica"));
    }

    @Override
    protected void init() {
        refresh();
        int left = this.width / 2 - 150;
        int top = this.height / 2 - 90;
        addRenderableWidget(Button.builder(Component.literal("Laden"), button -> {
            if (!files.isEmpty() && selected >= 0 && selected < files.size()) {
                if (SchematicManager.load(files.get(selected))) {
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§aGeladen: " + files.get(selected).getFileName()), false);
                    onClose();
                }
            }
        }).bounds(left, top + 25, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Löschen"), button -> {
            if (!files.isEmpty() && selected >= 0 && selected < files.size()) {
                SchematicManager.delete(files.get(selected));
                refresh();
                selected = Math.max(0, Math.min(selected, files.size()-1));
                clearWidgets();
                init();
            }
        }).bounds(left + 155, top + 25, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Ordner öffnen"), button -> {
            try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(SchematicManager.directory().toFile()); }
            catch (Exception ignored) {}
        }).bounds(left, top + 52, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Ebene: " + (SchematicManager.visibleLayer() < 0 ? "Alle" : SchematicManager.visibleLayer())), button -> {
            SchematicManager.cycleLayer();
            clearWidgets();
            init();
        }).bounds(left + 155, top + 52, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Drehen 90°"), button -> SchematicManager.rotate())
            .bounds(left, top + 79, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Verschieben +1 X"), button -> SchematicManager.move(1,0,0))
            .bounds(left + 155, top + 79, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Schließen"), button -> onClose())
            .bounds(left, top + 115, 300, 20).build());
    }

    private void refresh() { files = SchematicManager.files(); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 105, 0xFFFFFF);
        if (files.isEmpty()) {
            graphics.drawCenteredString(font, "Keine .slite-Dateien gefunden", width / 2, height / 2 - 55, 0xAAAAAA);
        } else {
            for (int i=0; i<files.size() && i<5; i++) {
                int color = i == selected ? 0xFFFF55 : 0xFFFFFF;
                graphics.drawCenteredString(font, (i == selected ? "> " : "") + files.get(i).getFileName(), width / 2, height / 2 - 55 + i*12, color);
            }
            graphics.drawCenteredString(font, "Mausrad/Links-Rechts: Datei auswählen", width/2, height/2 + 16, 0xAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && !files.isEmpty()) {
            int row = (event.y() - (height/2 - 55)) / 12;
            if (row >= 0 && row < Math.min(files.size(),5)) selected = row;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
