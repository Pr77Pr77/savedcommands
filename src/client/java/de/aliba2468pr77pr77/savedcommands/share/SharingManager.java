package de.aliba2468pr77pr77.savedcommands.share;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import de.aliba2468pr77pr77.savedcommands.IconButton;
import de.aliba2468pr77pr77.savedcommands.SavedCommandManager;
import de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;
import static de.aliba2468pr77pr77.savedcommands.SavedCommands.MOD_ID;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.OpenCommandScreen;

public class SharingManager {
    public static final String INITIAL_MESSAGE_TEXT = "%s tried to share commands with you, but you don't have the required mod. " +
            "Install '%s' (Fabric) to receive them properly. Commands: %s.";
    public static final String SHARE_MAGIC_CODE = "Sаvеd Cоmmаnds";
    public static final String SHARE_CODE_SEND = "SEND"; // Sent by recipient
    public static final String SHARE_CODE_DATA_UNFINISHED = "DATA_UNFINISHED"; // Sent by sender + Unfinished data
    public static final String SHARE_CODE_DATA_FINISHED = "DATA_FINISHED"; // Sent by sender + Finished data
    public static final String SHARE_CODE_DONE = "DONE"; // Sent by recipient
    public static final String SHARE_CODE_ERROR = "ERROR"; // Sent by recipient

    private static final Gson GSON = new GsonBuilder().create();

    // recipient:
    private final Map<String, StringBuilder> receivedDataByPlayerName = new HashMap<>(); // only unfinished data
    public Map<String, List<SavedCommandManager.CommandData>> receivedCommandsByPlayerName = new HashMap<>();

    public enum States {
        WAITING_FOR_SENDING("screen.savedcommands.share.status.waiting"),
        WAITING_FOR_RESPONSE("screen.savedcommands.share.status.waiting"),
        RECIPIENT_NOT_AVAILABLE("screen.savedcommands.share.status.notavailable"),
        SENDING_DATA("screen.savedcommands.share.status.sending"),
        RECIPIENT_ERROR("screen.savedcommands.share.status.recipienterror"),
        SENT("screen.savedcommands.share.status.sent");

        private final String translationKey;

        States(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getMessage() {
            return Component.translatable(translationKey);
        }
    }

    // sender:
    public Map<PlayerInfo, States> recipients = new HashMap<>();
    public List<SavedCommandManager.CommandData> commands;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ScheduledFuture<?> currentTimeout;

    private static String truncateCommandList(List<SavedCommandManager.CommandData> commands, int maxLength) {
        StringBuilder sb = new StringBuilder();
        for (SavedCommandManager.CommandData command : commands) {
            String entry = "'" + command.command + "', ";
            if (sb.length() + entry.length() > maxLength - 3) {
                sb.append("...");
                break;
            }
            sb.append(entry);
        }
        if (!sb.toString().endsWith("...") && sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }

    protected void sendInitialMessage() {
        assert Minecraft.getInstance().player != null;
        for (Map.Entry<PlayerInfo, States> player : recipients.entrySet()) {
            int baseLength = "msg ".length() + player.getKey().getProfile().name().length() + 1 + INITIAL_MESSAGE_TEXT.formatted(Minecraft.getInstance().player.getName().getString(), SHARE_MAGIC_CODE, "").length();
            String commandsString = truncateCommandList(commands, 255 - baseLength);

            Objects.requireNonNull(Minecraft.getInstance().getConnection()).sendCommand("msg " + player.getKey().getProfile().name() + " " +
                    INITIAL_MESSAGE_TEXT.formatted(Minecraft.getInstance().player.getName().getString(), SHARE_MAGIC_CODE, commandsString));
            player.setValue(States.WAITING_FOR_RESPONSE);
        }
        if (currentTimeout != null && !currentTimeout.isDone()) {
            currentTimeout.cancel(false);
        }
        currentTimeout = scheduler.schedule(() -> Minecraft.getInstance().execute(() -> {
            for (Map.Entry<PlayerInfo, States> player : recipients.entrySet()) {
                if (player.getValue() == States.WAITING_FOR_RESPONSE) {
                    player.setValue(States.RECIPIENT_NOT_AVAILABLE);
                }
                if (recipients.values().stream()
                        .noneMatch(entry -> entry == States.WAITING_FOR_RESPONSE) &&
                        Minecraft.getInstance().screen instanceof ShareStatusScreen shareStatusScreen) {
                    shareStatusScreen.closeButton.setMessage(Component.translatable("gui.done"));
                }
            }
        }), 15, TimeUnit.SECONDS);
    }

    public boolean shareHandler(String messageString) {
        if (!messageString.contains(SHARE_MAGIC_CODE)) {
            return true;
        }
        if (messageString.contains("tried to share commands with you")) {
            return shareHandler(messageString, messageString.substring(0, messageString.indexOf("tried to share commands with you")));
        }
        String code;
        if (messageString.contains(SHARE_CODE_DATA_UNFINISHED + " ")) {
            code = SHARE_CODE_DATA_UNFINISHED;
        } else if (messageString.contains(SHARE_CODE_DATA_FINISHED + " ")) {
            code = SHARE_CODE_DATA_FINISHED;
        } else if (messageString.contains(SHARE_CODE_SEND + " ")) {
            code = SHARE_CODE_SEND;
        } else if (messageString.contains(SHARE_CODE_DONE + " ")) {
            code = SHARE_CODE_DONE;
        } else if (messageString.contains(SHARE_CODE_ERROR + " ")) {
            code = SHARE_CODE_ERROR;
        } else {
            SystemToast receivedToast = SystemToast.multiline(Minecraft.getInstance(),
                    new SystemToast.SystemToastId(7500L),
                    Component.translatable("screen.savedcommands.share.sendernotrecognizable.title"),
                    Component.translatable("screen.savedcommands.share.sendernotrecognizable.message"));
            Minecraft.getInstance().getToastManager().addToast(receivedToast);
            return false;
        }

        int start = messageString.indexOf(code) + code.length() + 1;
        int end = messageString.indexOf(" ", start);
        return shareHandler(messageString, messageString.substring(start, end == -1 ? messageString.length() : end));
    }

    public boolean shareHandler(String messageString, String senderName) {
        assert Minecraft.getInstance().player != null;
        if (!messageString.contains(SHARE_MAGIC_CODE)) {
            return true;
        }
        if (Objects.equals(senderName, Minecraft.getInstance().player.getName().getString())) {
            return false;
        }
        if (messageString.contains(SHARE_CODE_SEND)) { // sender
            PlayerInfo found = recipients.keySet().stream()
                    .filter(p -> p.getProfile().name().equals(senderName))
                    .findFirst()
                    .orElse(null);
            if (found == null) {
                return false;
            }

            recipients.put(found, States.SENDING_DATA);
            if (recipients.values().stream()
                    .noneMatch(entry -> entry == States.WAITING_FOR_RESPONSE)) {
                if (currentTimeout != null && !currentTimeout.isDone()) {
                    currentTimeout.cancel(false);
                }
                if (Minecraft.getInstance().screen instanceof ShareStatusScreen shareStatusScreen) {
                    shareStatusScreen.closeButton.setMessage(Component.translatable("gui.done"));
                }
            }

            Minecraft.getInstance().execute(() -> {
                String JSONdataLeft = GSON.toJson(commands);
                List<String> stringsToSend = new ArrayList<>();
                assert Minecraft.getInstance().player != null;
                String unfinishedHeader = "msg " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_DATA_UNFINISHED + " " + Minecraft.getInstance().player.getName().getString() + " ";
                String finishedHeader = "msg " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_DATA_FINISHED + " " + Minecraft.getInstance().player.getName().getString() + " ";
                while (JSONdataLeft.length() + finishedHeader.length() > 255) {
                    stringsToSend.add(unfinishedHeader + JSONdataLeft.substring(0, 255 - unfinishedHeader.length()));
                    JSONdataLeft = JSONdataLeft.substring(255 - unfinishedHeader.length());
                }
                stringsToSend.add(finishedHeader + JSONdataLeft);
                for (String string : stringsToSend) {
                    Objects.requireNonNull(Minecraft.getInstance().getConnection())
                            .sendCommand(string);
                }
            });
        } else if (messageString.contains(SHARE_CODE_DATA_UNFINISHED)) { // recipient
            int dataStart = messageString.indexOf(" ", messageString.indexOf(SHARE_CODE_DATA_UNFINISHED) + SHARE_CODE_DATA_UNFINISHED.length() + 1) + 1;
            if (receivedDataByPlayerName.containsKey(senderName)) {
                receivedDataByPlayerName.get(senderName).append(messageString.substring(dataStart));
            } else {
                receivedDataByPlayerName.put(senderName, new StringBuilder(messageString.substring(dataStart)));
            }
        } else if (messageString.contains(SHARE_CODE_DATA_FINISHED)) { // recipient
            StringBuilder data;
            if (receivedDataByPlayerName.containsKey(senderName)) {
                data = receivedDataByPlayerName.get(senderName);
                receivedDataByPlayerName.remove(senderName);
            } else {
                data = new StringBuilder();
            }
            int dataStart = messageString.indexOf(" ", messageString.indexOf(SHARE_CODE_DATA_FINISHED) + SHARE_CODE_DATA_FINISHED.length() + 1) + 1;
            data.append(messageString.substring(dataStart));

            List<SavedCommandManager.CommandData> commandData;
            try {
                commandData = GSON.fromJson(data.toString(), new TypeToken<List<SavedCommandManager.CommandData>>() {
                }.getType());
            } catch (JsonSyntaxException e) {
                Minecraft.getInstance().execute(() -> {
                            assert Minecraft.getInstance().player != null;
                            LOGGER.error("A JSON error occurred while parsing shared commands by " + senderName + ": " + e.getMessage());
                            Objects.requireNonNull(Minecraft.getInstance().getConnection())
                                    .sendCommand("msg " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_ERROR + " " + Minecraft.getInstance().player.getName().getString());
                        }
                );
                return false;
            }

            if (receivedCommandsByPlayerName.containsKey(senderName)) {
                receivedCommandsByPlayerName.get(senderName).addAll(commandData);
            } else {
                receivedCommandsByPlayerName.put(senderName, commandData);
            }

            SystemToast receivedToast = SystemToast.multiline(Minecraft.getInstance(),
                    new SystemToast.SystemToastId(7500L),
                    Component.translatable("screen.savedcommands.share.recievednotification.title"),
                    Component.translatable("screen.savedcommands.share.recievednotification.message",
                            senderName, OpenCommandScreen.getTranslatedKeyMessage()));
            Minecraft.getInstance().getToastManager().addToast(receivedToast);

            if (Minecraft.getInstance().screen instanceof SavedCommandsScreen savedCommandsScreen) {
                savedCommandsScreen.notificationButton = new IconButton(20, 20, 20, 20, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/notification.png"),
                        button -> Minecraft.getInstance().setScreen(new ViewerSaverScreen(savedCommandsScreen)), Component.translatable("screen.savedcommands.share.notificationbutton"));
                savedCommandsScreen.addRenderableWidget(savedCommandsScreen.notificationButton);

                savedCommandsScreen.SearchBar.setPosition(20 + 20 + 5, 20);
                savedCommandsScreen.SearchBar.setSize(savedCommandsScreen.width - 40 - 22 - 20 - 5, 20);
            }

            Minecraft.getInstance().execute(() -> {
                        assert Minecraft.getInstance().player != null;
                        Objects.requireNonNull(Minecraft.getInstance().getConnection())
                                .sendCommand("msg " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_DONE + " " + Minecraft.getInstance().player.getName().getString());
                    }
            );
        } else if (messageString.contains(SHARE_CODE_DONE)) { // sender
            PlayerInfo found = recipients.keySet().stream()
                    .filter(p -> p.getProfile().name().equals(senderName))
                    .findFirst()
                    .orElse(null);
            if (found == null) {
                return false;
            }
            recipients.put(found, States.SENT);
        } else if (messageString.contains(SHARE_CODE_ERROR)) { // sender
            PlayerInfo found = recipients.keySet().stream()
                    .filter(p -> p.getProfile().name().equals(senderName))
                    .findFirst()
                    .orElse(null);
            if (found == null) {
                return false;
            }
            recipients.put(found, States.RECIPIENT_ERROR);
        } else { // recipient
            Minecraft.getInstance().execute(() -> {
                        assert Minecraft.getInstance().player != null;
                        Objects.requireNonNull(Minecraft.getInstance().getConnection())
                                .sendCommand("msg " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_SEND + " " + Minecraft.getInstance().player.getName().getString());
                    }
            );
        }
        return false;
    }
}
