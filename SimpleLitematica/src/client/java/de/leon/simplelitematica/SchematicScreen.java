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
    private enum View { MAIN, LOAD, DELETE }
    private View view = View.MAIN;
    private int selected = -1;
    private Path deleteCandidate;
    private List<Path> files = List.of();

    public SchematicScreen() {
        super(Component.literal(SchematicManager.smallCaps("SimpleLitematica")));
    }

    @Override
    protected void init() {
        refresh();
        int left = width / 2 - 150;
        int top = height / 2 - 105;

        if (view == View.LOAD || view == View.DELETE) {
            addRenderableWidget(Button.builder(label("BACK"), button -> {
                view = View.MAIN;
                selected = -1;
                deleteCandidate = null;
                rebuildWidgets();
            }).bounds(left, top + 209, 300, 20).build());

            int visible = Math.min(files.size(), 8);
            for (int i = 0; i < visible; i++) {
                final Path file = files.get(i);
                final int index = i;
                String prefix = (view == View.DELETE && file.equals(deleteCandidate)) ? "[X] " : "";
                addRenderableWidget(Button.builder(label(prefix + file.getFileName().toString()), button -> {
                    selected = index;
                    if (view == View.LOAD) {
                        if (SchematicManager.load(file)) {
                            showMessage("Schematic loaded.");
                            view = View.MAIN;
                            selected = -1;
                        } else showMessage("Could not load this schematic.");
                    } else {
                        deleteCandidate = file;
                    }
                    rebuildWidgets();
                }).bounds(left, top + 20 + i * 22, 300, 20).build());
            }

            if (view == View.DELETE) {
                addRenderableWidget(Button.builder(label("CONFIRM DELETE"), button -> {
                    if (deleteCandidate == null) {
                        showMessage("Select a schematic first.");
                        return;
                    }
                    boolean deleted = SchematicManager.delete(deleteCandidate);
                    deleteCandidate = null;
                    refresh();
                    selected = -1;
                    rebuildWidgets();
                    showMessage(deleted ? "Schematic file deleted." : "Could not delete selected file.");
                }).bounds(left, top + 182, 145, 20).build());
            }
            return;
        }

        addRenderableWidget(Button.builder(label("LOAD"), button -> {
            view = View.LOAD;
            selected = -1;
            refresh();
            rebuildWidgets();
        }).bounds(left, top + 18, 145, 20).build());

        addRenderableWidget(Button.builder(label("DELETE"), button -> {
            view = View.DELETE;
            selected = -1;
            deleteCandidate = null;
            refresh();
            rebuildWidgets();
        }).bounds(left + 155, top + 18, 145, 20).build());

        addRenderableWidget(Button.builder(label("UNLOAD"), button -> {
            boolean unloaded = SchematicManager.unload();
            showMessage(unloaded ? "Schematic unloaded. File kept." : "No schematic is currently loaded.");
        }).bounds(left, top + 45, 145, 20).build());

        addRenderableWidget(Button.builder(label("PASTE"), button -> SchematicManager.print(false))
            .bounds(left + 155, top + 45, 145, 20).build());

        addRenderableWidget(Button.builder(layerModeLabel(), button -> {
            SchematicManager.toggleLayerMode();
            button.setMessage(layerModeLabel());
        }).bounds(left, top + 72, 145, 20).build());

        addRenderableWidget(Button.builder(label("NEXT LAYER"), button -> {
            SchematicManager.cycleLayer();
            button.setMessage(layerLabel());
        }).bounds(left + 155, top + 72, 145, 20).build());

        addRenderableWidget(Button.builder(label("OPEN FOLDER"), button -> {
            try {
                Blaze3D.openPath(SchematicManager.directory());
            } catch (Exception exception) {
                exception.printStackTrace();
                showMessage("Could not open schematic folder. Check the game log.");
            }
        }).bounds(left, top + 99, 145, 20).build());

        addRenderableWidget(Button.builder(label("ROTATE 90"), button -> {
            SchematicManager.rotate();
            showMessage("Schematic rotated.");
        }).bounds(left + 155, top + 99, 145, 20).build());

        addRenderableWidget(Button.builder(label("MOVE +1 X"), button -> {
            SchematicManager.move(1, 0, 0);
            showMessage("Schematic moved.");
        }).bounds(left, top + 126, 145, 20).build());

        addRenderableWidget(Button.builder(label("CLOSE"), button -> onClose())
            .bounds(left + 155, top + 126, 145, 20).build());
    }

    private static Component label(String value) {
        return Component.literal(SchematicManager.smallCaps(value));
    }

    private Component layerModeLabel() {
        return label(SchematicManager.layerMode() ? "MODE: LAYER" : "MODE: ALL");
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
        graphics.centeredText(font, title, width / 2, height / 2 - 120, 0xFFFFFFFF);
        if (view == View.LOAD || view == View.DELETE) {
            graphics.centeredText(font, label(view == View.LOAD ? "SELECT SCHEMATIC TO LOAD" : "SELECT FILE TO DELETE"),
                width / 2, height / 2 - 100, 0xFFAAAAAA);
            if (files.isEmpty()) {
                graphics.centeredText(font, label("NO .SLITE FILES FOUND"), width / 2, height / 2 - 45, 0xFFAAAAAA);
            }
        } else {
            SchematicData active = SchematicManager.active();
            String activeName = active == null ? "NO SCHEMATIC LOADED" : "LOADED: " + active.name;
            graphics.centeredText(font, label(activeName), width / 2, height / 2 - 82, 0xFFFFFFFF);
            graphics.centeredText(font, label("SHIFT + M: MENU | UP/DOWN: MOVE"),
                width / 2, height / 2 + 88, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }
}
