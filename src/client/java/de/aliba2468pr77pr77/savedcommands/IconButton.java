package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

public class IconButton extends Button {
    private final ResourceLocation icon;
    private boolean iconShown = true;

    public IconButton(final int x, final int y, final int width, final int height, ResourceLocation icon, final OnPress onPress, Component tooltipNarration) {
        super(x, y, width, height, Component.empty(), onPress, (componentSupplier) -> (MutableComponent) tooltipNarration);
        setTooltip(Tooltip.create(tooltipNarration));
        this.icon = icon;
    }

    // Switchable between icon and component
    public IconButton(final int x, final int y, final int width, final int height, ResourceLocation iconSwitchable, final OnPress onPress, Component tooltipNarration, Component componentSwitchable, boolean initialIconShown) {
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
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
        if (iconShown) {
            int iconSize = 20;
            int iconX = this.getX() + (this.getWidth() - iconSize) / 2;
            int iconY = this.getY() + (this.getHeight() - iconSize) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        } else {
            int k = ARGB.color(this.alpha, this.active ? -1 : -6250336);
            this.renderString(graphics, Minecraft.getInstance().font, k);
        }
        if (this.isHovered()) {
            graphics.requestCursor(this.isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }
    }
}
