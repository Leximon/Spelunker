package de.leximon.spelunker.core;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface IWorld {

    void spelunkerUpdateBlock(BlockPos pos, BlockState oldBlock, BlockState newBlock);

}
