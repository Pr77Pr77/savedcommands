package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.minecraft.text.Text.translatable;

public class PopupWarningScreen extends Screen {
    Screen parent;
    Text message;

    public int popupW;
    public int popupH;
    public int popupX;
    public int popupY;

    PopupWarningScreen(Text header, Text message, Screen parent) {
        super(header);
        this.message = message;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        popupW = Math.min(290, this.width - 40);
        popupH = Math.min(90, this.height - 40);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        ButtonWidget closeButton = ButtonWidget.builder(translatable("gui.ok"), b -> exit()).dimensions(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        this.addDrawableChild(closeButton);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        popupW = Math.min(290, this.width - 40);
        popupH = Math.min(90, this.height - 40);
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

        textWidth = this.textRenderer.getWidth(message);
        ctx.drawText(
                this.textRenderer,
                message,
                (this.width - textWidth) / 2,
                popupY + 30,
                0xFFFFFFFF,
                false
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
            exit();
            return true;
        }
        return super.keyPressed(input);
    }

    void exit() {
        if (parent instanceof SavedCommandsScreen) {
            MinecraftClient.getInstance().setScreen(new SavedCommandsScreen());
        } else {
            MinecraftClient.getInstance().setScreen(parent);
        }
    }
}
