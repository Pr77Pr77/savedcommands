package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.aliba2468pr77pr77.savedcommands.share.SharePlayerSelectionScreen;
import de.aliba2468pr77pr77.savedcommands.share.ViewerSaverScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import java.util.Optional;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;
import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.*;

public class SavedCommandsScreen extends Screen {
    public TextFieldPlaceholderAlways searchBar;
    protected IconButton addButton;
    public IconButton notificationButton;
    public IconButton settingsButton;
    CommandList commandList;

    boolean otherPlayersOnServer; // disables/enables the share buttons

    int listWidth;
    int listHeight;
    int listTop = 50;
    int itemHeight = 30;

    CommandSuggestions commandSuggestor;

    @Nullable GuiEventListener focused;

    public boolean showReceivedCommands = true;
    boolean manualCategories = false;

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
        manualCategories = SettingsManager.getCombinedWorldAndGlobal(commandManager).manualCategories;

        if (!sharingManager.receivedCommandsByPlayerName.isEmpty()) {
            notificationButton = new IconButton(20, 20, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/notification.png"),
                    _ -> minecraft.gui.setScreen(new ViewerSaverScreen(this)), Component.translatable("screen.savedcommands.share.notificationbutton"));
            this.addRenderableWidget(notificationButton);
            if (this.searchBar == null) {
                this.searchBar = new TextFieldPlaceholderAlways(this.minecraft.font, 20 + 20 + 5, 20, this.width - 40 - 22 - 20 - 5 - 20 - 5, 20, Component.translatable("screen.savedcommands.searchsavebar"));
            } else {
                this.searchBar.setPosition(20 + 20 + 5, 20);
                this.searchBar.setSize(this.width - 40 - 22 - 20 - 5 - 20 - 5, 20);
            }
        } else {
            if (this.searchBar == null) {
                this.searchBar = new TextFieldPlaceholderAlways(this.minecraft.font, 20, 20, this.width - 40 - 22 - 20 - 5, 20, Component.translatable("screen.savedcommands.searchsavebar"));
            } else {
                this.searchBar.setPosition(20, 20);
                this.searchBar.setSize(this.width - 40 - 22 - 20 - 5, 20);
            }
        }
        this.searchBar.setMaxLength(256);
        this.searchBar.setBordered(true);
        this.searchBar.setResponder(this::updateSearchAndScroll);
        this.searchBar.setCanLoseFocus(false);
        this.searchBar.setHint(Component.translatable("screen.savedcommands.searchsavebar"));
        this.addRenderableWidget(this.searchBar);

        addButton = new IconButton(20 + this.width - 40 - 20 - 5 - 20, 20, 20, 20,
                Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/save.png"),
                _ -> {
                    LOGGER.info("Add command button clicked!");
                    addCommand();
                },
                Component.translatable("screen.savedcommands.savenewcommandbutton"),
                Component.literal("+"),
                false
        );
        this.addRenderableWidget(this.addButton);

        settingsButton = new IconButton(20 + this.width - 40 - 20, 20, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/settings.png"),
                _ -> minecraft.gui.setScreen(new PerWorldSettingsScreen(this)), Component.translatable("screen.savedcommands.settings"));
        this.addRenderableWidget(settingsButton);

        otherPlayersOnServer = minecraft.getConnection() != null
                && minecraft.getConnection().getOnlinePlayers().size() > 1;

        listWidth = this.width;
        listHeight = this.height - 50;

        this.commandList = createCommandList(this.minecraft, listWidth, listHeight, listTop, itemHeight);

        updateSearch(searchBar.getValue());

        this.addRenderableWidget(this.commandList);

        commandSuggestor = new CommandSuggestions(minecraft, this, searchBar, font, false, false, 1, 10, false, 0xD8000000);
        commandSuggestor.setAllowHiding(false);
        commandSuggestor.setAllowSuggestions(true);
        commandSuggestor.updateCommandInfo();

        if (!sharingManager.receivedCommandsByPlayerName.isEmpty() && showReceivedCommands) {
            minecraft.gui.setScreen(new ViewerSaverScreen(this));
            showReceivedCommands = false;
        }
    }

    @Override
    public void repositionElements() {
        if (notificationButton != null) {
            this.searchBar.setPosition(20 + 20 + 5, 20);
            this.searchBar.setSize(this.width - 40 - 22 - 20 - 5 - 20 - 5, 20);
        } else {
            this.searchBar.setPosition(20, 20);
            this.searchBar.setSize(this.width - 40 - 22 - 20 - 5, 20);
        }

        if (addButton != null) {
            addButton.setPosition(20 + this.width - 40 - 20 - 5 - 20, 20);
        }
        if (settingsButton != null) {
            settingsButton.setPosition(20 + this.width - 40 - 20, 20);
        }

        listWidth = this.width;
        listHeight = this.height - 50;

        this.commandList.setRectangle(listWidth, listHeight, 0, listTop);
        this.commandList.repositionEntries();

        commandSuggestor.updateCommandInfo();
    }

    public void setOtherPlayersOnServer(boolean otherPlayersOnServer) {
        this.otherPlayersOnServer = otherPlayersOnServer;
    }

    public void addNotificationButton() {
        notificationButton = new IconButton(20, 20, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/notification.png"),
                _ -> Minecraft.getInstance().gui.setScreen(new ViewerSaverScreen(this)), Component.translatable("screen.savedcommands.share.notificationbutton"));
        addRenderableWidget(notificationButton);

        searchBar.setPosition(20 + 20 + 5, 20);
        searchBar.setSize(width - 40 - 22 - 20 - 5 - 20 - 5, 20);
    }

    public void removeNotificationButton() {
        removeWidget(notificationButton);
        notificationButton = null;

        this.searchBar.setPosition(20, 20);
        this.searchBar.setSize(this.width - 40 - 22 - 20 - 5, 20);
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
        if (manualCategories) {
            for (int i = 0; i < newList.size(); i++) {
                if (newList.get(i) instanceof CommandList.CategoryTitleEntry titleEntry) {
                    if ((titleEntry.customCategory != null && Objects.equals(titleEntry.customCategory.id, data.categoryId)) ||
                            (titleEntry.customCategory == null && data.categoryId == null)) {
                        newList.add(i + 1, commandList.createCommandEntry(data.command, data.name, indexDataList));
                        return;
                    }
                }
            }
            // If the for loop didn't find the category, create it!
            if (data.categoryId == null) {
                newList.addFirst(commandList.createCategoryTitleEntry((SavedCommandManager.SavedCommandsData.CustomCategory) null));
                newList.add(1, commandList.createCommandEntry(data.command, data.name, indexDataList));
            } else {
                if (commandManager.data.customCategories == null) {
                    commandManager.data.customCategories = new ArrayList<>();
                    newList.add(1, commandList.createCommandEntry(data.command, data.name, indexDataList));
                }
                Optional<SavedCommandManager.SavedCommandsData.CustomCategory> customCategory = commandManager.data.customCategories.stream()
                        .filter((category) -> category.id.equals(data.categoryId)).findFirst();
                customCategory.ifPresentOrElse(
                        found -> { // found
                            newList.addFirst(commandList.createCategoryTitleEntry(found));
                            newList.add(1, commandList.createCommandEntry(data.command, data.name, indexDataList));
                        },
                        () -> { // not found
                            data.categoryId = null; // clean up orphanated id
                            commandManager.saveAsync();
                            addCommandRightPlace(data, indexDataList, newList); // Run again to add it.
                        }
                );
            }
        } else {
            String commandBase;
            if (data.command.contains(" ")) {
                commandBase = data.command.substring(0, data.command.indexOf(" "));
            } else {
                commandBase = data.command;
            }
            for (int i = 0; i < newList.size(); i++) {
                if (newList.get(i) instanceof CommandList.CategoryTitleEntry titleEntry) {
                    if (Objects.equals(titleEntry.categoryTitle, commandBase)) {
                        newList.add(i + 1, commandList.createCommandEntry(data.command, data.name, indexDataList));
                        return;
                    }
                }
            }
            // If the for loop didn't find the category, create it!
            newList.addFirst(commandList.createCategoryTitleEntry(commandBase));
            newList.add(1, commandList.createCommandEntry(data.command, data.name, indexDataList));
        }
    }

    public void updateSearch(String search) {
        if (search.isEmpty()) {
            addButton.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.savenewcommandbutton")));
            addButton.switchToComponent();
        } else {
            addButton.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.savecommandfromsearchbutton", search)));
            addButton.switchToIcon();
        }
        List<CommandList.BaseEntry> newList = new ArrayList<>();
        for (int i = 0; i < commandManager.data.commands.size(); i++) {
            SavedCommandManager.CommandData data = commandManager.data.commands.get(i);
            if (data.command.toLowerCase().contains(search.toLowerCase()) ||
                    (data.name != null && data.name.toLowerCase().contains(search.toLowerCase())) ||
                    (manualCategories && data.categoryId != null && commandManager.data.customCategories != null &&
                            commandManager.data.customCategories.stream().anyMatch((category) ->
                                    Objects.equals(data.categoryId, category.id) && category.name.toLowerCase().contains(search.toLowerCase())))) {
                addCommandRightPlace(data, i, newList);
            }
        }
        commandList.replaceEntries(newList);
        for (CommandList.BaseEntry entry : commandList.children()) {
            entry.init();
        }
        if (commandSuggestor != null) {
            commandSuggestor.updateCommandInfo();
        }
    }

    public void updateSearchAndScroll(String search) {
        updateSearch(search);
        commandList.setScrollAmount(0);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.isConfirmation()) {
            addCommand();
        }
        if (commandSuggestor.keyPressed(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (commandSuggestor.mouseScrolled(verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if (commandSuggestor.mouseClicked(click)) {
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
        this.setInitialFocus(this.searchBar);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        commandSuggestor.extractRenderState(context, mouseX, mouseY);
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
        minecraft.gui.setScreen(new EditCommandScreen(this, commandManager.addCommand(searchBar.getValue(), null)));
        searchBar.setValue("");
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

        public static abstract class BaseEntry extends ContainerObjectSelectionList.Entry<BaseEntry> {
            protected abstract void init();
        }

        protected CommandEntry createCommandEntry(String command, String name, int indexDataList) {
            return new CommandEntry(command, name, indexDataList);
        }

        protected CategoryTitleEntry createCategoryTitleEntry(String categoryTitle) {
            return new CategoryTitleEntry(categoryTitle);
        }

        protected CategoryTitleEntry createCategoryTitleEntry(SavedCommandManager.SavedCommandsData.CustomCategory customCategory) {
            return new CategoryTitleEntry(customCategory);
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

            @Override
            protected void init() {
                int y = this.getY() + 2;
                int entryHeight = this.getHeight() - 4;
                int entryWidth = this.getWidth();

                deleteButton = new IconButton(entryWidth - 20, y + (entryHeight - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/trash_can.png"), _ -> {
                    LOGGER.info("Clicked on delete " + this.command + " index " + indexDataList);
                    if (SettingsManager.getCombinedWorldAndGlobal(commandManager).deleteWarning) {
                        minecraft.gui.setScreen(new PopupConfirmScreen(Component.translatable("screen.savedcommands.commanddeletequestion"), Component.translatable("selectWorld.deleteWarning", name != null ? name : command), minecraft.gui.screen(), Component.translatable("selectWorld.deleteButton"), Component.translatable("gui.cancel"), showAgainState -> {
                            commandManager.removeCommand(indexDataList);
                            updateSearchAndScroll(searchBar.getValue());

                            switch (showAgainState) {
                                case WORLD_DISABLED -> {
                                    commandManager.data.worldSettings.deleteWarning = false;
                                    commandManager.saveAsync();
                                }
                                case GLOBAL_DISABLED -> {
                                    SettingsManager.globalSettings.deleteWarning = false;
                                    SettingsManager.saveAsync();
                                }
                            }
                        }, !SettingsManager.globalSettings.deleteWarning));
                    } else {
                        commandManager.removeCommand(indexDataList);
                        updateSearchAndScroll(searchBar.getValue());
                    }
                }, Component.translatable("selectWorld.deleteButton"));

                editButton = new IconButton(entryWidth - 50, y + (entryHeight - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/edit.png"), _ -> {
                    LOGGER.info("Clicked on edit " + this.command + " index " + indexDataList);
                    minecraft.gui.setScreen(new EditCommandScreen(minecraft.gui.screen(), commandManager.data.commands.get(indexDataList)));
                }, Component.translatable("selectWorld.edit"));

                shareButton = new IconButton(entryWidth - 80, y + (entryHeight - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/share.png"), _ -> {
                    LOGGER.info("Clicked on share " + this.command + " index " + indexDataList);
                    minecraft.gui.setScreen(new SharePlayerSelectionScreen(minecraft.gui.screen(), new ArrayList<>(List.of(commandManager.data.commands.get(indexDataList)))));
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
                minecraft.gui.setScreen(null);
                SavedCommandManager.sendCommandAndInsertVariables(commandManager.data.commands.get(indexDataList), null);
                return true;
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                int x = this.getX() + 2;
                int y = this.getY() + 2;
                int entryHeight = this.getHeight() - 4;
                int entryWidth = this.getWidth();

                context.fill(x, y, x + entryWidth, y + entryHeight, 0x44000000);

                if (this.name == null) {
                    int textX = x + 3;
                    int textY = y + (entryHeight - minecraft.font.lineHeight) / 2 + 1;

                    context.text(minecraft.font, Component.literal(shortenTextIfNeeded(this.command, entryWidth - 95, ChatFormatting.RESET)), textX, textY, 0xFFFFFFFF, true);
                } else {
                    int fontHeight = minecraft.font.lineHeight;
                    int textX = x + 3;
                    int textY = y + (entryHeight - minecraft.font.lineHeight * 2 - 2) / 2 + 1;

                    context.text(minecraft.font, Component.literal(shortenTextIfNeeded(this.name, entryWidth - 95, ChatFormatting.RESET)), textX, textY, 0xFFFFFFFF, true);

                    textY += fontHeight + 2;

                    context.text(minecraft.font, Component.literal(shortenTextIfNeeded(this.command, entryWidth - 95, ChatFormatting.RESET)), textX, textY, 0xFFBBBBBB, true);
                }

                buttonExtractActions(context, mouseX, mouseY, hovered, deltaTicks);
            }

            protected void buttonExtractActions(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                int y = this.getY() + 2;
                int entryHeight = this.getHeight() - 4;
                int entryWidth = this.getWidth();

                shareButton.active = otherPlayersOnServer;
                deleteButton.setPosition(entryWidth - 20, y + (entryHeight - 20) / 2);
                deleteButton.extractRenderState(context, mouseX, mouseY, deltaTicks);

                editButton.setPosition(entryWidth - 50, y + (entryHeight - 20) / 2);
                editButton.extractRenderState(context, mouseX, mouseY, deltaTicks);

                shareButton.setPosition(entryWidth - 80, y + (entryHeight - 20) / 2);
                shareButton.extractRenderState(context, mouseX, mouseY, deltaTicks);

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

        public class CategoryTitleEntry extends BaseEntry {
            private final String categoryTitle;
            private final SavedCommandManager.SavedCommandsData.CustomCategory customCategory;
            public boolean buttonExtraction = false;

            IconButton deleteButton;
            IconButton editButton;

            public CategoryTitleEntry(String categoryTitle) {
                this.categoryTitle = categoryTitle;
                this.customCategory = null;
            }

            public CategoryTitleEntry(SavedCommandManager.SavedCommandsData.CustomCategory customCategory) {
                if (customCategory != null) {
                    this.categoryTitle = customCategory.name;
                } else {
                    this.categoryTitle = Component.translatable("screen.savedcommands.nocategory").getString();
                }
                this.customCategory = customCategory;
                this.buttonExtraction = manualCategories;
            }

            @Override
            protected void init() {
                int y = this.getY() + 2;
                int entryHeight = this.getHeight() - 4;
                int entryWidth = this.getWidth();

                if (customCategory != null && buttonExtraction) {
                    deleteButton = new IconButton(entryWidth - 20, y + (entryHeight - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/trash_can.png"), _ -> {
                        if (SettingsManager.getCombinedWorldAndGlobal(commandManager).deleteWarning) {
                            minecraft.gui.setScreen(new PopupConfirmScreen(Component.translatable("screen.savedcommands.categorydeletequestion"),
                                    Component.translatable("screen.savedcommands.categorydeletemessage", categoryTitle),
                                    minecraft.gui.screen(), Component.translatable("selectWorld.deleteButton"), Component.translatable("gui.cancel"), showAgainState -> {
                                commandManager.data.commands.stream()
                                        .filter(commandData -> Objects.equals(commandData.categoryId, customCategory.id))
                                        .forEach(commandData -> commandData.categoryId = null);
                                if (commandManager.data.customCategories != null) {
                                    commandManager.data.customCategories.remove(customCategory);
                                }
                                updateSearchAndScroll(searchBar.getValue());
                                switch (showAgainState) {
                                    case WORLD_DISABLED -> {
                                        commandManager.data.worldSettings.deleteWarning = false;
                                        commandManager.saveAsync();
                                    }
                                    case GLOBAL_DISABLED -> {
                                        SettingsManager.globalSettings.deleteWarning = false;
                                        SettingsManager.saveAsync();
                                    }
                                }
                            }, !SettingsManager.globalSettings.deleteWarning));
                        } else {
                            commandManager.data.commands.stream()
                                    .filter(commandData -> Objects.equals(commandData.categoryId, customCategory.id))
                                    .forEach(commandData -> commandData.categoryId = null);
                            if (commandManager.data.customCategories != null) {
                                commandManager.data.customCategories.remove(customCategory);
                            }
                            updateSearchAndScroll(searchBar.getValue());
                        }
                    }, Component.translatable("selectWorld.deleteButton"));

                    editButton = new IconButton(entryWidth - 50, y + (entryHeight - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/edit.png"),
                            _ -> minecraft.gui.setScreen(new EditCategoryScreen(customCategory, minecraft.gui.screen())), Component.translatable("selectWorld.edit"));
                }
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                int x = this.getX() + 2;
                int y = this.getY() + 2;
                int entryHeight = this.getHeight() - 4;
                int entryWidth = this.getWidth();

                int textX;
                int textY;
                int availableWidth;
                if (manualCategories) {
                    context.fill(x, y, x + entryWidth, y + entryHeight, 0x44161616);
                    textX = x + 3;
                    textY = y + (entryHeight - 8) / 2;
                    availableWidth = entryWidth - 60;
                } else {
                    textX = x + 3;
                    textY = y + entryHeight - minecraft.font.lineHeight;
                    availableWidth = entryWidth - 10;
                }

                context.text(minecraft.font, Component.literal(shortenTextIfNeeded(this.categoryTitle, availableWidth, ChatFormatting.BOLD)).withStyle(ChatFormatting.BOLD), textX, textY, 0xFFFFFFFF, true);

                if (customCategory != null && buttonExtraction) {
                    deleteButton.setPosition(entryWidth - 20, y + (entryHeight - 20) / 2);
                    deleteButton.extractRenderState(context, mouseX, mouseY, deltaTicks);

                    editButton.setPosition(entryWidth - 50, y + (entryHeight - 20) / 2);
                    editButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
                }
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfString(categoryTitle));
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return customCategory != null && manualCategories ? List.of(editButton, deleteButton) : List.of();
            }
        }
    }
}