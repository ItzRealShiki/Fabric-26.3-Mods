package de.leon.simplelitematica;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class SimpleLitematicaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SchematicManager.ensureDirectory();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("slitematica")
                .executes(context -> {
                    Minecraft.getInstance().setScreen(new SchematicScreen());
                    return 1;
                }));

            dispatcher.register(ClientCommandManager.literal("litesave")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                .then(ClientCommandManager.argument("x1", IntegerArgumentType.integer())
                .then(ClientCommandManager.argument("y1", IntegerArgumentType.integer())
                .then(ClientCommandManager.argument("z1", IntegerArgumentType.integer())
                .then(ClientCommandManager.argument("x2", IntegerArgumentType.integer())
                .then(ClientCommandManager.argument("y2", IntegerArgumentType.integer())
                .then(ClientCommandManager.argument("z2", IntegerArgumentType.integer())
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
                        IntegerArgumentType.getInteger(context, "z2")
                    );
                    if (mc.player != null) mc.player.displayClientMessage(
                        Component.literal(saved ? "§aSimpleLitematica: Schematic gespeichert." :
                            "§cSimpleLitematica: Speichern fehlgeschlagen (Bereich zu groß oder ungültiger Name)."), false);
                    return saved ? 1 : 0;
                })))))))));

            dispatcher.register(ClientCommandManager.literal("liteprint")
                .executes(context -> SchematicManager.print(false))
                .then(ClientCommandManager.literal("slow")
                    .executes(context -> SchematicManager.print(true))));
        });
    }
}
