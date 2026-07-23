package com.lite.machinelite.module.impl;

import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.UpdateEvent;
import com.lite.machinelite.module.Module;
import com.lite.machinelite.utilities.IMC;
import com.lite.machinelite.utilities.TimerUtil;
import com.lite.machinelite.utilities.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class ElytraBoost extends Module implements IMC {
    private final TimerUtil timer = new TimerUtil();
    private static final float BOOST_INTERVAL_MS = 2000f;

    public ElytraBoost(String name, int keyCode) {
        super(name, keyCode);
    }

    @Override
    public void onEnabled() {
        timer.reset();
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled()) return;
        if (!(event instanceof UpdateEvent updateEvent)) return;
        if (!updateEvent.isPost()) return;
        if (mc.player == null) return;

        if (!mc.player.isGliding()) return;
        if (!timer.delay(BOOST_INTERVAL_MS)) return;

        int fireworkSlot = findFireworkSlot();
        if (fireworkSlot == -1) return;

        int prev = mc.player.getInventory().getSelectedSlot();
        Utils.switchItem(fireworkSlot);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        Utils.switchItem(prev);

        timer.reset();
    }

    private int findFireworkSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.FIREWORK_ROCKET) return i;
        }
        return -1;
    }
}
