package de.leximon.spelunker.core;

import de.leximon.spelunker.SpelunkerMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.List;

public class SpelunkerEffectManager {

    public static HashSet<Pair<Vec3i, BlockState>> findOresInChunk(World world, Vec3i sectionPos) {
        ChunkSection section = world.getChunk(sectionPos.getX(), sectionPos.getZ()).getSection(sectionPos.getY());
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

            renderer.addChunk(MinecraftClient.getInstance().world, pos, ores);
        }
    }

}
