package DaoOfModding.Cultivationcraft.Network.Packets;

import DaoOfModding.Cultivationcraft.Client.ClientKnowledge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.function.Supplier;

/** Only unlocked pages are sent; the slip itself holds a key and its physical title. */
public record KnowledgePagesPacket(List<Page> pages) {
    public record Page(String id, String title, String text) {}
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeCollection(pages, (buf, page) -> { buf.writeUtf(page.id); buf.writeUtf(page.title); buf.writeUtf(page.text); });
    }
    public static KnowledgePagesPacket decode(FriendlyByteBuf buffer) {
        return new KnowledgePagesPacket(buffer.readList(buf -> new Page(buf.readUtf(), buf.readUtf(), buf.readUtf())));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientKnowledge.update(pages)));
        context.setPacketHandled(true);
    }
}
