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
        public int layer;

        public BlockEntry() {}
        public BlockEntry(int x, int y, int z, String block) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.layer = y;
        }
    }
}
