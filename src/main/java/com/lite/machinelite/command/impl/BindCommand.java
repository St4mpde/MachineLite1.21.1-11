package com.lite.machinelite.command.impl;

import com.lite.machinelite.MachineLite;
import com.lite.machinelite.module.Module;
import com.lite.machinelite.command.Command;
import net.minecraft.client.util.InputUtil;

public class BindCommand extends Command {
    public BindCommand(String[] names, String description) {
        super(names, description);
    }

    public void fire(String[] args) {
        if (args == null || args.length == 0) {
            MachineLite.WriteChat("\2477" + MachineLite.getCommandPrefix() + "bind <Module> <Key>  |  " + MachineLite.getCommandPrefix() + "bind command <prefix>");
            return;
        }

        if (args[0].equalsIgnoreCase("command")) {
            if (args.length >= 2) {
                MachineLite.setCommandPrefix(args[1]);
                MachineLite.WriteChat("Command prefix set to \247f" + args[1]);
            } else {
                MachineLite.WriteChat("Current prefix: \247f" + MachineLite.getCommandPrefix());
            }
            return;
        }

        Module module = MachineLite.getModuleManager().getModuleByString(args[0]);
        if (module != null) {
            if (args.length == 1) {
                MachineLite.WriteChat(String.format("\2477Current key for \247b%s \2477is \2479%s.",
                        module.getName(), InputUtil.Type.KEYSYM.createFromCode(module.getKeyCode()).getLocalizedText().getString()));
            } else {
                try {
                    int key = InputUtil.fromTranslationKey("key.keyboard." + args[1].toLowerCase()).getCode();
                    module.setKeyCode(key);
                    MachineLite.WriteChat("\2477" + module.getName() + " bound to \247f" + args[1].toUpperCase() + "\2477.");
                } catch (Exception e) {
                    MachineLite.WriteChat("\247cInvalid key!");
                }
            }
        } else {
            MachineLite.WriteChat("\247cModule not found.");
        }
    }
}
