package de.leximon.spelunker.mixin;

import de.leximon.spelunker.SpelunkerMod;
import de.leximon.spelunker.SpelunkerModClient;
import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkerEffectManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    private double lastPosX, lastPosY, lastPosZ;
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
        if(SpelunkerConfig.serverSideValidating && world.isClient())
            return;
        double x = getX();
        double y = getY();
        double z = getZ();

        int lastCx = ChunkSectionPos.getSectionCoord(lastPosX);
        int lastCy = ChunkSectionPos.getSectionCoord(lastPosY);
        int lastCz = ChunkSectionPos.getSectionCoord(lastPosZ);
        int cx = ChunkSectionPos.getSectionCoord(x);
        int cy = ChunkSectionPos.getSectionCoord(y);
        int cz = ChunkSectionPos.getSectionCoord(z);

        // update if player crosses chunk border
        if (cx != lastCx || cy != lastCy || cz != lastCz || forceOreChunkUpdate) {
            forceOreChunkUpdate = false;
            HashSet<Pair<Vec3i, ChunkSection>> newChunks = SpelunkerEffectManager.getSurroundingChunkSections(world, getPos());

            // calc difference
            HashSet<Vec3i> remove = new HashSet<>();
            spelunkerEffectChunks.removeIf(p -> {
                if (newChunks.stream().noneMatch(pair -> p.equals(pair.getLeft()))) {
                    remove.add(p);
                    return true;
                }
                return false;
            });
            HashSet<Vec3i> add = new HashSet<>();
            for (Pair<Vec3i, ChunkSection> section : newChunks) {
                if (spelunkerEffectChunks.stream().noneMatch(s -> s.equals(section.getLeft()))) {
                    add.add(section.getLeft());
                    spelunkerEffectChunks.add(section.getLeft());
                }
            }

            // handle new and removed chunk sections
            if(world.isClient()) {
                SpelunkerModClient.spelunkerEffectRenderer.updateChunks(world, remove, add);
            } else if(SpelunkerConfig.serverSideValidating) {
                PacketByteBuf buf = SpelunkerEffectManager.findOresAndWritePacket(world, remove, add);;
                ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, SpelunkerMod.PACKET_ORE_CHUNKS, buf);
            }
        }

        lastPosX = x;
        lastPosY = y;
        lastPosZ = z;
    }

}
