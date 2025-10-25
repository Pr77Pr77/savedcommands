package de.aliba2468pr77pr77.savedcommands.mixin.client;

import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatInputSuggestor.SuggestionWindow.class)
public interface SuggestionWindowInvoker {
    @Invoker("<init>")
    static ChatInputSuggestor.SuggestionWindow invokeInit(
            ChatInputSuggestor outer,
            int x, int y, int width,
            List<Suggestion> suggestions,
            boolean narrateFirstSuggestion) {
        throw new AssertionError();
    }
}
