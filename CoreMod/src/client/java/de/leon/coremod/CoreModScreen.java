package de.leon.coremod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class CoreModScreen extends Screen {
    private enum Page { HOME, SETTINGS, META, FARM, SCHEMATICS }
    private Page page = Page.HOME;
    private final List<EditBox> fields = new ArrayList<>();
    private String status = "READY";

    public CoreModScreen() {
        super(Component.literal(CoreModText.smallCaps("CoreMod")));
    }

    @Override
    protected void init() {
        clearWidgets();
        fields.clear();
        int x = width / 2 - 150;
        int y = height / 2 - 75;

        if (page == Page.HOME) {
            add("META PRICE CHECK", x, y, () -> open(Page.META));
            add("SIMPLE FARM CALCULATOR", x, y + 25, () -> open(Page.FARM));
            add("MY SCHEMATIC LINKS", x, y + 50, () -> open(Page.SCHEMATICS));
            add("QUICK COMMANDS", x, y + 75, () -> open(Page.SETTINGS));
            add("FULLBRIGHT: ON", x, y + 100, () -> status = "FULLBRIGHT IS ALWAYS ENABLED");
            add("CLOSE", x, y + 125, this::onClose);
        } else if (page == Page.SETTINGS) {
            for (int i = 0; i < 4; i++) {
                final int slot = i;
                EditBox f = field(x, y + i * 27, 300, CoreModConfig.quickCommands[i], "COMMAND");
                add("SAVE SLOT " + (i + 1), x + 306, y + i * 27, () -> {
                    CoreModConfig.quickCommands[slot] = f.getValue().trim();
                    CoreModConfig.save();
                    status = "COMMAND SAVED";
                });
            }
            add("BUTTON SIDE: " + CoreModConfig.quickButtonSide, x, y + 116, () -> {
                CoreModConfig.quickButtonSide = CoreModConfig.quickButtonSide.equalsIgnoreCase("LEFT") ? "RIGHT" : "LEFT";
                CoreModConfig.save();
                rebuildWidgets();
            });
            add("BACK", x, y + 143, () -> open(Page.HOME));
        } else if (page == Page.META) {
            field(x, y, 300, "", "ITEM NAME");
            field(x, y + 27, 145, "", "BUY PRICE EACH");
            field(x + 155, y + 27, 145, "", "SELL PRICE EACH");
            field(x, y + 54, 145, "1", "AMOUNT");
            add("CHECK PROFIT", x, y + 83, this::calculateMeta);
            add("BACK", x, y + 125, () -> open(Page.HOME));
        } else if (page == Page.FARM) {
            field(x, y, 300, "", "FARM NAME");
            field(x, y + 27, 145, "", "BUILD COST");
            field(x + 155, y + 27, 145, "", "ESTIMATED SELL VALUE");
            add("CALCULATE", x, y + 58, this::calculateFarm);
            add("BACK", x, y + 100, () -> open(Page.HOME));
        } else if (page == Page.SCHEMATICS) {
            field(x, y, 300, "", "SCHEMATIC NAME");
            field(x, y + 27, 300, "", "HTTPS LINK");
            add("SAVE LINK", x, y + 56, this::saveSchematic);
            add("COPY MY LINKS", x + 160, y + 56, () -> {
                MinecraftClipboard.copy(String.join("\n", CoreModConfig.schematicEntries));
                status = "LINKS COPIED";
            });
            add("BACK", x, y + 100, () -> open(Page.HOME));
        }
    }

    private EditBox field(int x, int y, int w, String value, String hint) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.literal(CoreModText.smallCaps(hint)));
        box.setMaxLength(256);
        box.setValue(value);
        addRenderableWidget(box);
        fields.add(box);
        return box;
    }

    private void add(String label, int x, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(CoreModText.smallCaps(label)), b -> action.run())
            .bounds(x, y, 150, 20).build());
    }

    private void open(Page next) {
        page = next;
        status = "READY";
        rebuildWidgets();
    }

    private double number(int index) {
        return Double.parseDouble(fields.get(index).getValue().trim().replace(",", "."));
    }

    private void calculateMeta() {
        try {
            String item = fields.get(0).getValue().trim();
            double buy = number(1), sell = number(2), amount = number(3);
            if (item.isEmpty() || buy < 0 || sell < 0 || amount <= 0) throw new NumberFormatException();
            double profit = (sell - buy) * amount;
            status = item.toUpperCase() + ": " + (profit >= 0 ? "PROFIT " : "LOSS ") + String.format("%.2f", Math.abs(profit));
        } catch (NumberFormatException ex) {
            status = "ENTER ITEM, PRICES AND AMOUNT";
        }
    }

    private void calculateFarm() {
        try {
            String farm = fields.get(0).getValue().trim();
            double cost = number(1), revenue = number(2);
            if (farm.isEmpty() || cost < 0 || revenue < 0) throw new NumberFormatException();
            double result = revenue - cost;
            status = farm.toUpperCase() + ": " + (result >= 0 ? "EST. PROFIT " : "EST. LOSS ") + String.format("%.2f", Math.abs(result));
        } catch (NumberFormatException ex) {
            status = "ENTER FARM NAME AND VALID COSTS";
        }
    }

    private void saveSchematic() {
        String name = fields.get(0).getValue().trim();
        String url = fields.get(1).getValue().trim();
        if (name.isEmpty() || !(url.startsWith("https://") || url.startsWith("http://"))) {
            status = "ENTER A NAME AND VALID LINK";
            return;
        }
        List<String> entries = new ArrayList<>(List.of(CoreModConfig.schematicEntries));
        entries.add(name + " | " + url);
        CoreModConfig.schematicEntries = entries.toArray(String[]::new);
        CoreModConfig.save();
        status = "LINK SAVED";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, title, width / 2, height / 2 - 105, 0xFFFFFFFF);
        String subtitle = switch (page) {
            case HOME -> "LIGHTWEIGHT PUBLIC SERVER TOOLS";
            case SETTINGS -> "EDIT YOUR FOUR QUICK COMMANDS";
            case META -> "MANUALLY COMPARE BUY AND SELL PRICES";
            case FARM -> "ESTIMATE BUILD COST VS SELL VALUE";
            case SCHEMATICS -> "SAVE YOUR OWN SCHEMATIC LINKS";
        };
        g.centeredText(font, Component.literal(CoreModText.smallCaps(subtitle)), width / 2, height / 2 - 90, 0xFFD9C8FF);

        if (page == Page.SCHEMATICS) {
            int yy = height / 2 + 48;
            for (String entry : CoreModConfig.schematicEntries) {
                if (yy > height / 2 + 100) break;
                g.centeredText(font, Component.literal(CoreModText.smallCaps(entry)), width / 2, yy, 0xFFFFFFFF);
                yy += 11;
            }
        }
        g.centeredText(font, Component.literal(CoreModText.smallCaps(status)), width / 2, height / 2 + 125, 0xFFFFFFFF);
    }

    private static final class MinecraftClipboard {
        static void copy(String value) {
            net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(value);
        }
    }
}
