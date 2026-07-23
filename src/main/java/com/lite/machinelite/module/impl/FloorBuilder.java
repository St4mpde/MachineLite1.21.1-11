package com.lite.machinelite.module.impl;

import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.UpdateEvent;
import com.lite.machinelite.module.Module;
import com.lite.machinelite.utilities.Utils;
import net.minecraft.util.math.BlockPos;

public class FloorBuilder extends Module {
    private static final int RANGE = 4;
    private static final double REACH = 4.5;

    public FloorBuilder(String name, int keyCode) {
        super(name, keyCode);
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled()) return;
        if (!(event instanceof UpdateEvent u) || !u.isPost()) return;
        if (mc.player == null || mc.world == null) return;
        if (!Utils.isBuilderBlock(mc.player.getMainHandStack())) return;

        BlockPos origin = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ());

        for (int dist = 0; dist <= RANGE; dist++) {
            for (int dx = -dist; dx <= dist; dx++) {
                for (int dz = -dist; dz <= dist; dz++) {
                    if (Math.abs(dx) != dist && Math.abs(dz) != dist) continue;
                    BlockPos pos = origin.add(dx, 0, dz);
                    if (mc.world.getBlockState(pos).isReplaceable() && Utils.placeBlock(REACH, pos)) {
                        return;
                    }
                }
            }
        }
    }
}
