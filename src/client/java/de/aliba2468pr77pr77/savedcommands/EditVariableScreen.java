package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;
import static net.minecraft.text.Text.*;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

public class EditVariableScreen extends Screen {
    private final Screen parent;
    private ButtonWidget closeButton;
    private ButtonWidget deleteButton;

    private TextFieldWidget nameTextField;
    private TextFieldWidget abbreviationTextField;
    CyclingButtonWidget<SavedCommandManager.CommandData.variable.types> typeButton;
    private TextFieldWidget defaultValueTextField;

    private boolean abbreviationExists = false;

    private SavedCommandManager.CommandData.@Nullable variable data;
    private final SavedCommandManager.@Nullable CommandData commandData;

    public int popupW;
    public int popupH;
    public int popupX;
    public int popupY;

    public EditVariableScreen(Screen parent, SavedCommandManager.@Nullable CommandData commandData) {
        super(translatable("screen.savedcommands.createvariable"));
        this.parent = parent;
        this.commandData = commandData;
    }

    public EditVariableScreen(Screen parent, SavedCommandManager.CommandData.@Nullable variable data, SavedCommandManager.@Nullable CommandData commandData) {
        super(translatable("screen.savedcommands.editvariable"));
        this.parent = parent;
        this.data = data;
        this.commandData = commandData;
    }

    @Override
    protected void init() {
        super.init();

        popupW = Math.min(280, this.width - 20);
        popupH = Math.min(190, this.height - 20);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        if (data != null) {
            this.closeButton = ButtonWidget.builder(translatable("gui.done"), b -> exit()).dimensions(popupX + (popupW - 100 - 100 - 5) / 2, popupY + popupH - 20 - 20, 100, 20).build();
            this.deleteButton = ButtonWidget.builder(translatable("selectWorld.deleteButton"), b -> {
                assert commandData != null;
                commandData.variables.remove(data);
                nameTextField.setText("");
                abbreviationTextField.setText("");
                exit();
            }).dimensions(popupX + (popupW - 100 - 100 - 5) / 2 + 100 + 5, popupY + popupH - 20 - 20, 100, 20).build();
            this.addDrawableChild(this.deleteButton);
        } else {
            this.closeButton = ButtonWidget.builder(translatable("gui.done"), b -> exit()).dimensions(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        }

        this.addDrawableChild(this.closeButton);

        assert this.client != null;
        this.nameTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 40, popupW - 40, 20, translatable("screen.savedcommands.name"));
        this.nameTextField.setMaxLength(256);
        this.nameTextField.setDrawsBackground(true);
        this.nameTextField.setFocusUnlocked(true);
        if (data != null && data.name != null) {
            this.nameTextField.setText(data.name);
        }
        this.addDrawableChild(this.nameTextField);

        this.abbreviationTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20 + client.textRenderer.getWidth(Text.translatable("screen.savedcommands.abbreviation")) + 5, popupY + 65, 30, 20, translatable("screen.savedcommands.abbreviation"));
        this.abbreviationTextField.setMaxLength(1);
        this.abbreviationTextField.setDrawsBackground(true);
        this.abbreviationTextField.setFocusUnlocked(true);
        if (data != null && data.abbreviation != null) {
            this.abbreviationTextField.setText(String.valueOf(data.abbreviation));
        }
        this.addDrawableChild(this.abbreviationTextField);

        this.nameTextField.setChangedListener((String input) -> {
            closeButton.setMessage(input.isEmpty() || abbreviationTextField.getText().isEmpty() || abbreviationExists ? translatable("gui.cancel") : translatable("gui.done"));
        });
        this.abbreviationTextField.setChangedListener((String input) -> {
            if (!input.isEmpty() && commandData != null && commandData.variables != null && commandData.variables.stream().anyMatch(data -> data.abbreviation.equals(input.charAt(0)) && !data.equals(this.data))) {
                abbreviationExists = true;
                closeButton.setMessage(translatable("gui.cancel"));
                return;
            } else {
                abbreviationExists = false;
            }
            if (input.isEmpty() || nameTextField.getText().isEmpty()) {
                closeButton.setMessage(translatable("gui.cancel"));
            } else {
                closeButton.setMessage(translatable("gui.done"));
            }
        });

        typeButton =
                CyclingButtonWidget.builder(SavedCommandManager.CommandData.variable.types::getText, (data != null && data.type != null) ? data.type : SavedCommandManager.CommandData.variable.types.STRING)
                        .values(SavedCommandManager.CommandData.variable.types.values())
                        .build(popupX + 20, popupY + 90, popupW - 40, 20, Text.translatable("screen.savedcommands.vartype"),
                                (btn, value) -> {
                                    LOGGER.info("Chosen variable: " + value);
                                    setDefaultValueLimitations(value);

                                    if (nameTextField.getText().isEmpty() ||
                                            SavedCommandManager.CommandData.variable.types.byTranslation(nameTextField.getText()).isPresent()) {
                                        nameTextField.setText(value.userEditable ? "" : Text.translatable(value.getTranslationKey()).getString());
                                    }
                                    if (abbreviationTextField.getText().isEmpty() ||
                                            SavedCommandManager.CommandData.variable.types.byAbbreviationTranslation(abbreviationTextField.getText()).isPresent()) {
                                        abbreviationTextField.setText(value.userEditable ? "" : Text.translatable(value.getAbbreviationTranslationKey()).getString());
                                    }
                                });
        this.addDrawableChild(this.typeButton);

        this.defaultValueTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 125, popupW - 40, 20, translatable("screen.savedcommands.defaultvalue"));
        this.defaultValueTextField.setMaxLength(256);
        this.defaultValueTextField.setDrawsBackground(true);
        this.defaultValueTextField.setFocusUnlocked(true);
        if (data != null && data.defaultValue != null) {
            this.defaultValueTextField.setText(data.defaultValue);
            setDefaultValueLimitations(typeButton.getValue());
        }
        this.addDrawableChild(this.defaultValueTextField);

        closeButton.setMessage(nameTextField.getText().isEmpty() || abbreviationTextField.getText().isEmpty() ? translatable("gui.cancel") : translatable("gui.done"));
    }

    void setDefaultValueLimitations(SavedCommandManager.CommandData.variable.types value) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        switch (value) {
            case STRING:
                this.defaultValueTextField.setTextPredicate(text -> true);
                this.defaultValueTextField.setEditable(true);
                break;
            case INT:
                this.defaultValueTextField.setTextPredicate(text -> text.matches("-?\\d*"));
                this.defaultValueTextField.setEditable(true);
                this.defaultValueTextField.setText(this.defaultValueTextField.getText()
                        .replaceAll("[^0-9-]", "")
                        .replaceAll("(?<!^)-", "")); // Removing unwanted characters
                break;
            case FLOAT:
                this.defaultValueTextField.setTextPredicate(text ->
                        text.matches("-?(\\d+\\.\\d*|\\d*\\.\\d+|\\d+)?")
                );
                this.defaultValueTextField.setEditable(true);
                this.defaultValueTextField.setText(this.defaultValueTextField.getText()
                        .replaceAll("[^0-9.-]", "")
                        .replaceAll("(?<!^)-", "")
                        .replaceAll("(\\..*)\\.", "$1")); // Removing unwanted characters
                break;
            case ITEMHAND:
                this.defaultValueTextField.setTextPredicate(text -> true);
                this.defaultValueTextField.setEditable(false);
                if (player != null) {
                    this.defaultValueTextField.setText(String.valueOf(Registries.ITEM.getId(player.getMainHandStack().getItem())));
                } else {
                    this.defaultValueTextField.setText("");
                }
                break;
            case PLAYERPOSX:
                this.defaultValueTextField.setTextPredicate(text -> true);
                if (player != null) {
                    this.defaultValueTextField.setText(String.valueOf(player.getBlockX()));
                } else {
                    this.defaultValueTextField.setText("");
                }
                this.defaultValueTextField.setEditable(false);
                break;
            case PLAYERPOSY:
                this.defaultValueTextField.setTextPredicate(text -> true);
                if (player != null) {
                    this.defaultValueTextField.setText(String.valueOf(player.getBlockY()));
                } else {
                    this.defaultValueTextField.setText("");
                }
                this.defaultValueTextField.setEditable(false);
                break;
            case PLAYERPOSZ:
                this.defaultValueTextField.setTextPredicate(text -> true);
                if (player != null) {
                    this.defaultValueTextField.setText(String.valueOf(player.getBlockZ()));
                } else {
                    this.defaultValueTextField.setText("");
                }
                this.defaultValueTextField.setEditable(false);
                break;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        popupW = Math.min(280, this.width - 20);
        popupH = Math.min(190, this.height - 20);
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
                translatable("screen.savedcommands.name"),
                popupX + 20,
                popupY + 30,
                0xFFFFFFFF,
                true
        );

        ctx.drawText(
                this.textRenderer,
                translatable("screen.savedcommands.abbreviation"),
                popupX + 20,
                popupY + 70,
                0xFFFFFFFF,
                true
        );

        if (abbreviationExists) {
            ctx.drawText(
                    this.textRenderer,
                    translatable("screen.savedcommands.abbreviationAlreadyExists"),
                    popupX + 20 + client.textRenderer.getWidth(Text.translatable("screen.savedcommands.abbreviation")) + 40,
                    popupY + 70,
                    0xfffcfc54,
                    true
            );
        }

        ctx.drawText(
                this.textRenderer,
                translatable("screen.savedcommands.defaultvalue"),
                popupX + 20,
                popupY + 115,
                0xFFFFFFFF,
                true
        );

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
            if (data == null) { // New variable
                exit(false);
            } else {
                exit();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    private void exit() {
        exit(true);
    }

    private void exit(boolean save) {
        if (!nameTextField.getText().isEmpty() && !abbreviationTextField.getText().isEmpty() && save &&
                commandData != null && (commandData.variables == null || commandData.variables.stream().noneMatch(data -> data.abbreviation.equals(abbreviationTextField.getText().charAt(0)) && !data.equals(this.data)))) {
            if (data == null) {
                if (commandData.variables == null) {
                    commandData.variables = new ArrayList<>();
                }
                commandData.variables.add(new SavedCommandManager.CommandData.variable());
                data = commandData.variables.getLast();
            }

            assert data != null;
            data.name = nameTextField.getText();
            data.abbreviation = abbreviationTextField.getText().charAt(0);
            data.type = typeButton.getValue();
            data.defaultValue = defaultValueTextField.getText();

            commandManager.saveAsync();
        }
        if (parent instanceof SavedCommandsScreen) {
            MinecraftClient.getInstance().setScreen(new SavedCommandsScreen());
        } else {
            MinecraftClient.getInstance().setScreen(parent);
        }
    }
}
