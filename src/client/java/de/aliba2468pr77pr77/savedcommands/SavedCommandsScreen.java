package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;
import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;

public class SavedCommandsScreen extends Screen {
    protected TextFieldWidget SearchBar;
    ButtonWidget AddButton;
    CommandList commandList;
    SavedCommandManager commandManager;

    int listWidth;
    int listHeight;
    int listTop = 50;
    int itemHeight = 30;

    protected SavedCommandsScreen() {
        super(Text.translatable("screen.savedcommands.commandscreentitle"));
    }

    public static String getWorldOrServerId() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Integrated server
        IntegratedServer integrated = client.getServer();
        if (integrated != null) {
            try {
                Object saveProps = integrated.getSaveProperties();
                if (saveProps != null) {
                    java.lang.reflect.Method m = saveProps.getClass().getMethod("getLevelName");
                    Object levelName = m.invoke(saveProps);
                    if (levelName != null) return "singleplayer/" + levelName;
                }
            } catch (NoSuchMethodException e) {
                return "singleplayer/unknown";
            } catch (Throwable t) {
                t.printStackTrace();
                return "singleplayer/unknown";
            }
        }

        // External multiplayer
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null) {
            return "multiplayer/" + server.address;
        }

        // No world open
        return "none";
    }

    protected void init() {
        assert this.client != null;
        this.SearchBar = new TextFieldWidget(this.client.advanceValidatingTextRenderer, 20, 20, this.width - 40 - 22, 20, Text.translatable("screen.savedcommands.searchsavebar"));
        this.SearchBar.setMaxLength(256);
        this.SearchBar.setDrawsBackground(true);
        this.SearchBar.setChangedListener(this::updateSearch);
        this.SearchBar.setFocusUnlocked(false);
        this.addDrawableChild(this.SearchBar);

        commandManager = new SavedCommandManager(getWorldOrServerId());

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

        this.commandList = new CommandList(this.client, listWidth, listHeight, listTop, itemHeight, 0);

        updateSearch(SearchBar.getText());

        this.addSelectableChild(this.commandList);
    }

    private void addCommandRightPlace(SavedCommandManager.CommandData data, int indexDataList) {
        String commandBase;
        if (data.command.contains(" ")) {
            commandBase = data.command.substring(0, data.command.indexOf(" "));
        } else {
            commandBase = data.command;
        }
        for (int i = 0; i < commandList.children().size(); i++) {
            if (this.commandList.children().get(i) instanceof CategoryTitleEntry TitleEntry) {
                if (Objects.equals(TitleEntry.categoryTitle, commandBase)) {
                    this.commandList.children().add(i + 1, new CommandEntry(data.command, data.name, indexDataList));
                    return;
                }
            }
        }
        // If the for loop didn't find the category, create it!
        this.commandList.children().addFirst(new CategoryTitleEntry(commandBase));
        this.commandList.children().add(1, new CommandEntry(data.command, data.name, indexDataList));
    }

    public void updateSearch(String search) {
        commandList.children().clear();
        for (int i = 0; i < commandManager.data.commands.size(); i++) {
            SavedCommandManager.CommandData data = commandManager.data.commands.get(i);
            if (data.command.contains(search)) {
                addCommandRightPlace(data, i);
            }
        }
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

        super.render(context, mouseX, mouseY, delta);
    }

    public static class BaseEntry extends ElementListWidget.Entry<BaseEntry> implements Element, Selectable {
        @Override
        public List<? extends Element> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return Collections.emptyList();
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickProgress) {
        }

        @Override
        public SelectionType getType() {
            return null;
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
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
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
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

                // TODO: Open edit screen.
                return true;
            }
            LOGGER.info("Clicked on command " + this.command);
            if (client.player != null) {
                ClientPlayerEntity player = client.player;
                if (this.command.charAt(0) == '/') {
                    player.networkHandler.sendChatCommand(this.command.substring(1));
                } else {
                    player.networkHandler.sendChatMessage(this.command);
                }
            }
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickProgress) {
            MinecraftClient client = MinecraftClient.getInstance();
            context.fill(x, y, x + entryWidth, y + entryHeight, 0x44000000);

            if (this.name == null) {
                int fontHeight = client.textRenderer.fontHeight;
                int textX = x + 3;
                int textY = y + (entryHeight - fontHeight) / 2 + 1;

                context.drawTextWithShadow(client.textRenderer, Text.literal(this.command), textX, textY, 0xFFFFFFFF);
            } else {
                int fontHeight = client.textRenderer.fontHeight;
                int textX = x + 3;
                int textY = y + (entryHeight - fontHeight * 2 - 2) / 2 + 1;

                context.drawTextWithShadow(client.textRenderer, Text.literal(this.name), textX, textY, 0xFFFFFFFF);

                textY += fontHeight + 2;

                context.drawTextWithShadow(client.textRenderer, Text.literal(this.command), textX, textY, 0xFFBBBBBB);
            }

            deleteButton.setPosition(entryWidth - 20, y + (entryHeight - 20) / 2);
            deleteButton.visible = true;

            editButton.setPosition(entryWidth - 50, y + (entryHeight - 20) / 2);
            editButton.visible = true;
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
            if (this.name == null) {
                builder.put(NarrationPart.TITLE, Text.literal(this.command));
            } else {
                builder.put(NarrationPart.TITLE, Text.literal(this.name));
                builder.put(NarrationPart.HINT, Text.literal(this.command));
            }
        }
    }

    public static class CategoryTitleEntry extends BaseEntry {
        private final String categoryTitle;

        public CategoryTitleEntry(String categoryTitle) {
            this.categoryTitle = categoryTitle;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                LOGGER.info("Clicked on category title " + this.categoryTitle);
                return true;
            }
            return false;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickProgress) {
            MinecraftClient client = MinecraftClient.getInstance();

            int fontHeight = client.textRenderer.fontHeight;
            int textX = x + 3;
            int textY = y + entryHeight - fontHeight - 2;

            context.drawTextWithShadow(client.textRenderer, Text.literal(this.categoryTitle).formatted(Formatting.BOLD), textX, textY, 0xFFFFFFFF);
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, Text.literal(this.categoryTitle));
        }
    }

    private static class CommandList extends ElementListWidget<BaseEntry> {
        public CommandList(MinecraftClient client, int width, int height, int top, int itemHeight, int headerHeight) {
            super(client, width, height, top, itemHeight, headerHeight);
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