package de.leximon.spelunker.mixin.client;

import de.leximon.spelunker.SpelunkerModClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @SuppressWarnings("InvalidInjectorMethodSignature") // Minecraft Development plugin is crazy again
    @ModifyVariable(method = "render", at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0), ordinal = 4)
    private boolean modify(boolean value) {
        return value || SpelunkerModClient.spelunkerEffectRenderer.isActive();
    }
}
