package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class SavedCommandsClient implements ClientModInitializer {
    public static KeyMapping OpenCommandScreen;
    public static SavedCommandManager commandManager;
    public static List<InputConstants.Key> pressedPartialCombination;

    final static char VariablePlaceholder = '\uE177'; // Added in front of the abbreviation to ensure that it is a variable

    @Override
    public void onInitializeClient() {
        KeyMapping.Category SavedcommandsKeyindCategory = new KeyMapping.Category(Identifier.fromNamespaceAndPath("savedcommands", "savedcommands"));
        OpenCommandScreen = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.savedcommands.opencommandscreen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Y, // Y on QWERTY and Z on QWERTZ (key next to T)
                SavedcommandsKeyindCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OpenCommandScreen.consumeClick()) {
                client.execute(() -> {
                    if (client.screen == null) {
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