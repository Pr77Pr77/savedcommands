package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.aliba2468pr77pr77.savedcommands.mixin.client.KeyBindingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.VariablePlaceholder;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;
import static net.minecraft.network.chat.Component.translatable;

public class EditCommandScreen extends Screen {
    private final Screen parent;
    private Button closeButton;
    public EditBox commandTextField;
    private EditBox nameTextField;
    private Button keybindButton;
    private Button removeKeybindButton;
    public boolean keybindSetting = false;
    private SavedCommandManager.CommandData data;
    private final List<conflictSavedCommands> KeybindConflictsSavedCommands = new ArrayList<>();
    private final List<conflictMinecraftKB> KeybindConflictsMinecraft = new ArrayList<>();
    private Button addVariableButton;
    public InsertedVariables insertedVariables;

    CommandSuggestions CommandSuggestor;

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
            for (KeyMapping keybind : Minecraft.getInstance().options.keyMappings) {
                if (!keybind.isUnbound() &&
                        Objects.equals(((KeyBindingAccessor) keybind).getBoundKey(), data.keybinds.toKeys().getFirst())) {
                    conflictMinecraftKB conflict = new conflictMinecraftKB();
                    conflict.conflictingKey = data.keybinds.toKeys().getFirst();
                    conflict.conflictingKeybind = keybind;
                    KeybindConflictsMinecraft.add(conflict);
                }
            }

            MutableComponent tooltipContent = Component.empty();
            if (!KeybindConflictsSavedCommands.isEmpty()) {
                tooltipContent.append(translatable("screen.savedcommands.duplicateKeybindCombinationSC"));

                for (conflictSavedCommands keybindConflictSavedCommands : KeybindConflictsSavedCommands) {
                    if (keybindConflictSavedCommands.conflictingKeys != null && !keybindConflictSavedCommands.conflictingKeys.isEmptyOrNull()) {
                        tooltipContent.append(Component.literal("\n"));
                        boolean OneElemAlreadyAdded = false;
                        MutableComponent KeyText = Component.empty();
                        for (InputConstants.Key Key : keybindConflictSavedCommands.conflictingKeys.toKeys()) {
                            if (OneElemAlreadyAdded) {
                                KeyText.append(Component.literal(" + "));
                            }
                            KeyText.append(Key.getDisplayName());
                            OneElemAlreadyAdded = true;
                        }
                        KeyText.append(Component.literal(": "));
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
                    tooltipContent.append(Component.literal("\n"));
                }
                tooltipContent.append(translatable("screen.savedcommands.duplicateKeybindCombinationMC"));

                for (conflictMinecraftKB KeybindConflictMinecraft : KeybindConflictsMinecraft) {
                    if (KeybindConflictMinecraft.conflictingKeybind != null && KeybindConflictMinecraft.conflictingKey != null) {
                        tooltipContent.append(Component.literal("\n"));
                        MutableComponent KeyText = Component.empty();
                        KeyText.append(KeybindConflictMinecraft.conflictingKey.getDisplayName());
                        KeyText.append(Component.literal(": "));
                        tooltipContent.append(KeyText.withColor(0xfffcfc54));
                        tooltipContent.append(KeybindConflictMinecraft.conflictingKeybind.getTranslatedKeyMessage());
                    }
                }
            }
            if (keybindButton != null) {
                keybindButton.setTooltip(Tooltip.create(tooltipContent));
            }
        }
    }

    private Component getKeybindButtonText() {
        MutableComponent buttonContent = Component.empty();

        if (keybindSetting) {
            buttonContent.append(Component.literal("< ").withColor(0xfffcfc54));
        } else if (!KeybindConflictsSavedCommands.isEmpty() || !KeybindConflictsMinecraft.isEmpty()) {
            buttonContent.append(Component.literal("[ ").withColor(0xfffcfc54));
        }
        if (data != null && data.keybinds != null && !data.keybinds.isEmptyOrNull()) {
            boolean OneElemAlreadyAdded = false;
            for (InputConstants.Key Key : data.keybinds.toKeys()) {
                if (OneElemAlreadyAdded) {
                    buttonContent.append(Component.literal(" + "));
                }
                buttonContent.append(Key.getDisplayName());
                OneElemAlreadyAdded = true;
            }
        } else {
            buttonContent.append(translatable("key.keyboard.unknown"));
        }
        if (keybindSetting) {
            buttonContent.append(Component.literal(" >").withColor(0xfffcfc54));
        } else if (!KeybindConflictsSavedCommands.isEmpty() || !KeybindConflictsMinecraft.isEmpty()) {
            buttonContent.append(Component.literal(" ]").withColor(0xfffcfc54));
        }
        return buttonContent;
    }

    @Override
    protected void init() {
        super.init();

        popupW = Math.min(300, this.width - 20);
        popupH = Math.min(230, this.height - 20);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        this.closeButton = Button.builder(translatable("gui.done"), b -> exit()).bounds(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();

        this.addRenderableWidget(this.closeButton);

        this.commandTextField = new EditBox(this.minecraft.font, popupX + 20, popupY + 40, popupW - 40, 20, translatable("advMode.command"));
        this.commandTextField.setMaxLength(256);
        this.commandTextField.setBordered(true);
        this.commandTextField.setCanLoseFocus(true);
        if (data != null && data.command != null) {
            this.commandTextField.setValue(data.command);
        }
        this.addRenderableWidget(this.commandTextField);

        this.addVariableButton = Button.builder(Component.literal("+"), b -> {
                    LOGGER.info("Creating Variable...");
                    if (!save(true)) {
                        return;
                    }
                    minecraft.setScreen(new EditVariableScreen(this, data));
                }).bounds(popupX + 20, popupY + 85, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.savedcommands.createvariable")))
                .createNarration((unused) -> Component.translatable("screen.savedcommands.createvariable")).build();
        this.addRenderableWidget(this.addVariableButton);

        if (data != null && data.variables != null) {
            for (int variableIndex = 0; variableIndex < data.variables.size(); variableIndex++) {
                int finalVariableIndex = variableIndex;
                Button variableButton = Button.builder(Component.literal(String.valueOf(data.variables.get(variableIndex).abbreviation)), bu -> {
                            LOGGER.info("Adding Variable " + finalVariableIndex);
                            commandTextField.setValue(commandTextField.getValue().substring(0, commandTextField.getCursorPosition()) + VariablePlaceholder + data.variables.get(finalVariableIndex).abbreviation + commandTextField.getValue().substring(commandTextField.getCursorPosition()));
                        }).bounds(popupX + 20 + 25 + 45 * variableIndex, popupY + 85, 20, 20)
                        .tooltip(Tooltip.create(Component.translatable("screen.savedcommands.insertvariablebutton", data.variables.get(variableIndex).name)))
                        .createNarration((unused) -> Component.translatable("screen.savedcommands.insertvariablebutton", data.variables.get(finalVariableIndex).name)).build();
                this.addRenderableWidget(variableButton);

                Button variableEditButton = new IconButton(popupX + 20 + 45 + 45 * variableIndex, popupY + 85, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/edit.png"), b -> {
                    LOGGER.info("Editing Variable...");
                    if (!save(true)) {
                        return;
                    }
                    minecraft.setScreen(new EditVariableScreen(this, data.variables.get(finalVariableIndex), data));
                }, Component.translatable("screen.savedcommands.editvariablebutton", data.variables.get(variableIndex).name));
                this.addRenderableWidget(variableEditButton);
            }
        }

        this.nameTextField = new EditBox(this.minecraft.font, popupX + 20, popupY + 120, popupW - 40, 20, translatable("screen.savedcommands.name"));
        this.nameTextField.setMaxLength(256);
        this.nameTextField.setBordered(true);
        this.nameTextField.setCanLoseFocus(true);
        if (data != null && data.name != null) {
            this.nameTextField.setValue(data.name);
        }
        this.addRenderableWidget(this.nameTextField);

        searchConflicts();

        this.keybindButton = Button.builder(getKeybindButtonText(), b -> {
            if (!save(true)) {
                return;
            }
            keybindSetting = true;
            keybindButton.setMessage(getKeybindButtonText());
            keybindButton.setTooltip(Tooltip.create(Component.empty()));
            if (data == null) {
                return;
            }
            data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
            LOGGER.info("Changing the keybind...");
        }).bounds(popupX + 20, popupY + 165, Math.round((popupW - 40) * 0.7F), 20).build();

        this.addRenderableWidget(this.keybindButton);

        this.removeKeybindButton = Button.builder(translatable("screen.savedcommands.remove"), b -> {
            if (data == null) {
                return;
            }
            LOGGER.info("Removing the keybind...");
            data.keybinds = null;
            searchConflicts();
            keybindButton.setTooltip(Tooltip.create(Component.empty()));
            keybindButton.setMessage(getKeybindButtonText());
        }).bounds(popupX + Math.round((popupW - 40) * 0.7F) + 25, popupY + 165, popupW - 45 - Math.round((popupW - 40) * 0.7F), 20).build();

        this.addRenderableWidget(this.removeKeybindButton);

        searchConflicts();

        CommandSuggestor = new CommandSuggestions(minecraft, this, commandTextField, font, false, false, 1, 10, false, 0xD8000000);
        CommandSuggestor.setAllowHiding(false);
        CommandSuggestor.setAllowSuggestions(true);
        this.commandTextField.setResponder((String text) -> {
            if (text.isEmpty()) {
                closeButton.setMessage(translatable("gui.cancel"));
            } else {
                closeButton.setMessage(translatable("gui.done"));
            }
            removeOrphanedVariablePlaceholders(text);
            insertedVariables.refresh();
            CommandSuggestor.updateCommandInfo();
        });
        if (commandTextField.getValue().isEmpty()) {
            closeButton.setMessage(translatable("gui.cancel"));
        } else {
            closeButton.setMessage(translatable("gui.done"));
        }
        insertedVariables.refresh();
        CommandSuggestor.updateCommandInfo();
    }

    public class InsertedVariables {
        String insertedVariableText;
        int cursor;

        public void refresh() {
            refresh(null);
        }

        public Integer refresh(Integer insertedIndex) {
            String original = commandTextField.getValue();
            if (data == null || data.variables == null || data.variables.isEmpty() || !original.contains(String.valueOf(VariablePlaceholder))) {
                insertedVariableText = original;
                cursor = commandTextField.getCursorPosition();
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
                        if (ContinuingIndex - 1 <= commandTextField.getCursorPosition() && index >= commandTextField.getCursorPosition() && !cursorSet) {
                            cursor = insertedCommand.length() + (commandTextField.getCursorPosition() - ContinuingIndex);
                            cursorSet = true;
                        }

                        if (insertedIndex != null && insertedCommand.length() <= insertedIndex && insertedCommand.length() + (index - ContinuingIndex) >= insertedIndex) {
                            newIndex = ContinuingIndex + (insertedIndex - insertedCommand.length());
                        }

                        insertedCommand.append(original.substring(ContinuingIndex, index));
                        int insertedIndexVariable = insertedCommand.length();
                        LocalPlayer player = minecraft.player;
                        assert player != null;
                        switch (optionalVariable.get().type) {
                            case ITEMHAND:
                                insertedCommand.append(player.getMainHandItem().getItem());
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

                        ContinuingIndex = index + 2;
                    }

                    index = original.indexOf(VariablePlaceholder, index + 1);
                }

                if (ContinuingIndex - 1 <= commandTextField.getCursorPosition() && !cursorSet) {
                    cursor = insertedCommand.length() + (commandTextField.getCursorPosition() - ContinuingIndex);
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
                int cursor = commandTextField.getCursorPosition() - 1;
                commandTextField.setValue(cleanCommand.toString());
                commandTextField.setCursorPosition(cursor);
            }
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        popupW = Math.min(300, this.width - 20);
        popupH = Math.min(230, this.height - 20);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        int textWidth = this.font.width(title);
        ctx.drawString(
                this.font,
                title,
                (this.width - textWidth) / 2,
                popupY + 10,
                0xFFFFFFFF,
                false
        );

        ctx.drawString(
                this.font,
                translatable("advMode.command"),
                popupX + 20,
                popupY + 30,
                0xFFFFFFFF,
                true
        );

        ctx.drawString(
                this.font,
                translatable("screen.savedcommands.variables"),
                popupX + 20,
                popupY + 75,
                0xFFFFFFFF,
                true
        );

        ctx.drawString(
                this.font,
                translatable("screen.savedcommands.name"),
                popupX + 20,
                popupY + 110,
                0xFFFFFFFF,
                true
        );

        ctx.drawString(
                this.font,
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
    public void renderBackground(@NonNull GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        if (parent == null) {
            super.renderBackground(context, mouseX, mouseY, deltaTicks);
        } else {
            this.parent.renderBackground(context, -2147483648, -2147483648, deltaTicks);
            this.parent.render(context, -2147483648, -2147483648, deltaTicks);
            context.requestCursor(CursorTypes.ARROW);
            context.fill(0, 0, this.width, this.height, 0x88000000);
            context.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("popup/background"), popupX, popupY, popupW, popupH);
        }
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        CommandSuggestor.setAllowSuggestions(focused == commandTextField);
        super.setFocused(focused);
    }

    @Override
    public boolean mouseClicked(final @NonNull MouseButtonEvent click, final boolean doubled) {
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
    public boolean mouseReleased(final @NonNull MouseButtonEvent click) {
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
    public boolean keyPressed(final @NonNull KeyEvent input) {
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
    public boolean keyReleased(final @NonNull KeyEvent input) {
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
            if (!commandTextField.getValue().isEmpty()) {
                data = commandManager.addCommand(commandTextField.getValue(), nameTextField.getValue());
            } else if (warnOnEmptyCommandIfNull) {
                minecraft.setScreen(new PopupWarningScreen(Component.translatable("screen.savedcommands.emptyCommandHeader"),
                        Component.translatable("screen.savedcommands.emptyCommandMessage"), this));
                return false;
            } else {
                return false;
            }
        }
        if (!Objects.equals(commandTextField.getValue(), "")) {
            data.command = commandTextField.getValue();
        }
        if (Objects.equals(nameTextField.getValue(), "")) {
            data.name = null;
        } else {
            data.name = nameTextField.getValue();
        }
        commandManager.saveAsync();
        return true;
    }

    private void exit() {
        save(false);
        if (parent instanceof SavedCommandsScreen) {
            minecraft.setScreen(new SavedCommandsScreen());
        } else {
            minecraft.setScreen(parent);
        }
    }

    static class conflictSavedCommands { // Conflict with other keybinds of this mod
        SavedCommandManager.CommandData conflictingCommand;

        SavedCommandManager.CommandData.keybindCombination conflictingKeys; // Only the ones in both commands
    }

    static class conflictMinecraftKB { // Conflict with minecraft keybinds
        KeyMapping conflictingKeybind;

        InputConstants.Key conflictingKey; // Only the one in both commands
    }
}
