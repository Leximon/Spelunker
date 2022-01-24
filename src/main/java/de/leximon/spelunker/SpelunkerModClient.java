package de.leximon.spelunker;

import de.leximon.spelunker.core.SpelunkyEffectRenderer;
import de.leximon.spelunker.mixin.WorldRendererAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

public class SpelunkerModClient implements ClientModInitializer {

    public static SpelunkyEffectRenderer spelunkyEffectRenderer = new SpelunkyEffectRenderer();
    public static boolean isAlreadyRenderingOutline = false;

    @Override
    public void onInitializeClient() {
        // register renderer
        WorldRenderEvents.LAST.register(context -> {
            WorldRendererAccessor worldRenderer = (WorldRendererAccessor) context.worldRenderer();
            MinecraftClient client = MinecraftClient.getInstance();

            if (spelunkyEffectRenderer.isEnabled()) {
                if (!isAlreadyRenderingOutline) { // prevent the outline shader from being rendered twice due glowing entities
                    worldRenderer.getEntityOutlineShader().render(context.tickDelta());
                    client.getFramebuffer().beginWrite(false);
                }
                spelunkyEffectRenderer.render(context.matrixStack(), context.camera(), worldRenderer.getBufferBuilders().getOutlineVertexConsumers());
            }
            isAlreadyRenderingOutline = false;
        });
    }
}
