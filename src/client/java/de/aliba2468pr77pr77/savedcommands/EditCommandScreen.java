package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.InputConstants;
import de.aliba2468pr77pr77.savedcommands.mixin.client.KeyBindingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
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

public class EditCommandScreen extends PopupScreen {
    private Button closeButton;
    public EditBox commandTextField;
    private EditBox nameTextField;
    private Button addVariableButton;
    private final List<ButtonVariablePair> variableButtons = new ArrayList<>();

    private Button keybindButton;
    private Button removeKeybindButton;
    public boolean keybindSetting = false;

    private SavedCommandManager.CommandData data;

    private final List<conflictSavedCommands> KeybindConflictsSavedCommands = new ArrayList<>();
    private final List<conflictMinecraftKB> KeybindConflictsMinecraft = new ArrayList<>();

    private CycleButton<CustomComponentCategory> categorySelectionButton;
    private boolean customCategoriesEnabled = false;
    private List<CustomComponentCategory> componentCategories = new ArrayList<>();
    private IconButton addCategoryButton;
    private EditBox newCategoryEditBox;

    public InsertedVariables insertedVariables;

    CommandSuggestions commandSuggestor;

    public EditCommandScreen(Screen parent, SavedCommandManager.CommandData data) {
        super(translatable("screen.savedcommands.editpopup"), parent, 200, 300);
        if (SettingsManager.getCombinedWorldAndGlobal(commandManager).manualCategories) {
            contentH = 225;
            customCategoriesEnabled = true;
            if (commandManager.data.customCategories == null) {
                commandManager.data.customCategories = new ArrayList<>();
            }
            componentCategories = CustomComponentCategory.fromSimpleCategories(commandManager.data.customCategories);
        }
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

                    conflict.conflictingKeys = new SavedCommandManager.CommandData.KeybindCombination();
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
                        tooltipContent.append(Component.translatable(KeybindConflictMinecraft.conflictingKeybind.getName()));
                    }
                }
            }
            if (keybindButton != null) {
                keybindButton.setTooltip(Tooltip.create(tooltipContent));
            }
        }
    }

    private Component getKeybindButtonText() {
        MutableComponent buttonContent = Component.translatable("controls.keybinds.title").append(Component.literal(": "));

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

        this.closeButton = Button.builder(translatable("gui.done"), button -> exit()).bounds(popupX + (popupW - 100) / 2, popupY + popupH - 10 - 20, 100, 20).build();

        this.addRenderableWidget(this.closeButton);

        this.commandTextField = new EditBox(this.minecraft.font, popupX + 20, popupY + 35, popupW - 40, 20, translatable("advMode.command"));
        this.commandTextField.setMaxLength(256);
        this.commandTextField.setBordered(true);
        this.commandTextField.setCanLoseFocus(true);
        if (data != null && data.command != null) {
            this.commandTextField.setValue(data.command);
        }
        this.addRenderableWidget(this.commandTextField);

        addVariableButton = Button.builder(Component.literal("+"), button -> {
                    LOGGER.info("Creating Variable...");
                    if (!save(true)) {
                        return;
                    }
                    minecraft.setScreen(new EditVariableScreen(this, data));
                }).bounds(popupX + 20, popupY + 80, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.savedcommands.createvariable")))
                .createNarration((componentSupplier) -> Component.translatable("screen.savedcommands.createvariable")).build();
        this.addRenderableWidget(addVariableButton);

        if (data != null && data.variables != null) {
            for (int variableIndex = 0; variableIndex < data.variables.size(); variableIndex++) {
                int finalVariableIndex = variableIndex;
                Button variableButton = Button.builder(Component.literal(String.valueOf(data.variables.get(variableIndex).abbreviation)), button -> {
                            LOGGER.info("Adding Variable " + finalVariableIndex);
                            commandTextField.setValue(commandTextField.getValue().substring(0, commandTextField.getCursorPosition()) + VariablePlaceholder + data.variables.get(finalVariableIndex).abbreviation + commandTextField.getValue().substring(commandTextField.getCursorPosition()));
                        }).bounds(popupX + 20 + 25 + 45 * variableIndex, popupY + 80, 20, 20)
                        .tooltip(Tooltip.create(Component.translatable("screen.savedcommands.insertvariablebutton", data.variables.get(variableIndex).name)))
                        .createNarration((componentSupplier) -> Component.translatable("screen.savedcommands.insertvariablebutton", data.variables.get(finalVariableIndex).name)).build();
                this.addRenderableWidget(variableButton);

                IconButton variableEditButton = new IconButton(popupX + 20 + 45 + 45 * variableIndex, popupY + 80, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/edit.png"), button -> {
                    LOGGER.info("Editing Variable...");
                    if (!save(true)) {
                        return;
                    }
                    minecraft.setScreen(new EditVariableScreen(this, data.variables.get(finalVariableIndex), data));
                }, Component.translatable("screen.savedcommands.editvariablebutton", data.variables.get(variableIndex).name));
                this.addRenderableWidget(variableEditButton);

                variableButtons.add(new ButtonVariablePair(variableButton, variableEditButton));
            }
        }

        this.nameTextField = new EditBox(this.minecraft.font, popupX + 20, popupY + 115, popupW - 40, 20, translatable("screen.savedcommands.name"));
        this.nameTextField.setMaxLength(256);
        this.nameTextField.setBordered(true);
        this.nameTextField.setCanLoseFocus(true);
        if (data != null && data.name != null) {
            this.nameTextField.setValue(data.name);
        }
        this.addRenderableWidget(this.nameTextField);

        searchConflicts();

        this.keybindButton = Button.builder(getKeybindButtonText(), button -> {
            if (!save(true)) {
                return;
            }
            keybindSetting = true;
            keybindButton.setMessage(getKeybindButtonText());
            keybindButton.setTooltip(Tooltip.create(Component.empty()));
            if (data == null) {
                return;
            }
            data.keybinds = new SavedCommandManager.CommandData.KeybindCombination();
            LOGGER.info("Changing the keybind...");
        }).bounds(popupX + 20, popupY + 140, Math.round((popupW - 40) * 0.7F), 20).build();

        this.addRenderableWidget(this.keybindButton);

        this.removeKeybindButton = Button.builder(translatable("screen.savedcommands.remove"), button -> {
            if (data == null) {
                return;
            }
            LOGGER.info("Removing the keybind...");
            data.keybinds = null;
            searchConflicts();
            keybindButton.setTooltip(Tooltip.create(Component.empty()));
            keybindButton.setMessage(getKeybindButtonText());
        }).bounds(popupX + Math.round((popupW - 40) * 0.7F) + 25, popupY + 140, popupW - 45 - Math.round((popupW - 40) * 0.7F), 20).build();

        this.addRenderableWidget(removeKeybindButton);

        searchConflicts();

        if (customCategoriesEnabled) {
            if (data == null || data.categoryId == null) {
                createCategorySelectionButton(CustomComponentCategory.NONE);
            } else {
                createCategorySelectionButton(componentCategories.stream()
                        .filter(category -> Objects.equals(data.categoryId, category.id))
                        .findFirst()
                        .orElse(CustomComponentCategory.NONE));
            }
            this.addRenderableWidget(categorySelectionButton);

            newCategoryEditBox = new EditBox(this.minecraft.font, popupX + 20, popupY + 165, popupW - 40 - 5 - 20, 20, translatable("screen.savedcommands.name"));
            newCategoryEditBox.setMaxLength(256);
            newCategoryEditBox.setBordered(true);
            newCategoryEditBox.setCanLoseFocus(true);
            newCategoryEditBox.setHint(Component.translatable("screen.savedcommands.createcategory.name"));
            newCategoryEditBox.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.createcategory.name")));
            newCategoryEditBox.setResponder((string) -> {
                if (string.isEmpty()) {
                    addCategoryButton.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.createcategory.cancel")));
                    addCategoryButton.switchToComponent();
                } else {
                    addCategoryButton.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.createcategory.createanduse", string.trim())));
                    addCategoryButton.switchToIcon();
                }
            });

            addCategoryButton = new IconButton(popupX + popupW - 20 - 20, popupY + 165, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/save.png"), button -> {
                if (children().contains(newCategoryEditBox)) {
                    if (!newCategoryEditBox.getValue().isEmpty()) {
                        if (commandManager.data.customCategories == null) {
                            commandManager.data.customCategories = new ArrayList<>();
                        }
                        SavedCommandManager.SavedCommandsData.CustomCategory newCategory = commandManager.data.new CustomCategory(newCategoryEditBox.getValue().trim());
                        commandManager.data.customCategories.add(newCategory);
                        componentCategories = CustomComponentCategory.fromSimpleCategories(commandManager.data.customCategories);

                        Optional<CustomComponentCategory> newComponentCategory = componentCategories.stream()
                                .filter((customComponentCategory) -> newCategory.id.equals(customComponentCategory.id)).findFirst();

                        this.removeWidget(categorySelectionButton);
                        newComponentCategory.ifPresent(this::createCategorySelectionButton);
                    }

                    this.addRenderableWidget(categorySelectionButton);
                    this.removeWidget(newCategoryEditBox);
                    this.newCategoryEditBox.setValue("");
                    button.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.createcategory")));
                    button.setMessage(Component.literal("+"));
                    if (button instanceof IconButton iconButton) {
                        iconButton.switchToComponent();
                    }
                } else {
                    this.addRenderableWidget(newCategoryEditBox);
                    this.removeWidget(categorySelectionButton);
                    button.setTooltip(Tooltip.create(Component.translatable("screen.savedcommands.createcategory.cancel")));
                    button.setMessage(Component.literal("×"));
                    if (button instanceof IconButton iconButton) {
                        iconButton.switchToComponent();
                    }
                }

            }, Component.translatable("screen.savedcommands.createcategory"), Component.literal("+"), false);
            this.addRenderableWidget(addCategoryButton);
        }

        commandSuggestor = new CommandSuggestions(minecraft, this, commandTextField, font, false, false, 1, 10, false, 0xD8000000);
        commandSuggestor.setAllowHiding(false);
        commandSuggestor.setAllowSuggestions(true);
        this.commandTextField.setResponder((String text) -> {
            if (text.isEmpty()) {
                closeButton.setMessage(translatable("gui.cancel"));
            } else {
                closeButton.setMessage(translatable("gui.done"));
            }
            removeOrphanedVariablePlaceholders(text);
            insertedVariables.refresh();
            commandSuggestor.updateCommandInfo();
        });
        if (commandTextField.getValue().isEmpty()) {
            closeButton.setMessage(translatable("gui.cancel"));
        } else {
            closeButton.setMessage(translatable("gui.done"));
        }
        insertedVariables.refresh();
        commandSuggestor.updateCommandInfo();
    }

    @Override
    public void repositionElements() {
        super.repositionElements();

        this.closeButton.setPosition(popupX + (popupW - 100) / 2, popupY + popupH - 10 - 20);
        this.nameTextField.setRectangle(popupW - 40, 20, popupX + 20, popupY + 115);
        this.commandTextField.setRectangle(popupW - 40, 20, popupX + 20, popupY + 35);
        this.addVariableButton.setPosition(popupX + 20, popupY + 80);

        for (int buttonPairIndex = 0; buttonPairIndex < variableButtons.size(); buttonPairIndex++) {
            variableButtons.get(buttonPairIndex).variableButton.setPosition(popupX + 20 + 25 + 45 * buttonPairIndex, popupY + 80);
            variableButtons.get(buttonPairIndex).editButton.setPosition(popupX + 20 + 45 + 45 * buttonPairIndex, popupY + 80);
        }

        this.keybindButton.setRectangle(Math.round((popupW - 40) * 0.7F), 20, popupX + 20, popupY + 140);
        this.removeKeybindButton.setRectangle(popupW - 45 - Math.round((popupW - 40) * 0.7F), 20, popupX + Math.round((popupW - 40) * 0.7F) + 25, popupY + 140);

        if (this.categorySelectionButton != null) {
            this.categorySelectionButton.setRectangle(popupW - 40 - 5 - 20, 20, popupX + 20, popupY + 165);
        }
        if (this.newCategoryEditBox != null) {
            this.newCategoryEditBox.setRectangle(popupW - 40 - 5 - 20, 20, popupX + 20, popupY + 165);
        }
        if (this.addCategoryButton != null) {
            this.addCategoryButton.setPosition(popupX + popupW - 20 - 20, popupY + 165);
        }

        commandSuggestor.updateCommandInfo();
    }

    record ButtonVariablePair(Button variableButton, IconButton editButton) {
    }

    private void createCategorySelectionButton(CustomComponentCategory defaultValue) {
        categorySelectionButton = CycleButton.builder(category -> category.name, defaultValue)
                .withValues(componentCategories)
                .withTooltip((customComponentCategory) -> Tooltip.create(Component.translatable("screen.savedcommands.manualcategories.disableinfo")))
                .create(
                        popupX + 20, popupY + 165, popupW - 40 - 5 - 20, 20,
                        Component.translatable("screen.savedcommands.manualcategory")
                );
    }

    public static class CustomComponentCategory {
        public @Nullable String id;
        public Component name;

        public static final CustomComponentCategory NONE = new CustomComponentCategory(translatable("screen.savedcommands.nocategory"), null);

        public CustomComponentCategory(Component name, @Nullable String id) {
            this.name = name;
            this.id = id;
        }

        private static List<CustomComponentCategory> fromSimpleCategories(List<SavedCommandManager.SavedCommandsData.CustomCategory> source) {
            List<CustomComponentCategory> result = new ArrayList<>();
            result.add(CustomComponentCategory.NONE);

            for (SavedCommandManager.SavedCommandsData.CustomCategory category : source) {
                result.add(new CustomComponentCategory(
                        Component.translatable(category.name),
                        category.id
                ));
            }

            return result;
        }
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
    public void render(@NonNull GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawString(
                this.font,
                translatable("advMode.command"),
                popupX + 20,
                popupY + 25,
                0xFFFFFFFF,
                true
        );

        ctx.drawString(
                this.font,
                translatable("screen.savedcommands.variables"),
                popupX + 20,
                popupY + 70,
                0xFFFFFFFF,
                true
        );

        ctx.drawString(
                this.font,
                translatable("screen.savedcommands.name"),
                popupX + 20,
                popupY + 105,
                0xFFFFFFFF,
                true
        );

        commandSuggestor.render(ctx, mouseX, mouseY);
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        commandSuggestor.setAllowSuggestions(focused == commandTextField);
        super.setFocused(focused);
    }

    @Override
    public boolean mouseClicked(final @NonNull MouseButtonEvent click, final boolean doubled) {
        if (keybindSetting && data != null && (data.keybinds == null || data.keybinds.isEmptyOrNull() || !(data.keybinds.keybindType.contains("MOUSE") && data.keybinds.keybindCode.contains(click.button())))) {
            if (data.keybinds == null) {
                data.keybinds = new SavedCommandManager.CommandData.KeybindCombination();
            }
            data.keybinds.keybindType.add("MOUSE");
            data.keybinds.keybindCode.add(click.button());
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        }
        if (commandSuggestor.mouseClicked(click)) {
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
        if (commandSuggestor.mouseScrolled(verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(final @NonNull KeyEvent input) {
        if (keybindSetting && data != null && (data.keybinds == null || data.keybinds.isEmptyOrNull() || !(data.keybinds.keybindType.contains("KEYSYM") && data.keybinds.keybindCode.contains(input.key())))) {
            if (data.keybinds == null) {
                data.keybinds = new SavedCommandManager.CommandData.KeybindCombination();
            }
            data.keybinds.keybindType.add("KEYSYM");
            data.keybinds.keybindCode.add(input.key());
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        }
        if (commandSuggestor.keyPressed(input)) {
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
        if (customCategoriesEnabled) {
            data.categoryId = categorySelectionButton.getValue().id;
        }
        commandManager.saveAsync();
        return true;
    }

    @Override
    protected void exit() {
        save(false);
        super.exit();
    }

    static class conflictSavedCommands { // Conflict with other keybinds of this mod
        SavedCommandManager.CommandData conflictingCommand;

        SavedCommandManager.CommandData.KeybindCombination conflictingKeys; // Only the ones in both commands
    }

    static class conflictMinecraftKB { // Conflict with minecraft keybinds
        KeyMapping conflictingKeybind;

        InputConstants.Key conflictingKey; // Only the one in both commands
    }
}
