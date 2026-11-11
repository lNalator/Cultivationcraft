package lNalator.Cultivationcraft.Network.Packets;

import lNalator.Cultivationcraft.Client.GUI.WindWrapOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Levitation + Wind synergy: how many entities the player carries in wind, for the counter near the Qi orb
public record WindWrapPacket(int count, int max) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(count);
        buffer.writeVarInt(max);
    }

    public static WindWrapPacket decode(FriendlyByteBuf buffer) {
        return new WindWrapPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WindWrapOverlay.set(count, max)));
        context.setPacketHandled(true);
    }
}
