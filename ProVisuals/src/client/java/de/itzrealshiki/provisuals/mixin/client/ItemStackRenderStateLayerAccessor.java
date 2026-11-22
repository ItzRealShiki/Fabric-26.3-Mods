package de.itzrealshiki.provisuals.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemStackRenderStateLayerAccessor {
    @Accessor("quads")
    ItemQuads provisuals$getQuads();
}
