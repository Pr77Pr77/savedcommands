package de.aliba2468pr77pr77.savedcommands.mixin.client;

import com.mojang.brigadier.suggestion.Suggestion;
import de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.text.OrderedText;
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

    @Redirect(method = "show(Z)V", at = @At(value = "NEW", target = "net/minecraft/client/gui/screen/ChatInputSuggestor$SuggestionWindow"))
    private ChatInputSuggestor.SuggestionWindow createWindow(ChatInputSuggestor chatInputSuggestor, int x, int y, int width, List<Suggestion> suggestions, boolean narrateFirstSuggestion) {
        if (MinecraftClient.getInstance().currentScreen instanceof SavedCommandsScreen commandsScreen) {

            return SuggestionWindowInvoker.invokeInit(chatInputSuggestor, x + 3, commandsScreen.SearchBar.getY() + commandsScreen.SearchBar.getHeight() + 2, width, suggestions, narrateFirstSuggestion);
        }
        return SuggestionWindowInvoker.invokeInit(chatInputSuggestor, x, y, width, suggestions, narrateFirstSuggestion);
    }

    @Inject(method = "renderMessages", at = @At("HEAD"), cancellable = true)
    private void renderMessages(DrawContext context, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof SavedCommandsScreen commandsScreen) {
            for (int i = 0; i < this.messages.size(); i++) {
                int y = commandsScreen.SearchBar.getY() + commandsScreen.SearchBar.getHeight() + 1 + 12 * i;
                context.fill(this.x - 1, y, this.x + this.width, y + 12, this.color);
                context.drawTextWithShadow(this.textRenderer, this.messages.get(i), this.x, y + 2, 0xFFFFFFFF);
            }
            ci.cancel();
        }
    }
}
