package de.leximon.spelunker.core;

import de.leximon.spelunker.mixin.ThreadedAnvilChunkStorageAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpelunkerEffectManager {

    public static Long2ObjectMap<List<Pair<BlockPos, Block>>> getNewBlocksFromChunks(World world, List<ChunkSectionPos> chunkSections) {
        Long2ObjectMap<List<Pair<BlockPos, Block>>> dirty = new Long2ObjectArrayMap<>();
        for (ChunkSectionPos section : chunkSections) {
            Chunk chunk;
            if (world instanceof ServerWorld sw) {
                ChunkHolder chunkHolder = ((ThreadedAnvilChunkStorageAccessor) sw.getChunkManager().threadedAnvilChunkStorage).spelunkerGetChunkHolder(ChunkPos.toLong(section.getX(), section.getZ()));
                if (chunkHolder != null) {
                    chunk = chunkHolder.getWorldChunk();
                } else chunk = world.getChunkManager().getWorldChunk(section.getX(), section.getZ(), true);
            } else chunk = world.getChunk(section.getX(), section.getZ(), ChunkStatus.FULL, false);
            if(chunk == null)
                continue;
            List<Pair<BlockPos, Block>> blocks = new ArrayList<>();
            for(int x = section.getMinX(); x <= section.getMaxX(); x++)
                for(int y = section.getMinY(); y <= section.getMaxY(); y++)
                    for(int z = section.getMinZ(); z <= section.getMaxZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        Block block = chunk.getBlockState(pos).getBlock();
                        if(SpelunkerEffectManager.isOreBlock(block))
                            blocks.add(new Pair<>(pos, block));
                    }
            dirty.put(section.asLong(), blocks);
        }
        return dirty;
    }

    public static boolean isOreBlock(Block block) {
        return SpelunkerConfig.parsedBlockHighlightColors.containsKey(block.getTranslationKey());
    }

    public static PacketByteBuf writePacket(Long2ObjectMap<List<Pair<BlockPos, Block>>> newChunks, long[] oldChunks, List<BlockPos> remove) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(newChunks.size()); // block count to write
        for (Long2ObjectMap.Entry<List<Pair<BlockPos, Block>>> entry : newChunks.long2ObjectEntrySet()) {
            buf.writeLong(entry.getLongKey());
            buf.writeVarInt(entry.getValue().size());
            for (Pair<BlockPos, Block> pair : entry.getValue()) {
                buf.writeShort(ChunkSectionPos.packLocal(pair.getLeft())); // write relative block pos
                buf.writeVarInt(Registry.BLOCK.getRawId(pair.getRight())); // write block id
            }
        }

        buf.writeVarInt(oldChunks.length); // block count to write
        for (long chunkPos : oldChunks)
            buf.writeVarLong(chunkPos);

        buf.writeVarInt(remove.size());
        for (BlockPos pos : remove)
            buf.writeLong(pos.asLong());
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void readPacket(SpelunkerEffectRenderer renderer, PacketByteBuf buf) {
        renderer.updateDirtyBlocks(readAddBuf(buf), readRemoveBuf(buf), Collections.emptyList());
    }

    @Environment(EnvType.CLIENT)
    private static Long2ObjectMap<List<Pair<BlockPos, Block>>> readAddBuf(PacketByteBuf buf) {
        Long2ObjectMap<List<Pair<BlockPos, Block>>> map = new Long2ObjectArrayMap<>();
        int size = buf.readVarInt(); // read block count
        for(int i = 0; i < size; i++) {
            long chunkPos = buf.readLong();
            ChunkSectionPos pos = ChunkSectionPos.from(chunkPos);
            int blockSize = buf.readVarInt();
            List<Pair<BlockPos, Block>> blocks = new ArrayList<>();
            for(int ii = 0; ii < blockSize; ii++)
                blocks.add(new Pair<>(pos.unpackBlockPos(buf.readShort()), Registry.BLOCK.get(buf.readVarInt())));
            map.put(chunkPos, blocks);
        }
        return map;
    }

    @Environment(EnvType.CLIENT)
    private static long[] readRemoveBuf(PacketByteBuf buf) {
        int size = buf.readVarInt();
        long[] array = new long[size];
        for(int i = 0; i < size; i++)
            array[i] = buf.readVarLong();
        return array;
    }
}
