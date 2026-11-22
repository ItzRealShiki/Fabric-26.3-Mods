package de.itzrealshiki.provisuals;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import de.itzrealshiki.provisuals.mixin.client.ItemStackRenderStateAccessor;
import de.itzrealshiki.provisuals.mixin.client.ItemStackRenderStateLayerAccessor;
import de.itzrealshiki.provisuals.mixin.client.SpriteContentsImageAccessor;

import java.util.Map;
import java.util.WeakHashMap;

/** Samples item model sprites once and derives a bright outline color from their visible pixels. */
public final class HeldItemGlowRenderer {
    private static final Map<SpriteContents, Integer> COLOR_CACHE = new WeakHashMap<>();
    private static final int FALLBACK = GlowOutlineRenderer.VIOLET;

    private HeldItemGlowRenderer() {
    }

    public static boolean isHandContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    public static int outlineColor(ItemStackRenderState state) {
        if (!ProVisualsConfig.heldItemTextureColor) return FALLBACK;

        try {
            ItemStackRenderStateAccessor access = (ItemStackRenderStateAccessor) state;
            ItemStackRenderState.LayerRenderState[] layers = access.provisuals$getLayers();
            int active = Math.min(access.provisuals$getActiveLayerCount(), layers.length);
            int[] tintLayers = ((ItemStackRenderStateLayerAccessor) layers[i]).provisuals$getTintLayers();

            long red = 0L;
            long green = 0L;
            long blue = 0L;
            long count = 0L;

            for (int i = 0; i < active; i++) {
                ItemQuads quads = ((ItemStackRenderStateLayerAccessor) layers[i]).provisuals$getQuads();
                if (quads == null) continue;

                for (BakedQuad quad : quads.all()) {
                    TextureAtlasSprite sprite = quad.materialInfo().sprite();
                    if (sprite == null) continue;
                    int sample = averageSpriteColor(sprite.contents());
                    if (sample == 0) continue;

                    int tintIndex = quad.materialInfo().tintIndex();
                    if (tintIndex >= 0 && tintIndex < tintLayers.length) {
                        int tint = tintLayers[tintIndex];
                        sample = multiplyTint(sample, tint);
                    }

                    red += ARGB.red(sample);
                    green += ARGB.green(sample);
                    blue += ARGB.blue(sample);
                    count++;
                    if (count >= 96) break;
                }
                if (count >= 96) break;
            }

            if (count == 0) return FALLBACK;
            int r = brighten((int) (red / count));
            int g = brighten((int) (green / count));
            int b = brighten((int) (blue / count));
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        } catch (Throwable throwable) {
            Diagnostics.warn("Could not sample held-item texture for glow: " + throwable.getClass().getSimpleName());
            return FALLBACK;
        }
    }

    private static int averageSpriteColor(SpriteContents contents) {
        synchronized (COLOR_CACHE) {
            Integer cached = COLOR_CACHE.get(contents);
            if (cached != null) return cached;
        }

        try {
            var image = ((SpriteContentsImageAccessor) (Object) contents).provisuals$getOriginalImage();
            int width = Math.max(1, contents.width());
            int height = Math.max(1, contents.height());
            int stepX = Math.max(1, width / 8);
            int stepY = Math.max(1, height / 8);
            long r = 0L, g = 0L, b = 0L, count = 0L;

            for (int y = 0; y < height; y += stepY) {
                for (int x = 0; x < width; x += stepX) {
                    int pixel = image.getPixel(x, y);
                    if (ARGB.alpha(pixel) < 32) continue;
                    r += ARGB.red(pixel);
                    g += ARGB.green(pixel);
                    b += ARGB.blue(pixel);
                    count++;
                }
            }

            int color = count == 0 ? 0
                    : 0xFF000000 | ((int) (r / count) << 16)
                    | ((int) (g / count) << 8) | (int) (b / count);
            synchronized (COLOR_CACHE) {
                COLOR_CACHE.put(contents, color);
            }
            return color;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int multiplyTint(int color, int tint) {
        int r = ARGB.red(color) * ARGB.red(tint) / 255;
        int g = ARGB.green(color) * ARGB.green(tint) / 255;
        int b = ARGB.blue(color) * ARGB.blue(tint) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int brighten(int value) {
        return Math.min(255, Math.max(64, Math.round(value * 0.72f + 72.0f)));
    }
}
