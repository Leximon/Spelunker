package de.leximon.spelunker.mixin;

import de.leximon.spelunker.core.IWorldRenderer;
import de.leximon.spelunker.core.SpelunkyEffectRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix4f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements IWorldRenderer {

    @Shadow @Final private BufferBuilderStorage bufferBuilders;
    @Shadow private @Nullable ClientWorld world;
    @Shadow @Final private MinecraftClient client;

    @Shadow private @Nullable ShaderEffect entityOutlineShader;
    private final SpelunkyEffectRenderer spelunkyEffectRenderer = new SpelunkyEffectRenderer((WorldRenderer) (Object) this);

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void inject(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {
        if(client.player != null && entityOutlineShader != null && spelunkyEffectRenderer.isEnabled()) {
            this.entityOutlineShader.render(tickDelta);
            this.client.getFramebuffer().beginWrite(false);
            spelunkyEffectRenderer.render(matrices, tickDelta, limitTime, renderBlockOutline, camera, bufferBuilders.getOutlineVertexConsumers());
        }
    }

    @Override
    public SpelunkyEffectRenderer getSpelunkyEffectRenderer() {
        return spelunkyEffectRenderer;
    }

    @Nullable
    @Override
    public ClientWorld getWorld() {
        return world;
    }
}
