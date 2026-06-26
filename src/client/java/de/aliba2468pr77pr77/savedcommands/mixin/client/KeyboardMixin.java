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

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKey(long handle, int action, KeyEvent input, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null ||
                mc.gui.screen() instanceof EditCommandScreen editScreen && editScreen.keybindSetting ||
                mc.gui.screen() instanceof KeyBindsScreen keyBindsScreen && keyBindsScreen.selectedKey != null ||
                (mc.gui.screen() != null && (mc.gui.screen().getFocused() instanceof EditBox eb && eb.canConsumeInput() ||
                        mc.gui.screen().getFocused() instanceof MultiLineEditBox ||
                        mc.gui.screen() instanceof AbstractSignEditScreen ||
                        mc.gui.screen().getClass().getName().startsWith("fi.dy.masa")))) {
            return;
        }

        if (commandManager != null) {
            if (action == GLFW.GLFW_PRESS) {
                for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                    if (command.keybinds != null && !command.keybinds.isEmptyOrNull()) {
                        if (pressedPartialCombination != null && pressedPartialCombination.size() < command.keybinds.keybindCode.size() && command.keybinds.keybindCode.get(pressedPartialCombination.size()) == input.key()) {
                            pressedPartialCombination.add(InputConstants.Type.valueOf("KEYSYM").getOrCreate(input.key()));
                            break;
                        }
                        if (command.keybinds.keybindCode.getFirst() == input.key() && pressedPartialCombination == null) {
                            pressedPartialCombination = new ArrayList<>();
                            pressedPartialCombination.add(InputConstants.Type.valueOf("KEYSYM").getOrCreate(input.key()));
                            break;
                        }
                    }
                }
            }

            if (commandManager != null && pressedPartialCombination != null &&
                    pressedPartialCombination.contains(InputConstants.Type.valueOf("KEYSYM").getOrCreate(input.key()))) {
                ci.cancel();
            }

            if (action == GLFW.GLFW_RELEASE) {
                if (pressedPartialCombination != null &&
                        pressedPartialCombination.contains(InputConstants.Type.valueOf("KEYSYM").getOrCreate(input.key()))) {
                    // after releasing the first key of the combination, search for it in commandManager.data.commands.keybinds:
                    for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                        if (command.keybinds != null && !command.keybinds.isEmptyOrNull() && command.keybinds.toKeys().equals(pressedPartialCombination)) {
                            LOGGER.info("Combination released/pressed: " + command.command);
                            SavedCommandManager.sendCommandAndInsertVariables(command, minecraft.gui.screen());
                        }
                    }
                    pressedPartialCombination.remove(InputConstants.Type.valueOf("KEYSYM").getOrCreate(input.key()));
                    if (pressedPartialCombination.isEmpty()) {
                        pressedPartialCombination = null;
                    }
                }
            }
        }
    }
}
