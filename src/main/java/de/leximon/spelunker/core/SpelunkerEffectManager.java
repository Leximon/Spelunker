package de.leximon.spelunker.core;

import de.leximon.spelunker.mixin.ThreadedAnvilChunkStorageAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SpelunkerEffectManager {

    public static Set<Pair<Vec3i, BlockState>> findOresInChunk(World world, Vec3i sectionPos) {
        Chunk chunk = null;
        if(world.getChunkManager().isChunkLoaded(sectionPos.getX(), sectionPos.getZ())) {
            if (world instanceof ServerWorld sw) {
                ChunkHolder chunkHolder = ((ThreadedAnvilChunkStorageAccessor) sw.getChunkManager().threadedAnvilChunkStorage)
                        .spelunkerGetChunkHolder(ChunkPos.toLong(sectionPos.getX(), sectionPos.getZ()));
                if(chunkHolder != null)
                    chunk = chunkHolder.getWorldChunk();
            } else {
                chunk = world.getChunk(sectionPos.getX(), sectionPos.getZ(), ChunkStatus.FULL, false);
            }
        }
        if (chunk == null)
            return Collections.emptySet();
        ChunkSection section = chunk.getSection(sectionPos.getY());
        HashSet<Pair<Vec3i, BlockState>> ores = new HashSet<>();
        var blockStates = section.getBlockStateContainer();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockState state = blockStates.get(x, y, z);
                    if (isOreBlock(state)) {
                        Vec3i blockPos = new Vec3i(x, y, z);
                        ores.add(new Pair<>(blockPos, state));
                    }
                }
            }
        }
        return ores;
    }

    private static boolean isOreBlock(BlockState state) {
        return SpelunkerConfig.parsedBlockHighlightColors.containsKey(state.getBlock());
    }

    public static HashSet<Pair<Vec3i, ChunkSection>> getSurroundingChunkSections(World world, Vec3d playerPos) {
        int cx = ChunkSectionPos.getSectionCoord(playerPos.x);
        int cy = world.sectionCoordToIndex(ChunkSectionPos.getSectionCoord(playerPos.y));
        int cz = ChunkSectionPos.getSectionCoord(playerPos.z);

        HashSet<Pair<Vec3i, ChunkSection>> sections = new HashSet<>();
        for (int x = cx - SpelunkerConfig.chunkRadius; x < cx + SpelunkerConfig.chunkRadius + 1; x++) {
            for (int z = cz - SpelunkerConfig.chunkRadius; z < cz + SpelunkerConfig.chunkRadius + 1; z++) {
                for (int y = cy - SpelunkerConfig.chunkRadius; y < cy + SpelunkerConfig.chunkRadius + 1; y++) {
                    WorldChunk chunk = world.getChunk(x, z);
                    ChunkSection[] sectionArray = chunk.getSectionArray();
                    if (y < 0 || y >= sectionArray.length)
                        continue;
                    sections.add(new Pair<>(new Vec3i(x, y, z), sectionArray[y]));
                }
            }
        }
        return sections;
    }

    public static PacketByteBuf writePacket(World world, Vec3i pos, boolean remove) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(remove);

        buf.writeVarInt(pos.getX());
        buf.writeVarInt(pos.getY());
        buf.writeVarInt(pos.getZ());

        if(!remove) {

        }
        return buf;
    }

    public static PacketByteBuf findOresAndWritePacket(World world, HashSet<Vec3i> remove, HashSet<Vec3i> add) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeVarInt(remove.size());
        for (Vec3i pos : remove) {
            buf.writeVarInt(pos.getX());
            buf.writeVarInt(pos.getY());
            buf.writeVarInt(pos.getZ());
        }

        buf.writeVarInt(add.size());
        for (Vec3i pos : add) {
            buf.writeVarInt(pos.getX());
            buf.writeVarInt(pos.getY());
            buf.writeVarInt(pos.getZ());

            var ores = findOresInChunk(world, pos);
            buf.writeVarInt(ores.size());
            for (Pair<Vec3i, BlockState> ore : ores) {
                Vec3i orePos = ore.getLeft();
                buf.writeByte(orePos.getX());
                buf.writeByte(orePos.getY());
                buf.writeByte(orePos.getZ());

                buf.writeVarInt(Registry.BLOCK.getRawId(ore.getRight().getBlock()));
            }
        }

        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void readPacket(SpelunkerEffectRenderer renderer, PacketByteBuf buf) {
        int c = buf.readVarInt();
        for (int i = 0; i < c; i++) {
            renderer.removeChunk(new Vec3i(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            ));
        }

        c = buf.readVarInt();
        HashMap<Vec3i, Set<Pair<Vec3i, BlockState>>> chunks = new HashMap<>();
        for (int i = 0; i < c; i++) {
            Vec3i pos = new Vec3i(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            );

            HashSet<Pair<Vec3i, BlockState>> ores = new HashSet<>();
            int cc = buf.readVarInt();
            for (int j = 0; j < cc; j++) {
                Vec3i orePos = new Vec3i(
                        buf.readByte(),
                        buf.readByte(),
                        buf.readByte()
                );
                ores.add(new Pair<>(orePos, Registry.BLOCK.get(buf.readVarInt()).getDefaultState()));
            }
            chunks.put(pos, ores);
        }
        renderer.addChunks(MinecraftClient.getInstance().world, chunks);
    }

}
