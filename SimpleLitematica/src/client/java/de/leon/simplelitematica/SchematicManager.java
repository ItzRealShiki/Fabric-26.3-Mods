package de.leon.simplelitematica;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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
    private static boolean missingBlocksEnabled = false;

    private SchematicManager() {}

    public static String smallCaps(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String normal = "abcdefghijklmnopqrstuvwxyz";
        String small = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
        StringBuilder result = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            int index = normal.indexOf(c);
            result.append(index >= 0 ? small.charAt(index) : c);
        }
        return result.toString();
    }

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
            return stream.filter(path -> { String n = path.getFileName().toString().toLowerCase(Locale.ROOT); return n.endsWith(".slite") || n.endsWith(".litematic") || n.endsWith(".schem") || n.endsWith(".nbt"); })
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    public static boolean load(Path path) {
        try {
            String file = path.getFileName().toString().toLowerCase(Locale.ROOT);
            SchematicData data;
            if (file.endsWith(".slite")) {
                data = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), SchematicData.class);
                if (data == null || data.formatVersion != 1 || data.blocks == null) return false;
            } else if (file.endsWith(".litematic") || file.endsWith(".schem") || file.endsWith(".nbt")) {
                data = NbtSchematicImporter.read(path);
            } else return false;
            active = data;
            offsetX = offsetY = offsetZ = 0;
            rotation = 0;
            visibleLayer = -1;
            missingBlocksEnabled = false;
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static boolean delete(Path path) {
        try {
            if (active != null && path.getFileName().toString().replaceFirst("\\.[^.]+$", "").equalsIgnoreCase(active.name)) unload();
            return Files.deleteIfExists(path);
        } catch (IOException exception) {
            return false;
        }
    }

    public static SchematicData active() { return active; }
    public static int visibleLayer() { return visibleLayer; }
    public static void cycleLayer() {
        if (active == null || active.sizeY <= 0) return;
        if (visibleLayer < 0) visibleLayer = 0;
        else {
            visibleLayer++;
            if (visibleLayer >= active.sizeY) visibleLayer = -1;
        }
    }
    public static void toggleLayerMode() {
        if (active == null || active.sizeY <= 0) return;
        visibleLayer = visibleLayer < 0 ? 0 : -1;
    }
    public static boolean layerMode() { return visibleLayer >= 0; }
    public static boolean missingBlocksEnabled() { return missingBlocksEnabled; }
    public static void toggleMissingBlocks() { missingBlocksEnabled = !missingBlocksEnabled; }
    public static int missingBlockCount(Level level, BlockPos origin) {
        if (active == null || level == null) return 0;
        int missing = 0;
        for (SchematicData.BlockEntry entry : active.blocks) {
            if (visibleLayer >= 0 && entry.y != visibleLayer) continue;
            BlockPos target = worldPosition(entry, origin);
            if (!matches(level.getBlockState(target), entry)) missing++;
        }
        return missing;
    }
    public static List<BlockPos> missingPositionsBatch(Level level, BlockPos origin, int start, int scanCount, int resultLimit) {
        if (active == null || level == null || scanCount <= 0 || resultLimit <= 0) return List.of();
        List<BlockPos> result = new ArrayList<>(Math.min(resultLimit, 512));
        int end = Math.min(active.blocks.size(), start + scanCount);
        for (int i = Math.max(0, start); i < end; i++) {
            SchematicData.BlockEntry entry = active.blocks.get(i);
            if (visibleLayer >= 0 && entry.y != visibleLayer) continue;
            BlockPos target = worldPosition(entry, origin);
            if (target.distSqr(origin) > 128.0 * 128.0) continue;
            if (!matches(level.getBlockState(target), entry)) {
                result.add(target);
                if (result.size() >= resultLimit) break;
            }
        }
        return result;
    }
    public static List<BlockPos> previewPositions(BlockPos origin, int resultLimit, double radius) {
        if (active == null || origin == null || resultLimit <= 0) return List.of();
        List<BlockPos> result = new ArrayList<>(Math.min(resultLimit, 1024));
        double radiusSquared = radius * radius;
        for (SchematicData.BlockEntry entry : active.blocks) {
            if (visibleLayer >= 0 && entry.y != visibleLayer) continue;
            BlockPos target = worldPosition(entry, origin);
            if (target.distSqr(origin) > radiusSquared) continue;
            result.add(target);
            if (result.size() >= resultLimit) break;
        }
        return result;
    }

    public record PreviewBlock(BlockPos position, BlockState state) {}

    public static List<PreviewBlock> previewBlocks(BlockPos origin, int resultLimit, double radius) {
        if (active == null || origin == null || resultLimit <= 0) return List.of();
        List<PreviewBlock> result = new ArrayList<>(Math.min(resultLimit, 1024));
        double radiusSquared = radius * radius;
        for (SchematicData.BlockEntry entry : active.blocks) {
            if (visibleLayer >= 0 && entry.y != visibleLayer) continue;
            BlockPos target = worldPosition(entry, origin);
            if (target.distSqr(origin) > radiusSquared) continue;
            BlockState state = blockState(entry);
            if (!state.isAir()) result.add(new PreviewBlock(target, state));
            if (result.size() >= resultLimit) break;
        }
        return result;
    }

    public static BlockState blockState(SchematicData.BlockEntry entry) {
        if (entry == null || entry.block == null) return Blocks.AIR.defaultBlockState();
        Block block = BuiltInRegistries.BLOCK.get(Identifier.parse(entry.block))
            .map(reference -> reference.value()).orElse(Blocks.AIR);
        BlockState state = block.defaultBlockState();
        String encoded = entry.blockState;
        if (encoded == null || encoded.isBlank()) return state;
        int open = encoded.indexOf('['), close = encoded.lastIndexOf(']');
        if (open < 0 || close <= open) return state;
        for (String pair : encoded.substring(open + 1, close).split(",")) {
            int equals = pair.indexOf('=');
            if (equals <= 0 || equals == pair.length() - 1) continue;
            Property<?> property = block.getStateDefinition().getProperty(pair.substring(0, equals).trim());
            if (property != null) state = applyProperty(state, property, pair.substring(equals + 1).trim());
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }

    private static String encodeState(BlockState state, String id) {
        StringBuilder encoded = new StringBuilder(id).append('[');
        boolean first = true;
        for (Property<?> property : state.getProperties()) {
            if (!first) encoded.append(',');
            first = false;
            encoded.append(property.getName()).append('=').append(propertyValue(state, property));
        }
        return encoded.append(']').toString();
    }

    private static <T extends Comparable<T>> String propertyValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static boolean matches(BlockState current, SchematicData.BlockEntry expected) {
        return current.equals(blockState(expected));
    }

    public static int blockEntryCount() { return active == null ? 0 : active.blocks.size(); }
    public static int totalVisibleBlocks() {
        if (active == null) return 0;
        if (visibleLayer < 0) return active.blocks.size();
        int count = 0;
        for (SchematicData.BlockEntry entry : active.blocks) if (entry.y == visibleLayer) count++;
        return count;
    }
    public static int offsetX() { return offsetX; }
    public static int offsetY() { return offsetY; }
    public static int offsetZ() { return offsetZ; }
    public static int rotation() { return rotation; }
    public static boolean unload() {
        boolean hadSchematic = active != null;
        active = null;
        visibleLayer = -1;
        offsetX = offsetY = offsetZ = 0;
        rotation = 0;
        missingBlocksEnabled = false;
        return hadSchematic;
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
                    Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    data.blocks.add(new SchematicData.BlockEntry(x-minX, y-minY, z-minZ,
                        id.toString(), encodeState(state, id.toString())));
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
            mc.player.sendSystemMessage(Component.literal(smallCaps("Liteprint is only available in creative mode.")));
            return 0;
        }
        if (active == null || active.blocks.isEmpty()) {
            mc.player.sendSystemMessage(Component.literal(smallCaps("No schematic loaded. Open the menu with Shift + M.")));
            return 0;
        }
        if (!mc.hasSingleplayerServer()) {
            mc.player.sendSystemMessage(Component.literal(smallCaps("Printing is currently available in singleplayer worlds only.")));
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
                BlockState state = blockState(entry);
                if (state.isAir()) continue;
                BlockPos target = worldPosition(entry, origin);
                serverLevel.setBlock(target, state, 3);
            }
        };
        if (slow) {
            mc.player.sendSystemMessage(Component.literal(smallCaps("Slow print mode is not implemented yet.")));
            return 1;
        }
        int placedCount = 0;
        for (SchematicData.BlockEntry entry : blocks) if (visibleLayer < 0 || entry.y == visibleLayer) placedCount++;
        server.execute(placement);
        mc.player.sendSystemMessage(Component.literal(smallCaps("Schematic placed locally: " + placedCount + " blocks.")));
        return 1;
    }
}
