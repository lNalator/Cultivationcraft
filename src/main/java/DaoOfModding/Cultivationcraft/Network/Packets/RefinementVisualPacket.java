package DaoOfModding.Cultivationcraft.Network.Packets;

import DaoOfModding.Cultivationcraft.Client.Renderers.RefinementItemRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record RefinementVisualPacket(UUID player, ResourceLocation dimension, ItemStack item, int color) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(player);
        buffer.writeResourceLocation(dimension);
        buffer.writeItem(item);
        buffer.writeInt(color);
    }

    public static RefinementVisualPacket decode(FriendlyByteBuf buffer) {
        return new RefinementVisualPacket(buffer.readUUID(), buffer.readResourceLocation(), buffer.readItem(), buffer.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RefinementItemRenderer.update(player, dimension, item, color)));
        context.setPacketHandled(true);
    }
}
