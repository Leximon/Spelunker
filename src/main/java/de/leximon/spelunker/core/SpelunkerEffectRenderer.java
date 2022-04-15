package de.leximon.spelunker.core;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SpelunkerEffectRenderer {

    private final HashMap<Vec3i, ChunkOres> chunkSections = new HashMap<>();
    private boolean active = false;

    public void render(MatrixStack matrices, Camera camera, OutlineVertexConsumerProvider vertexConsumers) {
        Vec3d pos = camera.getPos();
        matrices.push();
        matrices.translate(-pos.x, -pos.y, -pos.z);
        {
            synchronized (this) {
                for (Map.Entry<Vec3i, ChunkOres> chunkSection : chunkSections.entrySet())
                    chunkSection.getValue().render(matrices, pos, vertexConsumers);
            }
        }
        matrices.pop();
    }

    public boolean setActive(boolean value) {
        boolean init = value && !active;
        this.active = value;
        return init;
    }

    public boolean isActive() {
        return active;
    }

    public void clear() {
        synchronized (this) {
            chunkSections.clear();
        }
    }

    public void updateChunks(World world, Collection<Vec3i> remove, Collection<ChunkOres> add) {
        synchronized (this) {
            for (Vec3i v : remove)
                chunkSections.remove(v);
            for (ChunkOres chunk : add) {
                chunkSections.put(chunk.getPos(), chunk
                        .remapToWorldCoordinates(world.getBottomSectionCoord())
                );
            }
        }
    }

    public void removeChunk(Vec3i pos) {
        synchronized (this) {
            chunkSections.remove(pos);
        }
    }

    public ChunkOres get(Vec3i pos) {
        synchronized (this) {
            return chunkSections.get(pos);
        }
    }

    public void addChunks(int bottomSectionCord, Collection<ChunkOres> chunks) {
        synchronized (this) {
            for (ChunkOres chunk : chunks)
                chunkSections.put(chunk.getPos(), chunk.remapToWorldCoordinates(bottomSectionCord));
        }
    }
}
