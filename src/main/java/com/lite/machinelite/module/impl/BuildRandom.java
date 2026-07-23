package com.lite.machinelite.module.impl;

import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.UpdateEvent;
import com.lite.machinelite.module.Module;
import com.lite.machinelite.utilities.TimerUtil;
import com.lite.machinelite.utilities.Utils;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class BuildRandom extends Module {
    private final Random random;
    private final TimerUtil timer;

    public BuildRandom(String name, int keyCode) {
        super(name, keyCode);
        random = new Random();
        timer = new TimerUtil();
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled())
            return;

        if (event instanceof com.lite.machinelite.event.impl.RightClickMouseEvent) {
            int range = 4;
            int bound = range * 2 + 1;
            int attempts = 0;
            BlockPos pos;

            if (!checkHeldItem()) {
                return;
            }

            event.setCancelled(true);

            try {
                boolean placed = false;
                do {
                    pos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ()).add(
                            random.nextInt(bound) - range, random.nextInt(bound) - range, random.nextInt(bound) - range);
                    placed = tryToPlaceBlock(range, pos);
                } while (++attempts < 128 && timer.delay(80) && !placed);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean tryToPlaceBlock(double reach, BlockPos pos) {
        if (pos == null || !mc.world.getBlockState(pos).isReplaceable()) return false;

        BlockPos playerFeet = mc.player.getBlockPos();
        if (pos.equals(playerFeet) || pos.equals(playerFeet.up())) return false;

        if (Utils.placeBlock(reach, pos)) {
            timer.reset();
            return true;
        }

        return false;
    }

    private boolean checkHeldItem() {
        if (mc.player == null)
            return false;
        ItemStack stack = mc.player.getMainHandStack();
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }
}
