package de.leximon.spelunker.mixin;

import de.leximon.spelunker.SpelunkerMod;
import de.leximon.spelunker.SpelunkerModClient;
import de.leximon.spelunker.core.IWorld;
import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkerEffectManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
import java.util.HashSet;

@Mixin(World.class)
public abstract class WorldMixin implements IWorld {

    @Shadow public abstract boolean isClient();

    @Shadow @Nullable public abstract MinecraftServer getServer();

    private final HashSet<Vec3i> dirtySpelunkerChunks = new HashSet<>();

    @Inject(method = "onBlockChanged", at = @At("HEAD"))
    private void onBlockChangedInject(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        spelunkerUpdateBlock(pos, oldBlock, newBlock);
    }

    @Override
    public void spelunkerUpdateBlock(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
        Vec3i cPos = new Vec3i(
                ChunkSectionPos.getSectionCoord(pos.getX()),
                ((World) (Object) this).sectionCoordToIndex(ChunkSectionPos.getSectionCoord(pos.getY())),
                ChunkSectionPos.getSectionCoord(pos.getZ())
        );
        synchronized (dirtySpelunkerChunks) {
            dirtySpelunkerChunks.add(cPos);
        }
    }

    @Override
    public void spelunkerUpdateChunks() {
        if(dirtySpelunkerChunks.isEmpty())
            return;

        if (isClient()) {
            synchronized (dirtySpelunkerChunks) {
                spelunkerUpdateClient((World) (Object) this, dirtySpelunkerChunks);
                dirtySpelunkerChunks.clear();
            }
            return;
        }

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
            buf = SpelunkerEffectManager.findOresAndWritePacket((World) (Object) this, dirtySpelunkerChunks, dirtySpelunkerChunks);
            dirtySpelunkerChunks.clear();
        }
        for (ServerPlayerEntity p : players) {
            if (p.hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER))
                ServerPlayNetworking.send(p, SpelunkerMod.PACKET_ORE_CHUNKS, buf);
        }
    }

    @Environment(EnvType.CLIENT)
    private void spelunkerUpdateClient(World world, HashSet<Vec3i> chunks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((!SpelunkerConfig.serverValidating || client.isInSingleplayer()) && client.player != null && client.player.hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER)) {
            SpelunkerModClient.spelunkerEffectRenderer.updateChunks(world, chunks, chunks);
        }
    }
}
