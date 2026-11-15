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
        if (get(tag, "Regions") != null) readLitematic(tag, data);
        else if (get(tag, "BlockData") != null && get(tag, "Palette") instanceof Map<?, ?>) readSponge(tag, data);
        else if (get(tag, "blocks") instanceof List<?> && get(tag, "palette") instanceof List<?>) readStructure(tag, data);
        else throw new IOException("Unsupported NBT schematic layout");
        if (data.blocks.isEmpty()) throw new IOException("No blocks were found in this schematic");
        return data;
    }

    private record AbsoluteBlock(int x, int y, int z, String block, String blockState) {}

    private static void readLitematic(Map<String, Object> root, SchematicData out) throws IOException {
        Map<String, Object> regions = map(get(root, "Regions"));
        List<AbsoluteBlock> absolute = new ArrayList<>();
        int minX=Integer.MAX_VALUE, minY=Integer.MAX_VALUE, minZ=Integer.MAX_VALUE;
        int maxX=Integer.MIN_VALUE, maxY=Integer.MIN_VALUE, maxZ=Integer.MIN_VALUE;
        for (Object regionValue : regions.values()) {
            Map<String, Object> region = map(regionValue);
            int[] size = coordinates(get(region, "Size"));
            if (size.length < 3) continue;
            int sx=Math.abs(size[0]), sy=Math.abs(size[1]), sz=Math.abs(size[2]);
            if (sx==0 || sy==0 || sz==0) continue;
            List<?> palette=list(get(region,"BlockStatePalette"));
            long[] states=longs(get(region,"BlockStates"));
            if(palette.isEmpty() || states.length==0) continue;
            int bits=Math.max(2,32-Integer.numberOfLeadingZeros(palette.size()-1));
            long mask=(1L<<bits)-1L;
            int rx=0,ry=0,rz=0;
            int[] pos=coordinates(get(region,"Position"));
            if(pos.length>=3){rx=pos[0];ry=pos[1];rz=pos[2];}
            int endX=rx+(size[0]<0 ? -(sx-1) : sx-1);
            int endY=ry+(size[1]<0 ? -(sy-1) : sy-1);
            int endZ=rz+(size[2]<0 ? -(sz-1) : sz-1);
            minX=Math.min(minX,Math.min(rx,endX)); minY=Math.min(minY,Math.min(ry,endY)); minZ=Math.min(minZ,Math.min(rz,endZ));
            maxX=Math.max(maxX,Math.max(rx,endX)+1); maxY=Math.max(maxY,Math.max(ry,endY)+1); maxZ=Math.max(maxZ,Math.max(rz,endZ)+1);
            long volume=(long)sx*sy*sz;
            if(volume>2_000_000L || absolute.size()+volume>2_000_000L) throw new IOException("Schematic is too large");
            for(int index=0;index<volume;index++){
                int paletteIndex=packed(states,index,bits,mask);
                if(paletteIndex<0 || paletteIndex>=palette.size()) continue;
                String blockState=blockState(map(palette.get(paletteIndex)));
                String block=blockId(blockState);
                if(isAir(block)) continue;
                int x=index%sx, z=(index/sx)%sz, y=index/(sx*sz);

                // Litematica's signed Size points from the region anchor in the
                // declared direction. Packed indices still use folded dimensions
                // (x fastest, then z, then y), so a negative axis reverses its
                // coordinate relative to Position. Normalize to the global minimum
                // only after applying that signed direction.
                int localX = size[0] < 0 ? -x : x;
                int localY = size[1] < 0 ? -y : y;
                int localZ = size[2] < 0 ? -z : z;
                absolute.add(new AbsoluteBlock(rx + localX, ry + localY,
                    rz + localZ, block, blockState));
            }
        }
        if(absolute.isEmpty()) return;
        out.sizeX=maxX-minX; out.sizeY=maxY-minY; out.sizeZ=maxZ-minZ;
        for(AbsoluteBlock block:absolute) out.blocks.add(new SchematicData.BlockEntry(
            block.x-minX,block.y-minY,block.z-minZ,block.block,block.blockState));
    }

    private static void readSponge(Map<String, Object> root, SchematicData out) throws IOException {
        int width = number(get(root, "Width")), height = number(get(root, "Height")), length = number(get(root, "Length"));
        if (width <= 0 || height <= 0 || length <= 0 || (long)width*height*length > 2_000_000L)
            throw new IOException("Invalid or oversized Sponge schematic dimensions");
        Map<String, Object> paletteTag = map(get(root, "Palette"));
        Map<Integer, String> palette = new LinkedHashMap<>();
        for (var entry : paletteTag.entrySet()) palette.put(number(entry.getValue()), entry.getKey());
        byte[] encoded = bytes(get(root, "BlockData"));
        int[] cursor = {0};
        for (int i=0; i<(long)width*height*length && cursor[0]<encoded.length; i++) {
            int state = readVarInt(encoded, cursor);
            String blockState = palette.get(state);
            if (blockState == null) continue;
            blockState = normalizeState(blockState);
            String block = blockId(blockState);
            if (isAir(block)) continue;
            int x=i%width, z=(i/width)%length, y=i/(width*length);
            out.blocks.add(new SchematicData.BlockEntry(x,y,z,block,blockState));
        }
        out.sizeX=width; out.sizeY=height; out.sizeZ=length;
    }

    private static void readStructure(Map<String, Object> root, SchematicData out) throws IOException {
        List<?> paletteTags = list(get(root, "palette"));
        List<String> palette = new ArrayList<>();
        for (Object item : paletteTags) palette.add(blockState(map(item)));
        int[] size = coordinates(get(root, "size"));
        if (size.length >= 3) { out.sizeX=size[0]; out.sizeY=size[1]; out.sizeZ=size[2]; }
        for (Object item : list(get(root, "blocks"))) {
            Map<String, Object> blockTag = map(item);
            int[] pos = coordinates(get(blockTag, "pos"));
            int state = number(get(blockTag, "state"));
            if (pos.length < 3 || state < 0 || state >= palette.size()) continue;
            String blockState = palette.get(state);
            String block = blockId(blockState);
            if (isAir(block)) continue;
            out.blocks.add(new SchematicData.BlockEntry(pos[0],pos[1],pos[2],block,blockState));
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
                for(int i=0;i<n;i++) {
                    if (child == 10) {
                        // Some schematic exporters incorrectly include a full unnamed
                        // TAG_Compound header for each compound-list element. Accept
                        // that wrapper as well as standard NBT list encoding.
                        in.mark(3);
                        int first = in.readUnsignedByte();
                        if (first == 10 && in.readUnsignedShort() == 0) {
                            values.add(readPayload(in,10,depth+1));
                        } else {
                            in.reset();
                            values.add(readPayload(in,child,depth+1));
                        }
                    } else {
                        values.add(readPayload(in,child,depth+1));
                    }
                }
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

    private static Object get(Map<String,Object> map, String key) { Object value=map.get(key); if(value!=null)return value; for(var e:map.entrySet()) if(e.getKey().equalsIgnoreCase(key)) return e.getValue(); return null; }
    private static int checkedLength(int n) throws IOException {
        if (n < 0 || n > 8_000_000) throw new IOException("NBT array/list is too large");
        return n;
    }
    private static String readUtf(DataInputStream in) throws IOException { return in.readUTF(); }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object o) { return o instanceof Map<?,?> m ? (Map<String,Object>)m : Map.of(); }
    private static List<?> list(Object o) { return o instanceof List<?> l ? l : List.of(); }
    private static int[] coordinates(Object value) {
        if (value instanceof int[] array) return array;
        if (value instanceof Map<?, ?> map) {
            Object x = map.get("x"), y = map.get("y"), z = map.get("z");
            if (x instanceof Number nx && y instanceof Number ny && z instanceof Number nz) {
                return new int[]{nx.intValue(), ny.intValue(), nz.intValue()};
            }
        }
        return new int[0];
    }
    private static int[] ints(Object o) { return o instanceof int[] a ? a : new int[0]; }
    private static long[] longs(Object o) { return o instanceof long[] a ? a : new long[0]; }
    private static byte[] bytes(Object o) { return o instanceof byte[] a ? a : new byte[0]; }
    private static int number(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private static String blockState(Map<String,Object> state) {
        String id = normalizeId(String.valueOf(state.getOrDefault("Name","minecraft:air")));
        Object raw = get(state, "Properties");
        if (raw instanceof Map<?, ?> properties && !properties.isEmpty()) {
            StringBuilder result = new StringBuilder(id).append('[');
            boolean first = true;
            for (var entry : properties.entrySet()) {
                if (!first) result.append(',');
                first = false;
                result.append(entry.getKey()).append('=').append(entry.getValue());
            }
            return result.append(']').toString();
        }
        return id;
    }
    private static String normalizeState(String state) {
        int open = state.indexOf('[');
        String id = normalizeId(open < 0 ? state : state.substring(0, open));
        return open < 0 ? id : id + state.substring(open);
    }
    private static String normalizeId(String id) { return id.contains(":") ? id : "minecraft:"+id; }
    private static String blockId(String state) { int properties=state.indexOf('['); return properties<0 ? state : state.substring(0,properties); }
    private static boolean isAir(String id) { return id.equals("minecraft:air") || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air"); }
    private static String stripExtension(String name) { int i=name.lastIndexOf('.'); return i>0 ? name.substring(0,i) : name; }
}
