package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;
import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;

public class SavedCommandsScreen extends Screen {
    public TextFieldPlaceholderAlways SearchBar;
    ButtonWidget AddButton;
    CommandList commandList;

    int listWidth;
    int listHeight;
    int listTop = 50;
    int itemHeight = 30;

    ChatInputSuggestor CommandSuggestor;

    @Nullable Element focused;

    protected SavedCommandsScreen() {
        super(Text.translatable("screen.savedcommands.commandscreentitle"));
    }

    protected void init() {
        assert this.client != null;
        this.SearchBar = new TextFieldPlaceholderAlways(this.client.advanceValidatingTextRenderer, 20, 20, this.width - 40 - 22, 20, Text.translatable("screen.savedcommands.searchsavebar"));
        this.SearchBar.setMaxLength(256);
        this.SearchBar.setDrawsBackground(true);
        this.SearchBar.setChangedListener(this::updateSearch);
        this.SearchBar.setFocusUnlocked(false);
        this.SearchBar.setPlaceholder(Text.translatable("screen.savedcommands.searchsavebar"));
        this.addDrawableChild(this.SearchBar);

        AddButton = ButtonWidget.builder(
                Text.literal("+"),
                b -> {
                    LOGGER.info("Add command button clicked!");
                    commandManager.addCommand(SearchBar.getText(), null);
                    SearchBar.setText("");
                }
        ).dimensions(20 + this.width - 40 - 20, 20, 20, 20).build();

        this.addDrawableChild(this.AddButton);

        listWidth = this.width;
        listHeight = this.height - 50;

        this.commandList = new CommandList(this.client, listWidth, listHeight, listTop, itemHeight);

        updateSearch(SearchBar.getText());

        this.addSelectableChild(this.commandList);

        CommandSuggestor = new ChatInputSuggestor(client, this, SearchBar, textRenderer, false, false, 1, 10, false, 0xD8000000);
        CommandSuggestor.setCanLeave(false);
        CommandSuggestor.setWindowActive(true);
        CommandSuggestor.refresh();
    }

    @Override
    public @Nullable Element getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable Element focused) {
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

    private void addCommandRightPlace(SavedCommandManager.CommandData data, int indexDataList, List<BaseEntry> newList) {
        String commandBase;
        if (data.command.contains(" ")) {
            commandBase = data.command.substring(0, data.command.indexOf(" "));
        } else {
            commandBase = data.command;
        }
        for (int i = 0; i < newList.size(); i++) {
            if (newList.get(i) instanceof CategoryTitleEntry TitleEntry) {
                if (Objects.equals(TitleEntry.categoryTitle, commandBase)) {
                    newList.add(i + 1, new CommandEntry(data.command, data.name, indexDataList));
                    return;
                }
            }
        }
        // If the for loop didn't find the category, create it!
        newList.addFirst(new CategoryTitleEntry(commandBase));
        newList.add(1, new CommandEntry(data.command, data.name, indexDataList));
    }

    public void updateSearch(String search) {
        List<BaseEntry> newList = new ArrayList<>();
        for (int i = 0; i < commandManager.data.commands.size(); i++) {
            SavedCommandManager.CommandData data = commandManager.data.commands.get(i);
            if (data.command.toLowerCase().contains(search.toLowerCase()) || (data.name != null && data.name.toLowerCase().contains(search.toLowerCase()))) {
                addCommandRightPlace(data, i, newList);
            }
        }
        commandList.replaceEntries(newList);
        if (CommandSuggestor != null) {
            CommandSuggestor.refresh();
        }
    }

    public void resize(int width, int height) {
        if (CommandSuggestor != null) {
            CommandSuggestor.refresh();
        }
        super.resize(width, height);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEnter()) {
            commandManager.addCommand(SearchBar.getText(), null);
            SearchBar.setText("");
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
    public boolean mouseClicked(Click click, boolean doubled) {
        if (CommandSuggestor.mouseClicked(click)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    protected void setInitialFocus() {
        this.setInitialFocus(this.SearchBar);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // set all buttons to not visible in a loop (They will be set back to visible by commandList)
        for (BaseEntry entry : commandList.children()) {
            if (entry instanceof CommandEntry comEntry) {
                comEntry.deleteButton.visible = false;
                comEntry.editButton.visible = false;
            }
        }

        this.commandList.render(context, mouseX, mouseY, delta);

        context.enableScissor(0, listTop, this.width, this.height);

        for (BaseEntry e : this.commandList.children()) {
            if (e instanceof CommandEntry comEntry) {
                comEntry.deleteButton.render(context, mouseX, mouseY, delta);
                comEntry.editButton.render(context, mouseX, mouseY, delta);
            }
        }

        context.disableScissor();

        CommandSuggestor.render(context, mouseX, mouseY);

        context.fill(SearchBar.getX(), SearchBar.getY(), SearchBar.getX() + SearchBar.getWidth(), SearchBar.getY() + SearchBar.getHeight(), 0x44000000);

        super.render(context, mouseX, mouseY, delta);
    }

    public static String shortenTextIfNeeded(String text, int availableWidth, Formatting formatting) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer.getWidth(Text.literal(text).formatted(formatting)) <= availableWidth) {
            return text;
        }
        StringBuilder mutableString = new StringBuilder(text);
        mutableString.delete(mutableString.length() - 1, mutableString.length());
        mutableString.append("…");
        while (client.textRenderer.getWidth(Text.literal(mutableString.toString()).formatted(formatting)) > availableWidth) {
            mutableString.delete(mutableString.length() - 2, mutableString.length() - 1);
        }
        return mutableString.toString();
    }

    public static class BaseEntry extends ElementListWidget.Entry<BaseEntry> {
        @Override
        public List<? extends Element> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return Collections.emptyList();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
        }
    }

    public class CommandEntry extends BaseEntry {
        private final String command;
        String name;
        int indexDataList;
        IconButton deleteButton;
        IconButton editButton;

        public CommandEntry(String command, String name, int indexDataList) {
            this.command = command;
            this.name = name;
            this.indexDataList = indexDataList;

            deleteButton = new IconButton(0, 0, 20, 20, Identifier.of(MOD_ID, "textures/gui/trash_can.png"), null);
            deleteButton.visible = false;

            editButton = new IconButton(0, 0, 20, 20, Identifier.of(MOD_ID, "textures/gui/edit.png"), null);
            editButton.visible = false;

            assert MinecraftClient.getInstance().currentScreen != null;
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            if (click.button() != 0) {
                return false;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (deleteButton.isHovered()) {
                LOGGER.info("Clicked on delete " + this.command + " index " + indexDataList);
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                commandManager.removeCommand(indexDataList);
                updateSearch(SearchBar.getText());
                return true;
            }
            if (editButton.isHovered()) {
                LOGGER.info("Clicked on edit " + this.command + " index " + indexDataList);
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                client.setScreen(new EditCommandScreen(client.currentScreen, commandManager.data.commands.get(indexDataList)));
                return true;
            }
            LOGGER.info("Clicked on command " + this.command);
            MinecraftClient.getInstance().setScreen(null);
            SavedCommandManager.sendCommandAndInsertVariables(commandManager.data.commands.get(indexDataList), null);
            return true;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            int x = this.getX() + 2;
            int y = this.getY() + 2;
            int entryHeight = this.getHeight() - 4;
            int entryWidth = this.getWidth();

            MinecraftClient client = MinecraftClient.getInstance();
            context.fill(x, y, x + entryWidth, y + entryHeight, 0x44000000);

            if (this.name == null) {
                int fontHeight = client.textRenderer.fontHeight;
                int textX = x + 3;
                int textY = y + (entryHeight - fontHeight) / 2 + 1;

                context.drawTextWithShadow(client.textRenderer, Text.literal(shortenTextIfNeeded(this.command, entryWidth - 60, Formatting.RESET)), textX, textY, 0xFFFFFFFF);
            } else {
                int fontHeight = client.textRenderer.fontHeight;
                int textX = x + 3;
                int textY = y + (entryHeight - fontHeight * 2 - 2) / 2 + 1;

                context.drawTextWithShadow(client.textRenderer, Text.literal(shortenTextIfNeeded(this.name, entryWidth - 60, Formatting.RESET)), textX, textY, 0xFFFFFFFF);

                textY += fontHeight + 2;

                context.drawTextWithShadow(client.textRenderer, Text.literal(shortenTextIfNeeded(this.command, entryWidth - 60, Formatting.RESET)), textX, textY, 0xFFBBBBBB);
            }

            deleteButton.setPosition(entryWidth - 20, y + (entryHeight - 20) / 2);
            deleteButton.visible = true;

            editButton.setPosition(entryWidth - 50, y + (entryHeight - 20) / 2);
            editButton.visible = true;
        }
    }

    public static class CategoryTitleEntry extends BaseEntry {
        private final String categoryTitle;

        public CategoryTitleEntry(String categoryTitle) {
            this.categoryTitle = categoryTitle;
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            if (click.button() == 0) {
                LOGGER.info("Clicked on category title " + this.categoryTitle);
                return true;
            }
            return false;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            MinecraftClient client = MinecraftClient.getInstance();

            int fontHeight = client.textRenderer.fontHeight;
            int textX = this.getX() + 3;
            int textY = this.getY() + this.getHeight() - fontHeight - 2;

            context.drawTextWithShadow(client.textRenderer, Text.literal(shortenTextIfNeeded(this.categoryTitle, this.getWidth() - 10, Formatting.BOLD)).formatted(Formatting.BOLD), textX, textY, 0xFFFFFFFF);
        }
    }

    private static class CommandList extends ElementListWidget<BaseEntry> {
        public CommandList(MinecraftClient client, int width, int height, int top, int itemHeight) {
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
        protected int getScrollbarX() {
            return this.width - 6;
        }
    }
}