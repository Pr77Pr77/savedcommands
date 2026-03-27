package de.aliba2468pr77pr77.savedcommands;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TextFieldPlaceholderAlways extends EditBox {
    private @Nullable Component placeholder;
    private final Font font;
    private boolean centered;

    public TextFieldPlaceholderAlways(Font font, int width, int height, Component text) {
        super(font, width, height, text);
        this.font = font;
    }

    public TextFieldPlaceholderAlways(Font font, int x, int y, int width, int height, Component text) {
        super(font, x, y, width, height, text);
        this.font = font;
    }

    public TextFieldPlaceholderAlways(Font font, int x, int y, int width, int height, @Nullable EditBox copyFrom, Component text) {
        super(font, x, y, width, height, copyFrom, text);
        this.font = font;
    }

    public void setHint(Component hint) {
        this.placeholder = hint.getStyle().equals(Style.EMPTY) ? hint.copy().withStyle(DEFAULT_HINT_STYLE) : placeholder;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
        super.setCentered(centered);
    }

    @Override
    public void extractWidgetRenderState(final @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        if (this.isVisible() && this.placeholder != null && this.getValue().isEmpty()) {
            int placeholderX = getX() + (centered ? (getWidth() - font.width(placeholder)) / 2 : (isBordered() ? 4 : 0));
            int placeholderY = isBordered() ? getY() + (height - 8) / 2 : getY();
            if (isBordered()) {
                Identifier identifier = new WidgetSprites(Identifier.withDefaultNamespace("widget/text_field"), Identifier.withDefaultNamespace("widget/text_field_highlighted")).get(this.isActive(), this.isFocused());
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, getX(), getY(), getWidth(), getHeight());
            }

            graphics.text(font, placeholder, placeholderX, placeholderY, -2039584, true);

            if (isFocused() && (Util.getMillis()) / 300L % 2L == 0L) {
                graphics.text(font, "_", placeholderX, placeholderY, -2039584, true);
            }

            if (this.isHovered()) {
                graphics.requestCursor(CursorTypes.IBEAM);
            }
        } else {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, deltaTicks);
        }
    }
}
