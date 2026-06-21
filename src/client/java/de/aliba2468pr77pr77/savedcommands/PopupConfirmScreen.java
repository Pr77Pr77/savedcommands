package de.aliba2468pr77pr77.savedcommands;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class PopupConfirmScreen extends PopupScreen {
    final Component message;
    final Component yesComponent;
    final Component noComponent;
    final Consumer<ShowAgainState> callbackOnYes;
    final Runnable callbackOnYesSimple;
    final boolean globalAlreadyDisabled;
    boolean worldSelectedBeforeGlobal = false;
    boolean noCheckboxes;

    MultiLineTextWidget messageWidget;
    Checkbox worldShowAgain;
    Checkbox globalShowAgain;
    Button yesButton;
    Button noButton;

    enum ShowAgainState {
        SHOW_AGAIN,
        WORLD_DISABLED,
        GLOBAL_DISABLED
    }

    // With show again checkboxes
    PopupConfirmScreen(final Component header, final Component message, final Screen parent, final Component yesComponent, final Component noComponent,
                       final Consumer<ShowAgainState> callbackOnYes, final boolean globalAlreadyDisabled) {
        super(header, parent, 90, 310);
        this.message = message;
        this.yesComponent = yesComponent;
        this.noComponent = noComponent;
        this.callbackOnYes = callbackOnYes;
        this.callbackOnYesSimple = null;
        this.globalAlreadyDisabled = globalAlreadyDisabled;
        this.noCheckboxes = false;
    }

    // Without show again checkboxes
    PopupConfirmScreen(final Component header, final Component message, final Screen parent, final Component yesComponent, final Component noComponent,
                       final Runnable callbackOnYes) {
        super(header, parent, 90, 310);
        this.message = message;
        this.yesComponent = yesComponent;
        this.noComponent = noComponent;
        this.callbackOnYes = null;
        this.callbackOnYesSimple = callbackOnYes;
        this.globalAlreadyDisabled = false;
        this.noCheckboxes = true;
    }

    @Override
    protected void init() {
        super.init();

        messageWidget = new MultiLineTextWidget(this.message, this.font);
        messageWidget.setCentered(true);
        messageWidget.setPosition(popupX + 10, popupY + 30);
        messageWidget.setMaxWidth(popupW - 20);
        messageWidget.setMaxRows(15);
        messageWidget.setMessage(message);
        addRenderableWidget(messageWidget);

        contentH = 30 + messageWidget.getHeight() + 5 + 10 + 20;

        if (!noCheckboxes) {
            worldShowAgain = Checkbox.builder(Component.translatable("screen.savedcommands.dontshowagain.world"), font)
                    .pos(popupX + 10, popupY + 30 + messageWidget.getHeight() + 5).maxWidth(popupW - 20).build();
            addRenderableWidget(worldShowAgain);

            contentH += worldShowAgain.getHeight() + 5;

            if (!globalAlreadyDisabled) {
                globalShowAgain = Checkbox.builder(Component.translatable("screen.savedcommands.dontshowagain.global"), font)
                        .pos(popupX + 10, popupY + 30 + messageWidget.getHeight() + 5 + worldShowAgain.getHeight() + 5).maxWidth(popupW - 20).onValueChange(((checkbox, value) -> {
                            worldShowAgain.active = !value;
                            if (value) {
                                worldSelectedBeforeGlobal = worldShowAgain.selected();
                                worldShowAgain.selected = true;
                            } else {
                                worldShowAgain.selected = worldSelectedBeforeGlobal;
                            }
                        })).build();
                addRenderableWidget(globalShowAgain);

                contentH += globalShowAgain.getHeight() + 5;
            }
        }

        calculateRectangle();

        messageWidget.setPosition(popupX + (popupW - messageWidget.getWidth()) / 2, popupY + 30);
        if (!noCheckboxes) {
            worldShowAgain.setPosition(popupX + (popupW - worldShowAgain.getWidth()) / 2, popupY + 30 + messageWidget.getHeight() + 5);
            if (!globalAlreadyDisabled) {
                globalShowAgain.setPosition(popupX + (popupW - globalShowAgain.getWidth()) / 2, popupY + 30 + messageWidget.getHeight() + 5 + globalShowAgain.getHeight() + 5);
            }
        }

        yesButton = Button.builder(yesComponent, button -> {
            if (callbackOnYes != null) {
                if (globalShowAgain != null && globalShowAgain.selected) {
                    callbackOnYes.accept(ShowAgainState.GLOBAL_DISABLED);
                } else if (worldShowAgain.selected) {
                    callbackOnYes.accept(ShowAgainState.WORLD_DISABLED);
                } else {
                    callbackOnYes.accept(ShowAgainState.SHOW_AGAIN);
                }
            } else if (callbackOnYesSimple != null) {
                callbackOnYesSimple.run();
            }
            exit();
        }).bounds(popupX + (popupW - 100 - 100 - 5) / 2, popupY + popupH - 10 - 20, 100, 20).build();
        addRenderableWidget(yesButton);

        noButton = Button.builder(noComponent, button -> exit()).
                bounds(popupX + (popupW - 100 + 100 + 5) / 2, popupY + popupH - 10 - 20, 100, 20).build();
        addRenderableWidget(noButton);
    }

    @Override
    public void repositionElements() {
        super.repositionElements();

        messageWidget.setMaxWidth(popupW - 20);
        contentH = 30 + messageWidget.getHeight() + 5 + 10 + 20;

        if (worldShowAgain != null) {
            worldShowAgain.setWidth(worldShowAgain.getAdjustedWidth(popupW - 20, worldShowAgain.getMessage(), font));
            contentH += worldShowAgain.getHeight() + 5;

            if (globalShowAgain != null) {
                globalShowAgain.setWidth(globalShowAgain.getAdjustedWidth(popupW - 20, globalShowAgain.getMessage(), font));
                contentH += globalShowAgain.getHeight() + 5;
            }
        }
        calculateRectangle();

        messageWidget.setPosition(popupX + (popupW - messageWidget.getWidth()) / 2, popupY + 30);
        if (worldShowAgain != null) {
            worldShowAgain.setPosition(popupX + (popupW - worldShowAgain.getWidth()) / 2, popupY + 30 + messageWidget.getHeight() + 5);
            if (globalShowAgain != null) {
                globalShowAgain.setPosition(popupX + (popupW - globalShowAgain.getWidth()) / 2, popupY + 30 + messageWidget.getHeight() + 5 + globalShowAgain.getHeight() + 5);
            }
        }

        yesButton.setPosition(popupX + (popupW - 100 - 100 - 5) / 2, popupY + popupH - 10 - 20);
        noButton.setPosition(popupX + (popupW - 100 + 100 + 5) / 2, popupY + popupH - 10 - 20);
    }
}
