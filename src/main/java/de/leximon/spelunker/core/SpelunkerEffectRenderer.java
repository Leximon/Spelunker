package de.leximon.spelunker.core;

import de.leximon.spelunker.SpelunkerMod;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class SpelunkerEffectRenderer {

    private static final ModelPart.Cuboid CUBE = new ModelPart.Cuboid(0, 0, 0, 0, 0, 16, 16, 16, 0, 0, 0, false, 0, 0);
    private static final RenderLayer RENDER_LAYER = RenderLayer.getOutline(SpelunkerMod.identifier("textures/none.png"));

    private final Long2ObjectMap<List<Pair<BlockPos, Block>>> cache = new Long2ObjectArrayMap<>();
    private boolean enabled = false;

    public void render(MatrixStack matrices, Camera camera, OutlineVertexConsumerProvider vertexConsumers) {
        Vec3d cameraPos = camera.getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        synchronized (cache) {
            for (Long2ObjectMap.Entry<List<Pair<BlockPos, Block>>> entry : cache.long2ObjectEntrySet()) {
                for (Pair<BlockPos, Block> pair : entry.getValue()) {
                    Vec3i pos = pair.getLeft();
                    double squareDistance = toSquaredDistanceFromCenter(pos, cameraPos.getX(), cameraPos.getY(), cameraPos.getZ());
                    if (squareDistance > SpelunkerConfig.blockRadiusMax)
                        continue;
                    float fade;
                    if (SpelunkerConfig.blockTransitions) {
                        fade = Math.min(1 - (float) ((squareDistance - SpelunkerConfig.blockRadiusMin) / (SpelunkerConfig.blockRadiusMax - SpelunkerConfig.blockRadiusMin)), 1);
                        fade = easeOutCirc(fade);
                    } else fade = 1;
                    matrices.push();
                    matrices.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    matrices.scale(fade, fade, fade);
                    {
                        matrices.push();
                        matrices.translate(-0.5, -0.5, -0.5);
                        CUBE.renderCuboid(matrices.peek(), setOutlineColor(pair.getRight(), vertexConsumers), 0, OverlayTexture.DEFAULT_UV, 0, 0, 0, 0);
                        matrices.pop();
                    }
                    matrices.pop();
                }
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
        synchronized (cache) {
            this.cache.clear();
        }
    }

    public void updateDirtyBlocks(Long2ObjectMap<List<Pair<BlockPos, Block>>> add, long[] chunks, List<BlockPos> remove) {
        synchronized (cache) {
            for (Long2ObjectMap.Entry<List<Pair<BlockPos, Block>>> entry : add.long2ObjectEntrySet())
                for (Pair<BlockPos, Block> pair : entry.getValue()) {
                    List<Pair<BlockPos, Block>> list = cache.get(entry.getLongKey());
                    if (list == null) {
                        list = new ArrayList<>();
                        list.add(pair);
                        cache.put(entry.getLongKey(), list);
                    } else list.add(pair);
                }
        }
        synchronized (cache) {
            for (long chunkPos : chunks)
                cache.remove(chunkPos);
            for (BlockPos pos : remove) {
                long chunkPos = ChunkSectionPos.from(pos).asLong();
                List<Pair<BlockPos, Block>> ores = cache.get(chunkPos);
                if (ores != null)
                    ores.removeIf(blockPosBlockPair -> blockPosBlockPair.getLeft().asLong() == pos.asLong());
            }
        }
    }

    private static float easeOutCirc(float x) {
        return (float) Math.sqrt(1 - Math.pow(x - 1, 2));
    }

    private VertexConsumer setOutlineColor(Block block, OutlineVertexConsumerProvider vertexConsumers) {
        int color = SpelunkerConfig.parsedBlockHighlightColors.getOrDefault(block, 0xffffff);
        vertexConsumers.setColor((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff, 255);
        return vertexConsumers.getBuffer(RENDER_LAYER);
    }

    public static double toSquaredDistanceFromCenter(Vec3i pos, double x, double y, double z) {
        double d = (double) pos.getX() + 0.5D - x;
        double e = (double) pos.getY() + 0.5D - y;
        double f = (double) pos.getZ() + 0.5D - z;
        return d * d + e * e + f * f;
    }
}
