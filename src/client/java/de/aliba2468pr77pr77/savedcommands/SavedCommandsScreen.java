package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.aliba2468pr77pr77.savedcommands.share.SharePlayerSelectionScreen;
import de.aliba2468pr77pr77.savedcommands.share.ViewerSaverScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;
import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.*;

public class SavedCommandsScreen extends Screen {
    public TextFieldPlaceholderAlways SearchBar;
    protected Button addButton;
    public IconButton notificationButton;
    public IconButton settingsButton;
    CommandList commandList;

    boolean otherPlayersOnServer; // disables/enables the share buttons

    int listWidth;
    int listHeight;
    int listTop = 50;
    int itemHeight = 30;

    CommandSuggestions CommandSuggestor;

    @Nullable GuiEventListener focused;

    public boolean showReceivedCommands = true;

    public SavedCommandsScreen() {
        super(Component.translatable("screen.savedcommands.commandscreentitle"));
    }

    public SavedCommandsScreen(boolean showReceivedCommands) {
        super(Component.translatable("screen.savedcommands.commandscreentitle"));
        this.showReceivedCommands = showReceivedCommands;
    }

    protected SavedCommandsScreen(Component title) {
        super(title);
    }

    protected void init() {
        if (!sharingManager.receivedCommandsByPlayerName.isEmpty()) {
            notificationButton = new IconButton(20, 20, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/notification.png"),
                    button -> minecraft.setScreen(new ViewerSaverScreen(this)), Component.translatable("screen.savedcommands.share.notificationbutton"));
            this.addRenderableWidget(notificationButton);
            if (this.SearchBar == null) {
                this.SearchBar = new TextFieldPlaceholderAlways(this.minecraft.font, 20 + 20 + 5, 20, this.width - 40 - 22 - 20 - 5 - 20 - 5, 20, Component.translatable("screen.savedcommands.searchsavebar"));
            } else {
                this.SearchBar.setPosition(20 + 20 + 5, 20);
                this.SearchBar.setSize(this.width - 40 - 22 - 20 - 5 - 20 - 5, 20);
            }
        } else {
            if (this.SearchBar == null) {
                this.SearchBar = new TextFieldPlaceholderAlways(this.minecraft.font, 20, 20, this.width - 40 - 22 - 20 - 5, 20, Component.translatable("screen.savedcommands.searchsavebar"));
            } else {
                this.SearchBar.setPosition(20, 20);
                this.SearchBar.setSize(this.width - 40 - 22 - 20 - 5, 20);
            }
        }
        this.SearchBar.setMaxLength(256);
        this.SearchBar.setBordered(true);
        this.SearchBar.setResponder(this::updateSearch);
        this.SearchBar.setCanLoseFocus(false);
        this.SearchBar.setHint(Component.translatable("screen.savedcommands.searchsavebar"));
        this.addRenderableWidget(this.SearchBar);

        addButton = Button.builder(
                        Component.literal("+"),
                        b -> {
                            LOGGER.info("Add command button clicked!");
                            addCommand();
                        }
                ).bounds(20 + this.width - 40 - 20 - 5 - 20, 20, 20, 20)
                .createNarration((componentSupplier) -> Component.translatable("screen.savedcommands.savenewcommandbutton")).build();
        this.addRenderableWidget(this.addButton);

        settingsButton = new IconButton(20 + this.width - 40 - 20, 20, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/settings.png"),
                button -> minecraft.setScreen(new PerWorldSettingsScreen(this)), Component.translatable("screen.savedcommands.settings"));
        this.addRenderableWidget(settingsButton);
        otherPlayersOnServer = minecraft.getConnection() != null
                && minecraft.getConnection().getOnlinePlayers().size() > 1;

        listWidth = this.width;
        listHeight = this.height - 50;

        this.commandList = createCommandList(this.minecraft, listWidth, listHeight, listTop, itemHeight);

        updateSearch(SearchBar.getValue());

        this.addWidget(this.commandList);

        CommandSuggestor = new CommandSuggestions(minecraft, this, SearchBar, font, false, false, 1, 10, false, 0xD8000000);
        CommandSuggestor.setAllowHiding(false);
        CommandSuggestor.setAllowSuggestions(true);
        CommandSuggestor.updateCommandInfo();

        if (!sharingManager.receivedCommandsByPlayerName.isEmpty() && showReceivedCommands) {
            minecraft.setScreen(new ViewerSaverScreen(this));
            showReceivedCommands = false;
        }
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> @NonNull T addRenderableWidget(final @NonNull T widget) {
        return super.addRenderableWidget(widget);
    }

    public void setOtherPlayersOnServer(boolean otherPlayersOnServer) {
        this.otherPlayersOnServer = otherPlayersOnServer;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (this.focused != focused) {
            if (this.focused != null) {
                this.focused.setFocused(false);
                if (this.focused.isFocused()) {
                    return; // Return if still focused
                }
            }
            if (focused != null) {
                focused.setFocused(true);
            }
            this.focused = focused;
        }
    }

    private void addCommandRightPlace(SavedCommandManager.CommandData data, int indexDataList, List<CommandList.BaseEntry> newList) {
        String commandBase;
        if (data.command.contains(" ")) {
            commandBase = data.command.substring(0, data.command.indexOf(" "));
        } else {
            commandBase = data.command;
        }
        for (int i = 0; i < newList.size(); i++) {
            if (newList.get(i) instanceof CommandList.CategoryTitleEntry TitleEntry) {
                if (Objects.equals(TitleEntry.categoryTitle, commandBase)) {
                    newList.add(i + 1, commandList.createCommandEntry(data.command, data.name, indexDataList));
                    return;
                }
            }
        }
        // If the for loop didn't find the category, create it!
        newList.addFirst(new CommandList.CategoryTitleEntry(commandBase));
        newList.add(1, commandList.createCommandEntry(data.command, data.name, indexDataList));
    }

    public void updateSearch(String search) {
        if (search.isEmpty()) {
            addButton.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.savenewcommandbutton")));
        } else {
            addButton.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.savecommandfromsearchbutton", search)));
        }
        List<CommandList.BaseEntry> newList = new ArrayList<>();
        for (int i = 0; i < commandManager.data.commands.size(); i++) {
            SavedCommandManager.CommandData data = commandManager.data.commands.get(i);
            if (data.command.toLowerCase().contains(search.toLowerCase()) || (data.name != null && data.name.toLowerCase().contains(search.toLowerCase()))) {
                addCommandRightPlace(data, i, newList);
            }
        }
        commandList.replaceEntries(newList);
        for (CommandList.BaseEntry Entry : commandList.children()) {
            if (Entry instanceof CommandList.CommandEntry commandEntry) {
                commandEntry.init();
            }
        }
        if (CommandSuggestor != null) {
            CommandSuggestor.updateCommandInfo();
        }
    }

    public void resize(int width, int height) {
        if (CommandSuggestor != null) {
            CommandSuggestor.updateCommandInfo();
        }
        super.resize(width, height);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.isConfirmation()) {
            addCommand();
        }
        if (CommandSuggestor.keyPressed(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (CommandSuggestor.mouseScrolled(verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if (CommandSuggestor.mouseClicked(click)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent click, double offsetX, double offsetY) {
        if (commandList.isHovered()) {
            return commandList.mouseDragged(click, offsetX, offsetY);
        } else {
            return super.mouseDragged(click, offsetX, offsetY);
        }
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent click) {
        if (commandList.isHovered()) {
            commandList.onRelease(click);
            return true;
        } else {
            return super.mouseReleased(click);
        }
    }

    protected void setInitialFocus() {
        this.setInitialFocus(this.SearchBar);
    }

    @Override
    public void render(@NonNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.commandList.render(context, mouseX, mouseY, delta);

        CommandSuggestor.render(context, mouseX, mouseY);

        context.fill(SearchBar.getX(), SearchBar.getY(), SearchBar.getX() + SearchBar.getWidth(), SearchBar.getY() + SearchBar.getHeight(), 0x44000000);

        super.render(context, mouseX, mouseY, delta);
    }

    public static String shortenTextIfNeeded(String text, int availableWidth, ChatFormatting formatting) {
        Minecraft client = Minecraft.getInstance();
        if (client.font.width(Component.literal(text).withStyle(formatting)) <= availableWidth) {
            return text;
        }
        StringBuilder mutableString = new StringBuilder(text);
        mutableString.delete(mutableString.length() - 1, mutableString.length());
        mutableString.append("…");
        while (client.font.width(Component.literal(mutableString.toString()).withStyle(formatting)) > availableWidth) {
            mutableString.delete(mutableString.length() - 2, mutableString.length() - 1);
        }
        return mutableString.toString();
    }

    public void addCommand() {
        minecraft.setScreen(new EditCommandScreen(this, commandManager.addCommand(SearchBar.getValue(), null)));
        SearchBar.setValue("");
    }


    protected CommandList createCommandList(Minecraft client, int width, int height, int top, int itemHeight) {
        return new CommandList(client, width, height, top, itemHeight);
    }

    protected class CommandList extends ContainerObjectSelectionList<CommandList.BaseEntry> {
        public CommandList(Minecraft client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
            this.clearEntries();
        }

        public void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        protected int scrollBarX() {
            return this.width - 6;
        }

        public static class BaseEntry extends ContainerObjectSelectionList.Entry<BaseEntry> {
            @Override
            public void renderContent(@NonNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float a) {
                // Implementation in CommandEntry and CategoryTitleEntry!
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of();
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of();
            }
        }

        protected CommandEntry createCommandEntry(String command, String name, int indexDataList) {
            return new CommandEntry(command, name, indexDataList);
        }

        public class CommandEntry extends BaseEntry {
            protected String command;
            protected String name;
            protected int indexDataList;
            IconButton deleteButton;
            IconButton editButton;
            IconButton shareButton;

            public CommandEntry(String command, String name, int indexDataList) {
                this.command = command;
                this.name = name;
                this.indexDataList = indexDataList;
            }

            protected void init() {
                int y = this.getY() + 2;
                int entryHeight = this.getHeight() - 4;
                int entryWidth = this.getWidth();

                deleteButton = new IconButton(entryWidth - 20, y + (entryHeight - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/trash_can.png"), button -> {
                    LOGGER.info("Clicked on delete " + this.command + " index " + indexDataList);
                    commandManager.removeCommand(indexDataList);
                    updateSearch(SearchBar.getValue());
                }, Component.translatable("selectWorld.deleteButton"));

                editButton = new IconButton(entryWidth - 50, y + (entryHeight - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/edit.png"), button -> {
                    LOGGER.info("Clicked on edit " + this.command + " index " + indexDataList);
                    minecraft.setScreen(new EditCommandScreen(minecraft.screen, commandManager.data.commands.get(indexDataList)));
                }, Component.translatable("selectWorld.edit"));

                shareButton = new IconButton(entryWidth - 80, y + (entryHeight - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/share.png"), button -> {
                    LOGGER.info("Clicked on share " + this.command + " index " + indexDataList);
                    minecraft.setScreen(new SharePlayerSelectionScreen(minecraft.screen, new ArrayList<>(List.of(commandManager.data.commands.get(indexDataList)))));
                }, Component.translatable("screen.savedcommands.share"));
                shareButton.active = otherPlayersOnServer;
            }

            @Override
            public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
                if (super.mouseClicked(click, doubled)) {
                    return true;
                }
                if (shareButton.isHovered()) { // Ignore clicks on share if the button is not active.
                    return false;
                }
                if (click.button() != 0) {
                    return false;
                }
                LOGGER.info("Clicked on command " + this.command);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                minecraft.setScreen(null);
                SavedCommandManager.sendCommandAndInsertVariables(commandManager.data.commands.get(indexDataList), null);
                return true;
            }

            @Override
            public void renderContent(@NonNull GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                int x = this.getX() + 2;
                int y = this.getY() + 2;
                int entryHeight = this.getHeight() - 4;
                int entryWidth = this.getWidth();

                context.fill(x, y, x + entryWidth, y + entryHeight, 0x44000000);

                if (this.name == null) {
                    int textX = x + 3;
                    int textY = y + (entryHeight - minecraft.font.lineHeight) / 2 + 1;

                    context.drawString(minecraft.font, Component.literal(shortenTextIfNeeded(this.command, entryWidth - 90, ChatFormatting.RESET)), textX, textY, 0xFFFFFFFF, true);
                } else {
                    int fontHeight = minecraft.font.lineHeight;
                    int textX = x + 3;
                    int textY = y + (entryHeight - minecraft.font.lineHeight * 2 - 2) / 2 + 1;

                    context.drawString(minecraft.font, Component.literal(shortenTextIfNeeded(this.name, entryWidth - 90, ChatFormatting.RESET)), textX, textY, 0xFFFFFFFF, true);

                    textY += fontHeight + 2;

                    context.drawString(minecraft.font, Component.literal(shortenTextIfNeeded(this.command, entryWidth - 90, ChatFormatting.RESET)), textX, textY, 0xFFBBBBBB, true);
                }

                buttonExtractActions(context, mouseX, mouseY, hovered, deltaTicks);
            }

            protected void buttonExtractActions(@NonNull GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                int y = this.getY() + 2;
                int entryHeight = this.getHeight() - 4;
                int entryWidth = this.getWidth();

                shareButton.active = otherPlayersOnServer;
                deleteButton.setPosition(entryWidth - 20, y + (entryHeight - 20) / 2);
                deleteButton.render(context, mouseX, mouseY, deltaTicks);

                editButton.setPosition(entryWidth - 50, y + (entryHeight - 20) / 2);
                editButton.render(context, mouseX, mouseY, deltaTicks);

                shareButton.setPosition(entryWidth - 80, y + (entryHeight - 20) / 2);
                shareButton.render(context, mouseX, mouseY, deltaTicks);

                if (hovered && !deleteButton.isHovered()
                        && !editButton.isHovered()
                        && !shareButton.isHovered()) {
                    context.setTooltipForNextFrame(command.charAt(0) == '/' ?
                            Component.translatable("screen.savedcommands.sendcommandfromlist") : Component.translatable("screen.savedcommands.sendchatfromlist"), mouseX, mouseY);
                    context.requestCursor(CursorTypes.POINTING_HAND);
                }
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                if (name != null) {
                    return List.of(NarratableEntryOfString(name), NarratableEntryOfString(command), shareButton, editButton, deleteButton);
                } else {
                    return List.of(NarratableEntryOfString(command), shareButton, editButton, deleteButton);
                }

            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(shareButton, editButton, deleteButton);
            }
        }

        public static class CategoryTitleEntry extends BaseEntry {
            private final String categoryTitle;

            public CategoryTitleEntry(String categoryTitle) {
                this.categoryTitle = categoryTitle;
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
                if (click.button() == 0) {
                    LOGGER.info("Clicked on category title " + this.categoryTitle);
                    return true;
                }
                return false;
            }

            @Override
            public void renderContent(@NonNull GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                Minecraft minecraft = Minecraft.getInstance();

                int textX = this.getX() + 3;
                int textY = this.getY() + this.getHeight() - minecraft.font.lineHeight - 2;

                context.drawString(minecraft.font, Component.literal(shortenTextIfNeeded(this.categoryTitle, this.getWidth() - 10, ChatFormatting.BOLD)).withStyle(ChatFormatting.BOLD), textX, textY, 0xFFFFFFFF, true);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfString(categoryTitle));
            }
        }
    }
}