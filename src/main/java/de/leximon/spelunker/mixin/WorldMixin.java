package de.leximon.spelunker.mixin;

import de.leximon.spelunker.SpelunkerMod;
import de.leximon.spelunker.SpelunkerModClient;
import de.leximon.spelunker.core.ChunkOres;
import de.leximon.spelunker.core.IWorld;
import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkerEffectManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

@Mixin(World.class)
public abstract class WorldMixin implements IWorld {

    @Shadow public abstract boolean isClient();

    @Shadow @Nullable public abstract MinecraftServer getServer();

    private final HashMap<Vec3i, ChunkOres> dirtySpelunkerChunks = new HashMap<>();

    @Inject(method = "onBlockChanged", at = @At("HEAD"))
    private void onBlockChangedInject(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        spelunkerUpdateBlock(pos, oldBlock, newBlock);
    }

    // add block to modified chunk list
    @Override
    public void spelunkerUpdateBlock(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
        Vec3i chunkPos = new Vec3i(
                ChunkSectionPos.getSectionCoord(pos.getX()),
                ((World) (Object) this).sectionCoordToIndex(ChunkSectionPos.getSectionCoord(pos.getY())),
                ChunkSectionPos.getSectionCoord(pos.getZ())
        );
        if (isClient()) {
            spelunkerUpdateBlockClient(chunkPos, pos, newBlock);
            return;
        }

        synchronized (dirtySpelunkerChunks) {
            dirtySpelunkerChunks.compute(chunkPos, (p, chunk) -> {
                if(chunk == null)
                    chunk = new ChunkOres(chunkPos);
                chunk.processBlock(pos, newBlock.getBlock());
                return chunk;
            });
        }
    }

    // directly update chunks clientside if server validating is turned off or in singleplayer
    @Environment(EnvType.CLIENT)
    private void spelunkerUpdateBlockClient(Vec3i chunkPos, BlockPos pos, BlockState newBlock) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((!SpelunkerConfig.serverValidating || client.isInSingleplayer()) && client.player != null && client.player.hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER)) {
            ChunkOres chunk = SpelunkerModClient.spelunkerEffectRenderer.get(chunkPos);
            if (chunk != null)
                chunk.processBlock(pos, newBlock.getBlock());
        }
    }

    // process all modified chunks
    @Override
    public void spelunkerUpdateChunks() {
        if(dirtySpelunkerChunks.isEmpty())
            return;

        if (!SpelunkerConfig.serverValidating)
            return;

        Collection<ServerPlayerEntity> players = PlayerLookup.all(getServer()).stream()
                .filter(p -> p.hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER))
                .toList();
        if (players.size() == 0)
            return;

        // send to clients
        PacketByteBuf buf;
        synchronized (dirtySpelunkerChunks) {
            buf = SpelunkerEffectManager.writePacket((World) (Object) this, Collections.emptyList(), dirtySpelunkerChunks.values());
            dirtySpelunkerChunks.clear();
        }
        for (ServerPlayerEntity p : players) {
            if (p.hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER))
                ServerPlayNetworking.send(p, SpelunkerMod.PACKET_ORE_CHUNKS, buf);
        }
    }
}
