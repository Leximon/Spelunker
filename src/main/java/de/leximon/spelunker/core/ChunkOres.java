package de.leximon.spelunker.core;

import de.leximon.spelunker.SpelunkerMod;
import de.leximon.spelunker.mixin.Vec3iAccessor;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.HashMap;
import java.util.Map;

public class ChunkOres extends HashMap<Vec3i, Block> {

    public static final ChunkOres EMPTY = new ChunkOres(Vec3i.ZERO);

    private final Vec3i pos;
    private boolean remapped = false;

    public ChunkOres(Vec3i pos) {
        this.pos = pos;
    }

    public Vec3i getPos() {
        return pos;
    }

    /**
     * adds or removes a block according to whether it is an ore block
     * @param pos the world coordinate
     * @param block the block
     */
    public void processBlock(BlockPos pos, Block block) {
        if(remapped) {
            processBlock(new Vec3i(pos.getX(), pos.getY(), pos.getZ()), block);
        } else {
            processBlock(new Vec3i(
                    ChunkSectionPos.getLocalCoord(pos.getX()),
                    ChunkSectionPos.getLocalCoord(pos.getY()),
                    ChunkSectionPos.getLocalCoord(pos.getZ())
            ), block);
        }
    }

    /**
     * adds or removes a block according to whether it is an ore block
     * @param pos the local coordinate in the chunk
     * @param block the block
     */
    public void processBlock(Vec3i pos, Block block) {
        if(SpelunkerConfig.isOreBlock(block))
            put(pos, block);
        else
            remove(pos);
    }

    /**
     * remaps all local coordinates to world coordinates
     * @param bottomSectionCord the bottom section cord of the world
     * @return this
     */
    public ChunkOres remapToWorldCoordinates(int bottomSectionCord) {
        remapped = true;
        HashMap<Vec3i, Block> clone = new HashMap<>(this);
        clear();
        for (Map.Entry<Vec3i, Block> pair : clone.entrySet()) {
            Vec3i p = pair.getKey();

            put(new Vec3i(
                    ChunkSectionPos.getBlockCoord(pos.getX()) + p.getX(),
                    ChunkSectionPos.getBlockCoord(pos.getY() + bottomSectionCord) + p.getY(),
                    ChunkSectionPos.getBlockCoord(pos.getZ()) + p.getZ()
            ), pair.getValue());
        }


        return this;
    }

    /*
     * RENDER
     */

    private static final ModelPart.Cuboid CUBE = new ModelPart.Cuboid(0, 0, 0, 0, 0, 16, 16, 16, 0, 0, 0, false, 0, 0);
    private static final RenderLayer RENDER_LAYER = RenderLayer.getOutline(SpelunkerMod.identifier("textures/none.png"));

    public void render(MatrixStack matrices, Vec3d playerPos, OutlineVertexConsumerProvider vertexConsumers) {
        for (Map.Entry<Vec3i, Block> ore : entrySet()) {
            Vec3i pos = ore.getKey();
            double squareDistance = toSquaredDistanceFromCenter(pos, playerPos.getX(), playerPos.getY(), playerPos.getZ());
            float fade;
            if (SpelunkerConfig.blockTransitions) {
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
                CUBE.renderCuboid(matrices.peek(), setOutlineColor(ore.getValue(), vertexConsumers), 0, OverlayTexture.DEFAULT_UV, 0, 0, 0, 0);
                matrices.pop();
            }
            matrices.pop();
        }
    }

    private VertexConsumer setOutlineColor(Block block, OutlineVertexConsumerProvider vertexConsumers) {
        int color = SpelunkerConfig.parsedBlockHighlightColors.getOrDefault(block, 0xffffff);
        vertexConsumers.setColor((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff, 255);
        return vertexConsumers.getBuffer(RENDER_LAYER);
    }

    private static float easeOutCirc(float x) {
        return (float) Math.sqrt(1 - Math.pow(x - 1, 2));
    }

    private static double toSquaredDistanceFromCenter(Vec3i pos, double x, double y, double z) {
        double d = (double) pos.getX() + 0.5D - x;
        double e = (double) pos.getY() + 0.5D - y;
        double f = (double) pos.getZ() + 0.5D - z;
        return d * d + e * e + f * f;
    }
}
