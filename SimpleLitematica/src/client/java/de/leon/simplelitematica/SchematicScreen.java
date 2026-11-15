package de.leon.simplelitematica;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
                    Minecraft.getInstance().gui.getChat().addMessage(
                        Component.literal("§aGeladen: " + files.get(selected).getFileName()));
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
        addRenderableWidget(Button.builder(Component.literal("Drehen 90°"), button -> {
            SchematicManager.rotate();
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§aSchematic gedreht."));
        }).bounds(left, top + 79, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Verschieben +1 X"), button -> {
            SchematicManager.move(1,0,0);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§aSchematic verschoben."));
        }).bounds(left + 155, top + 79, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Schließen"), button -> onClose())
            .bounds(left, top + 115, 300, 20).build());
    }

    private void refresh() { files = SchematicManager.files(); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, height / 2 - 105, 0xFFFFFFFF);
        if (files.isEmpty()) {
            graphics.centeredText(font, Component.literal("Keine .slite-Dateien gefunden"), width / 2, height / 2 - 55, 0xFFAAAAAA);
        } else {
            for (int i=0; i<files.size() && i<5; i++) {
                int color = i == selected ? 0xFFFFFF55 : 0xFFFFFFFF;
                graphics.centeredText(font, Component.literal((i == selected ? "> " : "") + files.get(i).getFileName()), width / 2, height / 2 - 55 + i*12, color);
            }
            graphics.centeredText(font, Component.literal("Mausrad/Links-Rechts: Datei auswählen"), width/2, height/2 + 16, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && !files.isEmpty()) {
            int row = ((int) event.y() - (height/2 - 55)) / 12;
            if (row >= 0 && row < Math.min(files.size(),5)) selected = row;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
