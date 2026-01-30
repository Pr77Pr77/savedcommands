package de.aliba2468pr77pr77.savedcommands.mixin.client;

import de.aliba2468pr77pr77.savedcommands.EditCommandScreen;
import de.aliba2468pr77pr77.savedcommands.SavedCommandManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
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

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen EditScreen && EditScreen.keybindSetting) {
            return;
        }
        if (MinecraftClient.getInstance().currentScreen instanceof KeybindsScreen EditScreen && EditScreen.selectedKeyBinding != null) {
            return;
        }

        if (commandManager != null) {
            if (action == GLFW.GLFW_PRESS) {
                for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                    if (command.keybinds != null && !command.keybinds.isEmptyOrNull()) {
                        if (pressedPartialCombination != null && pressedPartialCombination.size() < command.keybinds.keybindCode.size() && command.keybinds.keybindCode.get(pressedPartialCombination.size()) == input.key()) {
                            pressedPartialCombination.add(InputUtil.Type.valueOf("KEYSYM").createFromCode(input.key()));
                            break;
                        }
                        if (command.keybinds.keybindCode.getFirst() == input.key() && pressedPartialCombination == null) {
                            pressedPartialCombination = new ArrayList<>();
                            pressedPartialCombination.add(InputUtil.Type.valueOf("KEYSYM").createFromCode(input.key()));
                            break;
                        }
                    }
                }
            }

            if (commandManager != null && pressedPartialCombination != null &&
                    pressedPartialCombination.contains(InputUtil.Type.valueOf("KEYSYM").createFromCode(input.key()))) {
                ci.cancel();
            }

            if (action == GLFW.GLFW_RELEASE) {
                if (pressedPartialCombination != null &&
                        pressedPartialCombination.contains(InputUtil.Type.valueOf("KEYSYM").createFromCode(input.key()))) {
                    // after releasing the first key of the combination, search for it in commandManager.data.commands.keybinds:
                    for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                        if (command.keybinds != null && !command.keybinds.isEmptyOrNull() && command.keybinds.toKeys().equals(pressedPartialCombination)) {
                            LOGGER.info("Combination released/pressed: " + command.command);
                            SavedCommandManager.sendCommandAndInsertVariables(command, client.currentScreen);
                        }
                    }
                    pressedPartialCombination.remove(InputUtil.Type.valueOf("KEYSYM").createFromCode(input.key()));
                    if (pressedPartialCombination.isEmpty()) {
                        pressedPartialCombination = null;
                    }
                }
            }
        }
    }
}
