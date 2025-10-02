package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

public class SavedCommandsScreen extends Screen {
    protected TextFieldWidget SearchBar;
    CommandList commandList;

    protected SavedCommandsScreen() {
        super(Text.translatable("screen.savedcommands.commandscreentitle"));
    }

    protected void init() {
        assert this.client != null;
        this.SearchBar = new TextFieldWidget(this.client.advanceValidatingTextRenderer, 20, 20, this.width - 40, 20, Text.translatable("screen.savedcommands.searchsavebar"));
        this.SearchBar.setMaxLength(256);
        this.SearchBar.setDrawsBackground(true);
        this.SearchBar.setChangedListener(this::UpdateSearch);
        this.SearchBar.setFocusUnlocked(false);
        this.addDrawableChild(this.SearchBar);

        int listWidth = this.width;
        int listHeight = this.height - 60;
        int listTop = 50;
        int itemHeight = 20;

        this.commandList = new CommandList(this.client, listWidth, listHeight, listTop, listTop + listHeight, itemHeight);

        this.commandList.addCommand("/say hello");
        this.commandList.addCommand("/tp @p ~ ~1 ~");

        this.addSelectableChild(this.commandList);
    }

    protected void setInitialFocus() {
        this.setInitialFocus(this.SearchBar);
    }

    private void UpdateSearch(String searchContent){

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.commandList.render(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    private static class CommandList extends ElementListWidget<CommandList.CommandEntry> {
        public CommandList(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        public void addCommand(String command) {
            this.addEntry(new CommandEntry(command));
        }

        @Override
        protected int getScrollbarX() {
            return this.width - 6;
        }

        public static class CommandEntry extends ElementListWidget.Entry<CommandEntry> implements Element, Selectable {
            private final String command;

            public CommandEntry(String command) {
                this.command = command;
            }


            @Override
            public List<? extends Element> children() {
                return List.of();
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (button == 0) {
                    LOGGER.info("Click!");
                    return true;
                }
                return false;
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of();
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickProgress) {
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(this.command), x + 3, y + 3, 0xFFFFFF);
            }

            @Override
            public SelectionType getType() {
                return null;
            }

            @Override
            public void appendNarrations(NarrationMessageBuilder builder) {
                builder.put(NarrationPart.TITLE, Text.literal(this.command));
            }
        }
    }
}