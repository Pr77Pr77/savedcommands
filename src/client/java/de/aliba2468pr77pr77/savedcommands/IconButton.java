package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class IconButton extends Button {
    private final Identifier icon;

    public IconButton(final int x, final int y, final int width, final int height, Identifier icon, final OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
    protected void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);

        int iconSize = 20;
        int iconX = this.getX() + (this.getWidth() - iconSize) / 2;
        int iconY = this.getY() + (this.getHeight() - iconSize) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
    }
}
