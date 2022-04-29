package de.leximon.spelunker.core;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public interface IWorld {

    void spelunkerUpdateBlock(BlockPos pos, BlockState oldBlock, BlockState newBlock);

    void updateClientChunks();
    void updateServerChunks();

}
