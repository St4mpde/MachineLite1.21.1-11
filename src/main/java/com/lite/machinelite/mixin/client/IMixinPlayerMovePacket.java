package com.lite.machinelite.mixin.client;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface IMixinPlayerMovePacket {
    @Accessor("onGround")
    boolean isOnGround();

    @Mutable
    @Accessor("onGround")
    void setOnGround(boolean onGround);
}
