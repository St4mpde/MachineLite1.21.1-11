package com.lite.machinelite.module.impl;

import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.PacketEvent;
import com.lite.machinelite.mixin.client.IMixinPlayerMovePacket;
import com.lite.machinelite.module.Module;
import com.lite.machinelite.utilities.IMC;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module implements IMC {
    public NoFall(String name, int keyCode) {
        super(name, keyCode);
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled()) return;
        if (!(event instanceof PacketEvent packetEvent)) return;
        if (!packetEvent.isOutgoing()) return;
        if (!(packetEvent.getPacket() instanceof PlayerMoveC2SPacket movePacket)) return;
        if (mc.player == null) return;

        double velocityY = mc.player.getVelocity().y;

        if (velocityY < -0.1 && !mc.player.isOnGround()) {
            ((IMixinPlayerMovePacket) movePacket).setOnGround(true);
        }
    }
}
