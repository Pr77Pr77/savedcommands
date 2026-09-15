package de.aliba2468pr77pr77.savedcommands.mixin.client;

import de.aliba2468pr77pr77.savedcommands.EditCommandScreen;
import de.aliba2468pr77pr77.savedcommands.SavedCommandManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.pressedPartialCombination;
import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKey(long handle, int action, KeyEvent input, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null ||
                mc.screen instanceof EditCommandScreen editScreen && editScreen.keybindSetting ||
                mc.screen instanceof KeyBindsScreen keyBindsScreen && keyBindsScreen.selectedKey != null ||
                (mc.screen != null && (mc.screen.getFocused() instanceof EditBox eb && eb.canConsumeInput() ||
                        mc.screen.getFocused() instanceof MultiLineEditBox ||
                        mc.screen instanceof AbstractSignEditScreen ||
                        mc.screen.getClass().getName().startsWith("fi.dy.masa")))) {
            pressedPartialCombination = null;
            return;
        }

        if (commandManager != null) {
            if (action == InputConstants.PRESS) {
                for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                    if (command.keybinds == null || command.keybinds.isEmptyOrNull()) {
                        continue;
                    }
                    if (pressedPartialCombination != null && pressedPartialCombination.size() < command.keybinds.keys.size()) {
                        InputConstants.Key key = command.keybinds.keys.get(pressedPartialCombination.size());
                        if (key.getValue() == input.key() && key.getType() == InputConstants.Type.KEYSYM) {
                            pressedPartialCombination.add(InputConstants.Type.KEYSYM.getOrCreate(input.key()));
                            break;
                        }
                    }
                    if (command.keybinds.keys.getFirst().getValue() == input.key() &&
                            command.keybinds.keys.getFirst().getType() == InputConstants.Type.KEYSYM && pressedPartialCombination == null) {
                        pressedPartialCombination = new ArrayList<>();
                        pressedPartialCombination.add(InputConstants.Type.KEYSYM.getOrCreate(input.key()));
                        break;
                    }
                }
            }

            if (commandManager != null && pressedPartialCombination != null &&
                    pressedPartialCombination.contains(InputConstants.Type.KEYSYM.getOrCreate(input.key()))) {
                ci.cancel();
            }

            if (action == InputConstants.RELEASE) {
                if (pressedPartialCombination != null &&
                        pressedPartialCombination.contains(InputConstants.Type.KEYSYM.getOrCreate(input.key()))) {
                    // after releasing the first key of the combination, search for it in commandManager.data.commands.keybinds:
                    for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                        if (command.keybinds != null && !command.keybinds.isEmptyOrNull() && command.keybinds.keys.equals(pressedPartialCombination)) {
                            LOGGER.info("Combination released/pressed: " + command.command);
                            SavedCommandManager.sendCommandAndInsertVariables(command, minecraft.screen);
                        }
                    }
                    if (pressedPartialCombination.getLast().equals(InputConstants.Type.KEYSYM.getOrCreate(input.key())) &&
                            pressedPartialCombination.size() > 1) {
                        pressedPartialCombination.removeLast();
                    } else {
                        pressedPartialCombination = null;
                    }
                }
            }
        }
    }
}
