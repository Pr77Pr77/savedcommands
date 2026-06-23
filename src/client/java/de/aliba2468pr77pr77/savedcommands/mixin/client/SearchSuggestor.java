package de.aliba2468pr77pr77.savedcommands.mixin.client;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import de.aliba2468pr77pr77.savedcommands.EditCommandScreen;
import de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CommandSuggestions.class)
public class SearchSuggestor {
    @Shadow
    @Final
    private List<FormattedCharSequence> commandUsage;

    @Shadow
    private int commandUsagePosition;

    @Shadow
    private int commandUsageWidth;

    @Shadow
    @Final
   int fillColor;

    @Shadow
    @Final
    Font font;

    @Shadow
    @Final
    private Screen screen;

    @Redirect(method = "showSuggestions(Z)V", at = @At(value = "NEW", target = "net/minecraft/client/gui/components/CommandSuggestions$SuggestionsList"))
    private CommandSuggestions.SuggestionsList createWindow(CommandSuggestions commandSuggestions, int x, int y, int width, List<Suggestion> suggestions, boolean narrateFirstSuggestion) {
        if (screen instanceof SavedCommandsScreen commandsScreen) {
            return SuggestionWindowInvoker.invokeInit(commandSuggestions, x + 3, commandsScreen.searchBar.getY() + commandsScreen.searchBar.getHeight() + 2, width, suggestions, narrateFirstSuggestion);
        }
        if (screen instanceof EditCommandScreen editCommandScreen) {
            return SuggestionWindowInvoker.invokeInit(commandSuggestions, x + 3, editCommandScreen.commandTextField.getY() + editCommandScreen.commandTextField.getHeight() + 2, width, suggestions, narrateFirstSuggestion);
        }
        return SuggestionWindowInvoker.invokeInit(commandSuggestions, x, y, width, suggestions, narrateFirstSuggestion);
    }

    @Inject(method = "renderUsage", at = @At("HEAD"), cancellable = true)
    private void renderMessages(GuiGraphics graphics, CallbackInfo ci) {
        if (screen instanceof SavedCommandsScreen commandsScreen) {
            for (int i = 0; i < this.commandUsage.size(); i++) {
                int y = commandsScreen.searchBar.getY() + commandsScreen.searchBar.getHeight() + 1 + 12 * i;
                int x = Math.max(this.commandUsagePosition, 0);
                graphics.fill(x - 1, y, x + this.commandUsageWidth, y + 12, this.fillColor);
                graphics.drawString(this.font, this.commandUsage.get(i), x, y + 2, 0xFFFFFFFF, true);
            }
            ci.cancel();
        }
        if (screen instanceof EditCommandScreen editCommandScreen) {
            for (int i = 0; i < this.commandUsage.size(); i++) {
                int y = editCommandScreen.commandTextField.getY() + editCommandScreen.commandTextField.getHeight() + 1 + 12 * i;
                graphics.fill(editCommandScreen.popupX + 6, y, editCommandScreen.popupX + editCommandScreen.popupW - 6, y + 12, this.fillColor);
                StringBuilder sb = new StringBuilder();
                this.commandUsage.get(i).accept((index, style, codePoint) -> {
                    sb.appendCodePoint(codePoint);
                    return true;
                });
                graphics.drawString(this.font,
                        SavedCommandsScreen.shortenTextIfNeeded(sb.toString(), editCommandScreen.popupW - 12, ChatFormatting.RESET),
                        editCommandScreen.popupX + 8, y + 2, 0xFFFFFFFF, true);
            }
            ci.cancel();
        }
    }

    @Redirect(
            method = {
                    "sortSuggestions",
                    "updateCommandInfo",
                    "updateUsageInfo"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;getValue()Ljava/lang/String;"
            )
    )
    private String savedcommands$replaceGetValue(EditBox instance) {
        if (screen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getText();
        } else {
            return instance.getValue();
        }
    }

    @Redirect(
            method = {
                    "showSuggestions",
                    "updateUsageInfo"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;getScreenX(I)I"
            )
    )
    private int savedcommands$replaceGetScreenX(EditBox instance, int index) {
        if (screen instanceof EditCommandScreen editCommandScreen) {
            String text = instance.getValue();
            int indexUninserted = editCommandScreen.insertedVariables.getUninsertedIndex(index);
            return indexUninserted > text.length() ? instance.getX() : instance.getX() + font.width(text.substring(0, indexUninserted));
        } else {
            return instance.getScreenX(index);
        }
    }

    @Redirect(
            method = {
                    "updateCommandInfo",
                    "sortSuggestions",
                    "updateUsageInfo"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;getCursorPosition()I"
            )
    )
    private int savedcommands$replaceGetCursorPosition(EditBox instance) {
        if (screen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getCursor();
        } else {
            return instance.getCursorPosition();
        }
    }

    @Redirect(
            method = "formatText",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/context/StringRange;getEnd()I",
                    remap = false
            )
    )
    private static int savedcommands$replaceGetEnd(StringRange instance) {
        if (Minecraft.getInstance().screen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getUninsertedIndex(instance.getEnd());
        } else {
            return instance.getEnd();
        }
    }

    @Redirect(
            method = "formatText",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/context/StringRange;getStart()I",
                    remap = false
            )
    )
    private static int savedcommands$replaceGetStart(StringRange instance) {
        if (Minecraft.getInstance().screen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getUninsertedIndex(instance.getStart());
        } else {
            return instance.getStart();
        }
    }

    @Redirect(
            method = "formatText",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/ImmutableStringReader;getCursor()I",
                    remap = false
            )
    )
    private static int savedcommands$replaceStringReaderGetCursor(ImmutableStringReader instance) {
        if (Minecraft.getInstance().screen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getUninsertedIndex(instance.getCursor());
        } else {
            return instance.getCursor();
        }
    }

    @Redirect(
            method = "formatText",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/ImmutableStringReader;getRemainingLength()I",
                    remap = false
            )
    )
    private static int savedcommands$replaceStringReaderGetRemainingLength(ImmutableStringReader instance) {
        if (Minecraft.getInstance().screen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getUninsertedIndex(instance.getRemainingLength());
        } else {
            return instance.getRemainingLength();
        }
    }
}
