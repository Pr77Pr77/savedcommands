package de.aliba2468pr77pr77.savedcommands.mixin.client;

import de.aliba2468pr77pr77.savedcommands.EditCommandScreen;
import de.aliba2468pr77pr77.savedcommands.SavedCommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.MouseButtonInfo;
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

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null ||
                Minecraft.getInstance().gui.screen() instanceof EditCommandScreen editScreen && editScreen.keybindSetting ||
                Minecraft.getInstance().gui.screen() instanceof KeyBindsScreen keyBindsScreen && keyBindsScreen.selectedKey != null ||
                (Minecraft.getInstance().gui.screen() != null && input.button() == 0)) { // Prevent softlock
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
                        if (key.getValue() == input.button() && key.getType() == InputConstants.Type.MOUSE) {
                            pressedPartialCombination.add(InputConstants.Type.MOUSE.getOrCreate(input.button()));
                            break;
                        }
                    } else if (command.keybinds.keys.getFirst().getValue() == input.button() &&
                            command.keybinds.keys.getFirst().getType() == InputConstants.Type.MOUSE && pressedPartialCombination == null) {
                        pressedPartialCombination = new ArrayList<>();
                        pressedPartialCombination.add(InputConstants.Type.MOUSE.getOrCreate(input.button()));
                        break;
                    }
                }
            }

            if (commandManager != null && pressedPartialCombination != null &&
                    pressedPartialCombination.contains(InputConstants.Type.MOUSE.getOrCreate(input.button()))) {
                ci.cancel();
            }

            if (action == InputConstants.RELEASE) {
                if (pressedPartialCombination != null &&
                        pressedPartialCombination.contains(InputConstants.Type.MOUSE.getOrCreate(input.button()))) {
                    // after releasing the first key of the combination, search for it in commandManager.data.commands.keybinds:
                    for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                        if (command.keybinds != null && !command.keybinds.isEmptyOrNull() && command.keybinds.keys.equals(pressedPartialCombination)) {
                            LOGGER.info("Combination released/pressed: " + command.command);
                            SavedCommandManager.sendCommandAndInsertVariables(command, minecraft.gui.screen());
                        }
                    }
                    if (pressedPartialCombination.getLast().equals(InputConstants.Type.MOUSE.getOrCreate(input.button())) &&
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
