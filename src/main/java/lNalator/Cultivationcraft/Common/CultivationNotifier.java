package lNalator.Cultivationcraft.Common;

import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.Cultivationcraft.Network.Packets.NotificationPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

// Reusable toast notifications (design C4): quest completed, concept learnt, technique mastered, alerts...
public final class CultivationNotifier {

    public enum Type {
        SUCCESS, PROGRESS, ALERT
    }

    public static void notify(Player player, Type type, Component title, Component text) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.channel.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new NotificationPacket(type, title, text));
        }
    }
}
