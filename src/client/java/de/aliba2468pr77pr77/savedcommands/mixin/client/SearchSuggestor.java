package de.aliba2468pr77pr77.savedcommands.mixin.client;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import de.aliba2468pr77pr77.savedcommands.EditCommandScreen;
import de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatInputSuggestor.class)
public class SearchSuggestor {
    @Shadow
    @Final
    private List<OrderedText> messages;

    @Shadow
    private int x;

    @Shadow
    private int width;

    @Shadow
    @Final
    int color;

    @Shadow
    @Final
    TextRenderer textRenderer;

    @Shadow
    @Final
    private Screen owner;

    @Redirect(method = "show(Z)V", at = @At(value = "NEW", target = "net/minecraft/client/gui/screen/ChatInputSuggestor$SuggestionWindow"))
    private ChatInputSuggestor.SuggestionWindow createWindow(ChatInputSuggestor chatInputSuggestor, int x, int y, int width, List<Suggestion> suggestions, boolean narrateFirstSuggestion) {
        if (owner instanceof SavedCommandsScreen commandsScreen) {
            return SuggestionWindowInvoker.invokeInit(chatInputSuggestor, x + 3, commandsScreen.SearchBar.getY() + commandsScreen.SearchBar.getHeight() + 2, width, suggestions, narrateFirstSuggestion);
        }
        if (owner instanceof EditCommandScreen editCommandScreen) {
            return SuggestionWindowInvoker.invokeInit(chatInputSuggestor, x + 3, editCommandScreen.commandTextField.getY() + editCommandScreen.commandTextField.getHeight() + 2, width, suggestions, narrateFirstSuggestion);
        }
        return SuggestionWindowInvoker.invokeInit(chatInputSuggestor, x, y, width, suggestions, narrateFirstSuggestion);
    }

    @Inject(method = "renderMessages", at = @At("HEAD"), cancellable = true)
    private void renderMessages(DrawContext context, CallbackInfo ci) {
        if (owner instanceof SavedCommandsScreen commandsScreen) {
            for (int i = 0; i < this.messages.size(); i++) {
                int y = commandsScreen.SearchBar.getY() + commandsScreen.SearchBar.getHeight() + 1 + 12 * i;
                context.fill(this.x - 1, y, this.x + this.width, y + 12, this.color);
                context.drawTextWithShadow(this.textRenderer, this.messages.get(i), this.x, y + 2, 0xFFFFFFFF);
            }
            ci.cancel();
        }
        if (owner instanceof EditCommandScreen editCommandScreen) {
            for (int i = 0; i < this.messages.size(); i++) {
                int y = editCommandScreen.commandTextField.getY() + editCommandScreen.commandTextField.getHeight() + 1 + 12 * i;
                context.fill(editCommandScreen.popupX + 6, y, editCommandScreen.popupX + editCommandScreen.popupW - 6, y + 12, this.color);
                StringBuilder sb = new StringBuilder();
                this.messages.get(i).accept((index, style, codePoint) -> {
                    sb.appendCodePoint(codePoint);
                    return true;
                });
                context.drawTextWithShadow(this.textRenderer,
                        SavedCommandsScreen.shortenTextIfNeeded(sb.toString(), editCommandScreen.popupW - 12, Formatting.RESET),
                        editCommandScreen.popupX + 8, y + 2, 0xFFFFFFFF);
            }
            ci.cancel();
        }
    }

    @Redirect(
            method = {
                    "sortSuggestions",
                    "refresh",
                    "showCommandSuggestions",
                    "render"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getText()Ljava/lang/String;"
            )
    )
    private String savedcommands$replaceGetText(TextFieldWidget instance) {
        if (owner instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getText();
        } else {
            return instance.getText();
        }
    }

    @Redirect(
            method = {
                    "show",
                    "showUsages"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getCharacterX(I)I"
            )
    )
    private int savedcommands$replaceGetCharacterX(TextFieldWidget instance, int index) {
        if (owner instanceof EditCommandScreen editCommandScreen) {
            String text = instance.getText();
            int indexUninserted = editCommandScreen.insertedVariables.getUninsertedIndex(index);
            return indexUninserted > text.length() ? instance.getX() : instance.getX() + textRenderer.getWidth(text.substring(0, indexUninserted));
        } else {
            return instance.getCharacterX(index);
        }
    }

    @Redirect(
            method = {
                    "refresh",
                    "sortSuggestions",
                    "showCommandSuggestions",
                    "showUsages",
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getCursor()I"
            )
    )
    private int savedcommands$replaceGetCursor(TextFieldWidget instance) {
        if (owner instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getCursor();
        } else {
            return instance.getCursor();
        }
    }

    @Redirect(
            method = "highlight",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/context/StringRange;getEnd()I"
            )
    )
    private static int savedcommands$replaceGetEnd(StringRange instance) {
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getUninsertedIndex(instance.getEnd());
        } else {
            return instance.getEnd();
        }
    }

    @Redirect(
            method = "highlight",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/context/StringRange;getStart()I"
            )
    )
    private static int savedcommands$replaceGetStart(StringRange instance) {
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getUninsertedIndex(instance.getStart());
        } else {
            return instance.getStart();
        }
    }

    @Redirect(
            method = "highlight",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/ImmutableStringReader;getCursor()I"
            )
    )
    private static int savedcommands$replaceStringReaderGetCursor(ImmutableStringReader instance) {
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getUninsertedIndex(instance.getCursor());
        } else {
            return instance.getCursor();
        }
    }

    @Redirect(
            method = "highlight",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/ImmutableStringReader;getRemainingLength()I"
            )
    )
    private static int savedcommands$replaceStringReaderGetRemainingLength(ImmutableStringReader instance) {
        if (MinecraftClient.getInstance().currentScreen instanceof EditCommandScreen editCommandScreen) {
            return editCommandScreen.insertedVariables.getUninsertedIndex(instance.getRemainingLength());
        } else {
            return instance.getRemainingLength();
        }
    }
}
