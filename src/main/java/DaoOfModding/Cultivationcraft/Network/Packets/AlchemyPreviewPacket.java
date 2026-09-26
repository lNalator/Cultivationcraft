package DaoOfModding.Cultivationcraft.Network.Packets;

import DaoOfModding.Cultivationcraft.Common.Containers.AlchemyCauldronMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Viewer-specific preview, never an inventory slot or a craftable item. */
public record AlchemyPreviewPacket(int menu, ItemStack item, int qi, int purityLow, int purityHigh, boolean valid) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menu); buffer.writeItem(item); buffer.writeVarInt(qi);
        buffer.writeInt(purityLow); buffer.writeInt(purityHigh); buffer.writeBoolean(valid);
    }
    public static AlchemyPreviewPacket decode(FriendlyByteBuf buffer) {
        return new AlchemyPreviewPacket(buffer.readVarInt(), buffer.readItem(), buffer.readVarInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof AlchemyCauldronMenu cauldron && cauldron.containerId == menu)
                cauldron.setPreview(this);
        }));
        context.setPacketHandled(true);
    }
    public boolean sameAs(AlchemyPreviewPacket other) {
        return other != null && menu == other.menu && ItemStack.matches(item, other.item) && qi == other.qi
                && purityLow == other.purityLow && purityHigh == other.purityHigh && valid == other.valid;
    }
}
