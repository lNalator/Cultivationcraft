package DaoOfModding.Cultivationcraft.Network.Packets;

import DaoOfModding.Cultivationcraft.Client.ClientItemControl;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import DaoOfModding.Cultivationcraft.Common.Qi.Techniques.Technique;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import DaoOfModding.Cultivationcraft.Server.ServerItemControl;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class TechniqueInfoPacket extends Packet
{
    private int slotNumber;
    private int info;
    private UUID player;

    public TechniqueInfoPacket(int slot, int newInfo, UUID owner)
    {
        slotNumber = slot;
        info = newInfo;
        player = owner;
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeInt(slotNumber);
        buffer.writeInt(info);
        buffer.writeUUID(player);
    }

    public static TechniqueInfoPacket decode(PacketBuffer buffer)
    {
        TechniqueInfoPacket returnValue = new TechniqueInfoPacket(-1, -1, null);

        try
        {
            // Read in the sent values
            int readSlot = buffer.readInt();
            int readInfo = buffer.readInt();
            UUID readPlayer = buffer.readUUID();

            return new TechniqueInfoPacket(readSlot, readInfo, readPlayer);

        }
        catch (IllegalArgumentException | IndexOutOfBoundsException e)
        {
            Cultivationcraft.LOGGER.warn("Exception while reading TechniqueInfo message: " + e);
            return returnValue;
        }
    }

    // Read the packet received over the network
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);


        // Check to ensure that the packet has valid data values
        if (slotNumber < 0)
        {
            Cultivationcraft.LOGGER.warn("TechniqueInfoPacket was invalid: " + this.toString());
            return;
        }

        if (sideReceived.isClient())
            ctx.enqueueWork(() -> processPacket());
        else
            ctx.enqueueWork(() -> processPacket(ctx.getSender()));
    }

    // Process received packet on the Server
    private void processPacket(ServerPlayerEntity sender)
    {
        // Send the key press to the technique used
        Technique tech = CultivatorTechniques.getCultivatorTechniques(sender).getTechnique(slotNumber);

        if (tech != null)
            tech.processInfo(sender, info);

        // Send the info packet to the client
        PacketHandler.sendTechniqueInfoToClients(this, sender);
    }

    // Process received packet on the Client
    private void processPacket()
    {
        PlayerEntity playerEntity = ClientItemControl.thisWorld.getPlayerByUUID(player);

        // Send the key press to the technique used
        Technique tech = CultivatorTechniques.getCultivatorTechniques(playerEntity).getTechnique(slotNumber);

        if (tech != null)
            tech.processInfo(playerEntity, info);
    }
}