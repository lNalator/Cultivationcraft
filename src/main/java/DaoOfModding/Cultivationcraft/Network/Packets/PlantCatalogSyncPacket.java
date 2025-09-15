package DaoOfModding.Cultivationcraft.Network.Packets;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PlantCatalogSyncPacket extends Packet {
    public static class Entry {
        public final int id;
        public final String name;
        public final int color;
        public final String element; // ResourceLocation string
        public final int tier;
        public Entry(int id, String name, int color, String element, int tier) {
            this.id = id; this.name = name; this.color = color; this.element = element; this.tier = tier;
        }
    }

    private final List<Entry> entries;

    public PlantCatalogSyncPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeVarInt(e.id);
            buf.writeUtf(e.name);
            buf.writeVarInt(e.color);
            buf.writeUtf(e.element);
            buf.writeVarInt(e.tier);
        }
    }

    public static PlantCatalogSyncPacket decode(FriendlyByteBuf buf) {
        try {
            int n = buf.readVarInt();
            List<Entry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                int id = buf.readVarInt();
                String name = buf.readUtf(32767);
                int color = buf.readVarInt();
                String element = buf.readUtf(32767);
                int tier = buf.readVarInt();
                list.add(new Entry(id, name, color, element, tier));
            }
            return new PlantCatalogSyncPacket(list);
        } catch (Exception e) {
            Cultivationcraft.LOGGER.warn("Exception while reading PlantCatalogSyncPacket: " + e);
            return new PlantCatalogSyncPacket(List.of());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ClientPlantCatalog.clear();
            for (Entry e : entries) {
                ClientPlantCatalog.put(e.id, e.name, e.color, e.element, e.tier);
            }
        });
        ctx.setPacketHandled(true);
    }
}
