package de.itzrealshiki.provisuals.mixin;

import de.itzrealshiki.provisuals.MusicIslandHud;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
abstract class MusicIslandChatMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void provisuals$handleMusicIslandClick(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ChatScreen && MusicIslandHud.handleChatClick(event.x(), event.y())) {
            cir.setReturnValue(true);
        }
    }
}
