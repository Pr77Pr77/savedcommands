package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class IconButton extends Button {
    private final ResourceLocation icon;

    public IconButton(final int x, final int y, final int width, final int height, ResourceLocation icon, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.renderWidget(graphics, mouseX, mouseY, a);

        int iconSize = 20;
        int iconX = this.getX() + (this.getWidth() - iconSize) / 2;
        int iconY = this.getY() + (this.getHeight() - iconSize) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
    }
}
