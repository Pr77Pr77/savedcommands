package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class IconButton extends Button {
    private final Identifier icon;
    private boolean iconShown = true;

    public IconButton(final int x, final int y, final int width, final int height, Identifier icon, final OnPress onPress, Component tooltipNarration) {
        super(x, y, width, height, Component.empty(), onPress, (componentSupplier) -> (MutableComponent) tooltipNarration);
        setTooltip(Tooltip.create(tooltipNarration));
        this.icon = icon;
    }

    // Switchable between icon and component
    public IconButton(final int x, final int y, final int width, final int height, Identifier iconSwitchable, final OnPress onPress, Component tooltipNarration, Component componentSwitchable, boolean initialIconShown) {
        super(x, y, width, height, componentSwitchable, onPress, (componentSupplier) -> (MutableComponent) tooltipNarration);
        setTooltip(Tooltip.create(tooltipNarration));
        this.iconShown = initialIconShown;
        this.icon = iconSwitchable;
    }

    public void switchToIcon() {
        iconShown = true;
    }

    public void switchToComponent() {
        iconShown = false;
    }

    @Override
    protected void renderContents(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float a) {
        if (iconShown) {
            this.renderDefaultSprite(graphics);

            int iconSize = 20;
            int iconX = this.getX() + (this.getWidth() - iconSize) / 2;
            int iconY = this.getY() + (this.getHeight() - iconSize) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        } else {
            this.renderDefaultSprite(graphics);
            this.renderDefaultLabel(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        }
    }
}
