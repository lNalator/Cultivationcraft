package lNalator.Cultivationcraft.Network.Packets;

import lNalator.Cultivationcraft.Client.Renderers.TribulationRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Sent regularly while a player is tribulating, to every player around: the storm dissipates once they stop coming
public record TribulationPacket(int entityId, Vec3 center, int tick, int strikes) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeDouble(center.x);
        buffer.writeDouble(center.y);
        buffer.writeDouble(center.z);
        buffer.writeInt(tick);
        buffer.writeInt(strikes);
    }

    public static TribulationPacket decode(FriendlyByteBuf buffer) {
        return new TribulationPacket(buffer.readInt(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readInt(), buffer.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TribulationRenderer.update(entityId, center, tick, strikes)));
        context.setPacketHandled(true);
    }
}
