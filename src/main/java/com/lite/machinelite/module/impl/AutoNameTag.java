package com.lite.machinelite.module.impl;

import com.lite.machinelite.MachineLite;
import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.UpdateEvent;
import com.lite.machinelite.module.Module;
import com.lite.machinelite.utilities.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class AutoNameTag extends Module {
    public List<LivingEntity> targets;
    private LivingEntity target;

    public AutoNameTag(String name, int keyCode) {
        super(name, keyCode);
        this.targets = new ArrayList<>();
    }

    @Override
    public void onDisabled() {
        this.targets.clear();
        this.target = null;
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled())
            return;

        if (event instanceof UpdateEvent) {
            this.collectTarget();

            if (target != null) {
                int tagSlot = (mc.player.getMainHandStack().getItem() == Items.NAME_TAG)
                        ? mc.player.getInventory().getSelectedSlot()
                        : Utils.getItemSlotByToolBar(Items.NAME_TAG);
                int lastSlot = mc.player.getInventory().getSelectedSlot();
                ItemStack currentItemStack = tagSlot != -1 ? mc.player.getInventory().getStack(tagSlot)
                        : ItemStack.EMPTY;

                if (tagSlot != -1 && this.check(target, currentItemStack.getName().getString())) {
                    Utils.switchItem(tagSlot);

                    Box boundingBox = target.getBoundingBox();
                    Vec3d vec3d = new Vec3d((boundingBox.minX + boundingBox.maxX) / 2.0D,
                            boundingBox.minY + (boundingBox.maxY - boundingBox.minY) / 100.0D * 70,
                            (boundingBox.minZ + boundingBox.maxZ) / 2.0D);
                    float[] rotations = Utils.getNeededRotations(vec3d);
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotations[0],
                            rotations[1], mc.player.isOnGround(), false));
                    ActionResult result = mc.interactionManager.interactEntity(mc.player, target, Hand.MAIN_HAND);

                    if (result.isAccepted()) {
                        MachineLite.WriteChat("Tagged " + target.getName().getString());
                    }

                    Utils.switchItem(lastSlot);
                }
            }
        }
    }

    public void collectTarget() {
        this.targets.clear();
        if (mc.world == null)
            return;

        for (Entity entity : mc.world.getEntities()) {
            if (mc.player.distanceTo(entity) < 6.0F) {
                if (entity instanceof WitherEntity) {
                    targets.add((LivingEntity) entity);
                }
            }
        }

        this.target = !targets.isEmpty() ? targets.get(0) : null;
    }

    private boolean check(Entity entity, String stackName) {
        if (!entity.hasCustomName())
            return true;
        String name = entity.getCustomName().getString().toLowerCase().replaceAll(" ", "");
        return stackName.toLowerCase().replaceAll(" ", "").contains("ghosthax") ? !name.contains("ghosthax") : false;
    }
}
