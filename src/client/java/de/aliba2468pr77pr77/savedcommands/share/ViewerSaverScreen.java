package de.aliba2468pr77pr77.savedcommands.share;

import de.aliba2468pr77pr77.savedcommands.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.NarratableEntryOfString;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.commandManager;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen.shortenTextIfNeeded;

public class ViewerSaverScreen extends PopupScreen {
    private ViewerSaverList viewerSaverList;
    Button saveAllButton;
    Button editAllButton;
    Button deleteAllButton;
    Button continueLaterButton;

    boolean editingAll = false;

    public ViewerSaverScreen(Screen parent) {
        super(Component.translatable("screen.savedcommands.share.viewersaverscreen"), parent, 300, 450);
    }

    @Override
    protected void init() {
        super.init();
        viewerSaverList = new ViewerSaverList(minecraft, popupW - 6 * 2, popupH - 35 - 25, popupY + 25, 36);
        viewerSaverList.setX(popupX + 6);
        viewerSaverList.addSentCommandsAndPlayers(SavedCommandsClient.sharingManager.receivedCommandsByPlayerName);
        this.addRenderableWidget(this.viewerSaverList);

        this.saveAllButton = Button.builder(Component.translatable("screen.savedcommands.share.saveall"), _ -> exit(exitTypes.SAVE_ALL)).bounds((this.width - 100 - 5 - 100 - 5 - 100 - 5 - 100) / 2, popupY + popupH - 20 - 10, 100, 20).build();
        this.addRenderableWidget(this.saveAllButton);

        this.editAllButton = Button.builder(Component.translatable("screen.savedcommands.share.editall"), _ -> exit(exitTypes.EDIT_ALL)).bounds((this.width - 100 - 5 - 100 - 5 - 100 + 5 + 100) / 2, popupY + popupH - 20 - 10, 100, 20).build();
        this.addRenderableWidget(this.editAllButton);

        this.deleteAllButton = Button.builder(Component.translatable("screen.savedcommands.share.deleteall"), _ -> exit(exitTypes.DELETE_ALL)).bounds((this.width - 100 - 5 - 100 + 5 + 100 + 5 + 100) / 2, popupY + popupH - 20 - 10, 100, 20).build();
        this.addRenderableWidget(this.deleteAllButton);

        this.continueLaterButton = Button.builder(Component.translatable("screen.savedcommands.share.continuelater"), _ -> exit(exitTypes.CONTINUE_LATER)).bounds((this.width - 100 + 5 + 100 + 5 + 100 + 5 + 100) / 2, popupY + popupH - 20 - 10, 100, 20).build();
        this.addRenderableWidget(this.continueLaterButton);
    }

    @Override
    public void added() {
        if (editingAll) {
            if (parent instanceof SavedCommandsScreen savedCommandsScreen) {
                savedCommandsScreen.updateSearchAndScroll(savedCommandsScreen.searchBar.getValue()); // Done to update the list ib the background
            }
            exit(exitTypes.EDIT_ALL);
        }
    }

    @Override
    public void repositionElements() {
        super.repositionElements();

        viewerSaverList.setRectangle(popupW - 6 * 2, popupH - 35 - 25, popupX + 6, popupY + 25);
        viewerSaverList.repositionEntries();

        saveAllButton.setPosition((width - 100 - 5 - 100 - 5 - 100 - 5 - 100) / 2, popupY + popupH - 20 - 10);
        editAllButton.setPosition((width - 100 - 5 - 100 - 5 - 100 + 5 + 100) / 2, popupY + popupH - 20 - 10);
        deleteAllButton.setPosition((width - 100 - 5 - 100 + 5 + 100 + 5 + 100) / 2, popupY + popupH - 20 - 10);
        continueLaterButton.setPosition((width - 100 + 5 + 100 + 5 + 100 + 5 + 100) / 2, popupY + popupH - 20 - 10);
    }

    public enum exitTypes {
        SAVE_ALL,
        EDIT_ALL,
        DELETE_ALL,
        CONTINUE_LATER
    }

    @Override
    protected void exit() {
        exit(exitTypes.CONTINUE_LATER);
    }

    private void exit(exitTypes exitType) {
        switch (exitType) {
            case SAVE_ALL:
                for (List<SavedCommandManager.CommandData> commandDataList : SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.values()) {
                    commandManager.data.commands.addAll(commandDataList);
                }
                commandManager.saveAsync();
                if (parent instanceof SavedCommandsScreen savedCommandsScreen) {
                    savedCommandsScreen.updateSearchAndScroll(savedCommandsScreen.searchBar.getValue());
                    savedCommandsScreen.removeNotificationButton();
                }

                SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.clear();
                super.exit();
                break;
            case EDIT_ALL:
                Map.Entry<String, List<SavedCommandManager.CommandData>> commandDataListEntry = SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.entrySet().iterator().next();
                SavedCommandManager.CommandData commandData = commandDataListEntry.getValue().getFirst();

                commandManager.data.commands.add(commandData);
                boolean emptyMap = deleteCommand(commandDataListEntry.getKey(), commandData, viewerSaverList);
                minecraft.setScreen(new EditCommandScreen(emptyMap ? parent : this, commandData));
                if (emptyMap && parent instanceof SavedCommandsScreen savedCommandsScreen) {
                    savedCommandsScreen.removeNotificationButton();
                } else if (!emptyMap) {
                    viewerSaverList.setScrollAmount(0);
                }
                editingAll = true;
                // Next command after returning to this screen.
                break;
            case DELETE_ALL:
                if (parent instanceof SavedCommandsScreen savedCommandsScreen) {
                    savedCommandsScreen.removeNotificationButton();
                }
                SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.clear();
                super.exit();
                break;
            case CONTINUE_LATER:
                super.exit();
                break;
        }
    }

    private boolean deleteCommand(String playerName, SavedCommandManager.CommandData command, ViewerSaverList list) { // Returns weather the Map is completely empty
        SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.get(playerName).remove(command);
        if (SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.get(playerName).isEmpty()) {
            SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.remove(playerName);
            if (SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.isEmpty()) {
                return true;
            }
        }
        list.addSentCommandsAndPlayers(SavedCommandsClient.sharingManager.receivedCommandsByPlayerName);
        return false;
    }

    private static class ViewerSaverList extends ContainerObjectSelectionList<ViewerSaverList.BaseEntry> {
        public ViewerSaverList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
            super(minecraft, width, height, top, itemHeight);
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        protected int scrollBarX() {
            return this.getRowRight() - 1;
        }

        @Override
        protected void extractListBackground(@NonNull GuiGraphicsExtractor graphics) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x46000000);
        }

        protected void addSentCommandsAndPlayers(Map<String, List<SavedCommandManager.CommandData>> receivedCommandsByPlayerName) {
            clearEntries();
            for (Map.Entry<String, List<SavedCommandManager.CommandData>> player : receivedCommandsByPlayerName.entrySet()) {
                PlayerInfo playerInfo = Objects.requireNonNull(minecraft.getConnection()).getPlayerInfo(player.getKey());
                if (playerInfo != null) {
                    addEntry(new PlayerEntry(playerInfo));
                } else {
                    addEntry(new PlayerEntryPrimitive(player.getKey()));
                }
                for (SavedCommandManager.CommandData commandData : player.getValue()) {
                    addEntry(new CommandEntry(commandData, this, player.getKey()));
                }
            }
        }

        public static class BaseEntry extends Entry<BaseEntry> {
            BaseEntry() {
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of();
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of();
            }
        }

        public class PlayerEntry extends BaseEntry {
            PlayerInfo player;

            PlayerEntry(PlayerInfo player) {
                this.player = player;
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                Supplier<PlayerSkin> skinGetter = player::getSkin;
                PlayerFaceExtractor.extractRenderState(graphics, skinGetter.get(), getContentX() + 4, getContentY() + (getContentHeight() - 24) / 2, 24);
                graphics.text(minecraft.font, Component.literal(player.getProfile().name()).withStyle(ChatFormatting.BOLD), getContentX() + 4 + 24 + 4, getContentY() + (getContentHeight() - 8) / 2, 0xFFFFFFFF);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfString(player.getProfile().name()));
            }
        }

        public class PlayerEntryPrimitive extends BaseEntry {
            String player;

            PlayerEntryPrimitive(String player) {
                this.player = player;
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/unknown-player.png"), getContentX() + 4, getContentY() + (getContentHeight() - 24) / 2, 0, 0, 24, 24, 24, 24);
                graphics.text(minecraft.font, player, getContentX() + 4 + 24 + 4, getContentY() + (getContentHeight() - 8) / 2, 0xFFFFFFFF);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfString(player));
            }
        }

        public class CommandEntry extends BaseEntry {
            SavedCommandManager.CommandData command;
            ViewerSaverList list;
            String playerName;

            public enum MatchResult {NONE, SIMILAR, EXACT}

            Map.@Nullable Entry<SavedCommandManager.CommandData, Double> similarCommand; // command + similarity from 0 to 1
            MatchResult matchResult;

            IconButton saveButton;
            IconButton editButton;
            IconButton deleteButton;

            CommandEntry(SavedCommandManager.CommandData command, ViewerSaverList list, String playerName) {
                this.command = command;
                this.list = list;
                this.playerName = playerName;

                saveButton = new IconButton(getContentX() + getContentWidth() - 30 * 3, getContentY() + (getContentHeight() - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/save.png"), _ -> {
                    commandManager.data.commands.add(this.command);

                    delete();
                }, Component.translatable("selectWorld.edit.save"));

                editButton = new IconButton(getContentX() + getContentWidth() - 30 * 2, getContentY() + (getContentHeight() - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/edit.png"), _ -> {
                    commandManager.data.commands.add(this.command);
                    if (delete() && minecraft.screen instanceof ViewerSaverScreen viewerSaverScreen) {
                        minecraft.setScreen(new EditCommandScreen(viewerSaverScreen.parent, this.command));
                    } else {
                        minecraft.setScreen(new EditCommandScreen(minecraft.screen, this.command));
                    }

                }, Component.translatable("screen.savedcommands.share.editandsave"));

                deleteButton = new IconButton(getContentX() + getContentWidth() - 30, getContentY() + (getContentHeight() - 20) / 2, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/trash_can.png"),
                        _ -> delete(), Component.translatable("screen.savedcommands.share.delete"));

                similarCommand = checkSimilarity(command, commandManager.data.commands);

                if (similarCommand == null || similarCommand.getValue() < 0.50) {
                    matchResult = MatchResult.NONE;
                } else if (similarCommand.getValue() == 1) {
                    matchResult = MatchResult.EXACT;
                } else {
                    matchResult = MatchResult.SIMILAR;
                }
            }

            private boolean delete() { // Returns weather the Map is completely enmpty
                SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.get(playerName).remove(command);
                list.removeEntry(this);
                if (SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.get(playerName).isEmpty()) {
                    SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.remove(playerName);
                    if (SavedCommandsClient.sharingManager.receivedCommandsByPlayerName.isEmpty()) {
                        if (minecraft.screen instanceof ViewerSaverScreen viewerSaverScreen) {
                            if(viewerSaverScreen.parent instanceof SavedCommandsScreen savedCommandsScreen){
                                savedCommandsScreen.removeNotificationButton();
                            }
                            minecraft.setScreen(viewerSaverScreen.parent);
                            return true;
                        }
                    }
                    list.addSentCommandsAndPlayers(SavedCommandsClient.sharingManager.receivedCommandsByPlayerName);
                }
                return false;
            }

            public static Map.@Nullable Entry<SavedCommandManager.CommandData, Double> checkSimilarity(SavedCommandManager.CommandData command, List<SavedCommandManager.CommandData> allCommands) {
                double bestSimilarity = 0;
                SavedCommandManager.CommandData bestCommand = null;
                for (SavedCommandManager.CommandData existing : allCommands) {
                    if (existing.command.equalsIgnoreCase(command.command)) {
                        return Map.entry(existing, 1.0);
                    }
                    double similarity = getSimilarity(command.command, existing.command);
                    if (similarity > bestSimilarity) {
                        bestCommand = existing;
                        bestSimilarity = similarity;
                    }
                }
                if (bestCommand == null) {
                    return null;
                } else {
                    return Map.entry(bestCommand, bestSimilarity);
                }
            }

            private static double getSimilarity(String a, String b) {
                a = a.toLowerCase();
                b = b.toLowerCase();
                int maxLen = Math.max(a.length(), b.length());
                if (maxLen == 0) return 1.0;
                return 1.0 - (double) levenshtein(a, b) / maxLen;
            }

            private static int levenshtein(String a, String b) {
                int[] dp = new int[b.length() + 1];
                for (int i = 0; i <= b.length(); i++) dp[i] = i;
                for (int i = 1; i <= a.length(); i++) {
                    int prev = dp[0];
                    dp[0] = i;
                    for (int j = 1; j <= b.length(); j++) {
                        int temp = dp[j];
                        dp[j] = a.charAt(i - 1) == b.charAt(j - 1)
                                ? prev
                                : 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
                        prev = temp;
                    }
                }
                return dp[b.length()];
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentX() + getContentWidth(), getContentY() + getContentHeight(), 0x10FFFFFF);

                int textX = getContentX() + 4;
                if (command.name == null) {
                    int textY = getContentY() + (getContentHeight() - minecraft.font.lineHeight) / 2 + 1;

                    graphics.text(minecraft.font, Component.literal(shortenTextIfNeeded(command.command, getWidth() - 90, ChatFormatting.RESET)), textX, textY, 0xFFFFFFFF, true);
                } else {
                    int fontHeight = minecraft.font.lineHeight;
                    int textY = getContentY() + (getContentHeight() - minecraft.font.lineHeight * 2 - 2) / 2 + 1;

                    graphics.text(minecraft.font, Component.literal(shortenTextIfNeeded(command.name, getWidth() - 90, ChatFormatting.RESET)), textX, textY, 0xFFFFFFFF, true);

                    textY += fontHeight + 2;

                    graphics.text(minecraft.font, Component.literal(shortenTextIfNeeded(command.command, getWidth() - 90, ChatFormatting.RESET)), textX, textY, 0xFFBBBBBB, true);
                }

                graphics.blit(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath(MOD_ID,
                        switch (matchResult) {
                            case NONE -> "textures/gui/non-existent.png";
                            case SIMILAR -> "textures/gui/maybe-existent.png";
                            case EXACT -> "textures/gui/exactly-existent.png";
                        }), getContentX() + getContentWidth() - 30 * 4, getContentY() + (getContentHeight() - 20) / 2, 0, 0, 20, 20, 20, 20);

                saveButton.setPosition(getContentX() + getContentWidth() - 30 * 3, getContentY() + (getContentHeight() - 20) / 2);
                saveButton.extractRenderState(graphics, mouseX, mouseY, a);

                editButton.setPosition(getContentX() + getContentWidth() - 30 * 2, getContentY() + (getContentHeight() - 20) / 2);
                editButton.extractRenderState(graphics, mouseX, mouseY, a);

                deleteButton.setPosition(getContentX() + getContentWidth() - 30, getContentY() + (getContentHeight() - 20) / 2);
                deleteButton.extractRenderState(graphics, mouseX, mouseY, a);

                if (mouseX >= getContentX() + getContentWidth() - 30 * 4 && mouseX <= getContentX() + getContentWidth() - 30 * 4 + 20 &&
                        mouseY >= getContentY() + (getContentHeight() - 20) / 2 && mouseY <= getContentY() + (getContentHeight() - 20) / 2 + 20) { // Existing graphic hovered
                    graphics.setTooltipForNextFrame(
                            switch (matchResult) {
                                case NONE ->
                                        List.of(Component.translatable("screen.savedcommands.share.existingstatus.none").getVisualOrderText());
                                case SIMILAR -> List.of(
                                        Component.translatable("screen.savedcommands.share.existingstatus.maybe.line1", String.valueOf(Math.round(similarCommand.getValue() * 100.0))).getVisualOrderText(),
                                        Component.translatable("screen.savedcommands.share.existingstatus.maybe.line2", similarCommand.getKey().name != null ? similarCommand.getKey().name : similarCommand.getKey().command).getVisualOrderText());
                                case EXACT ->
                                        List.of(Component.translatable("screen.savedcommands.share.existingstatus.exact").getVisualOrderText());
                            }, mouseX, mouseY);
                }
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                if (command.name != null) {
                    return List.of(NarratableEntryOfString(command.command), NarratableEntryOfString(command.name));
                } else {
                    return List.of(NarratableEntryOfString(command.command));
                }
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(saveButton, editButton, deleteButton);
            }
        }
    }
}
