package de.itzrealshiki.provisuals.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface EntityRendererTextureAccessor {
    @Invoker("getTextureLocation")
    Identifier provisuals$getTextureLocation(EntityRenderState state);
}
