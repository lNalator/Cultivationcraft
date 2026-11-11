package lNalator.Cultivationcraft.Network.Packets;

import lNalator.Cultivationcraft.Client.Renderers.LightningArcRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Echo + Lightning synergy: draws our own lightning arc between two targets (not a vanilla lightning bolt)
public record LightningArcPacket(Vec3 from, Vec3 to) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(from.x);
        buffer.writeDouble(from.y);
        buffer.writeDouble(from.z);
        buffer.writeDouble(to.x);
        buffer.writeDouble(to.y);
        buffer.writeDouble(to.z);
    }

    public static LightningArcPacket decode(FriendlyByteBuf buffer) {
        return new LightningArcPacket(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LightningArcRenderer.add(from, to)));
        context.setPacketHandled(true);
    }
}
