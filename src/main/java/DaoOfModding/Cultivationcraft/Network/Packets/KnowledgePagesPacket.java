package DaoOfModding.Cultivationcraft.Network.Packets;

import DaoOfModding.Cultivationcraft.Client.ClientKnowledge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.function.Supplier;

/** Starting and learned pages share one tree; slips hold only a key and their physical title. */
public record KnowledgePagesPacket(List<Page> pages) {
    public record Page(String id, String title, String text, List<String> category, String translationKey,
                       boolean operatorOnly, int cultivationType, List<String> aliases) {}
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeCollection(pages, (buf, page) -> {
            buf.writeUtf(page.id);
            buf.writeUtf(page.title);
            buf.writeUtf(page.text);
            buf.writeCollection(page.category, (out, part) -> out.writeUtf(part));
            buf.writeUtf(page.translationKey);
            buf.writeBoolean(page.operatorOnly);
            buf.writeInt(page.cultivationType);
            buf.writeCollection(page.aliases, (out, alias) -> out.writeUtf(alias));
        });
    }
    public static KnowledgePagesPacket decode(FriendlyByteBuf buffer) {
        return new KnowledgePagesPacket(buffer.readList(buf -> new Page(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readList(in -> in.readUtf()),
                buf.readUtf(), buf.readBoolean(), buf.readInt(), buf.readList(in -> in.readUtf()))));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientKnowledge.update(pages)));
        context.setPacketHandled(true);
    }
}
