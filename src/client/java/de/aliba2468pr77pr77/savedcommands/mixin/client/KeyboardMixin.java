package de.aliba2468pr77pr77.savedcommands.mixin.client;

import de.aliba2468pr77pr77.savedcommands.SavedCommandManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.pressedPartialCombination;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "onKey(JIIII)V", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;

        if (commandManager != null) {
            if (action == GLFW.GLFW_PRESS) {
                for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                    if (command.keybinds != null && !command.keybinds.isEmptyOrNull()) {
                        if (pressedPartialCombination != null && pressedPartialCombination.size() < command.keybinds.keybindCode.size() && command.keybinds.keybindCode.get(pressedPartialCombination.size()) == key) {
                            pressedPartialCombination.add(InputUtil.Type.valueOf("KEYSYM").createFromCode(key));
                        }
                        if (command.keybinds.keybindCode.getFirst() == key && pressedPartialCombination == null) {
                            pressedPartialCombination = new ArrayList<>();
                            pressedPartialCombination.add(InputUtil.Type.valueOf("KEYSYM").createFromCode(key));
                        }
                    }
                }
            }

            if (action == GLFW.GLFW_RELEASE) {
                if (pressedPartialCombination != null &&
                        pressedPartialCombination.contains(InputUtil.Type.valueOf("KEYSYM").createFromCode(key))) {
                    // after releasing the first key of the combination, search for it in commandManager.data.commands.keybinds:
                    for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                        if (command.keybinds != null && !command.keybinds.isEmptyOrNull() && command.keybinds.toKeys().equals(pressedPartialCombination)) {
                            LOGGER.info("Combination released/pressed: " + command.command);
                        }
                    }
                    pressedPartialCombination.remove(InputUtil.Type.valueOf("KEYSYM").createFromCode(key));
                    if (pressedPartialCombination.isEmpty()) {
                        pressedPartialCombination = null;
                    }
                }
            }
        }

        if (commandManager != null && pressedPartialCombination != null &&
                pressedPartialCombination.contains(InputUtil.Type.valueOf("KEYSYM").createFromCode(key)) &&
                pressedPartialCombination.getFirst() != InputUtil.Type.valueOf("KEYSYM").createFromCode(key)) {
            ci.cancel();
        }
    }
}
