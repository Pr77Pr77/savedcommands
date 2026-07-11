package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.InputConstants;
import de.aliba2468pr77pr77.savedcommands.share.SharingManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class SavedCommandsClient implements ClientModInitializer {
    public static KeyMapping OpenCommandScreen;
    public static SavedCommandManager commandManager;
    public static List<InputConstants.Key> pressedPartialCombination;

    final static char VariablePlaceholder = '\uE177'; // Added in front of the abbreviation to ensure that it is a variable

    public static SharingManager sharingManager;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category SavedcommandsKeyindCategory = new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath("savedcommands", "savedcommands"));
        OpenCommandScreen = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.savedcommands.opencommandscreen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Y, // Y on QWERTY and Z on QWERTZ (key next to T)
                SavedcommandsKeyindCategory
        ));

        SettingsManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OpenCommandScreen.consumeClick()) {
                client.execute(() -> {
                    if (client.screen == null) {
                        client.setScreen(new SavedCommandsScreen());
                    }
                });
            }
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (sharingManager != null) {
                return sharingManager.shareHandler(message.getString());
            } else {
                return true;
            }
        });
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, playerChatMessage, sender, bound, instant) -> {
            if (sharingManager != null) {
                if (sender != null) {
                    return sharingManager.shareHandler(message.getString(), sender.name());
                } else {
                    return sharingManager.shareHandler(message.getString());
                }
            } else {
                return true;
            }
        });

        ClientPlayConnectionEvents.JOIN.register((clientPacketListener, packetSender, minecraft) -> {
            commandManager = new SavedCommandManager();
            sharingManager = new SharingManager();
        });
    }

    public static NarratableEntry NarratableEntryOfString(String text) {
        if (text == null) {
            return null;
        }
        return NarratableEntryOfComponent(Component.literal(text));
    }

    public static NarratableEntry NarratableEntryOfComponent(Component text) {
        if (text == null) {
            return null;
        }
        return new NarratableEntry() {
            @Override
            public @NotNull NarrationPriority narrationPriority() {
                return NarrationPriority.HOVERED;
            }

            @Override
            public void updateNarration(NarrationElementOutput output) {
                output.add(NarratedElementType.TITLE, text);
            }
        };
    }
}