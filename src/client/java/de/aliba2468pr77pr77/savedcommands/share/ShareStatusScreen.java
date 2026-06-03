package de.aliba2468pr77pr77.savedcommands.share;

import de.aliba2468pr77pr77.savedcommands.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Supplier;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.NarratableEntryOfString;

public class ShareStatusScreen extends PopupScreen {
    Button closeButton;

    private ShareStatusList shareStatusList;

    ShareStatusScreen(Screen grandParent) {
        super(Component.translatable("screen.savedcommands.share.status"), grandParent, 300, 320);
    }

    @Override
    protected void init() {
        super.init();
        shareStatusList = new ShareStatusList(minecraft, popupW - 6 * 2, popupH - 35 - 25, popupY + 25, 36);
        shareStatusList.setX(popupX + 6);
        shareStatusList.addPlayers(SavedCommandsClient.sharingManager.recipients);
        this.addRenderableWidget(this.shareStatusList);

        this.closeButton = Button.builder(CommonComponents.GUI_CANCEL, button -> exit()).bounds((this.width - 100) / 2, popupY + popupH - 20 - 10, 100, 20).build();
        this.addRenderableWidget(this.closeButton);
    }

    @Override
    protected void exit() {
        if (SavedCommandsClient.sharingManager.currentTimeout != null && !SavedCommandsClient.sharingManager.currentTimeout.isDone()) {
            SavedCommandsClient.sharingManager.currentTimeout.cancel(false);
        }
        SavedCommandsClient.sharingManager.recipients.clear();
        SavedCommandsClient.sharingManager.commands.clear();
        super.exit();
    }

    private static class ShareStatusList extends ContainerObjectSelectionList<ShareStatusList.PlayerEntry> {
        public ShareStatusList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
            super(minecraft, width, height, top, itemHeight);
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        protected int scrollBarX() {
            return this.getRowRight() - 1;
        }

        @Override
        protected void renderListBackground(@NonNull GuiGraphics graphics) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x46000000);
        }

        private void addPlayers(Map<PlayerInfo, SharingManager.States> players) {
            clearEntries();
            for (Map.Entry<PlayerInfo, SharingManager.States> player : players.entrySet()) {
                addEntry(new PlayerEntry(player));
            }
        }

        public class PlayerEntry extends Entry<ShareStatusList.PlayerEntry> {
            Map.Entry<PlayerInfo, SharingManager.States> player;


            PlayerEntry(Map.Entry<PlayerInfo, SharingManager.States> player) {
                this.player = player;
            }

            @Override
            public void renderContent(@NonNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentX() + getContentWidth(), getContentY() + getContentHeight(), 0x10FFFFFF);

                Supplier<PlayerSkin> skinGetter = player.getKey()::getSkin;
                PlayerFaceRenderer.draw(graphics, skinGetter.get(), getContentX() + 4, getContentY() + (getContentHeight() - 24) / 2, 24);
                graphics.drawString(minecraft.font, player.getKey().getProfile().name(), getContentX() + 4 + 24 + 4, getContentY() + (getContentHeight() - 8) / 2, 0xFFFFFFFF);

                int textWidth = minecraft.font.width(player.getValue().getMessage());
                graphics.drawString(
                        minecraft.font,
                        player.getValue().getMessage(),
                        getContentX() + getContentWidth() - textWidth - 4,
                        getContentY() + (getContentHeight() - 8) / 2,
                        0xFFFFFFFF,
                        false
                );
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfString(player.getKey().getProfile().name()));
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of();
            }
        }
    }
}
