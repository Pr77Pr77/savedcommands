package de.aliba2468pr77pr77.savedcommands;

import de.aliba2468pr77pr77.savedcommands.mixin.client.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;
import static net.minecraft.text.Text.*;

public class EditCommandScreen extends Screen {
    private final Screen parent;
    private ButtonWidget closeButton;
    private TextFieldWidget commandTextField;
    private TextFieldWidget nameTextField;
    private ButtonWidget keybindButton;
    private ButtonWidget removeKeybindButton;
    public boolean keybindSetting = false;
    final private SavedCommandManager.CommandData data;
    private final List<conflictSavedCommands> KeybindConflictsSavedCommands = new ArrayList<>();
    private final List<conflictMinecraftKB> KeybindConflictsMinecraft = new ArrayList<>();

    int popupW;
    int popupH;
    int popupX;
    int popupY;

    public EditCommandScreen(Screen parent, SavedCommandManager.CommandData data) {
        super(translatable("screen.savedcommands.editpopup"));
        this.parent = parent;
        this.data = data;
    }

    private void searchConflicts() {
        KeybindConflictsSavedCommands.clear();
        KeybindConflictsMinecraft.clear();
        if (data.keybinds != null && !data.keybinds.isEmptyOrNull()) {
            // Checking Keybinds of this mod (Only first one)
            for (SavedCommandManager.CommandData command : commandManager.data.commands) {
                if (command.keybinds != null && !command.keybinds.isEmptyOrNull() && Objects.equals(command.keybinds.keybindCode.getFirst(), data.keybinds.keybindCode.getFirst()) &&
                        Objects.equals(command.keybinds.keybindType.getFirst(), data.keybinds.keybindType.getFirst()) &&
                        !Objects.equals(command, data)) {
                    conflictSavedCommands conflict = new conflictSavedCommands();
                    conflict.conflictingCommand = command;

                    conflict.conflictingKeys = new SavedCommandManager.CommandData.keybindCombination();
                    boolean differentEndings = false;
                    for (int keybindIndex = 0; keybindIndex < command.keybinds.keybindType.size() && keybindIndex < data.keybinds.keybindType.size(); keybindIndex++) {
                        if (Objects.equals(command.keybinds.keybindCode.get(keybindIndex), data.keybinds.keybindCode.get(keybindIndex)) &&
                                Objects.equals(command.keybinds.keybindType.get(keybindIndex), data.keybinds.keybindType.get(keybindIndex))) {
                            conflict.conflictingKeys.keybindType.add(keybindIndex, data.keybinds.keybindType.get(keybindIndex));
                            conflict.conflictingKeys.keybindCode.add(keybindIndex, data.keybinds.keybindCode.get(keybindIndex));
                        } else {
                            differentEndings = true;
                            break;
                        }
                    }
                    if (!differentEndings) {
                        KeybindConflictsSavedCommands.add(conflict);
                    }
                }
            }

            // Checking Minecraft keybinds
            for (KeyBinding keybind : MinecraftClient.getInstance().options.allKeys) {
                if (!keybind.isUnbound() &&
                        Objects.equals(((KeyBindingAccessor) keybind).getBoundKey(), data.keybinds.toKeys().getFirst())) {
                    conflictMinecraftKB conflict = new conflictMinecraftKB();
                    conflict.conflictingKey = data.keybinds.toKeys().getFirst();
                    conflict.conflictingKeybind = keybind;
                    KeybindConflictsMinecraft.add(conflict);
                }
            }

            MutableText tooltipContent = empty();
            if (!KeybindConflictsSavedCommands.isEmpty()) {
                tooltipContent.append(translatable("screen.savedcommands.duplicateKeybindCombinationSC"));

                for (conflictSavedCommands keybindConflictSavedCommands : KeybindConflictsSavedCommands) {
                    if (keybindConflictSavedCommands.conflictingKeys != null && !keybindConflictSavedCommands.conflictingKeys.isEmptyOrNull()) {
                        tooltipContent.append(literal("\n"));
                        boolean OneElemAlreadyAdded = false;
                        MutableText KeyText = empty();
                        for (InputUtil.Key Key : keybindConflictSavedCommands.conflictingKeys.toKeys()) {
                            if (OneElemAlreadyAdded) {
                                KeyText.append(literal(" + "));
                            }
                            KeyText.append(Key.getLocalizedText());
                            OneElemAlreadyAdded = true;
                        }
                        KeyText.append(literal(": "));
                        tooltipContent.append(KeyText.withColor(0xfffcfc54));
                        if (keybindConflictSavedCommands.conflictingCommand.name != null && !keybindConflictSavedCommands.conflictingCommand.name.isEmpty()) {
                            tooltipContent.append(keybindConflictSavedCommands.conflictingCommand.name);
                        } else {
                            tooltipContent.append(keybindConflictSavedCommands.conflictingCommand.command);
                        }
                    }
                }
            }

            if (!KeybindConflictsMinecraft.isEmpty()) {
                if (!KeybindConflictsSavedCommands.isEmpty()) {
                    tooltipContent.append(literal("\n"));
                }
                tooltipContent.append(translatable("screen.savedcommands.duplicateKeybindCombinationMC"));

                for (conflictMinecraftKB KeybindConflictMinecraft : KeybindConflictsMinecraft) {
                    if (KeybindConflictMinecraft.conflictingKeybind != null && KeybindConflictMinecraft.conflictingKey != null) {
                        tooltipContent.append(literal("\n"));
                        MutableText KeyText = empty();
                        KeyText.append(KeybindConflictMinecraft.conflictingKey.getLocalizedText());
                        KeyText.append(literal(": "));
                        tooltipContent.append(KeyText.withColor(0xfffcfc54));
                        tooltipContent.append(translatable(KeybindConflictMinecraft.conflictingKeybind.getId()));
                    }
                }
            }
            if (keybindButton != null) {
                keybindButton.setTooltip(Tooltip.of(tooltipContent));
            }
        }
    }

    private Text getKeybindButtonText() {
        MutableText buttonContent = empty();

        if (keybindSetting) {
            buttonContent.append(literal("< ").withColor(0xfffcfc54));
        } else if (!KeybindConflictsSavedCommands.isEmpty() || !KeybindConflictsMinecraft.isEmpty()) {
            buttonContent.append(literal("[ ").withColor(0xfffcfc54));
        }
        if (data.keybinds != null && !data.keybinds.isEmptyOrNull()) {
            boolean OneElemAlreadyAdded = false;
            for (InputUtil.Key Key : data.keybinds.toKeys()) {
                if (OneElemAlreadyAdded) {
                    buttonContent.append(literal(" + "));
                }
                buttonContent.append(Key.getLocalizedText());
                OneElemAlreadyAdded = true;
            }
        } else {
            buttonContent.append(translatable("key.keyboard.unknown"));
        }
        if (keybindSetting) {
            buttonContent.append(literal(" >").withColor(0xfffcfc54));
        } else if (!KeybindConflictsSavedCommands.isEmpty() || !KeybindConflictsMinecraft.isEmpty()) {
            buttonContent.append(literal(" ]").withColor(0xfffcfc54));
        }
        return buttonContent;
    }

    @Override
    protected void init() {
        super.init();

        popupW = Math.min(300, this.width - 40);
        popupH = Math.min(190, this.height - 40);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        this.closeButton = ButtonWidget.builder(translatable("gui.done"), b -> exit()).dimensions(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();

        this.addDrawableChild(this.closeButton);

        assert this.client != null;
        this.commandTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 40, popupW - 40, 20, translatable("advMode.command"));
        this.commandTextField.setMaxLength(256);
        this.commandTextField.setDrawsBackground(true);
        this.commandTextField.setFocusUnlocked(true);
        if (data.command != null) {
            this.commandTextField.setText(data.command);
        }
        this.addDrawableChild(this.commandTextField);

        this.nameTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 80, popupW - 40, 20, translatable("screen.savedcommands.name"));
        this.nameTextField.setMaxLength(256);
        this.nameTextField.setDrawsBackground(true);
        this.nameTextField.setFocusUnlocked(true);
        if (data.name != null) {
            this.nameTextField.setText(data.name);
        }
        this.addDrawableChild(this.nameTextField);

        searchConflicts();

        this.keybindButton = ButtonWidget.builder(getKeybindButtonText(), b -> {
            LOGGER.info("Changing the keybind...");
            keybindSetting = true;
            keybindButton.setMessage(getKeybindButtonText());
            keybindButton.setTooltip(Tooltip.of(empty()));
            data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
        }).dimensions(popupX + 20, popupY + 125, Math.round((popupW - 40) * 0.7F), 20).build();

        this.addDrawableChild(this.keybindButton);

        this.removeKeybindButton = ButtonWidget.builder(translatable("screen.savedcommands.remove"), b -> {
            LOGGER.info("Removing the keybind...");
            data.keybinds = null;
            searchConflicts();
            keybindButton.setTooltip(Tooltip.of(empty()));
            keybindButton.setMessage(getKeybindButtonText());
        }).dimensions(popupX + Math.round((popupW - 40) * 0.7F) + 25, popupY + 125, popupW - 45 - Math.round((popupW - 40) * 0.7F), 20).build();

        this.addDrawableChild(this.removeKeybindButton);

        searchConflicts();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.parent != null) {
            this.parent.render(ctx, mouseX, mouseY, delta);
        } else {
            this.renderBackground(ctx, mouseX, mouseY, delta);
        }

        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        popupW = Math.min(300, this.width - 40);
        popupH = Math.min(190, this.height - 40);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.ofVanilla("popup/background"), popupX, popupY, popupW, popupH);

        int textWidth = this.textRenderer.getWidth(title);
        ctx.drawText(
                this.textRenderer,
                title,
                (this.width - textWidth) / 2,
                popupY + 10,
                0xFFFFFFFF,
                false
        );

        ctx.drawText(
                this.textRenderer,
                translatable("advMode.command"),
                popupX + 20,
                popupY + 30,
                0xFFFFFFFF,
                true
        );

        ctx.drawText(
                this.textRenderer,
                translatable("screen.savedcommands.name"),
                popupX + 20,
                popupY + 70,
                0xFFFFFFFF,
                true
        );

        ctx.drawText(
                this.textRenderer,
                translatable("controls.keybinds.title"),
                popupX + 20,
                popupY + 110,
                0xFFFFFFFF,
                true
        );

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (keybindSetting && (data.keybinds == null || data.keybinds.isEmptyOrNull() || !(data.keybinds.keybindType.contains("MOUSE") && data.keybinds.keybindCode.contains(click.button())))) {
            if (data.keybinds == null) {
                data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
            }
            data.keybinds.keybindType.add("MOUSE");
            data.keybinds.keybindCode.add(click.button());
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        } else {
            return super.mouseClicked(click, doubled);
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (keybindSetting && data.keybinds != null && data.keybinds.keybindType.contains("MOUSE") && data.keybinds.keybindCode.contains(click.button())) {
            keybindSetting = false;
            commandManager.saveAsync();
            searchConflicts();
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        } else {
            return super.mouseReleased(click);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (keybindSetting && (data.keybinds == null || data.keybinds.isEmptyOrNull() || !(data.keybinds.keybindType.contains("KEYSYM") && data.keybinds.keybindCode.contains(input.key())))) {
            if (data.keybinds == null) {
                data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
            }
            data.keybinds.keybindType.add("KEYSYM");
            data.keybinds.keybindCode.add(input.key());
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            exit();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (keybindSetting && data.keybinds != null && data.keybinds.keybindType.contains("KEYSYM") && data.keybinds.keybindCode.contains(input.key())) {
            keybindSetting = false;
            commandManager.saveAsync();
            searchConflicts();
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        } else {
            return super.keyReleased(input);
        }
    }

    private void exit() {
        if (commandTextField.getText() != null && !Objects.equals(commandTextField.getText(), "")) {
            data.command = commandTextField.getText();
        }
        if (nameTextField.getText() == null || Objects.equals(nameTextField.getText(), "")) {
            data.name = null;
        } else {
            data.name = nameTextField.getText();
        }
        commandManager.saveAsync();
        if (parent instanceof SavedCommandsScreen) {
            MinecraftClient.getInstance().setScreen(new SavedCommandsScreen());
        } else {
            MinecraftClient.getInstance().setScreen(parent);
        }
    }

    static class conflictSavedCommands { // Conflict with other keybinds of this mod
        SavedCommandManager.CommandData conflictingCommand;

        SavedCommandManager.CommandData.keybindCombination conflictingKeys; // Only the ones in both commands
    }

    static class conflictMinecraftKB { // Conflict with minecraft keybinds
        KeyBinding conflictingKeybind;

        InputUtil.Key conflictingKey; // Only the one in both commands
    }
}
