package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;

import java.util.Objects;

import static net.minecraft.text.Text.*;

public class EditCommandScreen extends Screen {
    private final Screen parent;
    private ButtonWidget closeButton;
    private TextFieldWidget commandTextField;
    private TextFieldWidget nameTextField;
    private ButtonWidget keybindButton;
    public boolean keybindSetting = false;
    final private SavedCommandManager.CommandData data;
    final private SavedCommandManager manager;

    int popupW;
    int popupH;
    int popupX;
    int popupY;

    public EditCommandScreen(Screen parent, SavedCommandManager.CommandData data, SavedCommandManager manager) {
        super(translatable("screen.savedcommands.editpopup"));
        this.manager = manager;
        this.parent = parent;
        this.data = data;
    }

    private Text getKeybindButtonText() {
        MutableText buttonContent = empty();
        if (keybindSetting) {
            buttonContent.append(literal("< ").withColor(0xfffcfc54));
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
        }
        return buttonContent;
    }

    @Override
    protected void init() {
        super.init();

        popupW = Math.min(300, this.width - 40);
        popupH = Math.min(210, this.height - 40);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        this.closeButton = ButtonWidget.builder(translatable("gui.done"), b -> exit()).dimensions(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();

        this.addDrawableChild(this.closeButton);

        assert this.client != null;
        this.commandTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 40, popupW - 40, 20, translatable("screen.savedcommands.command"));
        this.commandTextField.setMaxLength(256);
        this.commandTextField.setDrawsBackground(true);
        this.commandTextField.setFocusUnlocked(true);
        if (data.command != null) {
            this.commandTextField.setText(data.command);
        }
        this.addDrawableChild(this.commandTextField);

        this.nameTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 80, popupW - 40, 20, translatable("screen.savedcommands.command"));
        this.nameTextField.setMaxLength(256);
        this.nameTextField.setDrawsBackground(true);
        this.nameTextField.setFocusUnlocked(true);
        if (data.name != null) {
            this.nameTextField.setText(data.name);
        }
        this.addDrawableChild(this.nameTextField);

        this.keybindButton = ButtonWidget.builder(getKeybindButtonText(), b -> {
            LOGGER.info("Changing the keybind...");
            keybindSetting = true;
            keybindButton.setMessage(getKeybindButtonText());
            data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
        }).dimensions(popupX + 40, popupY + 120, popupW - 80, 20).build();

        this.addDrawableChild(this.keybindButton);
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
        popupH = Math.min(210, this.height - 40);
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
                translatable("screen.savedcommands.command"),
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

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (keybindSetting && (data.keybinds == null || data.keybinds.isEmptyOrNull() || !(data.keybinds.keybindType.contains("MOUSE") && data.keybinds.keybindCode.contains(button)))) {
            if (data.keybinds == null) {
                data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
            }
            data.keybinds.keybindType.add("MOUSE");
            data.keybinds.keybindCode.add(button);
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (keybindSetting && data.keybinds != null && data.keybinds.keybindType.contains("MOUSE") && data.keybinds.keybindCode.contains(button)) {
            keybindSetting = false;
            manager.saveAsync();
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        } else {
            return super.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keybindSetting && (data.keybinds == null || data.keybinds.isEmptyOrNull() || !(data.keybinds.keybindType.contains("KEYSYM") && data.keybinds.keybindCode.contains(keyCode)))) {
            if (data.keybinds == null) {
                data.keybinds = new SavedCommandManager.CommandData.keybindCombination();
            }
            data.keybinds.keybindType.add("KEYSYM");
            data.keybinds.keybindCode.add(keyCode);
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            exit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keybindSetting && data.keybinds != null && data.keybinds.keybindType.contains("KEYSYM") && data.keybinds.keybindCode.contains(keyCode)) {
            keybindSetting = false;
            manager.saveAsync();
            keybindButton.setMessage(getKeybindButtonText());
            return true;
        } else {
            return super.keyReleased(keyCode, scanCode, modifiers);
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
        manager.saveAsync();
        MinecraftClient.getInstance().setScreen(parent);
        if (parent instanceof SavedCommandsScreen commandParent) {
            commandParent.reloadCommands();
        }
    }
}
