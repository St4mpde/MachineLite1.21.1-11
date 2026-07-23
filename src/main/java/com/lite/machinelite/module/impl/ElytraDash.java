package com.lite.machinelite.module.impl;

import com.lite.machinelite.MachineLite;
import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.UpdateEvent;
import com.lite.machinelite.module.Module;
import com.lite.machinelite.utilities.IMC;
import com.lite.machinelite.utilities.Utils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class ElytraDash extends Module implements IMC {
    private enum State {
        IDLE, EQUIP, WAIT_EQUIP, JUMP, WAIT_AIR, ACTIVATE, WAIT_FLYING,
        BOOST, WAIT_BOOST, REEQUIP, DONE
    }

    private State state = State.IDLE;
    private int tickTimer = 0;
    private Item savedChestItem = null;

    public ElytraDash(String name, int keyCode) {
        super(name, keyCode);
    }

    @Override
    public void onEnabled() {
        state = State.EQUIP;
        tickTimer = 0;
        savedChestItem = null;
    }

    @Override
    public void onDisabled() {
        state = State.IDLE;
        tickTimer = 0;
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled()) return;
        if (!(event instanceof UpdateEvent u) || !u.isPost()) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        switch (state) {
            case EQUIP -> {
                ItemStack chest = mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);

                if (chest.isOf(Items.ELYTRA)) {
                    savedChestItem = Items.ELYTRA;
                    state = State.JUMP;
                    tickTimer = 0;
                    return;
                }

                savedChestItem = chest.isEmpty() ? null : chest.getItem();
                int elytraSlot = findElytraScreenSlot();
                if (elytraSlot == -1) {
                    MachineLite.WriteChat("\u00A7cElytraDash: elytra not found");
                    setDisable();
                    return;
                }
                swapSlots(elytraSlot, 6);
                state = State.WAIT_EQUIP;
                tickTimer = 0;
            }
            case WAIT_EQUIP -> {
                if (mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                    state = State.JUMP;
                    tickTimer = 0;
                } else if (++tickTimer > 15) {
                    MachineLite.WriteChat("\u00A7cElytraDash: equip failed");
                    setDisable();
                }
            }
            case JUMP -> {
                mc.player.jump();
                state = State.WAIT_AIR;
                tickTimer = 0;
            }
            case WAIT_AIR -> {
                if (!mc.player.isOnGround()) {
                    state = State.ACTIVATE;
                    tickTimer = 0;
                } else if (++tickTimer > 20) {
                    MachineLite.WriteChat("\u00A7cElytraDash: airborne timeout");
                    tryReequip();
                    setDisable();
                }
            }
            case ACTIVATE -> {
                mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), -90.0f, mc.player.isOnGround(), false)
                );
                mc.getNetworkHandler().sendPacket(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                );
                state = State.WAIT_FLYING;
                tickTimer = 0;
            }
            case WAIT_FLYING -> {
                if (mc.player.isGliding()) {
                    state = State.BOOST;
                    tickTimer = 0;
                } else {
                    tickTimer++;
                    if (tickTimer % 3 == 0 && tickTimer <= 12) {
                        mc.getNetworkHandler().sendPacket(
                            new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                        );
                    } else if (tickTimer > 15) {
                        MachineLite.WriteChat("\u00A7cElytraDash: activation failed");
                        tryReequip();
                        setDisable();
                    }
                }
            }
            case BOOST -> {
                int fireworkSlot = findFireworkHotbarSlot();
                if (fireworkSlot != -1) {
                    int prevSlot = mc.player.getInventory().getSelectedSlot();
                    Utils.switchItem(fireworkSlot);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    Utils.switchItem(prevSlot);
                } else {
                    MachineLite.WriteChat("\u00A7eno firework — boost skipped");
                }
                state = State.WAIT_BOOST;
                tickTimer = 0;
            }
            case WAIT_BOOST -> {
                if (++tickTimer >= 5) {
                    state = State.REEQUIP;
                }
            }
            case REEQUIP -> {
                tryReequip();
                state = State.DONE;
            }
            case DONE -> setDisable();
        }
    }

    private void tryReequip() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (savedChestItem == Items.ELYTRA) return;

        if (savedChestItem != null) {
            int armorSlot = findItemScreenSlot(savedChestItem);
            if (armorSlot != -1) {
                swapSlots(armorSlot, 6);
            }
        } else {
            ItemStack chest = mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
            if (chest.isOf(Items.ELYTRA)) {
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    6, 0, SlotActionType.QUICK_MOVE, mc.player
                );
            }
        }
    }

    private void swapSlots(int slotA, int slotB) {
        int syncId = mc.player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slotA, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, slotB, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, slotA, 0, SlotActionType.PICKUP, mc.player);
    }

    private int findElytraScreenSlot() {
        for (int i = 9; i <= 45; i++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
            if (stack.isOf(Items.ELYTRA)) return i;
        }
        return -1;
    }

    private int findFireworkHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) return i;
        }
        return -1;
    }

    private int findItemScreenSlot(Item target) {
        for (int i = 9; i <= 44; i++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
            if (stack.isOf(target)) return i;
        }
        return -1;
    }
}
