package com.lite.machinelite.mixin.client;

import com.lite.machinelite.event.impl.RenderOverlayEvent;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinGuiIngame {
    @Inject(method = "renderMainHud", at = @At("RETURN"))
    private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callbackInfo) {
        RenderOverlayEvent renderOverlayEvent = new RenderOverlayEvent();
        renderOverlayEvent.fire(context, tickCounter.getTickProgress(true)).call();
    }
}

