package de.leximon.spelunker.mixin;

import de.leximon.spelunker.SpelunkerMod;
import de.leximon.spelunker.SpelunkerModClient;
import de.leximon.spelunker.core.IWorld;
import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkerEffectManager;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
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
        HashSet<Vec3i> chunks = new HashSet<>();
        chunks.add(cPos);

        if (isClient()) {
            if(!SpelunkerConfig.serverValidating || MinecraftClient.getInstance().isInSingleplayer())
                SpelunkerModClient.spelunkerEffectRenderer.updateChunks((World) (Object) this, chunks, chunks);
            return;
        }

        if (!SpelunkerConfig.serverValidating)
            return;

        Collection<ServerPlayerEntity> players = PlayerLookup.tracking((ServerWorld) (Object) this, pos).stream()
                .filter(p -> p.hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER))
                .toList();
        if (players.size() == 0)
            return;

        // send to clients
        PacketByteBuf buf = SpelunkerEffectManager.findOresAndWritePacket((World) (Object) this, chunks, chunks);
        for (ServerPlayerEntity p : PlayerLookup.tracking((ServerWorld) (Object) this, pos)) {
            if (p.hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER))
                ServerPlayNetworking.send(p, SpelunkerMod.PACKET_ORE_CHUNKS, buf);
        }
    }
}
