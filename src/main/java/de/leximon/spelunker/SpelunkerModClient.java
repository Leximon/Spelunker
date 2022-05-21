package de.leximon.spelunker;

import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.core.SpelunkerEffectManager;
import de.leximon.spelunker.core.SpelunkerEffectRenderer;
import de.leximon.spelunker.mixin.client.WorldRendererAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

public class SpelunkerModClient implements ClientModInitializer {

    public static SpelunkerEffectRenderer spelunkerEffectRenderer = new SpelunkerEffectRenderer();
    public static boolean isAlreadyRenderingOutline = false;

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> SpelunkerConfig.initBlockHighlightConfig());

        // register renderer
        WorldRenderEvents.LAST.register(context -> {
            WorldRendererAccessor worldRenderer = (WorldRendererAccessor) context.worldRenderer();
            MinecraftClient client = MinecraftClient.getInstance();

            if (spelunkerEffectRenderer.isActive()) {
                if (!isAlreadyRenderingOutline) { // prevent the outline shader from being rendered twice due glowing entities
                    worldRenderer.getEntityOutlineShader().render(context.tickDelta());
                    client.getFramebuffer().beginWrite(false);
                }
                spelunkerEffectRenderer.render(context.matrixStack(), context.camera(), worldRenderer.getBufferBuilders().getOutlineVertexConsumers());
            }
            isAlreadyRenderingOutline = false;
        });

        ClientPlayNetworking.registerGlobalReceiver(SpelunkerMod.PACKET_ORE_CHUNKS, (client, handler, buf, sender) -> SpelunkerEffectManager.readPacket(spelunkerEffectRenderer, buf));
        ClientPlayNetworking.registerGlobalReceiver(SpelunkerMod.PACKET_CONFIG, (client, handler, buf, sender) -> SpelunkerConfig.readPacket(buf));
    }
}
