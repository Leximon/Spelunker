package de.leximon.spelunker.mixin;

import de.leximon.spelunker.SpelunkerMod;
import de.leximon.spelunker.SpelunkerModClient;
import de.leximon.spelunker.core.ChunkOres;
import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkerEffectManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    private int lastCx, lastCy, lastCz;
    private boolean forceOreChunkUpdate = true;
    private final HashSet<Vec3i> spelunkerEffectChunks = new HashSet<>();

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void moveEndInject(CallbackInfo ci) {
        if(!hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER)) {
            if(!spelunkerEffectChunks.isEmpty())
                spelunkerEffectChunks.clear();
            forceOreChunkUpdate = true;
            return;
        }
        if(SpelunkerConfig.serverValidating && world.isClient())
            return;

        int cx = ChunkSectionPos.getSectionCoord(getX());
        int cy = ChunkSectionPos.getSectionCoord(getY());
        int cz = ChunkSectionPos.getSectionCoord(getZ());

        // update if player crosses chunk border
        if (cx != lastCx || cy != lastCy || cz != lastCz || forceOreChunkUpdate) {
            forceOreChunkUpdate = false;
            HashMap<Vec3i, ChunkSection> newChunks = SpelunkerEffectManager.getSurroundingChunkSections(world, getPos());

            // calc difference and find ores
            HashSet<Vec3i> remove = new HashSet<>();
            spelunkerEffectChunks.removeIf(p -> {
                if (!newChunks.containsKey(p)) {
                    remove.add(p);
                    return true;
                }
                return false;
            });
            ArrayList<ChunkOres> add = new ArrayList<>();
            for (Map.Entry<Vec3i, ChunkSection> section : newChunks.entrySet()) {
                Vec3i pos = section.getKey();
                if (!spelunkerEffectChunks.contains(pos)) {
                    add.add(SpelunkerEffectManager.findOresInChunk(world, pos));
                    spelunkerEffectChunks.add(pos);
                }
            }

            // handle new and removed chunk sections
            if(world.isClient()) {
                SpelunkerModClient.spelunkerEffectRenderer.updateChunks(world, remove, add);
            } else if(SpelunkerConfig.serverValidating) {
                PacketByteBuf buf = SpelunkerEffectManager.writePacket(world, true, remove, add);
                ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, SpelunkerMod.PACKET_ORE_CHUNKS, buf);
            }
        }

        lastCx = cx;
        lastCy = cy;
        lastCz = cz;
    }

}
