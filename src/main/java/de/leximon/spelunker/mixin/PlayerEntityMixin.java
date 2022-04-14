package de.leximon.spelunker.mixin;

import de.leximon.spelunker.SpelunkerMod;
import de.leximon.spelunker.SpelunkerModClient;
import de.leximon.spelunker.core.IPlayerEntity;
import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkerEffectManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements IPlayerEntity {

    private long lastChunkPos = ChunkSectionPos.from(this).asLong();
    private long[] chunkCache = new long[0];

    private boolean forceOreChunkUpdate = true;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void moveEndInject(CallbackInfo ci) {
        if (!hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER)) {
            forceOreChunkUpdate = true;
            return;
        }
        if (world.isClient()) {
            if(notSingleplayer())
                return;
        } else if(!SpelunkerConfig.serverValidating)
                return;

        ChunkSectionPos currentChunkPos = ChunkSectionPos.from(this);
        long chunkLong;
        // update if player crosses chunk border
        if (lastChunkPos != (chunkLong = currentChunkPos.asLong()) || forceOreChunkUpdate) {
            forceOreChunkUpdate = false;
            List<ChunkSectionPos> chunks = ChunkSectionPos.stream(currentChunkPos, SpelunkerConfig.chunkRadius).toList();
            final List<ChunkSectionPos> newChunks = chunks.stream().filter(chunkSectionPos -> { // filter for new chunks
                for (long pos : chunkCache)
                    if (chunkSectionPos.asLong() == pos)
                        return false;
                return true;
            }).toList();

            final long[] oldChunks = Arrays.stream(chunkCache).filter(value -> { // filter for old chunks
                for (ChunkSectionPos sectionPos : chunks)
                    if (sectionPos.asLong() == value)
                        return false;
                return true;
            }).toArray();

            Long2ObjectMap<List<Pair<BlockPos, Block>>> newList = SpelunkerEffectManager.getNewBlocksFromChunks(world, newChunks);
            if (newList.size() != 0 || oldChunks.length != 0) {
                if (world.isClient()) {
                    SpelunkerModClient.spelunkerEffectRenderer.updateDirtyBlocks(newList, oldChunks, Collections.emptyList());
                } else ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, SpelunkerMod.PACKET_ORE_CHUNKS, SpelunkerEffectManager.writePacket(newList, oldChunks, Collections.emptyList()));
            }

            chunkCache = chunks.stream().mapToLong(ChunkSectionPos::asLong).toArray(); // update old chunk cache
        }
        lastChunkPos = chunkLong;
    }

    @Environment(EnvType.CLIENT) // Prevent ClassNotFoundError
    private static boolean notSingleplayer() {
        return !net.minecraft.client.MinecraftClient.getInstance().isInSingleplayer() && SpelunkerConfig.serverValidating;
    }

    @Override
    public long[] getChunkCache() {
        return chunkCache;
    }
}
