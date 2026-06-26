package de.aliba2468pr77pr77.savedcommands.mixin.client;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import de.aliba2468pr77pr77.savedcommands.EditCommandScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.gui.components.CommandSuggestions$SuggestionsList")
public abstract class SearchSuggestionWindow {
    @Redirect(
            method = {
                    "<init>",
                    "select"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;getValue()Ljava/lang/String;"
            )
    )
    private String savedcommands$replaceGetValue(EditBox instance) {
        if (Minecraft.getInstance().gui.screen() instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getText();
        } else {
            return instance.getValue();
        }
    }

    @Redirect(
            method = {
                    "useSuggestion"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;setCursorPosition(I)V"
            )
    )
    private void savedcommands$replaceSetCursorPosition(EditBox instance, int cursor) {
        if (Minecraft.getInstance().gui.screen() instanceof EditCommandScreen editCommandScreen) {
            instance.setCursorPosition(editCommandScreen.insertedVariables.getUninsertedIndex(cursor));
        } else {
            instance.setCursorPosition(cursor);
        }
    }

    @Redirect(
            method = {
                    "useSuggestion"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;setHighlightPos(I)V"
            )
    )
    private void savedcommands$replaceSetHighlightPos(EditBox instance, int cursor) {
        if (Minecraft.getInstance().gui.screen() instanceof EditCommandScreen editCommandScreen) {
            instance.setHighlightPos(editCommandScreen.insertedVariables.getUninsertedIndex(cursor));
        } else {
            instance.setHighlightPos(cursor);
        }
    }

    @Redirect(
            method = {
                    "useSuggestion"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/suggestion/Suggestion;apply(Ljava/lang/String;)Ljava/lang/String;"
            )
    )
    private String savedcommands$replaceApplySuggestion(Suggestion instance, String input) {
        if (Minecraft.getInstance().gui.screen() instanceof EditCommandScreen editCommandScreen) {
            Suggestion suggestionUninserted = new Suggestion(
                    new StringRange(editCommandScreen.insertedVariables.getUninsertedIndex(instance.getRange().getStart()),
                            editCommandScreen.insertedVariables.getUninsertedIndex(instance.getRange().getEnd())),
                    instance.getText()
            );
            return suggestionUninserted.apply(editCommandScreen.commandTextField.getValue());
        } else {
            return instance.apply(input);
        }
    }
}