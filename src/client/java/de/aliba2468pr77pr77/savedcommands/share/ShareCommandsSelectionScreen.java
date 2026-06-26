package de.aliba2468pr77pr77.savedcommands.share;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.aliba2468pr77pr77.savedcommands.SavedCommandManager;
import de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.NarratableEntryOfString;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;

public class ShareCommandsSelectionScreen extends SavedCommandsScreen {
    SharePlayerSelectionScreen playerSelectionScreen;

    private Button cancelButton;
    private Button selectPlayersButton;

    protected ShareCommandsSelectionScreen(SharePlayerSelectionScreen playerSelectionScreen) {
        this.playerSelectionScreen = playerSelectionScreen;
        super(Component.translatable("screen.savedcommands.share.selectmorecommands"));
        showReceivedCommands = false;
    }

    @Override
    protected void init() {
        super.init();

        this.removeWidget(this.addButton);
        this.removeWidget(this.notificationButton);
        this.removeWidget(this.settingsButton);

        if (this.selectPlayersButton == null) {
            this.selectPlayersButton = Button.builder(CommonComponents.GUI_CONTINUE, _ -> minecraft.gui.setScreen(playerSelectionScreen)).bounds(width - 20 - 75 - 5 - 75, 20, 75, 20).build();
        } else {
            this.selectPlayersButton.setPosition(width - 20 - 75 - 5 - 75, 20);
        }
        this.addRenderableWidget(this.selectPlayersButton);

        this.cancelButton = Button.builder(CommonComponents.GUI_CANCEL, _ -> minecraft.gui.setScreen(playerSelectionScreen.parent)).bounds(width - 20 - 75, 20, 75, 20).build();
        this.addRenderableWidget(this.cancelButton);

        this.searchBar.setSize(width - 20 - 75 - 5 - 75 - 5 - 20, 20);
        this.searchBar.setPosition(20, 20);
        this.searchBar.setHint(Component.translatable("screen.savedcommands.share.searchcommands"));
    }

    @Override
    public void repositionElements() {
        super.repositionElements();

        this.selectPlayersButton.setPosition(width - 20 - 75 - 5 - 75, 20);
        this.cancelButton.setPosition(width - 20 - 75, 20);

        this.searchBar.setSize(width - 20 - 75 - 5 - 75 - 5 - 20, 20);
        this.searchBar.setPosition(20, 20);
    }

    @Override
    protected CommandList createCommandList(Minecraft client, int width, int height, int top, int itemHeight) {
        return new ShareCommandList(client, width, height, top, itemHeight);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(new SavedCommandsScreen(false));
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int textWidth = this.font.width(title);
        context.text(
                this.font,
                title,
                (this.width - textWidth) / 2,
                10,
                0xFFFFFFFF,
                false
        );
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    protected class ShareCommandList extends CommandList {

        public ShareCommandList(Minecraft client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
        }

        @Override
        protected CommandEntry createCommandEntry(String command, String name, int indexDataList) {
            return new ShareCommandEntry(command, name, indexDataList);
        }

        @Override
        protected CategoryTitleEntry createCategoryTitleEntry(SavedCommandManager.SavedCommandsData.CustomCategory customCategory) {
            CategoryTitleEntry categoryTitleEntry = new CategoryTitleEntry(customCategory);
            categoryTitleEntry.buttonExtraction = false;
            return categoryTitleEntry;
        }

        protected class ShareCommandEntry extends CommandEntry {
            private Checkbox checkbox;

            public ShareCommandEntry(String command, String name, int indexDataList) {
                super(command, name, indexDataList);
            }

            @Override
            protected void init() {
                checkbox = Checkbox.builder(Component.empty(), minecraft.font).pos(getContentX() + getContentWidth() - 30, getContentY() + (getContentHeight() - 17) / 2)
                        .selected(playerSelectionScreen.commands.contains(commandManager.data.commands.get(indexDataList))).build();
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                super.extractContent(context, mouseX, mouseY, hovered, deltaTicks);

                checkbox.setPosition(getContentX() + getContentWidth() - 30, getContentY() + (getContentHeight() - 17) / 2);
                checkbox.extractContents(context, mouseX, mouseY, deltaTicks);
            }

            @Override
            protected void buttonExtractActions(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                if (hovered) {
                    context.requestCursor(CursorTypes.POINTING_HAND);
                }
            }

            @Override
            public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
                checkbox.playDownSound(Minecraft.getInstance().getSoundManager());
                checkbox.onClick(click, doubled);

                if (checkbox.selected()) {
                    playerSelectionScreen.commands.add(commandManager.data.commands.get(indexDataList));
                } else {
                    playerSelectionScreen.commands.remove(commandManager.data.commands.get(indexDataList));
                }

                selectPlayersButton.active = !playerSelectionScreen.commands.isEmpty();

                return true;
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                if (name != null) {
                    return List.of(NarratableEntryOfString(name), NarratableEntryOfString(command));
                } else {
                    return List.of(NarratableEntryOfString(command));
                }
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(checkbox);
            }
        }
    }
}
