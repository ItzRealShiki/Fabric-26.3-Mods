package de.leon.simplelitematica;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SchematicManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIRECTORY = Minecraft.getInstance().gameDirectory.toPath()
        .resolve("simplelitematica").resolve("schematics");
    private static SchematicData active;
    private static int offsetX;
    private static int offsetY;
    private static int offsetZ;
    private static int rotation;
    private static int visibleLayer = -1;

    private SchematicManager() {}

    public static void ensureDirectory() {
        try { Files.createDirectories(DIRECTORY); }
        catch (IOException exception) { exception.printStackTrace(); }
    }

    public static Path directory() {
        ensureDirectory();
        return DIRECTORY;
    }

    public static List<Path> files() {
        ensureDirectory();
        try (var stream = Files.list(DIRECTORY)) {
            return stream.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".slite"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    public static boolean load(Path path) {
        try {
            SchematicData data = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), SchematicData.class);
            if (data == null || data.formatVersion != 1 || data.blocks == null) return false;
            active = data;
            offsetX = offsetY = offsetZ = 0;
            rotation = 0;
            visibleLayer = -1;
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static boolean delete(Path path) {
        try {
            if (active != null && path.getFileName().toString().equalsIgnoreCase(active.name + ".slite")) active = null;
            return Files.deleteIfExists(path);
        } catch (IOException exception) {
            return false;
        }
    }

    public static SchematicData active() { return active; }
    public static int visibleLayer() { return visibleLayer; }
    public static void cycleLayer() {
        if (active == null || active.sizeY <= 0) return;
        visibleLayer++;
        if (visibleLayer >= active.sizeY) visibleLayer = -1;
    }
    public static void rotate() { rotation = (rotation + 1) & 3; }
    public static void move(int x, int y, int z) { offsetX += x; offsetY += y; offsetZ += z; }

    public static BlockPos worldPosition(SchematicData.BlockEntry block, BlockPos origin) {
        int x = block.x, z = block.z;
        switch (rotation) {
            case 1 -> { int old = x; x = -z; z = old; }
            case 2 -> { x = -x; z = -z; }
            case 3 -> { int old = x; x = z; z = -old; }
            default -> {}
        }
        return origin.offset(x + offsetX, block.y + offsetY, z + offsetZ);
    }

    public static boolean saveRegion(Level level, String name, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,48}")) return false;
        int minX = Math.min(x1, x2), minY = Math.min(y1, y2), minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2), maxY = Math.max(y1, y2), maxZ = Math.max(z1, z2);
        long volume = (long)(maxX-minX+1) * (maxY-minY+1) * (maxZ-minZ+1);
        if (volume > 100_000L) return false;
        SchematicData data = new SchematicData();
        data.name = name;
        data.sizeX = maxX-minX+1;
        data.sizeY = maxY-minY+1;
        data.sizeZ = maxZ-minZ+1;
        for (int y=minY; y<=maxY; y++) {
            for (int z=minZ; z<=maxZ; z++) {
                for (int x=minX; x<=maxX; x++) {
                    BlockState state = level.getBlockState(new BlockPos(x,y,z));
                    if (state.isAir()) continue;
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    data.blocks.add(new SchematicData.BlockEntry(x-minX, y-minY, z-minZ, id.toString()));
                }
            }
        }
        try {
            ensureDirectory();
            Files.writeString(DIRECTORY.resolve(name + ".slite"), GSON.toJson(data), StandardCharsets.UTF_8);
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static int print(boolean slow) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return 0;
        if (!mc.player.getAbilities().instabuild) {
            mc.player.displayClientMessage(Component.literal("§c/liteprint ist nur im Kreativmodus verfügbar."), false);
            return 0;
        }
        if (active == null || active.blocks.isEmpty()) {
            mc.player.displayClientMessage(Component.literal("§cKeine Schematic geladen. Öffne /slitematica."), false);
            return 0;
        }
        if (!mc.hasSingleplayerServer()) {
            mc.player.displayClientMessage(Component.literal("§eDrucken ist in dieser ersten Version nur in Einzelspielerwelten verfügbar."), false);
            return 0;
        }
        var server = mc.getSingleplayerServer();
        if (server == null) return 0;
        var serverLevel = server.getLevel(mc.level.dimension());
        if (serverLevel == null) return 0;
        BlockPos origin = mc.player.blockPosition();
        List<SchematicData.BlockEntry> blocks = new ArrayList<>(active.blocks);
        blocks.sort(Comparator.comparingInt((SchematicData.BlockEntry b) -> b.y)
            .thenComparingInt(b -> b.z).thenComparingInt(b -> b.x));
        Runnable placement = () -> {
            for (SchematicData.BlockEntry entry : blocks) {
                if (visibleLayer >= 0 && entry.y != visibleLayer) continue;
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.block));
                if (block == Blocks.AIR) continue;
                BlockPos target = worldPosition(entry, origin);
                serverLevel.setBlock(target, block.defaultBlockState(), 3);
            }
        };
        if (slow) {
            mc.player.displayClientMessage(Component.literal("§eLangsamer Druck: wird vorbereitet. (Sequenzmodus folgt in einem Update.)"), false);
            return 1;
        }
        server.execute(placement);
        mc.player.displayClientMessage(Component.literal("§aSchematic lokal gesetzt: " + blocks.size() + " Blöcke."), false);
        return 1;
    }
}
