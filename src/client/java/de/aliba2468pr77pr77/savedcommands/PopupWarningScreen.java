package de.aliba2468pr77pr77.savedcommands;


import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class PopupWarningScreen extends PopupScreen {
    Component message;

    PopupWarningScreen(Component header, Component message, Screen parent) {
        super(header, parent, 90, 290);
        this.message = message;
    }

    @Override
    protected void init() {
        super.init();

        Button closeButton = Button.builder(CommonComponents.GUI_OK, _ -> exit()).bounds(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        this.addRenderableWidget(closeButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        int textWidth = this.font.width(message);
        ctx.text(
                this.font,
                message,
                (this.width - textWidth) / 2,
                popupY + 30,
                0xFFFFFFFF,
                false
        );
    }
}
