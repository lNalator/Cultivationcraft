package DaoOfModding.Cultivationcraft.Network.Packets;

import DaoOfModding.Cultivationcraft.Client.Renderers.BindingItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record QiStreamPacket(UUID player, ResourceLocation dimension, BlockPos target, int color, boolean active) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(player); buffer.writeResourceLocation(dimension); buffer.writeBlockPos(target);
        buffer.writeInt(color); buffer.writeBoolean(active);
    }
    public static QiStreamPacket decode(FriendlyByteBuf buffer) {
        return new QiStreamPacket(buffer.readUUID(), buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readInt(), buffer.readBoolean());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> BindingItemRenderer.transfer(player, dimension, target, color, active)));
        context.setPacketHandled(true);
    }
}
