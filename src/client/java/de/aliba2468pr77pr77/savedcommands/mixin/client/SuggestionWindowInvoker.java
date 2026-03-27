package de.aliba2468pr77pr77.savedcommands.mixin.client;

import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(CommandSuggestions.SuggestionsList.class)
public interface SuggestionWindowInvoker {
    @Invoker("<init>")
    static CommandSuggestions.SuggestionsList invokeInit(
            CommandSuggestions outer,
            int x, int y, int width,
            List<Suggestion> suggestions,
            boolean narrateFirstSuggestion) {
        throw new AssertionError();
    }
}
