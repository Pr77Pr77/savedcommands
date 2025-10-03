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

import java.util.Collections;
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
        int itemHeight = 30;

        this.commandList = new CommandList(this.client, listWidth, listHeight, listTop, itemHeight, 0);

        this.commandList.addCommand("/say hello");
        this.commandList.children().get(0).name = "Test!";
        this.commandList.addCommand("/tp @p ~ ~1 ~");

        this.addSelectableChild(this.commandList);
    }

    protected void setInitialFocus() {
        this.setInitialFocus(this.SearchBar);
    }

    private void UpdateSearch(String searchContent) {

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.commandList.render(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    private static class CommandList extends ElementListWidget<CommandList.CommandEntry> {
        public CommandList(MinecraftClient client, int width, int height, int top, int itemHeight, int headerHeight) {
            super(client, width, height, top, itemHeight, headerHeight);
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
            String name = null;

            public CommandEntry(String command) {
                this.command = command;
            }


            @Override
            public List<? extends Element> children() {
                return Collections.emptyList();
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (button == 0) {
                    LOGGER.info("Clicked on " + this.command);
                    return true;
                }
                return false;
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return Collections.emptyList();
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