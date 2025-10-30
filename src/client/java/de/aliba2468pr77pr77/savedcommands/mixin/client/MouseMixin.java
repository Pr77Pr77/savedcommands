package de.aliba2468pr77pr77.savedcommands.mixin.client;

import de.aliba2468pr77pr77.savedcommands.EditCommandScreen;
import de.aliba2468pr77pr77.savedcommands.SavedCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.pressedPartialCombination;
import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen) {
            return;
        }
        if (MinecraftClient.getInstance().currentScreen instanceof KeybindsScreen EditScreen && EditScreen.selectedKeyBinding != null) {
            return;
        }

        if (commandManager != null) {
            if (action == GLFW.GLFW_PRESS) {
                for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                    if (command.keybinds != null && !command.keybinds.isEmptyOrNull()) {
                        if (pressedPartialCombination != null && pressedPartialCombination.size() < command.keybinds.keybindCode.size() && command.keybinds.keybindCode.get(pressedPartialCombination.size()) == input.button()) {
                            pressedPartialCombination.add(InputUtil.Type.valueOf("MOUSE").createFromCode(input.button()));
                            break;
                        }
                        if (command.keybinds.keybindCode.getFirst() == input.button() && pressedPartialCombination == null) {
                            pressedPartialCombination = new ArrayList<>();
                            pressedPartialCombination.add(InputUtil.Type.valueOf("MOUSE").createFromCode(input.button()));
                            break;
                        }
                    }
                }
            }

            if (commandManager != null && pressedPartialCombination != null &&
                    pressedPartialCombination.contains(InputUtil.Type.valueOf("MOUSE").createFromCode(input.button()))) {
                ci.cancel();
            }

            if (action == GLFW.GLFW_RELEASE) {
                if (pressedPartialCombination != null &&
                        pressedPartialCombination.contains(InputUtil.Type.valueOf("MOUSE").createFromCode(input.button()))) {
                    // after releasing the first key of the combination, search for it in commandManager.data.commands.keybinds:
                    for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                        if (command.keybinds != null && !command.keybinds.isEmptyOrNull() && command.keybinds.toKeys().equals(pressedPartialCombination)) {
                            LOGGER.info("Combination released/pressed: " + command.command);
                            ClientPlayerEntity player = MinecraftClient.getInstance().player;
                            if (command.command.charAt(0) == '/') {
                                player.networkHandler.sendChatCommand(command.command.substring(1));
                            } else {
                                player.networkHandler.sendChatMessage(command.command);
                            }
                        }
                    }
                    pressedPartialCombination.remove(InputUtil.Type.valueOf("MOUSE").createFromCode(input.button()));
                    if (pressedPartialCombination.isEmpty()) {
                        pressedPartialCombination = null;
                    }
                }
            }
        }
    }
}
