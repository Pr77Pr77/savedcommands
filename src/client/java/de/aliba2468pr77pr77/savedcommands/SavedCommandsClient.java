package de.aliba2468pr77pr77.savedcommands;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class SavedCommandsClient implements ClientModInitializer {
    public static KeyBinding OpenCommandScreen;
    public static SavedCommandManager commandManager;
    public static List<InputUtil.Key> pressedPartialCombination;

    final static char VariablePlaceholder = '\uE177'; // Added in front of the abbreviation to ensure that it is a variable

    @Override
    public void onInitializeClient() {
        KeyBinding.Category SavedcommandsKeyindCategory = new KeyBinding.Category(Identifier.of("savedcommands", "savedcommands"));
        OpenCommandScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.savedcommands.opencommandscreen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y, // Y on QWERTY and Z on QWERTZ (key next to T)
                SavedcommandsKeyindCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OpenCommandScreen.wasPressed()) {
                client.execute(() -> {
                    if (client.currentScreen == null) {
                        client.setScreen(new SavedCommandsScreen());
                    }
                });
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            commandManager = new SavedCommandManager();
        });
    }
}