package de.leon.coremod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CoreModScreen extends Screen {
    private enum Page { HOME, SETTINGS, META, FARM_FINDER, SCHEMATICS, SCOREBOARD }
    private Page page = Page.HOME;
    private String status = "READY";

    public CoreModScreen() {
        super(Component.literal(CoreModText.smallCaps("CoreMod")));
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = width / 2 - 110;
        int y = height / 2 - 95;
        if (page == Page.HOME) {
            addButton("META GUI", x, y, () -> open(Page.META));
            addButton("FARM FINDER", x, y + 25, () -> open(Page.FARM_FINDER));
            addButton("SCHEMATIC CATALOG", x, y + 50, () -> open(Page.SCHEMATICS));
            addButton("CUSTOM SCOREBOARD", x, y + 75, () -> open(Page.SCOREBOARD));
            addButton("SETTINGS", x, y + 100, () -> open(Page.SETTINGS));
            addButton("CLOSE", x, y + 125, this::onClose);
        } else if (page == Page.SETTINGS) {
            for (int i = 0; i < 4; i++) {
                final int slot = i;
                addButton("QUICK " + (i + 1) + ": " + CoreModConfig.quickCommands[i],
                    x, y + i * 25, () -> {
                        CoreModConfig.quickCommands[slot] = CoreModConfig.quickCommands[slot].equals("/home")
                            ? "/spawn" : "/home";
                        CoreModConfig.save();
                        rebuildWidgets();
                    });
            }
            addButton("BACK", x, y + 110, () -> open(Page.HOME));
        } else if (page == Page.SCOREBOARD) {
            addButton("CUSTOM SCOREBOARD: " + (CoreModConfig.customScoreboardEnabled ? "ON" : "OFF"),
                x, y, () -> {
                    CoreModConfig.customScoreboardEnabled = !CoreModConfig.customScoreboardEnabled;
                    CoreModConfig.save();
                    status = "SCOREBOARD SETTING SAVED";
                    rebuildWidgets();
                });
            addButton("BACK", x, y + 30, () -> open(Page.HOME));
        } else {
            addButton("BACK", x, y + 70, () -> open(Page.HOME));
        }
        if (page == Page.HOME) {
            for (int i = 0; i < 4; i++) {
                final int slot = i;
                addButton(CoreModConfig.quickCommands[i], x + 225, y + i * 25, () -> {
                    CoreModClient.runQuickCommand(slot);
                    status = "COMMAND SENT";
                });
            }
        }
    }

    private void addButton(String label, int x, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(CoreModText.smallCaps(label)),
            button -> action.run()).bounds(x, y, 220, 20).build());
    }

    private void open(Page target) {
        page = target;
        status = "READY";
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, height / 2 - 125, 0xFFFFFFFF);
        String subtitle = switch (page) {
            case HOME -> "DONUT SMP CLIENT TOOLS";
            case SETTINGS -> "QUICK COMMAND SETTINGS";
            case META -> "MARKET DATA MODULE - IN DEVELOPMENT";
            case FARM_FINDER -> "FARM RECOMMENDATIONS - IN DEVELOPMENT";
            case SCHEMATICS -> "SCHEMATIC LINKS AND MATERIALS - IN DEVELOPMENT";
            case SCOREBOARD -> "CUSTOM DISPLAY SETTINGS";
        };
        graphics.centeredText(font, Component.literal(CoreModText.smallCaps(subtitle)),
            width / 2, height / 2 - 108, 0xFFB9A0FF);
        graphics.centeredText(font, Component.literal(CoreModText.smallCaps(status)),
            width / 2, height / 2 + 112, 0xFFAAAAAA);
    }
}
