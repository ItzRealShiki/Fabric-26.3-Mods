package de.itzrealshiki.provisuals.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import it.unimi.dsi.fastutil.ints.IntList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemStackRenderStateLayerAccessor {
    @Accessor("tintLayers")
    IntList provisuals$getTintLayers();

    @Accessor("quads")
    ItemQuads provisuals$getQuads();
}
