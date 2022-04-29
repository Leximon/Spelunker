package de.leximon.spelunker.mixin;

import de.leximon.spelunker.SpelunkerMod;
import de.leximon.spelunker.SpelunkerModClient;
import de.leximon.spelunker.core.IWorld;
import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkerEffectManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mixin(World.class)
public abstract class WorldMixin implements IWorld {

    @Shadow public abstract boolean isClient();

    @Shadow @Nullable public abstract MinecraftServer getServer();

    private final Long2ObjectMap<List<Pair<BlockPos, Block>>> newBlocks = new Long2ObjectArrayMap<>();
    private final List<BlockPos> oldBlocks = Collections.synchronizedList(new ArrayList<>());

    @Inject(method = "onBlockChanged", at = @At("HEAD"))
    private void onBlockChangedInject(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        spelunkerUpdateBlock(pos, oldBlock, newBlock);
    }

    @Override
    public void spelunkerUpdateBlock(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
        if (SpelunkerEffectManager.isOreBlock(newBlock.getBlock())) {
            long chunkPos = ChunkSectionPos.from(pos).asLong();
            List<Pair<BlockPos, Block>> blocks = newBlocks.get(chunkPos);
            if(blocks == null) {
                blocks = new ArrayList<>();
                blocks.add(new Pair<>(pos, newBlock.getBlock()));
                newBlocks.put(chunkPos, blocks);
            } else blocks.add(new Pair<>(pos, newBlock.getBlock()));
        } else if (SpelunkerEffectManager.isOreBlock(oldBlock.getBlock()))
            oldBlocks.add(pos);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void updateClientChunks() {
        if (newBlocks.size() == 0 && oldBlocks.size() == 0)
            return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.player.hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER))
            return;

        SpelunkerModClient.spelunkerEffectRenderer.updateDirtyBlocks(newBlocks, new long[] {}, oldBlocks);
        newBlocks.clear();
        oldBlocks.clear();
    }

    @Override
    @Environment(EnvType.SERVER)
    public void updateServerChunks() {
        if (newBlocks.size() == 0 && oldBlocks.size() == 0)
            return;
        if (!SpelunkerConfig.serverValidating)
            return;

        Collection<ServerPlayerEntity> players = PlayerLookup.all(getServer()).stream()
                .filter(p -> p.hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER))
                .toList();
        if (players.size() == 0)
            return;

        // send to clients
        PacketByteBuf buf = SpelunkerEffectManager.writePacket(newBlocks, new long[] {}, oldBlocks);
        newBlocks.clear();
        oldBlocks.clear();
        for (ServerPlayerEntity p : players)
            ServerPlayNetworking.send(p, SpelunkerMod.PACKET_ORE_CHUNKS, buf);
    }
}
