package de.leximon.spelunker.mixin;

import de.leximon.spelunker.core.IWorld;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "onBlockChanged", at = @At("HEAD"))
    public void onBlockChangedInject(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        ((IWorld) this).spelunkerUpdateBlock(pos, oldBlock, newBlock);
    }

}
