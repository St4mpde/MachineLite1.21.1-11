package com.lite.machinelite.mixin.client;

import com.lite.machinelite.MachineLite;
import com.lite.machinelite.module.impl.AutoSign;
import com.lite.machinelite.utilities.IMC;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public class MixinGuiEditSign {
    @Shadow
    @Final
    private SignBlockEntity blockEntity;

    @Shadow
    @Final
    private boolean front;

    @Shadow
    private String[] messages;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        if (MachineLite.getModuleManager().getModuleByString("AutoSign") != null
                && MachineLite.getModuleManager().getModuleByString("AutoSign").isEnabled()) {
            Text[] signText = ((AutoSign) MachineLite.getModuleManager().getModuleByString("AutoSign")).getSignText();
            if (signText != null && signText.length >= 4) {
                this.messages[0] = signText[0].getString();
                this.messages[1] = signText[1].getString();
                this.messages[2] = signText[2].getString();
                this.messages[3] = signText[3].getString();

                net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket packet = new net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket(
                        this.blockEntity.getPos(), this.front,
                        this.messages[0], this.messages[1], this.messages[2], this.messages[3]);
                IMC.mc.getNetworkHandler().sendPacket(packet);

                IMC.mc.setScreen(null);
                ci.cancel();
            }
        }
    }

    @Inject(method = "finishEditing", at = @At("HEAD"))
    protected void finishEditing(CallbackInfo ci) {
        if (MachineLite.getModuleManager().getModuleByString("AutoSign") != null
                && MachineLite.getModuleManager().getModuleByString("AutoSign").isEnabled()) {
            Text[] newTexts = new Text[] {
                    Text.literal(this.messages[0] == null ? "" : this.messages[0]),
                    Text.literal(this.messages[1] == null ? "" : this.messages[1]),
                    Text.literal(this.messages[2] == null ? "" : this.messages[2]),
                    Text.literal(this.messages[3] == null ? "" : this.messages[3])
            };
            ((AutoSign) MachineLite.getModuleManager().getModuleByString("AutoSign")).setSignTexts(newTexts);
            MachineLite.WriteChat("Set SignText");
        }
    }
}
