package de.aliba2468pr77pr77.savedcommands.mixin.client;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import de.aliba2468pr77pr77.savedcommands.EditCommandScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatInputSuggestor.SuggestionWindow.class)
public abstract class SearchSuggestionWindow {
    @Redirect(
            method = {
                    "<init>",
                    "select"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getText()Ljava/lang/String;"
            )
    )
    private String savedcommands$replaceGetText(TextFieldWidget instance) {
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getText();
        } else {
            return instance.getText();
        }
    }

    @Redirect(
            method = {
                    "complete"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setSelectionStart(I)V"
            )
    )
    private void savedcommands$replaceSetSelectionStart(TextFieldWidget instance, int cursor) {
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen editCommandScreen) {
            instance.setSelectionStart(editCommandScreen.insertedVariables.getUninsertedIndex(cursor));
        } else {
            instance.setSelectionStart(cursor);
        }
    }

    @Redirect(
            method = {
                    "complete"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setSelectionEnd(I)V"
            )
    )
    private void savedcommands$replaceSetSelectionEnd(TextFieldWidget instance, int cursor) {
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen editCommandScreen) {
            instance.setSelectionEnd(editCommandScreen.insertedVariables.getUninsertedIndex(cursor));
        } else {
            instance.setSelectionEnd(cursor);
        }
    }

    @Redirect(
            method = {
                    "complete"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/suggestion/Suggestion;apply(Ljava/lang/String;)Ljava/lang/String;",
                    remap = false
            )
    )
    private String savedcommands$replaceApplySuggestion(Suggestion instance, String input) {
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen editCommandScreen) {
            Suggestion suggestionUninserted = new Suggestion(
                    new StringRange(editCommandScreen.insertedVariables.getUninsertedIndex(instance.getRange().getStart()),
                            editCommandScreen.insertedVariables.getUninsertedIndex(instance.getRange().getEnd())),
                    instance.getText()
            );
            return suggestionUninserted.apply(editCommandScreen.commandTextField.getText());
        } else {
            return instance.apply(input);
        }
    }
}