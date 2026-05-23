package de.aliba2468pr77pr77.savedcommands.share;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.NarratableEntryOfString;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;

public class ShareCommandsSelectionScreen extends SavedCommandsScreen {
    SharePlayerSelectionScreen playerSelectionScreen;

    private Button cancelButton;
    private Button selectPlayersButton;

    protected ShareCommandsSelectionScreen(SharePlayerSelectionScreen playerSelectionScreen) {
        super(Component.translatable("screen.savedcommands.share.selectmorecommands"));
        this.playerSelectionScreen = playerSelectionScreen;
    }

    @Override
    protected void init() {
        super.init();

        this.removeWidget(this.addButton);

        this.selectPlayersButton = Button.builder(Component.translatable("gui.continue"), b -> {
            assert minecraft != null;
            minecraft.setScreen(playerSelectionScreen);
        }).bounds(width - 20 - 75 - 5 - 75, 20, 75, 20).build();
        this.addRenderableWidget(this.selectPlayersButton);

        this.cancelButton = Button.builder(Component.translatable("gui.cancel"), b -> {
            assert minecraft != null;
            minecraft.setScreen(new SavedCommandsScreen());
        }).bounds(width - 20 - 75, 20, 75, 20).build();
        this.addRenderableWidget(this.cancelButton);

        this.SearchBar.setSize(width - 20 - 75 - 5 - 75 - 5 - 20, 20);
        this.SearchBar.setHint(Component.translatable("screen.savedcommands.share.searchcommands"));
    }

    @Override
    protected CommandList createCommandList(Minecraft client, int width, int height, int top, int itemHeight) {
        return new ShareCommandList(client, width, height, top, itemHeight);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.isEscape() && this.shouldCloseOnEsc()) {
            assert minecraft != null;
            minecraft.setScreen(new SavedCommandsScreen());
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        int textWidth = this.font.width(title);
        context.drawString(
                this.font,
                title,
                (this.width - textWidth) / 2,
                10,
                0xFFFFFFFF,
                false
        );
        super.render(context, mouseX, mouseY, delta);
    }

    protected class ShareCommandList extends CommandList {

        public ShareCommandList(Minecraft client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
        }

        @Override
        protected CommandEntry createCommandEntry(String command, String name, int indexDataList) {
            return new ShareCommandEntry(command, name, indexDataList);
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
            public void renderContent(@NotNull GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                super.renderContent(context, mouseX, mouseY, hovered, deltaTicks);

                checkbox.setPosition(getContentX() + getContentWidth() - 30, getContentY() + (getContentHeight() - 17) / 2);
                checkbox.render(context, mouseX, mouseY, deltaTicks);
            }

            @Override
            protected void buttonExtractActions(@NotNull GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                if (hovered) {
                    context.requestCursor(CursorTypes.POINTING_HAND);
                }
            }

            @Override
            public boolean mouseClicked(@NotNull MouseButtonEvent click, boolean doubled) {
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
            public @NotNull List<? extends NarratableEntry> narratables() {
                if (name != null) {
                    return List.of(NarratableEntryOfString(name), NarratableEntryOfString(command));
                } else {
                    return List.of(NarratableEntryOfString(command));
                }
            }

            @Override
            public @NotNull List<? extends GuiEventListener> children() {
                return List.of(checkbox);
            }
        }
    }
}
