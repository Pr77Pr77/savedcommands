package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;

public class EditCategoryScreen extends PopupScreen {
    private Button exitButton;
    private EditBox nameEditBox;

    SavedCommandManager.SavedCommandsData.CustomCategory category;

    public EditCategoryScreen(SavedCommandManager.SavedCommandsData.CustomCategory category, @Nullable Screen parent) {
        super(Component.translatable("screen.savedcommands.editcategory"), parent, 145, 300);
        this.category = category;
    }

    @Override
    protected void init() {
        super.init();

        nameEditBox = new EditBox(this.minecraft.font, popupX + 20, popupY + 40, popupW - 40, 20,
                Component.translatable("screen.savedcommands.name"));
        nameEditBox.setMaxLength(256);
        nameEditBox.setBordered(true);
        nameEditBox.setCanLoseFocus(true);
        nameEditBox.setValue(category.name);
        nameEditBox.setResponder((text) -> {
            if (text.isEmpty()) {
                exitButton.setMessage(CommonComponents.GUI_CANCEL);
            } else {
                exitButton.setMessage(CommonComponents.GUI_DONE);
            }
        });
        addRenderableWidget(nameEditBox);

        exitButton = Button.builder(CommonComponents.GUI_DONE, _ -> exit()).bounds(popupX + (popupW - 100) / 2, popupY + popupH - 20 - 20, 100, 20).build();
        addRenderableWidget(exitButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        ctx.text(
                this.font,
                Component.translatable("screen.savedcommands.name"),
                popupX + 20,
                nameEditBox.getY() - 10,
                0xFFFFFFFF,
                true
        );

        ctx.text(
                this.font,
                Component.translatable("screen.savedcommands.categoryid").append(": "),
                popupX + 20,
                popupY + 70,
                0xFFFFFFFF,
                true
        );

        ctx.text(
                this.font,
                category.id,
                popupX + 20,
                popupY + 85,
                0xFFFFFFFF,
                true
        );
    }

    @Override
    protected void exit() {
        if (!nameEditBox.getValue().isEmpty()) {
            category.name = nameEditBox.getValue();

            commandManager.saveAsync();
        }
        super.exit();
    }
}
