package de.leximon.spelunker.mixin.client;

import de.leximon.spelunker.SpelunkerModClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @SuppressWarnings("InvalidInjectorMethodSignature") // Minecraft Development plugin is crazy again
    @ModifyVariable(method = "render", at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0), ordinal = 4)
    private boolean modify(boolean value) {
        return value || SpelunkerModClient.spelunkerEffectRenderer.isActive();
    }
}
