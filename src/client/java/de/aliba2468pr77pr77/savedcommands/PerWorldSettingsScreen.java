package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;
import static de.aliba2468pr77pr77.savedcommands.SettingsManager.globalSettings;
import static net.minecraft.network.chat.Component.translatable;


public class PerWorldSettingsScreen extends PopupScreen {
    Button closeButton;
    Button generalSettingsButton;

    CycleButton<BooleanObjectOptions> receiveSentCommandsButton;
    EditBox msgCommandEditBox;
    CycleButton<BooleanObjectOptions> deleteWarningButton;
    CycleButton<BooleanObjectOptions> manualCategoriesButton;

    public enum BooleanObjectOptions {
        DEFAULT("screen.savedcommands.worldsettings.default"),
        ENABLED("manageServer.resourcePack.enabled"),
        DISABLED("manageServer.resourcePack.disabled");

        private final String translationKey;

        BooleanObjectOptions(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getComponent() {
            return Component.translatable(translationKey);
        }

        public Component getComponent(boolean globalValue) {
            return Component.translatable(translationKey,
                    globalValue
                            ? Component.translatable("manageServer.resourcePack.enabled")
                            : Component.translatable("manageServer.resourcePack.disabled")
            );
        }

        public Boolean getBoolean() {
            return switch (this) {
                case DEFAULT -> null;
                case ENABLED -> true;
                case DISABLED -> false;
            };
        }
    }

    public BooleanObjectOptions booleanToEnum(Boolean bool) {
        if (bool == null) return BooleanObjectOptions.DEFAULT;
        return bool ? BooleanObjectOptions.ENABLED : BooleanObjectOptions.DISABLED;
    }

    public PerWorldSettingsScreen(Screen parent) {
        super(Component.translatable("screen.savedcommands.worldsettings"), parent, 230, 400);
    }

    @Override
    protected void init() {
        super.init();

        receiveSentCommandsButton =
                CycleButton.builder((BooleanObjectOptions option) -> switch (option) {
                            case ENABLED, DISABLED -> option.getComponent();
                            case DEFAULT -> option.getComponent(globalSettings.receiveCommands);
                        }, (commandManager != null && commandManager.data.worldSettings != null)
                                ? booleanToEnum(commandManager.data.worldSettings.receiveCommands) : BooleanObjectOptions.DEFAULT)
                        .withValues(BooleanObjectOptions.values())
                        .create(popupX + 20, popupY + 30, popupW - 40, 20, Component.translatable("screen.savedcommands.settings.receivecommands"));
        addRenderableWidget(receiveSentCommandsButton);

        msgCommandEditBox = new EditBox(this.minecraft.font, popupX + 20, receiveSentCommandsButton.getY() + receiveSentCommandsButton.getHeight() + 20, popupW - 40, 20, translatable("advMode.command"));
        msgCommandEditBox.setMaxLength(256);
        msgCommandEditBox.setBordered(true);
        msgCommandEditBox.setCanLoseFocus(true);
        msgCommandEditBox.setHint(Component.literal(globalSettings.msgCommand));
        msgCommandEditBox.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.worldsettings.leaveemtydefault")));
        if (commandManager != null && commandManager.data.worldSettings != null && commandManager.data.worldSettings.msgCommand != null) {
            msgCommandEditBox.setValue(commandManager.data.worldSettings.msgCommand);
        }
        this.addRenderableWidget(msgCommandEditBox);

        deleteWarningButton =
                CycleButton.builder((BooleanObjectOptions option) -> switch (option) {
                            case ENABLED, DISABLED -> option.getComponent();
                            case DEFAULT -> option.getComponent(globalSettings.deleteWarning);
                        }, (commandManager != null && commandManager.data.worldSettings != null)
                                ? booleanToEnum(commandManager.data.worldSettings.deleteWarning) : BooleanObjectOptions.DEFAULT)
                        .withValues(BooleanObjectOptions.values())
                        .create(popupX + 20, msgCommandEditBox.getY() + msgCommandEditBox.getHeight() + 10, popupW - 40, 20, Component.translatable("screen.savedcommands.settings.deletewarning"));
        addRenderableWidget(deleteWarningButton);

        manualCategoriesButton =
                CycleButton.builder((BooleanObjectOptions option) -> switch (option) {
                            case ENABLED, DISABLED -> option.getComponent();
                            case DEFAULT -> option.getComponent(globalSettings.manualCategories);
                        }, (commandManager != null && commandManager.data.worldSettings != null)
                                ? booleanToEnum(commandManager.data.worldSettings.manualCategories) : BooleanObjectOptions.DEFAULT)
                        .withValues(BooleanObjectOptions.values())
                        .create(popupX + 20, deleteWarningButton.getY() + deleteWarningButton.getHeight() + 10, popupW - 40, 20, Component.translatable("screen.savedcommands.settings.manualcategories"));
        addRenderableWidget(manualCategoriesButton);

        closeButton = Button.builder(CommonComponents.GUI_DONE, _ -> exit()).bounds(popupX + (popupW - 100 - 150 - 5) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        addRenderableWidget(closeButton);

        generalSettingsButton = Button.builder(Component.translatable("screen.savedcommands.globalsettings"), _ -> {
            exit();
            minecraft.setScreen(new GlobalSettingsScreen(parent));
        }).bounds(popupX + (popupW - 100 - 150 - 5) / 2 + 100 + 5, popupY + popupH - 20 - 20, 150, 20).build();
        addRenderableWidget(generalSettingsButton);
    }

    @Override
    public void repositionElements() {
        super.repositionElements();

        receiveSentCommandsButton.setRectangle(popupW - 40, 20, popupX + 20, popupY + 30);
        msgCommandEditBox.setRectangle(popupW - 40, 20, popupX + 20, receiveSentCommandsButton.getY() + receiveSentCommandsButton.getHeight() + 20);
        deleteWarningButton.setRectangle(popupW - 40, 20, popupX + 20, msgCommandEditBox.getY() + msgCommandEditBox.getHeight() + 10);
        manualCategoriesButton.setRectangle(popupW - 40, 20, popupX + 20, deleteWarningButton.getY() + deleteWarningButton.getHeight() + 10);

        closeButton.setPosition(popupX + (popupW - 100 - 150 - 5) / 2, popupY + popupH - 20 - 20);
        generalSettingsButton.setPosition(popupX + (popupW - 100 - 150 - 5) / 2 + 100 + 5, popupY + popupH - 20 - 20);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        ctx.text(
                this.font,
                translatable("screen.savedcommands.settings.msgcommand"),
                popupX + 20,
                receiveSentCommandsButton.getY() + receiveSentCommandsButton.getHeight() + 10,
                0xFFFFFFFF,
                true
        );
    }

    @Override
    public void exit() {
        if (commandManager == null) {
            super.exit();
            return;
        }
        if (commandManager.data.worldSettings == null) {
            commandManager.data.worldSettings = new SettingsManager.Settings();
        }

        if (receiveSentCommandsButton.getValue().equals(BooleanObjectOptions.DEFAULT)) {
            commandManager.data.worldSettings.receiveCommands = null; // Back to default
        } else {
            commandManager.data.worldSettings.receiveCommands = receiveSentCommandsButton.getValue().getBoolean();
        }

        if (msgCommandEditBox.getValue().isEmpty()) {
            commandManager.data.worldSettings.msgCommand = null; // Back to default
        } else {
            commandManager.data.worldSettings.msgCommand = msgCommandEditBox.getValue().trim();
        }

        if (deleteWarningButton.getValue().equals(BooleanObjectOptions.DEFAULT)) {
            commandManager.data.worldSettings.deleteWarning = null; // Back to default
        } else {
            commandManager.data.worldSettings.deleteWarning = deleteWarningButton.getValue().getBoolean();
        }

        if (manualCategoriesButton.getValue().equals(BooleanObjectOptions.DEFAULT)) {
            commandManager.data.worldSettings.manualCategories = null; // Back to default
        } else {
            commandManager.data.worldSettings.manualCategories = manualCategoriesButton.getValue().getBoolean();
        }

        if ((manualCategoriesButton.getValue().equals(BooleanObjectOptions.DISABLED) ||
                (manualCategoriesButton.getValue().equals(BooleanObjectOptions.DEFAULT) && !globalSettings.manualCategories)) && commandManager.data.customCategories != null) {
            if (commandManager.data.customCategories.isEmpty()) {
                commandManager.data.customCategories = null;
            } else {
                minecraft.setScreen(new PopupConfirmScreen(Component.translatable("screen.savedcommands.settings.manualcategories.clearquestion"), Component.translatable("screen.savedcommands.settings.manualcategories.clearmessage"),
                        parent, Component.translatable("screen.savedcommands.clear"), Component.translatable("screen.savedcommands.keep"), () -> {
                    commandManager.data.customCategories = null;
                    for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                        command.categoryId = null;
                    }
                    commandManager.saveAsync();
                }));
                commandManager.saveAsync();
                return;
            }
        }

        commandManager.saveAsync();
        super.exit();
    }
}
