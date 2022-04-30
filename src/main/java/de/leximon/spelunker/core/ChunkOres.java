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
            processBlock(new Vec3i(pos.getX(), pos.getY(), pos.getZ()), block);
        } else {
            processBlock(new Vec3i(
                    ChunkSectionPos.getLocalCoord(pos.getX()),
                    ChunkSectionPos.getLocalCoord(pos.getY()),
                    ChunkSectionPos.getLocalCoord(pos.getZ())
            ), block);
        }
    }

    /**
     * adds or removes a block according to whether it is an ore block
     * @param pos the local coordinate in the chunk
     * @param block the block
     */
    public void processBlock(Vec3i pos, Block block) {
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
        remapped = true;
        HashMap<Vec3i, Block> clone = new HashMap<>(this);
        clear();
        for (Map.Entry<Vec3i, Block> pair : clone.entrySet()) {
            Vec3i p = pair.getKey();

            put(new Vec3i(
                    ChunkSectionPos.getBlockCoord(pos.getX()) + p.getX(),
                    ChunkSectionPos.getBlockCoord(pos.getY() + bottomSectionCord) + p.getY(),
                    ChunkSectionPos.getBlockCoord(pos.getZ()) + p.getZ()
            ), pair.getValue());
        }


        return this;
    }
}
