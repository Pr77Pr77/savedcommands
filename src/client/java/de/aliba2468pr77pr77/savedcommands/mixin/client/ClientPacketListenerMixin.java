package de.aliba2468pr77pr77.savedcommands.mixin.client;

import de.aliba2468pr77pr77.savedcommands.SavedCommandsScreen;
import de.aliba2468pr77pr77.savedcommands.share.SharePlayerSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
    private void onPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        if (!(packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER))) {
            return;
        }

        if (Minecraft.getInstance().gui.screen() instanceof SavedCommandsScreen savedCommandsScreen) {
            savedCommandsScreen.setOtherPlayersOnServer(Minecraft.getInstance().getConnection() != null
                    && Minecraft.getInstance().getConnection().getOnlinePlayers().size() > 1);
        } else if (Minecraft.getInstance().gui.screen() instanceof SharePlayerSelectionScreen sharePlayerSelectionScreen) {
            sharePlayerSelectionScreen.reloadPlayers();
        }
    }

    @Inject(method = "handlePlayerInfoRemove", at = @At("TAIL"))
    private void onPlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() instanceof SavedCommandsScreen savedCommandsScreen) {
            savedCommandsScreen.setOtherPlayersOnServer(Minecraft.getInstance().getConnection() != null
                    && Minecraft.getInstance().getConnection().getOnlinePlayers().size() > 1);
        } else if (Minecraft.getInstance().gui.screen() instanceof SharePlayerSelectionScreen sharePlayerSelectionScreen) {
            sharePlayerSelectionScreen.reloadPlayers();
        }
    }
}