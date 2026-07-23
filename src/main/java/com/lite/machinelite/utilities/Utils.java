package com.lite.machinelite.utilities;

import com.lite.machinelite.MachineLite;
import com.lite.machinelite.module.impl.AntiGhostBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class Utils implements IMC {
    public static void switchItem(int slot) {
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            mc.player.getInventory().setSelectedSlot(slot);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    public static int getItemSlotByToolBar(Item itemIn) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack itemStack = mc.player.getInventory().getStack(slot);
            if (itemStack.getItem() == itemIn) {
                return slot;
            }
        }
        return -1;
    }

    public static Direction getFacing() {
        return mc.player.getHorizontalFacing();
    }

    public static boolean placeBlock(double reach, BlockPos pos, boolean silentRotate) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
                mc.player.getZ());
        final Vec3d posVec = Vec3d.ofCenter(pos);

        float[] targetRotations = null;
        Vec3d targetHitVec = null;
        Direction targetFacing = null;
        BlockPos targetNeighbor = null;

        for (Direction facing : Direction.values()) {
            final BlockPos neighbor = pos.offset(facing);
            BlockState neighborState = mc.world.getBlockState(neighbor);

            if (!neighborState.isReplaceable()) {
                final Vec3d dirVec = new Vec3d(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ());
                final Vec3d hitVec = posVec.add(dirVec.multiply(0.5));

                if (eyesPos.squaredDistanceTo(hitVec) <= 36.0) {
                    float[] rotations = Utils.getNeededRotations(hitVec);
                    HitResult traceResult = Utils.rayTraceBlocks(reach, rotations[0], rotations[1]);

                    if (traceResult.getType() == HitResult.Type.BLOCK) {
                        targetRotations = rotations;
                        targetHitVec = hitVec;
                        targetFacing = facing;
                        targetNeighbor = neighbor;
                        break;
                    }
                }
            }
        }

        if (targetRotations == null) return false;

        float prevYaw = mc.player.getYaw();
        float prevPitch = mc.player.getPitch();

        if (silentRotate) {
            mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.LookAndOnGround(targetRotations[0], targetRotations[1], mc.player.isOnGround(), false)
            );
            mc.player.setHeadYaw(targetRotations[0]);
        }

        BlockHitResult interactResult = new BlockHitResult(targetHitVec, targetFacing.getOpposite(), targetNeighbor, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, interactResult);
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (MachineLite.getModuleManager().isEnabled(AntiGhostBlock.class)) {
            mc.getNetworkHandler().sendPacket(
                new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, interactResult, 0)
            );
        }

        if (silentRotate) {
            mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.LookAndOnGround(prevYaw, prevPitch, mc.player.isOnGround(), false)
            );
        }

        return true;
    }

    public static boolean placeBlock(double reach, BlockPos pos) {
        return placeBlock(reach, pos, true);
    }

    public static float[] getNeededRotations(Vec3d vec) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
                mc.player.getZ());
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = mc.player.getYaw()
                + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F - mc.player.getYaw());
        float pitch = mc.player.getPitch()
                + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
        return new float[] { yaw, pitch };
    }

    public static Vec3d getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    public static HitResult rayTraceBlocks(double reach, float yaw, float pitch) {
        Vec3d start = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
                mc.player.getZ());
        Vec3d dir = Utils.getVectorForRotation(pitch, yaw);
        Vec3d end = start.add(dir.x * reach, dir.y * reach, dir.z * reach);
        return mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, mc.player));
    }

    public static boolean isBlockContainer(Block block) {
        return block == Blocks.CHEST || block == Blocks.ENDER_CHEST || block == Blocks.TRAPPED_CHEST ||
                block == Blocks.CRAFTING_TABLE || block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE ||
                block == Blocks.SMOKER || block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL ||
                block == Blocks.DAMAGED_ANVIL || block == Blocks.BARREL || block == Blocks.HOPPER ||
                block == Blocks.DROPPER || block == Blocks.DISPENSER || block == Blocks.BREWING_STAND ||
                block == Blocks.ENCHANTING_TABLE;
    }

    public static boolean isBuilderBlock(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof net.minecraft.item.BlockItem))
            return false;

        String name = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath();

        if (name.contains("sign") || name.contains("egg") || name.contains("anvil") ||
                name.contains("chest") || name.contains("shulker_box") || name.contains("bed") ||
                name.contains("banner") || name.contains("door") || name.contains("button") ||
                name.contains("pressure_plate") || name.contains("sand") || name.contains("gravel") ||
                name.contains("concrete_powder") || name.contains("tnt") ||
                name.contains("sapling") || name.contains("flower") || name.contains("mushroom") ||
                name.contains("torch") || name.contains("camp_fire") || name.contains("lantern") ||
                name.contains("skull") || name.contains("head") || name.contains("carpet")) {
            return false;
        }

        Block block = ((net.minecraft.item.BlockItem) stack.getItem()).getBlock();
        return !isBlockContainer(block);
    }
}
