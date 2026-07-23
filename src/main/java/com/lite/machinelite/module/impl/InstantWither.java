package com.lite.machinelite.module.impl;

import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.RightClickMouseEvent;
import com.lite.machinelite.event.impl.UpdateEvent;
import com.lite.machinelite.module.Module;
import com.lite.machinelite.utilities.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class InstantWither extends Module {
    private int delay, lastSlot;

    public InstantWither(String name, int keyCode) {
        super(name, keyCode);
    }

    @Override
    public void onDisabled() {
        delay = 0;
        lastSlot = 0;
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled())
            return;

        if (event instanceof RightClickMouseEvent) {
            if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
            if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK || delay > 0) {
                return;
            }

            BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
            if (mc.world.getBlockState(blockHit.getBlockPos()).isAir()
                    || mc.player.getMainHandStack().getItem() != Items.SOUL_SAND) {
                return;
            }

            BlockPos startPos = blockHit.getBlockPos().offset(blockHit.getSide());
            Direction front = mc.player.getHorizontalFacing();
            Direction left = front.rotateYCounterclockwise();
            lastSlot = mc.player.getInventory().getSelectedSlot();
            int[][] offset;
            byte b;
            int i;

            int sandSlot = Utils.getItemSlotByToolBar(Items.SOUL_SAND);
            int skullSlot = Utils.getItemSlotByToolBar(Items.WITHER_SKELETON_SKULL);

            if (sandSlot != -1) {
                offset = new int[][] { new int[3], { 0, 1, 0 }, { 1, 1, 0 }, { -1, 1, 0 } };
                for (i = offset.length, b = 0; b < i;) {
                    int[] pos = offset[b];
                    this.placeBlocks(startPos.up(pos[1]).offset(front, pos[2]).offset(left, pos[0]), sandSlot);
                    b++;
                }
            }

            if (skullSlot != -1) {
                offset = new int[][] { { 1, 2, 0 }, { 0, 2, 0 }, { -1, 2, 0 } };
                for (i = offset.length, b = 0; b < i;) {
                    int[] pos = offset[b];
                    this.placeBlocks(startPos.up(pos[1]).offset(front, pos[2]).offset(left, pos[0]), skullSlot);
                    b++;
                }
            }

            event.setCancelled(true);
        }
        if (event instanceof UpdateEvent) {
            delay--;
        }
    }

    private void placeBlocks(BlockPos pos, int slot) {
        if (mc.world.getBlockState(pos).isReplaceable()) {
            Utils.switchItem(slot);
            Utils.placeBlock(4, pos);
            Utils.switchItem(lastSlot);
            delay = 2;
        }
    }
}
