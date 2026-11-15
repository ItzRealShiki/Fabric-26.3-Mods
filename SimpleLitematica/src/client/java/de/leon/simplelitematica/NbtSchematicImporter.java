package de.leon.simplelitematica;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Small dependency-free reader for common Litematica, Sponge Schematic and
 * vanilla Structure NBT layouts. Block properties are intentionally reduced
 * to their block identifier in this first importer.
 */
final class NbtSchematicImporter {
    private NbtSchematicImporter() {}

    static SchematicData read(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length < 3) throw new IOException("File is empty or truncated");
        if ((bytes[0] & 255) == 0x1f && (bytes[1] & 255) == 0x8b) {
            try (var in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                bytes = in.readAllBytes();
            }
        } else if ((bytes[0] & 255) == 0x78) {
            try (var in = new InflaterInputStream(new ByteArrayInputStream(bytes))) {
                bytes = in.readAllBytes();
            } catch (IOException ignored) {
                // Most structure files are uncompressed NBT.
            }
        }
        Object root;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int type = in.readUnsignedByte();
            if (type != 10) throw new IOException("NBT root must be a compound");
            readUtf(in);
            root = readPayload(in, 10, 0);
        }
        Map<String, Object> tag = map(root);
        SchematicData data = new SchematicData();
        data.name = stripExtension(path.getFileName().toString());
        if (tag.containsKey("Regions")) readLitematic(tag, data);
        else if (tag.containsKey("Blocks") && tag.containsKey("Palette")) readStructure(tag, data);
        else if (tag.containsKey("BlockData") && tag.containsKey("Palette")) readSponge(tag, data);
        else throw new IOException("Unsupported NBT schematic layout");
        if (data.blocks.isEmpty()) throw new IOException("No blocks were found in this schematic");
        return data;
    }

    private static void readLitematic(Map<String, Object> root, SchematicData out) throws IOException {
        Map<String, Object> regions = map(root.get("Regions"));
        int minX = 0, minY = 0, minZ = 0;
        int maxX = 0, maxY = 0, maxZ = 0;
        boolean first = true;
        for (Object regionValue : regions.values()) {
            Map<String, Object> region = map(regionValue);
            int[] size = ints(region.get("Size"));
            if (size.length < 3) continue;
            int sx = Math.abs(size[0]), sy = Math.abs(size[1]), sz = Math.abs(size[2]);
            if (sx == 0 || sy == 0 || sz == 0) continue;
            List<?> palette = list(region.get("BlockStatePalette"));
            long[] states = longs(region.get("BlockStates"));
            if (palette.isEmpty() || states.length == 0) continue;
            int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
            long mask = (1L << bits) - 1L;
            int rx = 0, ry = 0, rz = 0;
            int[] pos = ints(region.get("Position"));
            if (pos.length >= 3) { rx = pos[0]; ry = pos[1]; rz = pos[2]; }
            if (first) { minX=rx; minY=ry; minZ=rz; maxX=rx+sx; maxY=ry+sy; maxZ=rz+sz; first=false; }
            else { minX=Math.min(minX,rx); minY=Math.min(minY,ry); minZ=Math.min(minZ,rz);
                maxX=Math.max(maxX,rx+sx); maxY=Math.max(maxY,ry+sy); maxZ=Math.max(maxZ,rz+sz); }
            long volume = (long)sx * sy * sz;
            if (volume > 2_000_000L) throw new IOException("Schematic region is too large");
            for (int index=0; index<volume; index++) {
                int paletteIndex = packed(states, index, bits, mask);
                if (paletteIndex < 0 || paletteIndex >= palette.size()) continue;
                String block = blockName(map(palette.get(paletteIndex)));
                if (block.equals("minecraft:air") || block.equals("minecraft:cave_air") || block.equals("minecraft:void_air")) continue;
                int x = index % sx;
                int z = (index / sx) % sz;
                int y = index / (sx * sz);
                out.blocks.add(new SchematicData.BlockEntry(rx-minX+x, ry-minY+y, rz-minZ+z, block));
            }
        }
        if (!first) { out.sizeX=maxX-minX; out.sizeY=maxY-minY; out.sizeZ=maxZ-minZ; }
    }

    private static void readSponge(Map<String, Object> root, SchematicData out) throws IOException {
        int width = number(root.get("Width")), height = number(root.get("Height")), length = number(root.get("Length"));
        if (width <= 0 || height <= 0 || length <= 0 || (long)width*height*length > 2_000_000L)
            throw new IOException("Invalid or oversized Sponge schematic dimensions");
        Map<String, Object> paletteTag = map(root.get("Palette"));
        Map<Integer, String> palette = new LinkedHashMap<>();
        for (var entry : paletteTag.entrySet()) palette.put(number(entry.getValue()), entry.getKey());
        byte[] encoded = bytes(root.get("BlockData"));
        int[] cursor = {0};
        for (int i=0; i<(long)width*height*length && cursor[0]<encoded.length; i++) {
            int state = readVarInt(encoded, cursor);
            String block = palette.get(state);
            if (block == null || isAir(block)) continue;
            int x=i%width, z=(i/width)%length, y=i/(width*length);
            out.blocks.add(new SchematicData.BlockEntry(x,y,z,normalize(block)));
        }
        out.sizeX=width; out.sizeY=height; out.sizeZ=length;
    }

    private static void readStructure(Map<String, Object> root, SchematicData out) throws IOException {
        List<?> paletteTags = list(root.get("Palette"));
        List<String> palette = new ArrayList<>();
        for (Object item : paletteTags) palette.add(blockName(map(item)));
        int[] size = ints(root.get("size"));
        if (size.length >= 3) { out.sizeX=size[0]; out.sizeY=size[1]; out.sizeZ=size[2]; }
        for (Object item : list(root.get("Blocks"))) {
            Map<String, Object> blockTag = map(item);
            int[] pos = ints(blockTag.get("pos"));
            int state = number(blockTag.get("state"));
            if (pos.length < 3 || state < 0 || state >= palette.size()) continue;
            String block = palette.get(state);
            if (isAir(block)) continue;
            out.blocks.add(new SchematicData.BlockEntry(pos[0],pos[1],pos[2],block));
            out.sizeX=Math.max(out.sizeX,pos[0]+1);
            out.sizeY=Math.max(out.sizeY,pos[1]+1);
            out.sizeZ=Math.max(out.sizeZ,pos[2]+1);
        }
    }

    private static int packed(long[] data, int index, int bits, long mask) {
        long bit = (long)index * bits;
        int cell = (int)(bit >>> 6), offset = (int)(bit & 63);
        if (cell >= data.length) return -1;
        long value = data[cell] >>> offset;
        if (offset + bits > 64 && cell + 1 < data.length) value |= data[cell+1] << (64-offset);
        return (int)(value & mask);
    }

    private static int readVarInt(byte[] data, int[] cursor) throws IOException {
        int value=0, shift=0;
        while (cursor[0] < data.length && shift < 35) {
            int b=data[cursor[0]++] & 255;
            value |= (b & 127) << shift;
            if ((b & 128)==0) return value;
            shift += 7;
        }
        throw new IOException("Invalid Sponge block-data varint");
    }

    private static Object readPayload(DataInputStream in, int type, int depth) throws IOException {
        if (depth > 64) throw new IOException("NBT nesting is too deep");
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> { int n=checkedLength(in.readInt()); byte[] a=new byte[n]; in.readFully(a); yield a; }
            case 8 -> readUtf(in);
            case 9 -> {
                int child=in.readUnsignedByte(), n=checkedLength(in.readInt());
                List<Object> values=new ArrayList<>(Math.min(n,100_000));
                for(int i=0;i<n;i++) values.add(readPayload(in,child,depth+1));
                yield values;
            }
            case 10 -> {
                Map<String,Object> values=new LinkedHashMap<>();
                while(true) {
                    int child=in.readUnsignedByte();
                    if(child==0) break;
                    String name=readUtf(in);
                    values.put(name,readPayload(in,child,depth+1));
                }
                yield values;
            }
            case 11 -> { int n=checkedLength(in.readInt()); int[] a=new int[n]; for(int i=0;i<n;i++) a[i]=in.readInt(); yield a; }
            case 12 -> { int n=checkedLength(in.readInt()); long[] a=new long[n]; for(int i=0;i<n;i++) a[i]=in.readLong(); yield a; }
            default -> throw new IOException("Unknown NBT tag: "+type);
        };
    }

    private static int checkedLength(int n) throws IOException {
        if (n < 0 || n > 8_000_000) throw new IOException("NBT array/list is too large");
        return n;
    }
    private static String readUtf(DataInputStream in) throws IOException { return in.readUTF(); }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object o) { return o instanceof Map<?,?> m ? (Map<String,Object>)m : Map.of(); }
    private static List<?> list(Object o) { return o instanceof List<?> l ? l : List.of(); }
    private static int[] ints(Object o) { return o instanceof int[] a ? a : new int[0]; }
    private static long[] longs(Object o) { return o instanceof long[] a ? a : new long[0]; }
    private static byte[] bytes(Object o) { return o instanceof byte[] a ? a : new byte[0]; }
    private static int number(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private static String blockName(Map<String,Object> state) { return normalize(String.valueOf(state.getOrDefault("Name","minecraft:air"))); }
    private static String normalize(String id) { return id.contains(":") ? id : "minecraft:"+id; }
    private static boolean isAir(String id) { return id.equals("minecraft:air") || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air"); }
    private static String stripExtension(String name) { int i=name.lastIndexOf('.'); return i>0 ? name.substring(0,i) : name; }
}
