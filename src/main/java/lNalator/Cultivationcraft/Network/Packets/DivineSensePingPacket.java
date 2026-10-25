package lNalator.Cultivationcraft.Network.Packets;

import lNalator.Cultivationcraft.Client.GUI.DivineSensePingOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Vigilance: sent to a player sensed by someone's Divine Sense, with where they are sensed from
public record DivineSensePingPacket(int observerId, Vec3 position) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(observerId);
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    public static DivineSensePingPacket decode(FriendlyByteBuf buffer) {
        return new DivineSensePingPacket(buffer.readInt(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DivineSensePingOverlay.add(observerId, position)));
        context.setPacketHandled(true);
    }
}
