package lNalator.Cultivationcraft.Network.Packets;

import lNalator.Cultivationcraft.Client.Renderers.GroundSlamRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// lift: how high the ground pieces rise, 1 = normal (Falling + Earth raises it)
public record GroundSlamPacket(Vec3 center, float radius, float lift) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(center.x);
        buffer.writeDouble(center.y);
        buffer.writeDouble(center.z);
        buffer.writeFloat(radius);
        buffer.writeFloat(lift);
    }

    public static GroundSlamPacket decode(FriendlyByteBuf buffer) {
        return new GroundSlamPacket(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat(), buffer.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GroundSlamRenderer.add(center, radius, lift)));
        context.setPacketHandled(true);
    }
}
