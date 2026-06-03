package de.aliba2468pr77pr77.savedcommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;

public class SettingsManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();
    private static final Path filePath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("settings.json");

    public static Settings globalSettings = new Settings(); // Per word settings are managed by the SavedCommandManager

    public static class Settings {
        Boolean receiveCommands;
        String msgCommand;
        Boolean deleteWarning;
        Boolean manualCategories;

        static final String DEFAULT_MSG_COMMAND = "msg";

        Settings(boolean insertDefaults) {
            if (!insertDefaults) {
                return;
            }
            receiveCommands = true;
            msgCommand = DEFAULT_MSG_COMMAND;
            deleteWarning = true;
            manualCategories = false;
        }

        Settings(Settings settingsToCopy) {
            if (settingsToCopy == null) {
                return;
            }
            receiveCommands = settingsToCopy.receiveCommands;
            msgCommand = settingsToCopy.msgCommand;
            deleteWarning = settingsToCopy.deleteWarning;
            manualCategories = settingsToCopy.manualCategories;
        }

        public Settings() {
        }
    }

    static void load() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                globalSettings = GSON.fromJson(json, Settings.class);
                if (globalSettings == null) {
                    globalSettings = new Settings(true);
                }
            } else {
                globalSettings = new Settings(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            globalSettings = new Settings(true);
        }
    }

    static void saveAsync() {
        Settings snapshot;
        synchronized (LOCK) {
            snapshot = new Settings(globalSettings);
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
