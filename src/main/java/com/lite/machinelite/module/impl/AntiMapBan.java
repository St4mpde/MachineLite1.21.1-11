package com.lite.machinelite.module.impl;

import com.lite.machinelite.MachineLite;
import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.PacketEvent;
import com.lite.machinelite.event.impl.UpdateEvent;
import com.lite.machinelite.mixin.client.IMixinMapData;
import com.lite.machinelite.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.item.map.MapDecoration;

import java.util.Map;

public class AntiMapBan extends Module {
    public AntiMapBan(String name, int keyCode) {
        super(name, keyCode);
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled())
            return;

        if (event instanceof UpdateEvent) {
            ItemStack currentItem = mc.player.getMainHandStack();
            if (!currentItem.isEmpty() && currentItem.getItem() instanceof FilledMapItem) {
                MapIdComponent mapId = currentItem.get(DataComponentTypes.MAP_ID);
                if (mapId != null) {
                    MapState mapState = mc.world.getMapState(mapId);
                    if (mapState != null) {
                        this.getMapDecorations(mapState).clear();
                    }
                }
            }

            if (mc.world != null) {
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof ItemFrameEntity) {
                        ItemFrameEntity frame = (ItemFrameEntity) entity;
                        ItemStack frameItem = frame.getHeldItemStack();
                        if (!frameItem.isEmpty() && frameItem.getItem() instanceof FilledMapItem) {
                            MapIdComponent mapId = frameItem.get(DataComponentTypes.MAP_ID);
                            if (mapId != null) {
                                MapState mapState = mc.world.getMapState(mapId);
                                if (mapState != null) {
                                    this.getMapDecorations(mapState).clear();
                                }
                            }
                        }
                    }
                }
            }
        }
        if (event instanceof PacketEvent) {
            if (((PacketEvent) event).getPacket() instanceof MapUpdateS2CPacket) {
                if (((PacketEvent) event).isIncoming()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public Map<String, MapDecoration> getMapDecorations(MapState mapState) {
        Map<String, MapDecoration> decorations = ((IMixinMapData) mapState).getMapDecorations();
        if (MachineLite.getModuleManager().isEnabled(Debug.class)) {
            MachineLite.WriteChat(String.format("MapInfo: [scale:%s, decorations:%s]",
                    mapState.scale, decorations.size()));
        }
        return decorations;
    }
}
