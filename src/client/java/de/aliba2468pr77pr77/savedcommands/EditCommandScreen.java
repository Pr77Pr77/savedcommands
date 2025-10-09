package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class EditCommandScreen extends Screen {
    private final Screen parent;
    private ButtonWidget closeButton;
    private TextFieldWidget commandTextField;
    private TextFieldWidget nameTextField;
    final private SavedCommandManager.CommandData data;

    int popupW;
    int popupH;
    int popupX;
    int popupY;

    public EditCommandScreen(Screen parent, SavedCommandManager.CommandData data) {
        super(Text.translatable("screen.savedcommands.editpopup"));
        this.parent = parent;
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();

        popupW = Math.min(300, this.width - 40);
        popupH = Math.min(160, this.height - 40);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        this.closeButton = ButtonWidget.builder(Text.translatable("gui.done"), b -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();

        this.addDrawableChild(this.closeButton);

        assert this.client != null;
        this.commandTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 40, popupW - 40, 20, Text.translatable("screen.savedcommands.command"));
        this.commandTextField.setMaxLength(256);
        this.commandTextField.setDrawsBackground(true);
        this.commandTextField.setFocusUnlocked(false);
        if (data.command != null) {
            this.commandTextField.setText(data.command);
        }
        this.addDrawableChild(this.commandTextField);

        this.nameTextField = new TextFieldWidget(this.client.advanceValidatingTextRenderer, popupX + 20, popupY + 80, popupW - 40, 20, Text.translatable("screen.savedcommands.command"));
        this.nameTextField.setMaxLength(256);
        this.nameTextField.setDrawsBackground(true);
        this.nameTextField.setFocusUnlocked(false);
        if (data.name != null) {
            this.nameTextField.setText(data.name);
        }
        this.addDrawableChild(this.nameTextField);
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
        popupH = Math.min(160, this.height - 40);
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
                Text.translatable("screen.savedcommands.command"),
                popupX + 20,
                popupY + 30,
                0xFFFFFFFF,
                true
        );

        ctx.drawText(
                this.textRenderer,
                Text.translatable("screen.savedcommands.name"),
                popupX + 20,
                popupY + 70,
                0xFFFFFFFF,
                true
        );

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
