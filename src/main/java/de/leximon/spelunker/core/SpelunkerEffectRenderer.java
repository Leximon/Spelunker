package de.leximon.spelunker.core;

import de.leximon.spelunker.SpelunkerMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SpelunkerEffectRenderer {

    private static final ModelPart.Cuboid CUBE = new ModelPart.Cuboid(0, 0, 0, 0, 0, 16, 16, 16, 0, 0, 0, false, 0, 0);
    private static final RenderLayer RENDER_LAYER = RenderLayer.getOutline(SpelunkerMod.identifier("textures/none.png"));

    private final HashSet<OreChunkSection> chunkSections = new HashSet<>();
    private boolean enabled = false;

    public void render(MatrixStack matrices, Camera camera, OutlineVertexConsumerProvider vertexConsumers) {
        Vec3d pos = camera.getPos();
        matrices.push();
        matrices.translate(-pos.x, -pos.y, -pos.z);
        {
            synchronized (this) {
                for (OreChunkSection chunkSection : chunkSections)
                    chunkSection.render(matrices, pos, vertexConsumers);
            }
        }
        matrices.pop();
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
        synchronized (this) {
            chunkSections.clear();
        }
    }

    public void updateChunks(World world, HashSet<Vec3i> remove, HashSet<Vec3i> add) {
        synchronized (this) {
            chunkSections.removeIf(s -> remove.stream().anyMatch(s.pos::equals));
            for (Vec3i pos : add) {
                chunkSections.add(new OreChunkSection(
                        pos,
                        SpelunkerEffectManager.findOresInChunk(world, pos).stream()
                                .peek(pair -> {
                                    var p = pair.getLeft();
                                    pair.setLeft(new Vec3i(
                                            ChunkSectionPos.getBlockCoord(pos.getX()) + p.getX(),
                                            ChunkSectionPos.getBlockCoord(world.sectionIndexToCoord(pos.getY())) + p.getY(),
                                            ChunkSectionPos.getBlockCoord(pos.getZ()) + p.getZ()
                                    ));
                                }).collect(Collectors.toSet())
                ));
            }
        }
    }

    public void removeChunk(Vec3i pos) {
        synchronized (this) {
            chunkSections.removeIf(s -> s.pos.equals(pos));
        }
    }

    public void addChunks(World world, HashMap<Vec3i, Set<Pair<Vec3i, BlockState>>> chunks) {
        synchronized (this) {
            for (Map.Entry<Vec3i, Set<Pair<Vec3i, BlockState>>> entry : chunks.entrySet()) {
                Vec3i pos = entry.getKey();
                Set<Pair<Vec3i, BlockState>> ores = entry.getValue();
                chunkSections.add(new OreChunkSection(pos, ores.stream()
                        .peek(pair -> {
                            var p = pair.getLeft();
                            pair.setLeft(new Vec3i(
                                    ChunkSectionPos.getBlockCoord(pos.getX()) + p.getX(),
                                    ChunkSectionPos.getBlockCoord(world.sectionIndexToCoord(pos.getY())) + p.getY(),
                                    ChunkSectionPos.getBlockCoord(pos.getZ()) + p.getZ()
                            ));
                        }).collect(Collectors.toSet())));
            }
        }
    }

    private static float easeOutCirc(float x) {
        return (float) Math.sqrt(1 - Math.pow(x - 1, 2));
    }

    private record OreChunkSection(Vec3i pos, Set<Pair<Vec3i, BlockState>> ores) {

        public void render(MatrixStack matrices, Vec3d playerPos, OutlineVertexConsumerProvider vertexConsumers) {
            for (Pair<Vec3i, BlockState> ore : ores) {
                Vec3i pos = ore.getLeft();
                double squareDistance = pos.getSquaredDistance(playerPos, true);
                float fade;
                if(SpelunkerConfig.transitions) {
                    fade = Math.min(1 - (float) ((squareDistance - SpelunkerConfig.blockRadiusMin) / (SpelunkerConfig.blockRadiusMax - SpelunkerConfig.blockRadiusMin)), 1);
                    fade = easeOutCirc(fade);
                } else {
                    fade = 1;
                }
                if (squareDistance > SpelunkerConfig.blockRadiusMax)
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
            int color = SpelunkerConfig.parsedBlockHighlightColors.getOrDefault(block.getBlock(), 0xffffff);
            vertexConsumers.setColor((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff, 255);
            return vertexConsumers.getBuffer(RENDER_LAYER);
        }

        public boolean samePos(Vec3i o) {
            return pos.equals(o);
        }
    }
}
