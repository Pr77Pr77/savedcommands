package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Function;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

public class EditVariableScreen extends Screen {
    private final Screen parent;
    private Button closeButton;
    private Button deleteButton;

    private EditBox nameTextField;
    private EditBox abbreviationTextField;
    CycleButton<SavedCommandManager.CommandData.variable.types> typeButton;
    private EditBox defaultValueTextField;
    private Function<String, String> defaultValueCleanup;

    private boolean abbreviationExists = false;

    private SavedCommandManager.CommandData.@Nullable variable data;
    private final SavedCommandManager.@Nullable CommandData commandData;

    public int popupW;
    public int popupH;
    public int popupX;
    public int popupY;

    public EditVariableScreen(Screen parent, SavedCommandManager.@Nullable CommandData commandData) {
        super(Component.translatable("screen.savedcommands.createvariable"));
        this.parent = parent;
        this.commandData = commandData;
    }

    public EditVariableScreen(Screen parent, SavedCommandManager.CommandData.@Nullable variable data, SavedCommandManager.@Nullable CommandData commandData) {
        super(Component.translatable("screen.savedcommands.editvariable"));
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
            this.closeButton = Button.builder(Component.translatable("gui.done"), b -> exit()).bounds(popupX + (popupW - 100 - 100 - 5) / 2, popupY + popupH - 20 - 20, 100, 20).build();
            this.deleteButton = Button.builder(Component.translatable("selectWorld.deleteButton"), b -> {
                assert commandData != null;
                commandData.variables.remove(data);
                nameTextField.setValue("");
                abbreviationTextField.setValue("");
                exit();
            }).bounds(popupX + (popupW - 100 - 100 - 5) / 2 + 100 + 5, popupY + popupH - 20 - 20, 100, 20).build();
            this.addRenderableWidget(this.deleteButton);
        } else {
            this.closeButton = Button.builder(Component.translatable("gui.done"), b -> exit()).bounds(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        }

        this.addRenderableWidget(this.closeButton);

        assert this.minecraft != null;
        this.nameTextField = new EditBox(this.minecraft.font, popupX + 20, popupY + 40, popupW - 40, 20, Component.translatable("screen.savedcommands.name"));
        this.nameTextField.setMaxLength(256);
        this.nameTextField.setBordered(true);
        this.nameTextField.setCanLoseFocus(true);
        if (data != null && data.name != null) {
            this.nameTextField.setValue(data.name);
        }
        this.addRenderableWidget(this.nameTextField);

        this.abbreviationTextField = new EditBox(this.minecraft.font, popupX + 20 + minecraft.font.width(Component.translatable("screen.savedcommands.abbreviation")) + 5, popupY + 65, 30, 20, Component.translatable("screen.savedcommands.abbreviation"));
        this.abbreviationTextField.setMaxLength(1);
        this.abbreviationTextField.setBordered(true);
        this.abbreviationTextField.setCanLoseFocus(true);
        if (data != null && data.abbreviation != null) {
            this.abbreviationTextField.setValue(String.valueOf(data.abbreviation));
        }
        this.addRenderableWidget(this.abbreviationTextField);

        this.nameTextField.setResponder((String input) -> {
            closeButton.setMessage(input.isEmpty() || abbreviationTextField.getValue().isEmpty() || abbreviationExists ? Component.translatable("gui.cancel") : Component.translatable("gui.done"));
        });
        this.abbreviationTextField.setResponder((String input) -> {
            if (!input.isEmpty() && commandData != null && commandData.variables != null && commandData.variables.stream().anyMatch(data -> data.abbreviation.equals(input.charAt(0)) && !data.equals(this.data))) {
                abbreviationExists = true;
                closeButton.setMessage(Component.translatable("gui.cancel"));
                return;
            } else {
                abbreviationExists = false;
            }
            if (input.isEmpty() || nameTextField.getValue().isEmpty()) {
                closeButton.setMessage(Component.translatable("gui.cancel"));
            } else {
                closeButton.setMessage(Component.translatable("gui.done"));
            }
        });

        typeButton =
                CycleButton.builder(SavedCommandManager.CommandData.variable.types::getText)
                        .withInitialValue((data != null && data.type != null) ? data.type : SavedCommandManager.CommandData.variable.types.STRING)
                        .withValues(SavedCommandManager.CommandData.variable.types.values())
                        .create(popupX + 20, popupY + 90, popupW - 40, 20, Component.translatable("screen.savedcommands.vartype"),
                                (btn, value) -> {
                                    LOGGER.info("Chosen variable: " + value);
                                    setDefaultValueLimitations(value);

                                    if (nameTextField.getValue().isEmpty() ||
                                            SavedCommandManager.CommandData.variable.types.byTranslation(nameTextField.getValue()).isPresent()) {
                                        nameTextField.setValue(value.userEditable ? "" : Component.translatable(value.getTranslationKey()).getString());
                                    }
                                    if (abbreviationTextField.getValue().isEmpty() ||
                                            SavedCommandManager.CommandData.variable.types.byAbbreviationTranslation(abbreviationTextField.getValue()).isPresent()) {
                                        abbreviationTextField.setValue(value.userEditable ? "" : Component.translatable(value.getAbbreviationTranslationKey()).getString());
                                    }
                                });
        this.addRenderableWidget(this.typeButton);

        this.defaultValueTextField = new EditBox(this.minecraft.font, popupX + 20, popupY + 125, popupW - 40, 20, Component.translatable("screen.savedcommands.defaultvalue"));
        this.defaultValueTextField.setMaxLength(256);
        this.defaultValueTextField.setBordered(true);
        this.defaultValueTextField.setCanLoseFocus(true);
        if (data != null && data.defaultValue != null) {
            this.defaultValueTextField.setValue(data.defaultValue);
            setDefaultValueLimitations(typeButton.getValue());
        } else {
            this.defaultValueCleanup = text -> text;
        }
        this.defaultValueTextField.setResponder(input -> {
            String cleaned = defaultValueCleanup.apply(input);

            if (!cleaned.equals(input)) {
                defaultValueTextField.setValue(cleaned);
            }
        });
        this.addRenderableWidget(this.defaultValueTextField);

        closeButton.setMessage(nameTextField.getValue().isEmpty() || abbreviationTextField.getValue().isEmpty() ? Component.translatable("gui.cancel") : Component.translatable("gui.done"));
    }

    void setDefaultValueLimitations(SavedCommandManager.CommandData.variable.types value) {
        assert minecraft != null;
        LocalPlayer player = minecraft.player;
        switch (value) {
            case STRING:
                this.defaultValueCleanup = text -> text;
                this.defaultValueTextField.setEditable(true);
                break;
            case INT:
                this.defaultValueCleanup = text -> text
                        .replaceAll("[^0-9-]", "")
                        .replaceAll("(?<!^)-", "");  // Removing unwanted characters
                this.defaultValueTextField.setEditable(true);
                this.defaultValueTextField.setValue(defaultValueCleanup.apply(defaultValueTextField.getValue()));
                break;
            case FLOAT:
                this.defaultValueCleanup = text -> text
                        .replaceAll("[^0-9.-]", "")
                        .replaceAll("(?<!^)-", "")
                        .replaceAll("(\\..*)\\.", "$1");
                this.defaultValueTextField.setEditable(true);
                this.defaultValueTextField.setValue(defaultValueCleanup.apply(defaultValueTextField.getValue()));
                break;
            case ITEMHAND:
                this.defaultValueCleanup = text -> text;
                this.defaultValueTextField.setEditable(false);
                if (player != null) {
                    this.defaultValueTextField.setValue(String.valueOf(player.getMainHandItem().getItem()));
                } else {
                    this.defaultValueTextField.setValue("");
                }
                break;
            case PLAYERPOSX:
                this.defaultValueCleanup = text -> text;
                if (player != null) {
                    this.defaultValueTextField.setValue(String.valueOf(player.getBlockX()));
                } else {
                    this.defaultValueTextField.setValue("");
                }
                this.defaultValueTextField.setEditable(false);
                break;
            case PLAYERPOSY:
                this.defaultValueCleanup = text -> text;
                if (player != null) {
                    this.defaultValueTextField.setValue(String.valueOf(player.getBlockY()));
                } else {
                    this.defaultValueTextField.setValue("");
                }
                this.defaultValueTextField.setEditable(false);
                break;
            case PLAYERPOSZ:
                this.defaultValueCleanup = text -> text;
                if (player != null) {
                    this.defaultValueTextField.setValue(String.valueOf(player.getBlockZ()));
                } else {
                    this.defaultValueTextField.setValue("");
                }
                this.defaultValueTextField.setEditable(false);
                break;
        }
    }

    @Override
    public void render(final GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        popupW = Math.min(280, this.width - 20);
        popupH = Math.min(190, this.height - 20);
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
                Component.translatable("screen.savedcommands.name"),
                popupX + 20,
                popupY + 30,
                0xFFFFFFFF,
                true
        );

        ctx.drawString(
                this.font,
                Component.translatable("screen.savedcommands.abbreviation"),
                popupX + 20,
                popupY + 70,
                0xFFFFFFFF,
                true
        );

        if (abbreviationExists) {
            ctx.drawString(
                    this.font,
                    Component.translatable("screen.savedcommands.abbreviationAlreadyExists"),
                    popupX + 20 + this.font.width(Component.translatable("screen.savedcommands.abbreviation")) + 40,
                    popupY + 70,
                    0xfffcfc54,
                    true
            );
        }

        ctx.drawString(
                this.font,
                Component.translatable("screen.savedcommands.defaultvalue"),
                popupX + 20,
                popupY + 115,
                0xFFFFFFFF,
                true
        );

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        if (parent == null) {
            super.renderBackground(context, mouseX, mouseY, deltaTicks);
        } else {
            this.parent.renderBackground(context, -2147483648, -2147483648, deltaTicks);
            this.parent.render(context, -2147483648, -2147483648, deltaTicks);
            context.requestCursor(CursorTypes.ARROW);
            context.fill(0, 0, this.width, this.height, 0x88000000);
            context.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ResourceLocation.withDefaultNamespace("popup/background"), popupX, popupY, popupW, popupH);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
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
        if (!nameTextField.getValue().isEmpty() && !abbreviationTextField.getValue().isEmpty() && save &&
                commandData != null && (commandData.variables == null || commandData.variables.stream().noneMatch(data -> data.abbreviation.equals(abbreviationTextField.getValue().charAt(0)) && !data.equals(this.data)))) {
            if (data == null) {
                if (commandData.variables == null) {
                    commandData.variables = new ArrayList<>();
                }
                commandData.variables.add(new SavedCommandManager.CommandData.variable());
                data = commandData.variables.getLast();
            }

            assert data != null;
            data.name = nameTextField.getValue();
            data.abbreviation = abbreviationTextField.getValue().charAt(0);
            data.type = typeButton.getValue();
            data.defaultValue = defaultValueTextField.getValue();

            commandManager.saveAsync();
        }
        assert minecraft != null;
        if (parent instanceof SavedCommandsScreen) {
            minecraft.setScreen(new SavedCommandsScreen());
        } else {
            minecraft.setScreen(parent);
        }
    }
}
