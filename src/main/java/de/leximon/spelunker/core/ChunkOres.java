package de.leximon.spelunker.core;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;

import java.util.HashMap;
import java.util.Map;

public class ChunkOres extends HashMap<Vec3i, SpelunkerConfig.ChunkBlockConfig> {

    public static final ChunkOres EMPTY = new ChunkOres(Vec3i.ZERO);

    private final Vec3i pos;
    private boolean remapped = false; // true: block coordinates; false: local coordinates
    private int bottomSectionCord; // only available if remapped is true

    public ChunkOres(Vec3i pos) {
        this.pos = pos;
    }

    public Vec3i getPos() {
        return this.pos;
    }

    /**
     * adds or removes a block according to whether it is an ore block
     * @param pos the local coordinate in the chunk
     * @param conf the config
     */
    public void processConfig(Vec3i pos, SpelunkerConfig.ChunkBlockConfig conf, boolean localPos) {
        if(this.remapped && localPos)
            pos = toBlockCoord(pos, this.pos, this.bottomSectionCord);
        else if(!this.remapped && !localPos)
            pos = toLocalCoord(pos);
        if(conf == null)
            remove(pos);
        else put(pos, conf);
    }

    /**
     * remaps all local coordinates to block coordinates
     * @param bottomSectionCord the bottom section cord of the world
     * @return this
     */
    public ChunkOres remapToBlockCoordinates(int bottomSectionCord) {
        this.remapped = true;
        this.bottomSectionCord = bottomSectionCord;
        HashMap<Vec3i, SpelunkerConfig.ChunkBlockConfig> clone = new HashMap<>(this);
        clear();
        for (Map.Entry<Vec3i, SpelunkerConfig.ChunkBlockConfig> pair : clone.entrySet())
            put(toBlockCoord(pair.getKey(), this.pos, bottomSectionCord), pair.getValue());
        return this;
    }

    public static Vec3i toLocalCoord(Vec3i blockPos) {
        return new Vec3i(
                ChunkSectionPos.getLocalCoord(blockPos.getX()),
                ChunkSectionPos.getLocalCoord(blockPos.getY()),
                ChunkSectionPos.getLocalCoord(blockPos.getZ())
        );
    }

    public static Vec3i toBlockCoord(Vec3i localPos, Vec3i sectionPos, int bottomSectionCord) {
        return new Vec3i(
                ChunkSectionPos.getBlockCoord(sectionPos.getX()) + localPos.getX(),
                ChunkSectionPos.getBlockCoord(sectionPos.getY() + bottomSectionCord) + localPos.getY(),
                ChunkSectionPos.getBlockCoord(sectionPos.getZ()) + localPos.getZ()
        );
    }
}
