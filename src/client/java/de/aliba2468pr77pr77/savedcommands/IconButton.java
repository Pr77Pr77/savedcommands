package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Identifier;

public class IconButton extends ButtonWidget {
    private final Identifier icon;

    public IconButton(int x, int y, int width, int height, Identifier icon, PressAction onPress) {
        super(x, y, width, height, net.minecraft.text.Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.icon = icon;
    }

    @Override
    public void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.drawButton(ctx);

        int iconSize = 20;
        int iconX = this.getX() + (this.getWidth() - iconSize) / 2;
        int iconY = this.getY() + (this.getHeight() - iconSize) / 2;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
    }
}
