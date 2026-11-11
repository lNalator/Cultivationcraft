package lNalator.Cultivationcraft.Network.Packets;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Network.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Element chosen in the Qi Condensation screen to be purged while meditating (noElement stops the purge)
public class PurgeElementPacket extends Packet {

    protected final ResourceLocation element;

    public PurgeElementPacket(ResourceLocation element) {
        this.element = element;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(element);
    }

    public static PurgeElementPacket decode(FriendlyByteBuf buffer) {
        return new PurgeElementPacket(buffer.readResourceLocation());
    }

    // Read the packet received over the network
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived.isServer()) {
            ctx.enqueueWork(() -> processPacket(ctx.getSender()));
        } else {
            Cultivationcraft.LOGGER.warn("Server sent purge element message to player");
        }
    }

    // Process received packet on the Server: only a Qi Condensation cultivator can purge
    protected void processPacket(ServerPlayer player) {
        if (player != null && CultivatorStats.getCultivatorStats(player).getCultivation() instanceof QiCondenserCultivation condenser) {
            condenser.setPurgingElement(element);
            PacketHandler.sendCultivatorStatsToClient(player);
        }
    }
}
