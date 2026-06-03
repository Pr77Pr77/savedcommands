package de.aliba2468pr77pr77.savedcommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.VariablePlaceholder;

public class SavedCommandManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path filePath;
    public SavedCommandsData data;

    public SavedCommandManager(String World) {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path myDir = configDir.resolve(MOD_ID);
        this.filePath = myDir.resolve(World + ".json");
        load();
    }

    public SavedCommandManager() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path myDir = configDir.resolve(MOD_ID);
        this.filePath = myDir.resolve(getWorldOrServerId() + ".json");
        load();
    }

    public static String getWorldOrServerId() {
        Minecraft minecraft = Minecraft.getInstance();

        // Integrated server
        IntegratedServer integratedServer = minecraft.getSingleplayerServer();
        if (integratedServer != null) {
            return "singleplayer/" + integratedServer.getWorldPath(LevelResource.ROOT).getParent().getFileName().toString();
        }

        // External multiplayer
        ServerData server = minecraft.getCurrentServer();
        if (server != null) {
            return "multiplayer/" + server.ip;
        }

        // No world open
        return "none";
    }

    public static class CommandData {
        public String command;
        public String name;
        public keybindCombination keybinds;
        public List<variable> variables;

        CommandData(String command, String name) {
            this.command = command;
            this.name = name;
        }

        public static class keybindCombination {
            public List<String> keybindType = new ArrayList<>();
            public List<Integer> keybindCode = new ArrayList<>();

            public List<InputConstants.Key> toKeys() {
                List<InputConstants.Key> keybinds = new ArrayList<>();
                for (int i = 0; i < keybindCode.size(); i++) {
                    keybinds.add(InputConstants.Type.valueOf(keybindType.get(i)).getOrCreate(keybindCode.get(i)));
                }
                return keybinds;
            }

            public boolean isEmptyOrNull() {
                return this.keybindCode == null || this.keybindType == null || this.keybindCode.isEmpty() || this.keybindType.isEmpty();
            }
        }

        public static class variable {
            public String name;
            public Character abbreviation;

            public types type;
            public String defaultValue;

            public enum types {
                STRING("screen.savedcommands.vartype.string", true),
                INT("screen.savedcommands.vartype.int", true),
                FLOAT("screen.savedcommands.vartype.float", true),

                PLAYERPOSX("argument.entity.options.x.description", false, "screen.savedcommands.vartype.abbreviation.x"),
                PLAYERPOSY("argument.entity.options.y.description", false, "screen.savedcommands.vartype.abbreviation.y"),
                PLAYERPOSZ("argument.entity.options.z.description", false, "screen.savedcommands.vartype.abbreviation.z"),
                ITEMHAND("screen.savedcommands.vartype.itemhand", false, "screen.savedcommands.vartype.abbreviation.itemhand");

                private final String translationKey;
                final boolean userEditable;
                private final String abbreviationTranslationKey;

                types(String translationKey, boolean userEditable, String abbreviationTranslationKey) {
                    this.translationKey = translationKey;
                    this.userEditable = userEditable;
                    this.abbreviationTranslationKey = abbreviationTranslationKey;
                }

                types(String translationKey, boolean userEditable) {
                    this.translationKey = translationKey;
                    this.userEditable = userEditable;
                    this.abbreviationTranslationKey = null;
                }

                public String getTranslationKey() {
                    return translationKey;
                }

                public String getAbbreviationTranslationKey() {
                    return abbreviationTranslationKey;
                }

                public Component getText() {
                    return Component.translatable(translationKey);
                }

                public static Optional<types> byTranslation(String text) {
                    return Arrays.stream(values())
                            .filter(t -> text.equals(Component.translatable(t.translationKey).getString()))
                            .findFirst();
                }

                public static Optional<types> byAbbreviationTranslation(String text) {
                    return Arrays.stream(values())
                            .filter(t -> t.abbreviationTranslationKey != null && text.equals(Component.translatable(t.abbreviationTranslationKey).getString()))
                            .findFirst();
                }
            }

        }
    }

    public static class SavedCommandsData {
        public List<CommandData> commands = new ArrayList<>();

        SettingsManager.Settings worldSettings;
    }

    public synchronized CommandData addCommand(String command, String name) {
        if (command == null || command.isEmpty()) {
            return null;
        }
        CommandData newCommand = new CommandData(command, name);
        data.commands.add(newCommand);
        saveAsync();
        return newCommand;
    }

    public synchronized void removeCommand(int index) {
        if (index >= 0 && index < data.commands.size()) {
            data.commands.remove(index);
            saveAsync();
        }
    }

    public static void sendCommandAndInsertVariables(SavedCommandManager.CommandData command, Screen parentScreen) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        if (command.variables == null || command.variables.isEmpty() || !command.command.contains(String.valueOf(VariablePlaceholder))) {
            sendCommand(command.command);
        } else { // There are variables
            StringBuilder insertedCommand = new StringBuilder();
            int index = command.command.indexOf(VariablePlaceholder);
            int ContinuingIndex = 0;
            List<CommandData.variable> userEditableVariablesLeft = new ArrayList<>();
            while (index != -1) {
                int finalIndex = index;
                Optional<CommandData.variable> optionalVariable = command.variables.stream().filter(v -> command.command.length() > finalIndex + 1 && v.abbreviation == command.command.charAt(finalIndex + 1)).findFirst();
                if (optionalVariable.isPresent()) {
                    if (!optionalVariable.get().type.userEditable) {
                        insertedCommand.append(command.command.substring(ContinuingIndex, index));
                        LocalPlayer player = Minecraft.getInstance().player;
                        switch (optionalVariable.get().type) {
                            case ITEMHAND:
                                insertedCommand.append(player.getMainHandItem().getItem());
                                break;
                            case PLAYERPOSX:
                                insertedCommand.append(player.getBlockX());
                                break;
                            case PLAYERPOSY:
                                insertedCommand.append(player.getBlockY());
                                break;
                            case PLAYERPOSZ:
                                insertedCommand.append(player.getBlockZ());
                                break;
                        }
                        ContinuingIndex = index + 2;
                    } else if (!userEditableVariablesLeft.contains(optionalVariable.get())) {
                        userEditableVariablesLeft.add(optionalVariable.get());
                    }
                }

                index = command.command.indexOf(VariablePlaceholder, index + 1);
            }
            insertedCommand.append(command.command.substring(ContinuingIndex));

            if (!userEditableVariablesLeft.isEmpty()) {
                Minecraft.getInstance().setScreen(new InputVariableScreen(insertedCommand.toString(), userEditableVariablesLeft, parentScreen));
            } else {
                sendCommand(insertedCommand.toString());
            }
        }
    }

    public static void sendCommand(String command) {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        if (command.charAt(0) == '/') {
            player.connection.sendCommand(command.substring(1));
        } else {
            player.connection.sendChat(command);
        }
    }

    private void load() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                this.data = GSON.fromJson(json, SavedCommandsData.class);
                if (this.data == null) this.data = new SavedCommandsData();
            } else {
                this.data = new SavedCommandsData();
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.data = new SavedCommandsData();
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(filePath.getParent());
            String json = GSON.toJson(this.data);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void saveAsync() {
        SavedCommandsData snapshot;
        synchronized (this) {
            snapshot = new SavedCommandsData();
            snapshot.commands = new ArrayList<>(this.data.commands);
            snapshot.worldSettings = new SettingsManager.Settings(data.worldSettings);
        }
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(filePath.getParent());
                String json = GSON.toJson(snapshot);
                Files.writeString(filePath, json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
