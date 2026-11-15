package de.leon.simplelitematica;

import java.util.ArrayList;
import java.util.List;

public final class SchematicData {
    public int formatVersion = 1;
    public String name = "Unnamed";
    public int sizeX;
    public int sizeY;
    public int sizeZ;
    public final List<BlockEntry> blocks = new ArrayList<>();

    public static final class BlockEntry {
        public int x;
        public int y;
        public int z;
        public String block;
        public String blockState;
        public int layer;

        public BlockEntry() {}
        public BlockEntry(int x, int y, int z, String block) {
            this(x, y, z, block, block);
        }
        public BlockEntry(int x, int y, int z, String block, String blockState) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.blockState = blockState == null || blockState.isBlank() ? block : blockState;
            this.layer = y;
        }
    }
}
