package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

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
                        })
                        .withInitialValue((SavedCommandsClient.commandManager != null && SavedCommandsClient.commandManager.data.worldSettings != null)
                                ? booleanToEnum(SavedCommandsClient.commandManager.data.worldSettings.receiveCommands) : BooleanObjectOptions.DEFAULT)
                        .withValues(BooleanObjectOptions.values())
                        .create(popupX + 20, popupY + 30, popupW - 40, 20, Component.translatable("screen.savedcommands.settings.receivecommands"));
        addRenderableWidget(receiveSentCommandsButton);

        assert this.minecraft != null;
        msgCommandEditBox = new EditBox(this.minecraft.font, popupX + 20, receiveSentCommandsButton.getY() + receiveSentCommandsButton.getHeight() + 20, popupW - 40, 20, translatable("advMode.command"));
        msgCommandEditBox.setMaxLength(256);
        msgCommandEditBox.setBordered(true);
        msgCommandEditBox.setCanLoseFocus(true);
        msgCommandEditBox.setHint(Component.literal(globalSettings.msgCommand));
        msgCommandEditBox.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.worldsettings.leaveemtydefault")));
        if (SavedCommandsClient.commandManager != null && SavedCommandsClient.commandManager.data.worldSettings != null && SavedCommandsClient.commandManager.data.worldSettings.msgCommand != null) {
            msgCommandEditBox.setValue(SavedCommandsClient.commandManager.data.worldSettings.msgCommand);
        }
        this.addRenderableWidget(msgCommandEditBox);

        deleteWarningButton =
                CycleButton.builder((BooleanObjectOptions option) -> switch (option) {
                            case ENABLED, DISABLED -> option.getComponent();
                            case DEFAULT -> option.getComponent(globalSettings.deleteWarning);
                        })
                        .withInitialValue((SavedCommandsClient.commandManager != null && SavedCommandsClient.commandManager.data.worldSettings != null)
                                ? booleanToEnum(SavedCommandsClient.commandManager.data.worldSettings.deleteWarning) : BooleanObjectOptions.DEFAULT)
                        .withValues(BooleanObjectOptions.values())
                        .create(popupX + 20, msgCommandEditBox.getY() + msgCommandEditBox.getHeight() + 10, popupW - 40, 20, Component.translatable("screen.savedcommands.settings.deletewarning"));
        addRenderableWidget(deleteWarningButton);

        manualCategoriesButton =
                CycleButton.builder((BooleanObjectOptions option) -> switch (option) {
                            case ENABLED, DISABLED -> option.getComponent();
                            case DEFAULT -> option.getComponent(globalSettings.manualCategories);
                        })
                        .withInitialValue((SavedCommandsClient.commandManager != null && SavedCommandsClient.commandManager.data.worldSettings != null)
                                ? booleanToEnum(SavedCommandsClient.commandManager.data.worldSettings.manualCategories) : BooleanObjectOptions.DEFAULT)
                        .withValues(BooleanObjectOptions.values())
                        .create(popupX + 20, deleteWarningButton.getY() + deleteWarningButton.getHeight() + 10, popupW - 40, 20, Component.translatable("screen.savedcommands.settings.manualcategories"));
        addRenderableWidget(manualCategoriesButton);

        closeButton = Button.builder(CommonComponents.GUI_DONE, button -> exit()).bounds(popupX + (popupW - 100 - 150 - 5) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        addRenderableWidget(closeButton);

        generalSettingsButton = Button.builder(Component.translatable("screen.savedcommands.globalsettings"), button -> {
            exit();
            minecraft.setScreen(new GlobalSettingsScreen(parent));
        }).bounds(popupX + (popupW - 100 - 150 - 5) / 2 + 100 + 5, popupY + popupH - 20 - 20, 150, 20).build();
        addRenderableWidget(generalSettingsButton);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawString(
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
        if (SavedCommandsClient.commandManager == null) {
            super.exit();
            return;
        }
        if (SavedCommandsClient.commandManager.data.worldSettings == null) {
            SavedCommandsClient.commandManager.data.worldSettings = new SettingsManager.Settings();
        }

        if (receiveSentCommandsButton.getValue().equals(BooleanObjectOptions.DEFAULT)) {
            SavedCommandsClient.commandManager.data.worldSettings.receiveCommands = null; // Back to default
        } else {
            SavedCommandsClient.commandManager.data.worldSettings.receiveCommands = receiveSentCommandsButton.getValue().getBoolean();
        }

        if (msgCommandEditBox.getValue().isEmpty()) {
            SavedCommandsClient.commandManager.data.worldSettings.msgCommand = null; // Back to default
        } else {
            SavedCommandsClient.commandManager.data.worldSettings.msgCommand = msgCommandEditBox.getValue().trim();
        }

        if (deleteWarningButton.getValue().equals(BooleanObjectOptions.DEFAULT)) {
            SavedCommandsClient.commandManager.data.worldSettings.deleteWarning = null; // Back to default
        } else {
            SavedCommandsClient.commandManager.data.worldSettings.deleteWarning = deleteWarningButton.getValue().getBoolean();
        }

        if (manualCategoriesButton.getValue().equals(BooleanObjectOptions.DEFAULT)) {
            SavedCommandsClient.commandManager.data.worldSettings.manualCategories = null; // Back to default
        } else {
            SavedCommandsClient.commandManager.data.worldSettings.manualCategories = manualCategoriesButton.getValue().getBoolean();
        }

        SavedCommandsClient.commandManager.saveAsync();
        super.exit();
    }
}
