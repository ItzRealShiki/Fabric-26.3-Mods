package de.leon.simplelitematica;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

public final class SchematicScreen extends Screen {
    private int selected = 0;
    private List<Path> files = List.of();

    public SchematicScreen() {
        super(Component.literal(SchematicManager.smallCaps("SimpleLitematica")));
    }

    @Override
    protected void init() {
        refresh();
        int left = width / 2 - 150;
        int top = height / 2 - 90;
        addRenderableWidget(Button.builder(label("LOAD"), button -> {
            if (selected >= 0 && selected < files.size()) {
                Path chosen = files.get(selected);
                if (SchematicManager.load(chosen)) {
                    showMessage("Loaded: " + chosen.getFileName());
                    onClose();
                } else showMessage("Could not load selected schematic.");
            }
        }).bounds(left, top + 25, 145, 20).build());

        addRenderableWidget(Button.builder(label("DELETE"), button -> {
            if (selected >= 0 && selected < files.size()) {
                boolean deleted = SchematicManager.delete(files.get(selected));
                refresh();
                selected = Math.max(0, Math.min(selected, files.size() - 1));
                rebuildWidgets();
                if (!deleted) showMessage("Could not delete selected file.");
            }
        }).bounds(left + 155, top + 25, 145, 20).build());

        addRenderableWidget(Button.builder(label("OPEN FOLDER"), button -> {
            try {
                Blaze3D.openPath(SchematicManager.directory());
            } catch (Exception exception) {
                exception.printStackTrace();
                showMessage("Could not open schematic folder. Check the game log.");
            }
        }).bounds(left, top + 52, 145, 20).build());

        addRenderableWidget(Button.builder(layerLabel(), button -> {
            SchematicManager.cycleLayer();
            button.setMessage(layerLabel());
        }).bounds(left + 155, top + 52, 145, 20).build());

        addRenderableWidget(Button.builder(label("ROTATE 90"), button -> {
            SchematicManager.rotate();
            showMessage("Schematic rotated.");
        }).bounds(left, top + 79, 145, 20).build());

        addRenderableWidget(Button.builder(label("MOVE +1 X"), button -> {
            SchematicManager.move(1, 0, 0);
            showMessage("Schematic moved.");
        }).bounds(left + 155, top + 79, 145, 20).build());

        addRenderableWidget(Button.builder(label("CLOSE"), button -> onClose())
            .bounds(left, top + 115, 300, 20).build());
    }

    private static Component label(String value) {
        return Component.literal(SchematicManager.smallCaps(value));
    }

    private Component layerLabel() {
        int layer = SchematicManager.visibleLayer();
        return label("LAYER: " + (layer < 0 ? "ALL" : Integer.toString(layer + 1)));
    }

    private void showMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(label(message));
    }

    private void refresh() {
        files = SchematicManager.files();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, height / 2 - 105, 0xFFFFFFFF);
        if (files.isEmpty()) {
            graphics.centeredText(font, label("NO .SLITE FILES FOUND"), width / 2, height / 2 - 55, 0xFFAAAAAA);
        } else {
            for (int i = 0; i < files.size() && i < 5; i++) {
                int color = i == selected ? 0xFFFFFF55 : 0xFFFFFFFF;
                String prefix = i == selected ? "> " : "";
                graphics.centeredText(font, Component.literal(prefix +
                    SchematicManager.smallCaps(files.get(i).getFileName().toString())),
                    width / 2, height / 2 - 55 + i * 12, color);
            }
            graphics.centeredText(font, label("RIGHT CLICK A FILE TO SELECT"),
                width / 2, height / 2 + 16, 0xFFAAAAAA);
        }
        graphics.centeredText(font, label("SHIFT + M: MENU | UP/DOWN: MOVE"),
            width / 2, height / 2 + 30, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && !files.isEmpty()) {
            int row = ((int) event.y() - (height / 2 - 55)) / 12;
            if (row >= 0 && row < Math.min(files.size(), 5)) selected = row;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
