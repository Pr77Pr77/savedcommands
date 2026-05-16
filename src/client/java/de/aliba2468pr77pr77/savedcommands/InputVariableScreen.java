package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandManager.sendCommand;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.VariablePlaceholder;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen.shortenTextIfNeeded;

public class InputVariableScreen extends PopupScreen {
    private Button cancelButton;
    private Button doneButton;

    private List<EditBox> variableInputTextFields = new ArrayList<>();

    private final String preInsertedCommand;
    List<SavedCommandManager.CommandData.variable> userEditableVariablesLeft;

    public InputVariableScreen(String preInsertedCommand, List<SavedCommandManager.CommandData.variable> userEditableVariablesLeft, @Nullable Screen parent) {
        super(Component.translatable("screen.savedcommands.entervariablevalues"), parent, 60, 300);
        this.preInsertedCommand = preInsertedCommand;
        this.userEditableVariablesLeft = userEditableVariablesLeft;
    }

    @Override
    protected void init() {
        contentH = (userEditableVariablesLeft.size() + 2) * 40;
        super.init();

        variableInputTextFields = new ArrayList<>();

        for (SavedCommandManager.CommandData.variable Variable : userEditableVariablesLeft) {
            if (Variable.type.userEditable) {
                variableInputTextFields.add(new EditBox(this.minecraft.font, popupX + 20, popupY + 40 * (variableInputTextFields.size() + 1), popupW - 40, 20,
                        Component.literal(Variable.name + " (").append(Component.translatable(Variable.type.getTranslationKey())).append(Component.literal(")"))));
                variableInputTextFields.getLast().setMaxLength(256);
                variableInputTextFields.getLast().setBordered(true);
                variableInputTextFields.getLast().setCanLoseFocus(true);
                variableInputTextFields.getLast().setValue(Variable.defaultValue);
                if (variableInputTextFields.size() == 1) { // Select the first one
                    this.setFocused(variableInputTextFields.getFirst());
                }

                int variableInputIndex = variableInputTextFields.size() - 1;
                switch (Variable.type) { // String does not need a statement
                    case INT:
                        variableInputTextFields.getLast().setResponder(text -> variableInputTextFields.get(variableInputIndex).setValue(
                                text.replaceAll("[^0-9-]", "")
                                        .replaceAll("(?<!^)-", "")));
                        break;
                    case FLOAT:
                        variableInputTextFields.getLast().setResponder(text -> variableInputTextFields.get(variableInputIndex).setValue(
                                text.replaceAll("[^0-9.-]", "")
                                        .replaceAll("(?<!^)-", "")
                                        .replaceAll("(\\..*)\\.", "$1")));
                        break;
                }
                this.addRenderableWidget(variableInputTextFields.getLast());
            }
        }

        this.cancelButton = Button.builder(Component.translatable("gui.cancel"), b -> exit()).bounds(popupX + (popupW - 100 + 100 + 5) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        this.addRenderableWidget(this.cancelButton);

        this.doneButton = Button.builder(Component.translatable("screen.savedcommands.send"), b -> exit(true)).bounds(popupX + (popupW - 100 - 100 - 5) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        this.addRenderableWidget(this.doneButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        for (EditBox variableInputTextField : variableInputTextFields) {
            ctx.text(
                    this.font,
                    shortenTextIfNeeded(variableInputTextField.getMessage().getString(), popupW - 25, ChatFormatting.RESET),
                    popupX + 20,
                    variableInputTextField.getY() - 10,
                    0xFFFFFFFF,
                    true
            );
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.isConfirmation()) {
            exit(true);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    void exit() {
        exit(false);
    }

    private void exit(boolean sendCommand) {
        if (sendCommand) {
            StringBuilder insertedCommand = new StringBuilder(preInsertedCommand);
            for (int variableIndex = 0; variableIndex < userEditableVariablesLeft.size() && variableIndex < variableInputTextFields.size(); variableIndex++) {
                if (userEditableVariablesLeft.get(variableIndex).type.userEditable) {
                    String variableValue =
                            switch (userEditableVariablesLeft.get(variableIndex).type) {
                                case INT -> this.variableInputTextFields.get(variableIndex).getValue()
                                        .replaceAll("[^0-9-]", "")
                                        .replaceAll("(?<!^)-", "");
                                case FLOAT -> this.variableInputTextFields.get(variableIndex).getValue()
                                        .replaceAll("[^0-9.-]", "")
                                        .replaceAll("(?<!^)-", "")
                                        .replaceAll("(\\..*)\\.", "$1");
                                case STRING -> this.variableInputTextFields.get(variableIndex).getValue();
                                default -> "";
                            };
                    insertedCommand = insertVariableValue(variableIndex, variableValue, insertedCommand);
                }
            }
            sendCommand(insertedCommand.toString());
        }

        if (parent instanceof SavedCommandsScreen) {
            minecraft.setScreen(new SavedCommandsScreen());
        } else {
            minecraft.setScreen(parent);
        }
    }

    private @NonNull StringBuilder insertVariableValue(int variableIndex, String variableValue, StringBuilder insertedCommand) {
        int index = insertedCommand.indexOf(String.valueOf(
                new char[]{VariablePlaceholder, userEditableVariablesLeft.get(variableIndex).abbreviation}));
        int ContinuingIndex = 0;
        StringBuilder insertedCommandOneVariable = new StringBuilder();
        while (index != -1) {
            insertedCommandOneVariable.append(insertedCommand.substring(ContinuingIndex, index));
            insertedCommandOneVariable.append(variableValue);
            ContinuingIndex = index + 2;

            index = insertedCommand.indexOf(String.valueOf(
                    new char[]{VariablePlaceholder, userEditableVariablesLeft.get(variableIndex).abbreviation}), index + 2);
        }
        insertedCommandOneVariable.append(insertedCommand.substring(ContinuingIndex));
        return insertedCommandOneVariable;
    }
}
