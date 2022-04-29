package de.leximon.spelunker.mixin;

import de.leximon.spelunker.SpelunkerModClient;
import de.leximon.spelunker.core.IPlayerEntity;
import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkerEffectManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ClientChunkManager.class)
public abstract class ClientChunkManagerMixin extends ChunkManager {

    @Shadow @Final ClientWorld world;

    @Inject(method = "loadChunkFromPacket", at = @At(value = "RETURN", ordinal = 1))
    public void loadChunkFromPacket(int x, int z, PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfoReturnable<@Nullable WorldChunk> cir) {
        if(SpelunkerConfig.serverValidating)
            return;
        WorldChunk chunk = cir.getReturnValue();
        long[] chunkCache = ((IPlayerEntity) MinecraftClient.getInstance().player).getChunkCache();
        for (ChunkSection section : chunk.getSectionArray()) {
            for (long l : chunkCache) {
                ChunkSectionPos pos = ChunkSectionPos.from(chunk.getPos().x, section.getYOffset(), chunk.getPos().z);
                if(pos.asLong() == l) {
                    SpelunkerModClient.spelunkerEffectRenderer.updateDirtyBlocks(
                            SpelunkerEffectManager.getNewBlocksFromChunks(world, List.of(pos)),
                            new long[] {},
                            Collections.emptyList()
                    );
                }
            }
        }
    }
}
