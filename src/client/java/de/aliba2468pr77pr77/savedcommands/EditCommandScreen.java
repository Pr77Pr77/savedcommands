package de.aliba2468pr77pr77.savedcommands;

import de.aliba2468pr77pr77.savedcommands.mixin.client.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.VariablePlaceholder;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;
import static net.minecraft.text.Text.*;

public class EditCommandScreen extends Screen {
    private final Screen parent;
    private ButtonWidget closeButton;
    public TextFieldWidget commandTextField;
    private TextFieldWidget nameTextField;
    private ButtonWidget keybindButton;
    private ButtonWidget removeKeybindButton;
    public boolean keybindSetting = false;
    private SavedCommandManager.CommandData data;
    private final List<conflictSavedCommands> KeybindConflictsSavedCommands = new ArrayList<>();
    private final List<conflictMinecraftKB> KeybindConflictsMinecraft = new ArrayList<>();
    private ButtonWidget addVariableButton;
    public InsertedVariables insertedVariables;

    ChatInputSuggestor CommandSuggestor;

    public int popupW;
    public int popupH;
    public int popupX;
    public int popupY;

    public EditCommandScreen(Screen parent, SavedCommandManager.CommandData data) {
        super(translatable("screen.savedcommands.editpopup"));
        this.parent = parent;
        this.data = data;
        insertedVariables = new InsertedVariables();
    }

    private void searchConflicts() {
        KeybindConflictsSavedCommands.clear();
        KeybindConflictsMinecraft.clear();
        if (data != null && data.keybinds != null && !data.keybinds.isEmptyOrNull()) {
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
        if (data != null && data.keybinds != null && !data.keybinds.isEmptyOrNull()) {
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
        popupH = Math.min(230, this.height - 40);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        this.closeButton = ButtonWidget.builder(translatable("gui.done"), b -> exit()).dimensions(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();

        this.addDrawableChild(this.closeButton);

        assert this.client != null;
        this.commandTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 40, popupW - 40, 20, translatable("advMode.command"));
        this.commandTextField.setMaxLength(256);
        this.commandTextField.setDrawsBackground(true);
        this.commandTextField.setFocusUnlocked(true);
        if (data != null && data.command != null) {
            this.commandTextField.setText(data.command);
        }
        this.addDrawableChild(this.commandTextField);

        this.addVariableButton = ButtonWidget.builder(Text.literal("+"), b -> {
            LOGGER.info("Creating Variable...");
            if(!save(true)){
                return;
            }
            MinecraftClient.getInstance().setScreen(new EditVariableScreen(this, data));
        }).dimensions(popupX + 20, popupY + 85, 20, 20).build();
        this.addDrawableChild(this.addVariableButton);

        if (data != null && data.variables != null) {
            for (int variableIndex = 0; variableIndex < data.variables.size(); variableIndex++) {
                int finalVariableIndex = variableIndex;
                ButtonWidget variableButton = ButtonWidget.builder(Text.literal(String.valueOf(data.variables.get(variableIndex).abbreviation)), bu -> {
                    LOGGER.info("Adding Variable " + finalVariableIndex);
                    commandTextField.setText(commandTextField.getText().substring(0, commandTextField.getCursor()) + VariablePlaceholder + data.variables.get(finalVariableIndex).abbreviation + commandTextField.getText().substring(commandTextField.getCursor()));
                }).dimensions(popupX + 20 + 25 + 45 * variableIndex, popupY + 85, 20, 20).build();
                this.addDrawableChild(variableButton);

                ButtonWidget variableEditButton = new IconButton(popupX + 20 + 45 + 45 * variableIndex, popupY + 85, 20, 20, Identifier.of(MOD_ID, "textures/gui/edit.png"), b -> {
                    LOGGER.info("Editing Variable...");
                    if(!save(true)){
                        return;
                    }
                    MinecraftClient.getInstance().setScreen(new EditVariableScreen(this, data.variables.get(finalVariableIndex), data));
                });
                this.addDrawableChild(variableEditButton);
            }
        }

        this.nameTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 120, popupW - 40, 20, translatable("screen.savedcommands.name"));
        this.nameTextField.setMaxLength(256);
        this.nameTextField.setDrawsBackground(true);
        this.nameTextField.setFocusUnlocked(true);
        if (data != null && data.name != null) {
            this.nameTextField.setText(data.name);
        }
        this.addDrawableChild(this.nameTextField);

        searchConflicts();

        this.keybindButton = ButtonWidget.builder(getKeybindButtonText(), b -> {
            if(!save(true)){
                return;
            }
            keybindSetting = true;
            keybindButton.setMessage(getKeybindButtonText());
            keybindButton.setTooltip(Tooltip.of(empty()));
            if(data == null){
                return;
            }
            data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
            LOGGER.info("Changing the keybind...");
        }).dimensions(popupX + 20, popupY + 165, Math.round((popupW - 40) * 0.7F), 20).build();

        this.addDrawableChild(this.keybindButton);

        this.removeKeybindButton = ButtonWidget.builder(translatable("screen.savedcommands.remove"), b -> {
            if(data == null){
                return;
            }
            LOGGER.info("Removing the keybind...");
            data.keybinds = null;
            searchConflicts();
            keybindButton.setTooltip(Tooltip.of(empty()));
            keybindButton.setMessage(getKeybindButtonText());
        }).dimensions(popupX + Math.round((popupW - 40) * 0.7F) + 25, popupY + 165, popupW - 45 - Math.round((popupW - 40) * 0.7F), 20).build();

        this.addDrawableChild(this.removeKeybindButton);

        searchConflicts();

        CommandSuggestor = new ChatInputSuggestor(client, this, commandTextField, textRenderer, false, false, 1, 10, false, 0xD8000000);
        CommandSuggestor.setCanLeave(false);
        CommandSuggestor.setWindowActive(true);
        this.commandTextField.setChangedListener((String text) -> {
            if (text.isEmpty()) {
                closeButton.setMessage(translatable("gui.cancel"));
            } else {
                closeButton.setMessage(translatable("gui.done"));
            }
            removeOrphanedVariablePlaceholders(text);
            insertedVariables.refresh();
            CommandSuggestor.refresh();
        });
        if (commandTextField.getText().isEmpty()) {
            closeButton.setMessage(translatable("gui.cancel"));
        } else {
            closeButton.setMessage(translatable("gui.done"));
        }
        insertedVariables.refresh();
        CommandSuggestor.refresh();
    }

    public class InsertedVariables {
        String insertedVariableText;
        int cursor;

        public void refresh() {
            refresh(null);
        }

        public Integer refresh(Integer insertedIndex) {
            String original = commandTextField.getText();
            if (data == null || data.variables == null || data.variables.isEmpty() || !original.contains(String.valueOf(VariablePlaceholder))) {
                insertedVariableText = original;
                cursor = commandTextField.getCursor();
                return insertedIndex;
            } else { // There are variables
                StringBuilder insertedCommand = new StringBuilder();
                int index = original.indexOf(VariablePlaceholder);
                int ContinuingIndex = 0;
                boolean cursorSet = false;
                Integer newIndex = null;
                while (index != -1) {
                    int finalIndex = index;
                    Optional<SavedCommandManager.CommandData.variable> optionalVariable = data.variables.stream().filter(v -> original.length() > finalIndex + 1 && v.abbreviation == original.charAt(finalIndex + 1)).findFirst();
                    if (optionalVariable.isPresent()) {
                        if (ContinuingIndex - 1 <= commandTextField.getCursor() && index >= commandTextField.getCursor() && !cursorSet) {
                            cursor = insertedCommand.length() + (commandTextField.getCursor() - ContinuingIndex);
                            cursorSet = true;
                        }

                        if (insertedIndex != null && insertedCommand.length() <= insertedIndex && insertedCommand.length() + (index - ContinuingIndex) >= insertedIndex) {
                            newIndex = ContinuingIndex + (insertedIndex - insertedCommand.length());
                        }

                        insertedCommand.append(original.substring(ContinuingIndex, index));
                        int insertedIndexVariable = insertedCommand.length();
                        ClientPlayerEntity player = MinecraftClient.getInstance().player;
                        assert player != null;
                        switch (optionalVariable.get().type) {
                            case ITEMHAND:
                                insertedCommand.append(Registries.ITEM.getId(player.getMainHandStack().getItem()));
                                break;
                            case PLAYERPOSX:
                                insertedCommand.append(player.getBlockX());
                                break;
                            case PLAYERPOSY:
                                insertedCommand.append(player.getBlockY());
                                break;
                            case PLAYERPOSZ:
                                insertedCommand.append(player.getBlockZ());
                                break;
                            default:
                                insertedCommand.append(optionalVariable.get().defaultValue);
                        }

                        if (insertedIndex != null && insertedIndexVariable <= insertedIndex && insertedCommand.length() > insertedIndex) {
                            newIndex = index;
                        }
                        // Hallo -X Bla -Y ÖÖÖ -Z LLLL
                        // Hallo 123456 Bla 123456 ÖÖÖ 78901 LLLL

                        ContinuingIndex = index + 2;
                    }

                    index = original.indexOf(VariablePlaceholder, index + 1);
                }

                if (ContinuingIndex - 1 <= commandTextField.getCursor() && !cursorSet) {
                    cursor = insertedCommand.length() + (commandTextField.getCursor() - ContinuingIndex);
                }
                if (newIndex == null && insertedIndex != null && insertedCommand.length() <= insertedIndex) {
                    newIndex = ContinuingIndex + (insertedIndex - insertedCommand.length());
                }

                insertedCommand.append(original.substring(ContinuingIndex));

                insertedVariableText = insertedCommand.toString();

                return newIndex;
            }
        }

        public String getText() {
            if (insertedVariableText == null) {
                refresh();
            }
            return insertedVariableText;
        }

        public int getCursor() {
            if (insertedVariableText == null) {
                refresh();
            }
            return cursor;
        }

        public int getUninsertedIndex(int insertedIndex) {
            return refresh(insertedIndex);
        }
    }

    private void removeOrphanedVariablePlaceholders(String command) {
        if (command.contains(String.valueOf(VariablePlaceholder))) {
            StringBuilder cleanCommand = new StringBuilder();
            int index = command.indexOf(VariablePlaceholder);
            int ContinuingIndex = 0;
            while (index != -1) {
                int finalIndex = index;
                if (data != null && data.variables.stream().noneMatch(v -> command.length() > finalIndex + 1 && v.abbreviation == command.charAt(finalIndex + 1))) {
                    cleanCommand.append(command.substring(ContinuingIndex, index));
                    ContinuingIndex = index + 1;
                }

                index = command.indexOf(VariablePlaceholder, index + 1);
            }
            cleanCommand.append(command.substring(ContinuingIndex));
            if (!cleanCommand.toString().equals(command)) {
                int cursor = commandTextField.getCursor() - 1;
                commandTextField.setText(cleanCommand.toString());
                commandTextField.setCursor(cursor, false);
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        popupW = Math.min(300, this.width - 40);
        popupH = Math.min(230, this.height - 40);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

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
                translatable("screen.savedcommands.variables"),
                popupX + 20,
                popupY + 75,
                0xFFFFFFFF,
                true
        );

        ctx.drawText(
                this.textRenderer,
                translatable("screen.savedcommands.name"),
                popupX + 20,
                popupY + 110,
                0xFFFFFFFF,
                true
        );

        ctx.drawText(
                this.textRenderer,
                translatable("controls.keybinds.title"),
                popupX + 20,
                popupY + 150,
                0xFFFFFFFF,
                true
        );

        super.render(ctx, mouseX, mouseY, delta);

        CommandSuggestor.render(ctx, mouseX, mouseY);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (parent == null) {
            super.renderBackground(context, mouseX, mouseY, deltaTicks);
        } else {
            this.parent.renderBackground(context, -2147483648, -2147483648, deltaTicks);
            this.parent.render(context, -2147483648, -2147483648, deltaTicks);
            context.setCursor(StandardCursors.ARROW);
            context.fill(0, 0, this.width, this.height, 0x88000000);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.ofVanilla("popup/background"), popupX, popupY, popupW, popupH);
        }
    }

    @Override
    public void setFocused(@Nullable Element focused) {
        CommandSuggestor.setWindowActive(focused == commandTextField);
        super.setFocused(focused);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (keybindSetting && data != null && (data.keybinds == null || data.keybinds.isEmptyOrNull() || !(data.keybinds.keybindType.contains("MOUSE") && data.keybinds.keybindCode.contains(click.button())))) {
            if (data.keybinds == null) {
                data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
            }
            data.keybinds.keybindType.add("MOUSE");
            data.keybinds.keybindCode.add(click.button());
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        }
        if (CommandSuggestor.mouseClicked(click)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (keybindSetting && data != null && data.keybinds != null && data.keybinds.keybindType.contains("MOUSE") && data.keybinds.keybindCode.contains(click.button())) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (CommandSuggestor.mouseScrolled(verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (keybindSetting && data != null && (data.keybinds == null || data.keybinds.isEmptyOrNull() || !(data.keybinds.keybindType.contains("KEYSYM") && data.keybinds.keybindCode.contains(input.key())))) {
            if (data.keybinds == null) {
                data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
            }
            data.keybinds.keybindType.add("KEYSYM");
            data.keybinds.keybindCode.add(input.key());
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        }
        if (CommandSuggestor.keyPressed(input)) {
            return true;
        }
        if (input.isEscape() && this.shouldCloseOnEsc()) {
            exit();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (keybindSetting && data != null && data.keybinds != null && data.keybinds.keybindType.contains("KEYSYM") && data.keybinds.keybindCode.contains(input.key())) {
            keybindSetting = false;
            commandManager.saveAsync();
            searchConflicts();
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        } else {
            return super.keyReleased(input);
        }
    }

    private boolean save(boolean warnOnEmptyCommandIfNull) {
        if (data == null) {
            if (!commandTextField.getText().isEmpty()) {
                data = commandManager.addCommand(commandTextField.getText(), nameTextField.getText());
            } else if (warnOnEmptyCommandIfNull) {
                MinecraftClient.getInstance().setScreen(new PopupWarningScreen(Text.translatable("screen.savedcommands.emptyCommandHeader"),
                        Text.translatable("screen.savedcommands.emptyCommandMessage"), this));
                return false;
            } else {
                return false;
            }
        }
        if (commandTextField.getText() != null && !Objects.equals(commandTextField.getText(), "")) {
            data.command = commandTextField.getText();
        }
        if (nameTextField.getText() == null || Objects.equals(nameTextField.getText(), "")) {
            data.name = null;
        } else {
            data.name = nameTextField.getText();
        }
        commandManager.saveAsync();
        return true;
    }

    private void exit() {
        save(false);
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
