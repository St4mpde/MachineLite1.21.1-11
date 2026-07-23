package com.lite.machinelite.module.impl;

import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.PacketEvent;
import com.lite.machinelite.module.Module;
import com.lite.machinelite.utilities.IMC;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayDeque;
import java.util.Deque;

public class Blink extends Module implements IMC {
    private static final int MAX_PACKETS = 100;
    private final Deque<Packet<?>> packetQueue = new ArrayDeque<>();
    private boolean flushing = false;

    public Blink(String name, int keyCode) {
        super(name, keyCode);
    }

    @Override
    public void onEnabled() {
        packetQueue.clear();
    }

    @Override
    public void onDisabled() {
        flush();
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled()) return;
        if (flushing) return;
        if (!(event instanceof PacketEvent packetEvent)) return;
        if (!packetEvent.isOutgoing()) return;

        Packet<?> packet = packetEvent.getPacket();
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround) return;

        event.setCancelled(true);
        packetQueue.addLast(packet);

        if (packetQueue.size() >= MAX_PACKETS) {
            flush();
            setDisable();
        }
    }

    private void flush() {
        if (mc.getNetworkHandler() == null) {
            packetQueue.clear();
            return;
        }
        flushing = true;
        while (!packetQueue.isEmpty()) {
            mc.getNetworkHandler().sendPacket(packetQueue.pollFirst());
        }
        flushing = false;
    }
}
