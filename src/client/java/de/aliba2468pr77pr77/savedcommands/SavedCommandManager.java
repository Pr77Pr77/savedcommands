package de.aliba2468pr77pr77.savedcommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

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
    private SavedCommandsData data;

    public SavedCommandManager(String World) {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path myDir = configDir.resolve(MOD_ID);
        this.filePath = myDir.resolve(World + ".json");
        load();
    }

    public static class CommandData{
        public String command;
        public String name;

        CommandData(String command, String name){
            this.command = command;
            this.name = name;
        }
    }

    public static class SavedCommandsData {
        public List<CommandData> commands = new ArrayList<>();
    }

    public synchronized List<CommandData> getCommands() {
        return new ArrayList<>(data.commands);
    }

    public synchronized void addCommand(String command, String name) {
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

    private void saveAsync() {
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
