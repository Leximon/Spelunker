package de.leximon.spelunker.core;

import de.leximon.spelunker.SpelunkerMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;

@Environment(EnvType.CLIENT)
public class SpelunkyEffectRenderer {

    private static final ModelPart.Cuboid CUBE = new ModelPart.Cuboid(0, 0, 0, 0, 0, 16, 16, 16, 0, 0, 0, false, 0, 0);
    private static final RenderLayer RENDER_LAYER = RenderLayer.getOutline(SpelunkerMod.identifier("textures/none.png"));

    private final WorldRenderer worldRenderer;
    private final HashSet<OreChunkSection> chunkSections = new HashSet<>();
    private HashMap<Block, Integer> blockHighlightColors = new HashMap<>();
    private int chunkRadius = 1;
    private int blockRadiusMax = (int) Math.pow(16, 2);
    private int blockRadiusMin = (int) Math.pow(15, 2);
    private Vec3d playerPos = Vec3d.ZERO;
    private boolean enabled = false;

    public SpelunkyEffectRenderer(WorldRenderer worldRenderer) {
        this.worldRenderer = worldRenderer;
    }

    public void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, OutlineVertexConsumerProvider vertexConsumers) {
        if(!enabled)
            return;
        Vec3d pos = camera.getPos();
        matrices.push();
        matrices.translate(-pos.x, -pos.y, -pos.z);
        {
            synchronized (chunkSections) {
                for (OreChunkSection chunkSection : chunkSections)
                    chunkSection.render(matrices, pos, tickDelta, limitTime, renderBlockOutline, vertexConsumers);
            }
        }
        matrices.pop();
    }

    public void setPlayerLocation(Vec3d pos) {
        this.playerPos = pos;
    }

    public boolean setEnabled(boolean value) {
        boolean init = value && !enabled;
        this.enabled = value;
        return init;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void clear() {
        synchronized (chunkSections) {
            chunkSections.clear();
        }
    }

    public void updateChunks() {
        ClientWorld world = ((IWorldRenderer) worldRenderer).getWorld();
        int cx = ChunkSectionPos.getSectionCoord(playerPos.x);
        int cy = world.sectionCoordToIndex(ChunkSectionPos.getSectionCoord(playerPos.y));
        int cz = ChunkSectionPos.getSectionCoord(playerPos.z);

        HashSet<Pair<Vec3i, ChunkSection>> sections = new HashSet<>();
        for (int x = cx - chunkRadius; x < cx + chunkRadius + 1; x++) {
            for (int z = cz - chunkRadius; z < cz + chunkRadius + 1; z++) {
                for (int y = cy - chunkRadius; y < cy + chunkRadius + 1; y++) {
                    WorldChunk chunk = world.getChunk(x, z);
                    ChunkSection[] sectionArray = chunk.getSectionArray();
                    if(y < 0 || y >= sectionArray.length)
                        continue;
                    sections.add(new Pair<>(new Vec3i(x, y, z), sectionArray[y]));
                }
            }
        }
        synchronized (chunkSections) {
            // remove
            chunkSections.removeIf(oreSection -> sections.stream().noneMatch(pair -> oreSection.samePos(pair.getLeft())));

            // add
            for (Pair<Vec3i, ChunkSection> section : sections)
                if (chunkSections.stream().noneMatch(s -> s.samePos(section.getLeft())))
                    chunkSections.add(new OreChunkSection(section.getLeft(), section.getRight()));
        }
    }

    public void parseConfig() {
        chunkRadius = (int) Math.ceil(SpelunkerConfig.radius / 16f);
        blockRadiusMax = (int) Math.pow(SpelunkerConfig.radius, 2);
        blockRadiusMin = (int) Math.pow(SpelunkerConfig.radius - 1, 2);

        blockHighlightColors.clear();
        for (SpelunkerConfig.BlockEntry blockEntry : SpelunkerConfig.blockHightlightColors) {
            int color = HexFormat.fromHexDigits(blockEntry.highlightColor.substring(1));
            for (String blockId : blockEntry.blockIds) {
                Block block = Registry.BLOCK.get(new Identifier(blockId));
                blockHighlightColors.put(block, color);
            }
        }
    }

    public void updateBlock(BlockPos pos) {
        ClientWorld world = ((IWorldRenderer) worldRenderer).getWorld();
        int cx = ChunkSectionPos.getSectionCoord(pos.getX());
        int cy = world.sectionCoordToIndex(ChunkSectionPos.getSectionCoord(pos.getY()));
        int cz = ChunkSectionPos.getSectionCoord(pos.getZ());
        Vec3i chunkPos = new Vec3i(cx, cy, cz);

        OreChunkSection oreChunk = chunkSections.stream().filter(section -> section.samePos(chunkPos)).findFirst().orElse(null);
        if(oreChunk == null)
            return;
        oreChunk.updateOres();
    }

    private boolean isOreBlock(BlockState state) {
        return blockHighlightColors.containsKey(state.getBlock());
    }

    private static float easeOutCirc(float x) {
        return (float) Math.sqrt(1 - Math.pow(x - 1, 2));
    }

    private class OreChunkSection {
        private final Vec3i pos;
        private final ChunkSection chunkSection;
        private final HashSet<Pair<BlockPos, BlockState>> ores = new HashSet<>();

        public OreChunkSection(Vec3i pos, ChunkSection chunkSection) {
            this.pos = pos;
            this.chunkSection = chunkSection;
            updateOres();
        }

        public void updateOres() {
            ores.clear();
            ClientWorld world = ((IWorldRenderer) worldRenderer).getWorld();
            var blockStates = chunkSection.getBlockStateContainer();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = blockStates.get(x, y, z);
                        if(isOreBlock(state)) {
                            BlockPos blockPos = new BlockPos(
                                    ChunkSectionPos.getBlockCoord(pos.getX()) + x,
                                    ChunkSectionPos.getBlockCoord(world.sectionIndexToCoord(pos.getY())) + y,
                                    ChunkSectionPos.getBlockCoord(pos.getZ()) + z
                            );
                            ores.add(new Pair<>(blockPos, state));
                        }
                    }
                }
            }
        }

        public void render(MatrixStack matrices, Vec3d playerPos, float tickDelta, long limitTime, boolean renderBlockOutline, OutlineVertexConsumerProvider vertexConsumers) {
            for (Pair<BlockPos, BlockState> ore : ores) {

                BlockPos pos = ore.getLeft();
                double squareDistance = pos.getSquaredDistance(playerPos, true);
                float fade = Math.min(1 - (float) ((squareDistance - blockRadiusMin) / (blockRadiusMax - blockRadiusMin)), 1);
                fade = easeOutCirc(fade);
                if(squareDistance > blockRadiusMax)
                    continue;
                matrices.push();
                matrices.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                matrices.scale(fade, fade, fade);
                {
                    matrices.push();
                    matrices.translate(-0.5, -0.5, -0.5);
                    CUBE.renderCuboid(matrices.peek(), setOutlineColor(ore.getRight(), vertexConsumers), 0, OverlayTexture.DEFAULT_UV, 0, 0, 0, 0);
                    matrices.pop();
                }
                matrices.pop();
            }
        }

        private VertexConsumer setOutlineColor(BlockState block, OutlineVertexConsumerProvider vertexConsumers) {
            int color = blockHighlightColors.getOrDefault(block.getBlock(), 0xffffff);
            vertexConsumers.setColor((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff, 255);
            return vertexConsumers.getBuffer(RENDER_LAYER);
        }

        public boolean samePos(Vec3i o) {
            return pos.equals(o);
        }
    }
}
