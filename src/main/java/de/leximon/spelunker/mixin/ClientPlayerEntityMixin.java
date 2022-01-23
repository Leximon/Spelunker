package de.leximon.spelunker.mixin;

import com.mojang.authlib.GameProfile;
import de.leximon.spelunker.SpelunkerMod;
import de.leximon.spelunker.core.IWorldRenderer;
import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkyEffectRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

//    private Vec3d lastPos;

    @Shadow @Final protected MinecraftClient client;
    private double lastPosX, lastPosY, lastPosZ;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

//    @Inject(method = "move", at = @At("HEAD"))
//    private void moveStartInject(MovementType movementType, Vec3d movement, CallbackInfo ci) {
//
//    }

    @Inject(method = "move", at = @At("TAIL"))
    private void moveEndInject(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        SpelunkyEffectRenderer renderer = ((IWorldRenderer) client.worldRenderer).getSpelunkyEffectRenderer();

        if(renderer.isEnabled()) {
            int lastCx = ChunkSectionPos.getSectionCoord(lastPosX);
            int lastCy = ChunkSectionPos.getSectionCoord(lastPosY);
            int lastCz = ChunkSectionPos.getSectionCoord(lastPosZ);
            int cx = ChunkSectionPos.getSectionCoord(getX());
            int cy = ChunkSectionPos.getSectionCoord(getY());
            int cz = ChunkSectionPos.getSectionCoord(getZ());

            renderer.setPlayerLocation(getPos());
            if (cx != lastCx || cy != lastCy || cz != lastCz) {
                renderer.updateChunks();
            }
        }
        lastPosX = getX();
        lastPosY = getY();
        lastPosZ = getZ();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickInject(CallbackInfo ci) {
        SpelunkyEffectRenderer renderer = ((IWorldRenderer) client.worldRenderer).getSpelunkyEffectRenderer();
        if (renderer.setEnabled(hasStatusEffect(SpelunkerMod.STATUS_EFFECT_SPELUNKER))) {
            renderer.parseConfig();
            renderer.clear();
            renderer.setPlayerLocation(getPos());
            renderer.updateChunks();
        }
    }

}
