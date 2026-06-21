package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.NarratableEntryOfComponent;
import static de.aliba2468pr77pr77.savedcommands.SettingsManager.globalSettings;

public class GlobalSettingsScreen extends Screen {
    private final Screen lastScreen;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private SettingsList list;

    public GlobalSettingsScreen(Screen lastScreen) {
        super(Component.translatable("screen.savedcommands.globalsettings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        list = layout.addToContents(new SettingsList(minecraft, width, layout));
        addOptions();

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE,
                button -> onClose()).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    protected void addOptions() {
        list.addButon(Component.translatable("screen.savedcommands.settings.receivecommands"), globalSettings.receiveCommands,
                (button, value) -> globalSettings.receiveCommands = value);
        list.addEditBox(Component.translatable("screen.savedcommands.settings.msgcommand"), globalSettings.msgCommand, Component.literal(globalSettings.msgCommand),
                (value) -> {
                    if (value.isEmpty()) {
                        globalSettings.msgCommand = SettingsManager.Settings.DEFAULT_MSG_COMMAND;
                    } else {
                        globalSettings.msgCommand = value;
                    }
                });
        list.addButon(Component.translatable("screen.savedcommands.settings.deletewarning"), globalSettings.deleteWarning,
                (button, value) -> globalSettings.deleteWarning = value);
        list.addButon(Component.translatable("screen.savedcommands.settings.manualcategories"), globalSettings.manualCategories,
                (button, value) -> globalSettings.manualCategories = value);
    }

    @Override
    public void repositionElements() {
        layout.arrangeElements();
        if (list != null) list.updateSize(width, layout);
        assert list != null;
        for (SettingsList.Entry entry : list.children()) {
            entry.init();
        }
    }

    @Override
    public void onClose() {
        SettingsManager.saveAsync();
        if (lastScreen instanceof SavedCommandsScreen) {
            minecraft.setScreen(new SavedCommandsScreen(false));
        } else {
            minecraft.setScreen(lastScreen);
        }
    }

    public static class SettingsList extends ContainerObjectSelectionList<SettingsList.Entry> {

        public SettingsList(Minecraft minecraft, int width, HeaderAndFooterLayout layout) {
            super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), 30);
        }

        public void addButon(Component description, Boolean value, CycleButton.OnValueChange<Boolean> onValueChange) {
            Entry entry = new ButtonEntry(description, value, onValueChange);
            addEntry(entry);
        }

        public void addEditBox(Component label, String value, Component hint, Consumer<String> responder) {
            addEntry(new EditBoxEntry(minecraft, label, value, hint, responder), 40);
        }

        @Override
        public int getRowWidth() {
            return Math.min(400, width - 50);
        }

        @Override
        protected int scrollBarX() {
            return this.width - 6;
        }

        public abstract static class Entry
                extends ContainerObjectSelectionList.Entry<Entry> {
            abstract void init(); // Initializer after adding, getContentWidth and positions available.
        }

        public static class ButtonEntry extends Entry {
            private final CycleButton<Boolean> button;

            private ButtonEntry(Component description, Boolean value, CycleButton.OnValueChange<Boolean> onValueChange) {
                button = CycleButton.builder((Boolean option) ->
                                        option ? Component.translatable("manageServer.resourcePack.enabled") : Component.translatable("manageServer.resourcePack.disabled"),
                                value)
                        .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                        .create(0, 0, 0, 20, description, onValueChange); // Position and size set in init
            }

            @Override
            void init() {
                button.setPosition(getContentX(), getContentY());
                button.setSize(getContentWidth(), 20);
            }

            @Override
            public void renderContent(@NonNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float a) {
                button.setY(getContentY());
                button.render(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(button);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(button);
            }
        }

        public static class EditBoxEntry extends Entry {
            public final EditBox editBox;
            private final Component label;
            private final Minecraft minecraft;
            private final String initialValue;
            private boolean initialized = false;

            public EditBoxEntry(Minecraft minecraft, Component label, String value, Component hint, Consumer<String> responder) {
                this.minecraft = minecraft;
                this.label = label;
                this.initialValue = value;
                this.editBox = new EditBox(minecraft.font, 0, 0, 0, 20, label); // Position and size set in init
                this.editBox.setHint(hint);
                this.editBox.setResponder(responder);
            }

            @Override
            void init() {
                editBox.setPosition(getContentX(), getContentY());
                editBox.setSize(getContentWidth(), 20);
                if (!initialized) {
                    editBox.setValue(initialValue);
                    initialized = true;
                }
            }

            @Override
            public void renderContent(@NonNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.drawString(
                        minecraft.font,
                        label,
                        getContentX(),
                        getContentY(),
                        0xFFFFFFFF,
                        true
                );
                editBox.setY(getContentY() + 10);
                editBox.render(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(editBox);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfComponent(label), editBox);
            }
        }
    }
}
