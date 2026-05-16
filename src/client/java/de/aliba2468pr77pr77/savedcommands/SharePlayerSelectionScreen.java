package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Supplier;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.NarratableEntryOfString;

public class SharePlayerSelectionScreen extends PopupScreen {
    private Button cancelButton;
    private Button sendButton;
    private Button selectMoreCommandsButton;

    public TextFieldPlaceholderAlways searchBar;

    List<SavedCommandManager.CommandData> commands;
    public List<PlayerInfo> chosenPlayers = new ArrayList<>();

    private ShareSelectionList shareSelectionList;

    SharePlayerSelectionScreen(Screen parent, List<SavedCommandManager.CommandData> commands) {
        super(Component.translatable("screen.savedcommands.share.selectplayers"), parent, 300, 320);
        this.commands = commands;
    }

    @Override
    protected void init() {
        super.init();

        this.searchBar = new TextFieldPlaceholderAlways(this.minecraft.font, popupX + 10, popupY + 22, popupW - 135 - 20 - 5, 20, Component.translatable("screen.savedcommands.share.searchplayers"));
        this.searchBar.setMaxLength(16);
        this.searchBar.setBordered(true);
        this.searchBar.setResponder(this::updateSearch);
        this.searchBar.setHint(Component.translatable("screen.savedcommands.share.searchplayers"));
        this.addRenderableWidget(this.searchBar);

        this.selectMoreCommandsButton = Button.builder(Component.translatable("screen.savedcommands.share.selectmorecommands"), b -> exit(exitTypes.SELECT_MORE_COMMANDS)).bounds(popupX + popupW - 135 - 10, popupY + 22, 135, 20).build();
        this.addRenderableWidget(this.selectMoreCommandsButton);

        shareSelectionList = new ShareSelectionList(minecraft, popupW - 6 * 2, popupH - 35 - 35 - 25, popupY + 35 + 25, 36);
        shareSelectionList.setX(popupX + 6);
        shareSelectionList.addPlayers(getOtherPlayers());
        this.addRenderableWidget(this.shareSelectionList);

        this.sendButton = Button.builder(Component.translatable("screen.savedcommands.send"), b -> exit(exitTypes.SEND)).bounds(popupX + (popupW - 100 - 5 - 100) / 2, popupY + popupH - 20 - 10, 100, 20).build();
        sendButton.active = !chosenPlayers.isEmpty();
        this.addRenderableWidget(this.sendButton);

        this.cancelButton = Button.builder(Component.translatable("gui.cancel"), b -> exit()).bounds(popupX + (popupW - 100 + 5 + 100) / 2, popupY + popupH - 20 - 10, 100, 20).build();
        this.addRenderableWidget(this.cancelButton);
    }

    public void updateSearch(String search) {
        Collection<PlayerInfo> players = getOtherPlayers();
        players.removeIf((player) -> !player.getProfile().name().toLowerCase().contains(search.toLowerCase()));
        shareSelectionList.addPlayers(players);
    }

    public Collection<PlayerInfo> getOtherPlayers() {
        assert minecraft.player != null;
        assert minecraft.getConnection() != null;
        Collection<PlayerInfo> players = new ArrayList<>(Objects.requireNonNull(minecraft.getConnection()).getOnlinePlayers());
        players.remove(Objects.requireNonNull(minecraft.getConnection()).getPlayerInfo(minecraft.player.getUUID()));
        return players;
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(searchBar);
    }

    public enum exitTypes {
        CANCEL,
        SEND,
        SELECT_MORE_COMMANDS
    }

    @Override
    void exit() {
        exit(exitTypes.CANCEL);
    }

    private void exit(exitTypes exitType) {
        switch (exitType) {
            case SEND:
                // TODO
                break;
            case SELECT_MORE_COMMANDS:
                minecraft.setScreen(new ShareCommandsSelectionScreen(this));
                break;
            case CANCEL:
                if (parent instanceof SavedCommandsScreen) {
                    minecraft.setScreen(new SavedCommandsScreen());
                } else {
                    minecraft.setScreen(parent);
                }
                break;
        }
    }

    private class ShareSelectionList extends ContainerObjectSelectionList<ShareSelectionList.PlayerEntry> {
        public ShareSelectionList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
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

        private void addPlayers(Collection<PlayerInfo> players) {
            clearEntries();
            for (PlayerInfo playerInfo : players) {
                super.addEntry(new PlayerEntry(this.minecraft, playerInfo, chosenPlayers.contains(playerInfo)));
                super.children().getLast().init();
            }
        }

        public class PlayerEntry extends ContainerObjectSelectionList.Entry<ShareSelectionList.PlayerEntry> {
            Minecraft minecraft;
            PlayerInfo playerInfo;

            private Checkbox checkbox;
            private final boolean checkedInitial;

            PlayerEntry(Minecraft minecraft, PlayerInfo playerInfo, boolean checked) {
                this.minecraft = minecraft;
                this.playerInfo = playerInfo;
                checkedInitial = checked;
            }

            void init() {
                checkbox = Checkbox.builder(Component.empty(), minecraft.font).pos(getContentX() + getContentWidth() - 30, getContentY() + (getContentHeight() - 17) / 2).selected(checkedInitial).build();
            }

            @Override
            public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
                checkbox.playDownSound(Minecraft.getInstance().getSoundManager());
                checkbox.onClick(event, doubleClick);

                if (checkbox.selected()) {
                    chosenPlayers.add(playerInfo);
                } else {
                    chosenPlayers.remove(playerInfo);
                }
                sendButton.active = !chosenPlayers.isEmpty();

                return true;
            }

            @Override
            public void renderContent(@NonNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentX() + getContentWidth(), getContentY() + getContentHeight(), 0x10FFFFFF);

                Supplier<PlayerSkin> skinGetter = playerInfo::getSkin;
                PlayerFaceRenderer.draw(graphics, skinGetter.get(), getContentX() + 4, getContentY() + (getContentHeight() - 24) / 2, 24);
                graphics.drawString(minecraft.font, playerInfo.getProfile().name(), getContentX() + 4 + 24 + 4, getContentY() + (getContentHeight() - 8) / 2, 0xFFFFFFFF);

                checkbox.setPosition(getContentX() + getContentWidth() - 30, getContentY() + (getContentHeight() - 17) / 2);
                checkbox.render(graphics, mouseX, mouseY, a);

                if (hovered) {
                    graphics.requestCursor(CursorTypes.POINTING_HAND);
                }
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfString(playerInfo.getProfile().name()));
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(checkbox);
            }
        }
    }
}
