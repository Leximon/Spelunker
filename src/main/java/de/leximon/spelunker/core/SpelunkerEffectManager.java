package de.leximon.spelunker.core;

import de.leximon.spelunker.mixin.server.ThreadedAnvilChunkStorageAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SpelunkerEffectManager {

    public static ChunkOres findOresInChunk(World world, Vec3i sectionPos) {
        Chunk chunk = null;
        if(world.getChunkManager().isChunkLoaded(sectionPos.getX(), sectionPos.getZ())) {
            if (world instanceof ServerWorld sw) {
                ChunkHolder chunkHolder = ((ThreadedAnvilChunkStorageAccessor) sw.getChunkManager().threadedAnvilChunkStorage)
                        .spelunkerGetChunkHolder(ChunkPos.toLong(sectionPos.getX(), sectionPos.getZ())); // prevent random server crash ¯\_(ツ)_/¯
                if(chunkHolder != null)
                    chunk = chunkHolder.getWorldChunk();
            } else {
                chunk = world.getChunk(sectionPos.getX(), sectionPos.getZ(), ChunkStatus.FULL, false);
            }
        }
        if (chunk == null)
            return ChunkOres.EMPTY;
        ChunkSection section = chunk.getSection(sectionPos.getY());
        ChunkOres ores = new ChunkOres(sectionPos);
        var blockStates = section.getBlockStateContainer();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    Block block = blockStates.get(x, y, z).getBlock();
                    if (SpelunkerConfig.isOreBlock(block)) {
                        Vec3i blockPos = new Vec3i(x, y, z);
                        ores.put(blockPos, SpelunkerConfig.blockConfigs.get(block));
                    }
                }
            }
        }
        return ores;
    }

    public static HashMap<Vec3i, ChunkSection> getSurroundingChunkSections(World world, Vec3d playerPos) {
        int cx = ChunkSectionPos.getSectionCoord(playerPos.x);
        int cy = world.sectionCoordToIndex(ChunkSectionPos.getSectionCoord(playerPos.y));
        int cz = ChunkSectionPos.getSectionCoord(playerPos.z);

        HashMap<Vec3i, ChunkSection> sections = new HashMap<>();
        for (int x = cx - SpelunkerConfig.chunkRadius; x < cx + SpelunkerConfig.chunkRadius + 1; x++) {
            for (int z = cz - SpelunkerConfig.chunkRadius; z < cz + SpelunkerConfig.chunkRadius + 1; z++) {
                for (int y = cy - SpelunkerConfig.chunkRadius; y < cy + SpelunkerConfig.chunkRadius + 1; y++) {
                    WorldChunk chunk = world.getChunk(x, z);
                    ChunkSection[] sectionArray = chunk.getSectionArray();
                    if (y < 0 || y >= sectionArray.length)
                        continue;
                    sections.put(new Vec3i(x, y, z), sectionArray[y]);
                }
            }
        }
        return sections;
    }

    public static PacketByteBuf writePacket(World world, boolean overwrite, Collection<Vec3i> remove, Collection<ChunkOres> add) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeBoolean(overwrite);
        buf.writeVarInt(remove.size());
        for (Vec3i pos : remove) {
            buf.writeVarInt(pos.getX());
            buf.writeVarInt(pos.getY());
            buf.writeVarInt(pos.getZ());
        }

        buf.writeVarInt(add.size());
        synchronized (ChunkOres.SYNCHRONIZER) {
            for (ChunkOres ores : add) {
                Vec3i pos = ores.getPos();
                buf.writeVarInt(pos.getX());
                buf.writeVarInt(pos.getY());
                buf.writeVarInt(pos.getZ());

                buf.writeVarInt(ores.size());
                for (Map.Entry<Vec3i, SpelunkerConfig.ChunkBlockConfig> ore : ores.entrySet()) {
                    Vec3i orePos = ore.getKey();
                    buf.writeByte(orePos.getX());
                    buf.writeByte(orePos.getY());
                    buf.writeByte(orePos.getZ());

                    SpelunkerConfig.ChunkBlockConfig conf = ore.getValue();
                    buf.writeVarInt(conf == null ? -1 : Registry.BLOCK.getRawId(conf.getBlock()));
                }
            }
        }
        if(overwrite)
            buf.writeVarInt(world.getBottomSectionCoord());

        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void readPacket(SpelunkerEffectRenderer renderer, PacketByteBuf buf) {
        boolean overwrite = buf.readBoolean();
        int c = buf.readVarInt();
        for (int i = 0; i < c; i++) {
            renderer.removeChunk(new Vec3i(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            ));
        }

        c = buf.readVarInt();
        ArrayList<ChunkOres> chunks = new ArrayList<>(c);
        for (int i = 0; i < c; i++) {
            Vec3i pos = new Vec3i(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            );

            ChunkOres ores = overwrite ? new ChunkOres(pos) : renderer.get(pos);
            int cc = buf.readVarInt();
            for (int j = 0; j < cc; j++) {
                Vec3i orePos = new Vec3i(
                        buf.readByte(),
                        buf.readByte(),
                        buf.readByte()
                );
                int blockId = buf.readVarInt();

                if(ores != null)
                    ores.processConfig(orePos, blockId == -1 ? null : SpelunkerConfig.blockConfigs.get(Registry.BLOCK.get(blockId)), true);
            }
            if(overwrite)
                chunks.add(ores);
        }

        if(overwrite) {
            int bottomSectionCord = buf.readVarInt();
            renderer.addChunks(bottomSectionCord, chunks);
        }
    }

}
