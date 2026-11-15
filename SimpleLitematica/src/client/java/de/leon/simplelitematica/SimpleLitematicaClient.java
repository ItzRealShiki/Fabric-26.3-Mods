package de.leon.simplelitematica;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class SimpleLitematicaClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("simplelitematica", "main"));
    private static KeyMapping openMenuKey;
    private static KeyMapping moveUpKey;
    private static KeyMapping moveDownKey;

    @Override
    public void onInitializeClient() {
        SchematicManager.ensureDirectory();
        MissingBlockRenderer.register();
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.simplelitematica.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, CATEGORY));
        moveUpKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.simplelitematica.move_up", InputConstants.Type.KEYBOARD, InputConstants.KEY_UP, CATEGORY));
        moveDownKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.simplelitematica.move_down", InputConstants.Type.KEYBOARD, InputConstants.KEY_DOWN, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (!client.hasShiftDown()) continue;
                if (client.gui.screen() instanceof SchematicScreen) client.gui.setScreen(null);
                else client.gui.setScreen(new SchematicScreen());
            }
            if (client.gui.screen() == null && client.player != null && SchematicManager.active() != null) {
                while (moveUpKey.consumeClick()) SchematicManager.move(0, 1, 0);
                while (moveDownKey.consumeClick()) SchematicManager.move(0, -1, 0);
            } else {
                while (moveUpKey.consumeClick()) { }
                while (moveDownKey.consumeClick()) { }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("slitematica").executes(context -> {
                Minecraft.getInstance().execute(() ->
                    Minecraft.getInstance().gui.setScreen(new SchematicScreen()));
                return 1;
            }));
            dispatcher.register(ClientCommands.literal("litesave")
                .then(ClientCommands.argument("name", StringArgumentType.word())
                .then(ClientCommands.argument("x1", IntegerArgumentType.integer())
                .then(ClientCommands.argument("y1", IntegerArgumentType.integer())
                .then(ClientCommands.argument("z1", IntegerArgumentType.integer())
                .then(ClientCommands.argument("x2", IntegerArgumentType.integer())
                .then(ClientCommands.argument("y2", IntegerArgumentType.integer())
                .then(ClientCommands.argument("z2", IntegerArgumentType.integer())
                .executes(context -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) return 0;
                    String name = StringArgumentType.getString(context, "name");
                    boolean saved = SchematicManager.saveRegion(
                        mc.level, name,
                        IntegerArgumentType.getInteger(context, "x1"),
                        IntegerArgumentType.getInteger(context, "y1"),
                        IntegerArgumentType.getInteger(context, "z1"),
                        IntegerArgumentType.getInteger(context, "x2"),
                        IntegerArgumentType.getInteger(context, "y2"),
                        IntegerArgumentType.getInteger(context, "z2"));
                    String message = saved ? "Schematic saved." : "Save failed: invalid name or area too large.";
                    if (mc.player != null) mc.player.sendSystemMessage(Component.literal(SchematicManager.smallCaps(message)));
                    return saved ? 1 : 0;
                })))))))));
            dispatcher.register(ClientCommands.literal("liteprint")
                .executes(context -> SchematicManager.print(false))
                .then(ClientCommands.literal("slow").executes(context -> SchematicManager.print(true))));
        });
    }
}
