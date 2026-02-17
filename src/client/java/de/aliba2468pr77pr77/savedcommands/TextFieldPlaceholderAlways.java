package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class TextFieldPlaceholderAlways extends TextFieldWidget {
    private @Nullable Text placeholder;
    private final TextRenderer textRenderer;
    private boolean centered;

    public TextFieldPlaceholderAlways(TextRenderer textRenderer, int width, int height, Text text) {
        super(textRenderer, width, height, text);
        this.textRenderer = textRenderer;
    }

    public TextFieldPlaceholderAlways(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
        this.textRenderer = textRenderer;
    }

    public TextFieldPlaceholderAlways(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable TextFieldWidget copyFrom, Text text) {
        super(textRenderer, x, y, width, height, copyFrom, text);
        this.textRenderer = textRenderer;
    }

    public void setPlaceholder(Text placeholder) {
        this.placeholder = placeholder.getStyle().equals(Style.EMPTY) ? placeholder.copy().fillStyle(PLACEHOLDER_STYLE) : placeholder;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
        super.setCentered(centered);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (this.isVisible() && this.placeholder != null && this.getText().isEmpty()) {
            int placeholderX = getX() + (centered ? (getWidth() - textRenderer.getWidth(placeholder)) / 2 : (drawsBackground() ? 4 : 0));
            int placeholderY = drawsBackground() ? getY() + (height - 8) / 2 : getY();
            if (drawsBackground()) {
                Identifier identifier = new ButtonTextures(Identifier.ofVanilla("widget/text_field"), Identifier.ofVanilla("widget/text_field_highlighted")).get(this.isInteractable(), this.isFocused());
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, getX(), getY(), getWidth(), getHeight());
            }

            context.drawTextWithShadow(textRenderer, placeholder, placeholderX, placeholderY, -2039584);

            if(isFocused() && (Util.getMeasuringTimeMs()) / 300L % 2L == 0L){
                context.drawText(textRenderer, "_", placeholderX, placeholderY, -2039584, true);
            }

            if (isHovered()) {
                context.setCursor(StandardCursors.IBEAM);
            }
        } else {
            super.renderWidget(context, mouseX, mouseY, deltaTicks);
        }
    }
}
