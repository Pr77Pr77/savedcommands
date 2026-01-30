package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandManager.sendCommand;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.VariablePlaceholder;
import static net.minecraft.text.Text.*;

public class InputVariableScreen extends Screen {
    @Nullable
    private final Screen parent;

    private ButtonWidget cancelButton;
    private ButtonWidget doneButton;

    private final List<TextFieldWidget> variableInputTextFields = new ArrayList<>();

    private final String preInsertedCommand;
    List<SavedCommandManager.CommandData.variable> userEditableVariablesLeft;

    public int popupW;
    public int popupH;
    public int popupX;
    public int popupY;
    private int contentH = 60;

    public InputVariableScreen(String preInsertedCommand, List<SavedCommandManager.CommandData.variable> userEditableVariablesLeft, @Nullable Screen parent) {
        super(translatable("screen.savedcommands.entervariablevalues"));
        this.parent = parent;
        this.preInsertedCommand = preInsertedCommand;
        this.userEditableVariablesLeft = userEditableVariablesLeft;
    }

    @Override
    protected void init() {
        super.init();

        contentH = (userEditableVariablesLeft.size() + 2) * 40;

        popupW = Math.min(300, this.width - 40);
        popupH = Math.min(contentH, this.height - 40);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        assert this.client != null;
        for (SavedCommandManager.CommandData.variable Variable : userEditableVariablesLeft) {
            if (Variable.type.userEditable) {
                variableInputTextFields.add(new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 40 * (variableInputTextFields.size() + 1), popupW - 40, 20,
                        literal(Variable.name + " (").append(translatable(Variable.type.getTranslationKey())).append(literal(")"))));
                variableInputTextFields.getLast().setMaxLength(256);
                variableInputTextFields.getLast().setDrawsBackground(true);
                variableInputTextFields.getLast().setFocusUnlocked(true);
                variableInputTextFields.getLast().setText(Variable.defaultValue);
                switch (Variable.type) { // String does not need a statement
                    case INT:
                        variableInputTextFields.getLast().setTextPredicate(text -> text.matches("-?\\d*"));
                        break;
                    case FLOAT:
                        variableInputTextFields.getLast().setTextPredicate(text ->
                                text.matches("-?(\\d+\\.\\d*|\\d*\\.\\d+|\\d+)?")
                        );
                        break;
                }
                this.addDrawableChild(variableInputTextFields.getLast());
            }
        }

        this.cancelButton = ButtonWidget.builder(translatable("gui.cancel"), b -> exit()).dimensions(popupX + (popupW - 100 + 100 + 5) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        this.addDrawableChild(this.cancelButton);

        this.doneButton = ButtonWidget.builder(translatable("screen.savedcommands.send"), b -> exit(true)).dimensions(popupX + (popupW - 100 - 100 - 5) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        this.addDrawableChild(this.doneButton);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        popupW = Math.min(300, this.width - 40);
        popupH = Math.min(contentH, this.height - 40);
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

        for (TextFieldWidget variableInputTextField : variableInputTextFields) {
            ctx.drawText(
                    this.textRenderer,
                    variableInputTextField.getMessage(),
                    popupX + 20,
                    variableInputTextField.getY() - 10,
                    0xFFFFFFFF,
                    true
            );
        }

        super.render(ctx, mouseX, mouseY, delta);
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
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape() && this.shouldCloseOnEsc()) {
            exit();
            return true;
        } else if (input.isEnter()) {
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
                                case INT -> this.variableInputTextFields.get(variableIndex).getText()
                                        .replaceAll("[^0-9-]", "")
                                        .replaceAll("(?<!^)-", "");
                                case FLOAT -> this.variableInputTextFields.get(variableIndex).getText()
                                        .replaceAll("[^0-9.-]", "")
                                        .replaceAll("(?<!^)-", "")
                                        .replaceAll("(\\..*)\\.", "$1");
                                case STRING -> this.variableInputTextFields.get(variableIndex).getText();
                                default -> "";
                            };
                    insertedCommand = insertVariableValue(variableIndex, variableValue, insertedCommand);
                }
            }
            sendCommand(insertedCommand.toString());
        }

        if (parent instanceof SavedCommandsScreen) {
            MinecraftClient.getInstance().setScreen(new SavedCommandsScreen());
        } else {
            MinecraftClient.getInstance().setScreen(parent);
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
