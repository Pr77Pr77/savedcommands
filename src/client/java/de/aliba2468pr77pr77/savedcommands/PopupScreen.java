package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public abstract class PopupScreen extends Screen {
    public Screen parent;

    public int popupW;
    public int popupH;
    public int popupX;
    public int popupY;
    public int contentH;
    public int contentW;

    public PopupScreen(Component title, Screen parent, int contentH, int contentW) {
        super(title);
        this.parent = parent;
        this.contentH = contentH;
        this.contentW = contentW;
    }

    @Override
    protected void init() {
        super.init();

        calculateRectangle();
    }

    @Override
    public void repositionElements() {
        parent.resize(width, height);
        calculateRectangle();
    }

    protected void calculateRectangle() {
        popupW = Math.min(contentW, this.width - 20);
        popupH = Math.min(contentH, this.height - 20);
        popupX = (this.width - popupW) / 2;
        popupY = (this.height - popupH) / 2;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        calculateRectangle();

        int textWidth = this.font.width(title);
        ctx.drawString(
                this.font,
                title,
                (this.width - textWidth) / 2,
                popupY + 10,
                0xFFFFFFFF,
                false
        );

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(@NonNull GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        if (parent == null) {
            super.render(context, mouseX, mouseY, deltaTicks);
        } else {
            this.parent.renderBackground(context, -2147483648, -2147483648, deltaTicks);
            this.parent.render(context, -2147483648, -2147483648, deltaTicks);
            context.requestCursor(CursorTypes.ARROW);
            context.fill(0, 0, this.width, this.height, 0x88000000);
            context.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("popup/background"), popupX, popupY, popupW, popupH);
        }
    }

    @Override
    public void onClose() {
        exit();
    }

    protected void exit() {
        minecraft.setScreen(parent);
    }
}
