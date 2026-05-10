package de.aliba2468pr77pr77.savedcommands;


import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PopupWarningScreen extends Screen {
    Screen parent;
    Component message;

    public int popupW;
    public int popupH;
    public int popupX;
    public int popupY;

    PopupWarningScreen(Component header, Component message, Screen parent) {
        super(header);
        this.message = message;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        popupW = Math.min(290, this.width - 20);
        popupH = Math.min(90, this.height - 20);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;

        Button closeButton = Button.builder(Component.translatable("gui.ok"), b -> exit()).bounds(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        this.addRenderableWidget(closeButton);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        popupW = Math.min(290, this.width - 20);
        popupH = Math.min(90, this.height - 20);
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

        textWidth = this.font.width(message);
        ctx.drawString(
                this.font,
                message,
                (this.width - textWidth) / 2,
                popupY + 30,
                0xFFFFFFFF,
                false
        );

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground( GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
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
            exit();
            return true;
        }
        return super.keyPressed(input);
    }

    void exit() {
        assert minecraft != null;
        if (parent instanceof SavedCommandsScreen) {
            minecraft.setScreen(new SavedCommandsScreen());
        } else {
            minecraft.setScreen(parent);
        }
    }
}
