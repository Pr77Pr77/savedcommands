package de.aliba2468pr77pr77.savedcommands;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

public class SavedCommandsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ChatScreen) {
                int w = 150, h = 20;
                int x = scaledWidth / 2 - w / 2;
                int y = 29;

                ButtonWidget dreamBtn = ButtonWidget.builder(
                        Text.translatable("button.savedcommands.opencommandscreen"),
                        b -> {
                            LOGGER.info("Open Command Screen Button pressed!");
                            client.setScreen(new SavedCommandsScreen());
                        }
                ).dimensions(x, y, w, h).build();

                Screens.getButtons(screen).add(dreamBtn);
            }
        });
	}
}