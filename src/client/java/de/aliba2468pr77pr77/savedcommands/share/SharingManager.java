package de.aliba2468pr77pr77.savedcommands.share;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import de.aliba2468pr77pr77.savedcommands.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static de.aliba2468pr77pr77.savedcommands.SavedCommands.LOGGER;
import static de.aliba2468pr77pr77.savedcommands.SavedCommandsClient.OpenCommandScreen;

public class SharingManager {
    public static final String INITIAL_MESSAGE_TEXT = "%s tried to share commands with you, but you don't have the required mod. " +
            "Install '%s' (Fabric) to receive them properly. Commands: %s";
    public static final String SHARE_MAGIC_CODE = "Sаvеd Cоmmаnds";
    public static final String SHARE_CODE_SEND = "send"; // Sent by recipient
    public static final String SHARE_CODE_DATA_UNFINISHED = "data_unfinished"; // Sent by sender + Unfinished data
    public static final String SHARE_CODE_DATA_FINISHED = "data_finished"; // Sent by sender + Finished data
    public static final String SHARE_CODE_DONE = "done"; // Sent by recipient
    public static final String SHARE_CODE_ERROR = "error"; // Sent by recipient

    private static final Gson GSON = new GsonBuilder().create();

    // recipient:
    private static final int DATA_UNFINISHED_RESEND_TIMEOUT_SECONDS = 3;
    private final Map<String, ReceiveState> receiveStates = new HashMap<>();
    public Map<String, List<SavedCommandManager.CommandData>> receivedCommandsByPlayerName = new HashMap<>();

    private static class ReceiveState {
        final SortedMap<Integer, String> chunks = new TreeMap<>();
        int totalPackets = -1;
        boolean finishedReceived = false;
        ScheduledFuture<?> timeout;


        void addChunk(int index, int total, String payload) {
            if (totalPackets < 0) {
                totalPackets = total;
            } else if (total > totalPackets) {
                totalPackets = total;
            }
            if (!payload.isEmpty()) {
                chunks.put(index, payload);
            }
        }

        boolean isComplete() {
            return totalPackets > 0 && chunks.size() == totalPackets;
        }

        String buildDataString() {
            StringBuilder combined = new StringBuilder();
            for (String chunk : chunks.values()) {
                combined.append(chunk);
            }
            return combined.toString();
        }
    }

    private record SeqPacket(int index, int total, String payload) {
    }

    private static String makeSequence(int index, int total) {
        return index + "/" + total + " ";
    }

    private static SeqPacket parseSeqPacket(String messageString, String senderName, String code) {
        int codeIndex = messageString.indexOf(code);
        if (codeIndex < 0) {
            return null;
        }
        int afterCode = codeIndex + code.length() + 1;
        int nameEnd = messageString.indexOf(" ", afterCode);
        if (nameEnd < 0) {
            return null;
        }
        String actualName = messageString.substring(afterCode, nameEnd);
        if (!actualName.equals(senderName)) {
            return null;
        }
        int seqStart = nameEnd + 1;
        int seqEnd = messageString.indexOf(" ", seqStart);
        if (seqEnd < 0) {
            return null;
        }
        String seq = messageString.substring(seqStart, seqEnd);
        int slash = seq.indexOf('/');
        if (slash < 0) {
            return null;
        }
        int index;
        int total;
        try {
            index = Integer.parseInt(seq.substring(0, slash));
            total = Integer.parseInt(seq.substring(slash + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        String payload = messageString.substring(seqEnd + 1);
        return new SeqPacket(index, total, payload);
    }

    private static List<String> buildPacketLines(String jsonData, String unfinishedHeader, String finishedHeader) {
        int total = 1;
        while (true) {
            int maxUnfinishedPayload = 255 - (unfinishedHeader.length() + makeSequence(total, total).length());
            int maxFinishedPayload = 255 - (finishedHeader.length() + makeSequence(total, total).length());
            if (jsonData.length() <= maxFinishedPayload) {
                total = 1;
                break;
            }
            int remainder = jsonData.length() - maxFinishedPayload;
            int packetCount = 1 + ((remainder + maxUnfinishedPayload - 1) / maxUnfinishedPayload);
            if (packetCount == total) {
                break;
            }
            total = packetCount;
        }
        List<String> lines = new ArrayList<>();
        int index = 1;
        while (index < total) {
            String sequence = makeSequence(index, total);
            int chunkSize = Math.min(jsonData.length(), 255 - (unfinishedHeader.length() + sequence.length()));
            lines.add(unfinishedHeader + sequence + jsonData.substring(0, chunkSize));
            jsonData = jsonData.substring(chunkSize);
            index++;
        }
        String sequence = makeSequence(total, total);
        lines.add(finishedHeader + sequence + jsonData);
        return lines;
    }

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

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
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
            int baseLength = SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).msgCommand.length() + 1 + player.getKey().getProfile().name().length() + 1 + INITIAL_MESSAGE_TEXT.formatted(Minecraft.getInstance().player.getName().getString(), SHARE_MAGIC_CODE, "").length();
            String commandsString = truncateCommandList(commands, 255 - baseLength);

            Objects.requireNonNull(Minecraft.getInstance().getConnection()).sendCommand(SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).msgCommand + " " + player.getKey().getProfile().name() + " " +
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
                        Minecraft.getInstance().gui.screen() instanceof ShareStatusScreen shareStatusScreen) {
                    shareStatusScreen.closeButton.setMessage(CommonComponents.GUI_DONE);
                }
            }
        }), 15, TimeUnit.SECONDS);
    }

    public boolean shareHandler(String messageString) {
        if (!messageString.contains(SHARE_MAGIC_CODE)) {
            return true;
        }
        if (messageString.contains("tried to share commands with you")) {
            String before = messageString.substring(0, messageString.indexOf("tried to share commands with you") - 1);
            return shareHandler(messageString, before.substring(before.lastIndexOf(' ') + 1));
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
            SystemToast.add(Minecraft.getInstance().gui.toastManager(),
                    new SystemToast.SystemToastId(7500L),
                    Component.translatable("screen.savedcommands.share.sendernotrecognizable.title"),
                    Component.translatable("screen.savedcommands.share.sendernotrecognizable.message"));
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
                if (Minecraft.getInstance().gui.screen() instanceof ShareStatusScreen shareStatusScreen) {
                    shareStatusScreen.closeButton.setMessage(CommonComponents.GUI_DONE);
                }
            }

            Minecraft.getInstance().execute(() -> {
                // Getting JSON without categoryId
                Map<SavedCommandManager.CommandData, String> backup = new HashMap<>();
                commands.forEach(c -> {
                    backup.put(c, c.categoryId);
                    c.categoryId = null;
                });
                String JSONdataLeft = GSON.toJson(commands);
                JSONdataLeft = CaseEncoder.encode(JSONdataLeft);
                commands.forEach(c -> c.categoryId = backup.get(c));

                assert Minecraft.getInstance().player != null;
                String unfinishedHeader = SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).msgCommand + " " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_DATA_UNFINISHED + " " + Minecraft.getInstance().player.getName().getString() + " ";
                String finishedHeader = SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).msgCommand + " " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_DATA_FINISHED + " " + Minecraft.getInstance().player.getName().getString() + " ";
                List<String> packetLines = buildPacketLines(JSONdataLeft, unfinishedHeader, finishedHeader);

                packetLines.forEach(line -> LOGGER.info("Sent share command: " + line));
                for (String string : packetLines) {
                    Objects.requireNonNull(Minecraft.getInstance().getConnection())
                            .sendCommand(string);
                }
            });
        } else if (messageString.contains(SHARE_CODE_DATA_UNFINISHED)) { // recipient
            if (!SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).receiveCommands) {
                SystemToast.add(Minecraft.getInstance().gui.toastManager(),
                        new SystemToast.SystemToastId(7500L),
                        Component.translatable("screen.savedcommands.share.disabled.title"),
                        Component.translatable("screen.savedcommands.share.disabled.message"));
                return false;
            }
            SeqPacket seqPacket = parseSeqPacket(messageString, senderName, SHARE_CODE_DATA_UNFINISHED);
            if (seqPacket == null) {
                return false;
            }
            ReceiveState state = receiveStates.computeIfAbsent(senderName, _ -> new ReceiveState());
            state.addChunk(seqPacket.index, seqPacket.total, seqPacket.payload);
            state.finishedReceived = false;
            if (state.timeout != null && !state.timeout.isDone()) {
                state.timeout.cancel(false);
            }
            state.timeout = scheduler.schedule(() -> Minecraft.getInstance().execute(() -> {
                ReceiveState timeoutState = receiveStates.get(senderName);
                if (timeoutState == null || timeoutState.finishedReceived || timeoutState.isComplete()) {
                    return;
                }
                Minecraft.getInstance().execute(() -> {
                    assert Minecraft.getInstance().player != null;
                    Objects.requireNonNull(Minecraft.getInstance().getConnection())
                            .sendCommand(SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).msgCommand + " " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_ERROR + " " + Minecraft.getInstance().player.getName().getString());
                });
                receiveStates.remove(senderName);
            }), DATA_UNFINISHED_RESEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            LOGGER.info(senderName + " sent unfinished chunk " + seqPacket.index + "/" + seqPacket.total);
        } else if (messageString.contains(SHARE_CODE_DATA_FINISHED)) { // recipient
            if (!SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).receiveCommands) {
                SystemToast.add(Minecraft.getInstance().gui.toastManager(),
                        new SystemToast.SystemToastId(7500L),
                        Component.translatable("screen.savedcommands.share.disabled.title"),
                        Component.translatable("screen.savedcommands.share.disabled.message"));
                return false;
            }
            SeqPacket seqPacket = parseSeqPacket(messageString, senderName, SHARE_CODE_DATA_FINISHED);
            if (seqPacket == null) {
                return false;
            }
            ReceiveState state = receiveStates.computeIfAbsent(senderName, _ -> new ReceiveState());
            state.addChunk(seqPacket.index, seqPacket.total, seqPacket.payload);
            state.finishedReceived = true;
            if (state.timeout != null && !state.timeout.isDone()) {
                state.timeout.cancel(false);
            }
            if (!state.isComplete()) {
                // If not all packets are present when the final packet arrives, send an error and drop state
                Minecraft.getInstance().execute(() -> {
                    assert Minecraft.getInstance().player != null;
                    Objects.requireNonNull(Minecraft.getInstance().getConnection())
                            .sendCommand(SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).msgCommand + " " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_ERROR + " " + Minecraft.getInstance().player.getName().getString());
                });
                receiveStates.remove(senderName);
                return false;
            }

            String dataString = CaseEncoder.decode(state.buildDataString());
            receiveStates.remove(senderName);

            LOGGER.info(senderName + " sent last: " + seqPacket.payload);

            List<SavedCommandManager.CommandData> commandData;
            try {
                commandData = GSON.fromJson(dataString, new TypeToken<List<SavedCommandManager.CommandData>>() {
                }.getType());
            } catch (JsonSyntaxException e) {
                Minecraft.getInstance().execute(() -> {
                            assert Minecraft.getInstance().player != null;
                            LOGGER.error("A JSON error occurred while parsing shared commands by " + senderName + ": " + e.getMessage());
                            Objects.requireNonNull(Minecraft.getInstance().getConnection())
                                    .sendCommand(SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).msgCommand + " " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_ERROR + " " + Minecraft.getInstance().player.getName().getString());
                        }
                );
                return false;
            }

            if (receivedCommandsByPlayerName.containsKey(senderName)) {
                receivedCommandsByPlayerName.get(senderName).addAll(commandData);
            } else {
                receivedCommandsByPlayerName.put(senderName, commandData);
            }

            SystemToast.add(Minecraft.getInstance().gui.toastManager(),
                    new SystemToast.SystemToastId(7500L),
                    Component.translatable("screen.savedcommands.share.recievednotification.title"),
                    Component.translatable("screen.savedcommands.share.recievednotification.message",
                            senderName, OpenCommandScreen.getTranslatedKeyMessage()));

            if (Minecraft.getInstance().gui.screen() instanceof SavedCommandsScreen savedCommandsScreen) {
                savedCommandsScreen.addNotificationButton();
            }

            Minecraft.getInstance().execute(() -> {
                        assert Minecraft.getInstance().player != null;
                        Objects.requireNonNull(Minecraft.getInstance().getConnection())
                                .sendCommand(SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).msgCommand + " " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_DONE + " " + Minecraft.getInstance().player.getName().getString());
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
            if (!SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).receiveCommands) {
                SystemToast.add(Minecraft.getInstance().gui.toastManager(),
                        new SystemToast.SystemToastId(7500L),
                        Component.translatable("screen.savedcommands.share.disabled.title"),
                        Component.translatable("screen.savedcommands.share.disabled.message"));
                return false;
            }
            Minecraft.getInstance().execute(() -> {
                        assert Minecraft.getInstance().player != null;
                        Objects.requireNonNull(Minecraft.getInstance().getConnection())
                                .sendCommand(SettingsManager.getCombinedWorldAndGlobal(SavedCommandsClient.commandManager).msgCommand + " " + senderName + " " + SHARE_MAGIC_CODE + " " + SHARE_CODE_SEND + " " + Minecraft.getInstance().player.getName().getString());
                    }
            );
        }
        return false;
    }

    public static class CaseEncoder { // Make everything lowercase to prevent excessive caps filters flagging the messages
        private static final char MARK = '~';

        public static String encode(String input) {
            StringBuilder out = new StringBuilder();
            boolean inUpperRun = false;
            for (char c : input.toCharArray()) {
                if (c == MARK) {
                    out.append(MARK).append(MARK); // Literal MARK, like "~~" encoded
                    continue;
                }
                boolean isUpper = Character.isUpperCase(c);
                if (isUpper != inUpperRun) {
                    out.append(MARK);
                    inUpperRun = isUpper;
                }
                out.append(isUpper ? Character.toLowerCase(c) : c);
            }
            if (inUpperRun) out.append(MARK);
            return out.toString();
        }

        public static String decode(String input) {
            StringBuilder out = new StringBuilder();
            boolean inUpperRun = false;
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (c == MARK) {
                    if (i + 1 < input.length() && input.charAt(i + 1) == MARK) {
                        out.append(MARK); // Literal MARK, like "~~" encoded
                        i++;
                    } else {
                        inUpperRun = !inUpperRun;
                    }
                    continue;
                }
                out.append(inUpperRun ? Character.toUpperCase(c) : c);
            }
            return out.toString();
        }
    }
}
