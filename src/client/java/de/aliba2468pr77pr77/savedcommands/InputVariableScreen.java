package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandManager.sendCommand;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.VariablePlaceholder;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen.shortenTextIfNeeded;

public class InputVariableScreen extends Screen {
    @Nullable
    private final Screen parent;

    private Button cancelButton;
    private Button doneButton;

    private List<EditBox> variableInputTextFields = new ArrayList<>();

    private final String preInsertedCommand;
    List<SavedCommandManager.CommandData.variable> userEditableVariablesLeft;

    public int popupW;
    public int popupH;
    public int popupX;
    public int popupY;
    private int contentH = 60;

    public InputVariableScreen(String preInsertedCommand, List<SavedCommandManager.CommandData.variable> userEditableVariablesLeft, @Nullable Screen parent) {
        super(Component.translatable("screen.savedcommands.entervariablevalues"));
        this.parent = parent;
        this.preInsertedCommand = preInsertedCommand;
        this.userEditableVariablesLeft = userEditableVariablesLeft;
    }

    @Override
    protected void init() {
        super.init();

        contentH = (userEditableVariablesLeft.size() + 2) * 40;

        popupW = Math.min(300, this.width - 20);
        popupH = Math.min(contentH, this.height - 20);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

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
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        popupW = Math.min(300, this.width - 20);
        popupH = Math.min(contentH, this.height - 20);
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

        for (EditBox variableInputTextField : variableInputTextFields) {
            ctx.drawString(
                    this.font,
                    shortenTextIfNeeded(variableInputTextField.getMessage().getString(), popupW - 25, ChatFormatting.RESET),
                    popupX + 20,
                    variableInputTextField.getY() - 10,
                    0xFFFFFFFF,
                    true
            );
        }

        super.render(ctx, mouseX, mouseY, delta);
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
            context.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("popup/background"), popupX, popupY, popupW, popupH);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.isEscape() && this.shouldCloseOnEsc()) {
            exit();
            return true;
        } else if (input.isConfirmation()) {
            exit(true);
        }
        return super.keyPressed(input);
    }

    private void exit() {
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
