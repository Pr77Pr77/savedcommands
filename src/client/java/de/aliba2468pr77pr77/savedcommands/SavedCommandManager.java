package de.aliba2468pr77pr77.savedcommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.integrated.IntegratedServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;

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
        MinecraftClient client = MinecraftClient.getInstance();

        // Integrated server
        IntegratedServer integrated = client.getServer();
        if (integrated != null) {
            try {
                Object saveProps = integrated.getSaveProperties();
                if (saveProps != null) {
                    java.lang.reflect.Method m = saveProps.getClass().getMethod("getLevelName");
                    Object levelName = m.invoke(saveProps);
                    if (levelName != null) return "singleplayer/" + levelName;
                }
            } catch (NoSuchMethodException e) {
                return "singleplayer/unknown";
            } catch (Throwable t) {
                t.printStackTrace();
                return "singleplayer/unknown";
            }
        }

        // External multiplayer
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null) {
            return "multiplayer/" + server.address;
        }

        // No world open
        return "none";
    }

    public static class CommandData {
        public String command;
        public String name;
        public keybindCombination keybinds;

        CommandData(String command, String name) {
            this.command = command;
            this.name = name;
        }

        public static class keybindCombination {
            public List<String> keybindType = new ArrayList<>();
            public List<Integer> keybindCode = new ArrayList<>();

            public List<InputUtil.Key> toKeys() {
                List<InputUtil.Key> keybinds = new ArrayList<>();
                for (int i = 0; i < keybindCode.size(); i++) {
                    keybinds.add(InputUtil.Type.valueOf(keybindType.get(i)).createFromCode(keybindCode.get(i)));
                }
                return keybinds;
            }

            public boolean isEmptyOrNull(){
                return this.keybindCode == null || this.keybindType == null || this.keybindCode.isEmpty() || this.keybindType.isEmpty();
            }
        }
    }

    public static class SavedCommandsData {
        public List<CommandData> commands = new ArrayList<>();
    }

    public synchronized void addCommand(String command, String name) {
        if (command == null || command.isEmpty()) {
            return;
        }
        data.commands.add(new CommandData(command, name));
        saveAsync();
    }

    public synchronized void removeCommand(int index) {
        if (index >= 0 && index < data.commands.size()) {
            data.commands.remove(index);
            saveAsync();
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
