package lNalator.Cultivationcraft.Network.Packets;

import lNalator.Cultivationcraft.Client.GUI.CultivationToast;
import lNalator.Cultivationcraft.Common.CultivationNotifier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record NotificationPacket(CultivationNotifier.Type type, Component title, Component text) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(type);
        buffer.writeComponent(title);
        buffer.writeComponent(text);
    }

    public static NotificationPacket decode(FriendlyByteBuf buffer) {
        return new NotificationPacket(buffer.readEnum(CultivationNotifier.Type.class), buffer.readComponent(), buffer.readComponent());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CultivationToast.show(type, title, text)));
        context.setPacketHandled(true);
    }
}
