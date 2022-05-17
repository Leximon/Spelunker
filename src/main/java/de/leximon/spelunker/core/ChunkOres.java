package de.leximon.spelunker.core;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;

import java.util.HashMap;
import java.util.Map;

public class ChunkOres extends HashMap<Vec3i, Block> {

    public static final ChunkOres EMPTY = new ChunkOres(Vec3i.ZERO);

    private final Vec3i pos;
    private boolean remapped = false;
    private int bottomSectionCord;

    public ChunkOres(Vec3i pos) {
        this.pos = pos;
    }

    public Vec3i getPos() {
        return pos;
    }

    /**
     * adds or removes a block according to whether it is an ore block
     * @param pos the world coordinate
     * @param block the block
     */
    public void processBlock(BlockPos pos, Block block) {
        if(remapped) {
            processBlock((Vec3i) pos, block);
        } else {
            processBlock(toLocalCoord(pos), block);
        }
    }

    /**
     * adds or removes a block according to whether it is an ore block
     * @param pos the local coordinate in the chunk
     * @param block the block
     */
    public void processBlock(Vec3i pos, Block block) {
        if(remapped)
            pos = toBlockCoord(pos, this.pos, bottomSectionCord);
        if(SpelunkerConfig.isOreBlock(block))
            put(pos, block);
        else
            remove(pos);
    }

    /**
     * remaps all local coordinates to world coordinates
     * @param bottomSectionCord the bottom section cord of the world
     * @return this
     */
    public ChunkOres remapToWorldCoordinates(int bottomSectionCord) {
        this.remapped = true;
        this.bottomSectionCord = bottomSectionCord;
        HashMap<Vec3i, Block> clone = new HashMap<>(this);
        clear();
        for (Map.Entry<Vec3i, Block> pair : clone.entrySet()) {
            Vec3i p = pair.getKey();

            put(toBlockCoord(p, pos, bottomSectionCord), pair.getValue());
        }


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
