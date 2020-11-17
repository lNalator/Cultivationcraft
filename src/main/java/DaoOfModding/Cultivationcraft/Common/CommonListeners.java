package DaoOfModding.Cultivationcraft.Common;

import DaoOfModding.Cultivationcraft.Client.ClientItemControl;
import DaoOfModding.Cultivationcraft.Client.ClientListeners;
import DaoOfModding.Cultivationcraft.Client.GUI.SkillHotbarOverlay;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.IChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import DaoOfModding.Cultivationcraft.Server.ServerItemControl;
import DaoOfModding.Cultivationcraft.Server.ServerListeners;
import DaoOfModding.Cultivationcraft.Server.SkillHotbarServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommonListeners
{
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.player.getEntityWorld().isRemote)
            ClientListeners.playerTick(event);
        else
            ServerListeners.playerTick(event);
    }

    @SubscribeEvent
    public static void worldLoad(WorldEvent.Load event)
    {
        if (event.getWorld().isRemote())
            ClientItemControl.thisWorld = event.getWorld();
        else
            ServerItemControl.loaded = true;
    }

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load event)
    {
        // Only on server
        if (!event.getWorld().isRemote())
        {
            // If the Chunk's Qi sources have no been generated yet, generate them
            IChunkQiSources sources = ChunkQiSources.getChunkQiSources((Chunk) event.getChunk());
            if (sources.getChunkPos() == null)
            {
                sources.setChunkPos(event.getChunk().getPos());
                sources.generateQiSources();

                // Mark the chunk as dirty so it will save the updated capability
                ((Chunk) event.getChunk()).markDirty();

                // Send the new capability data to all tracking clients
                PacketHandler.sendChunkQiSourcesToClient((Chunk) event.getChunk());
            }
        }
    }


    // Fired off when an player logs into the world
    @SubscribeEvent
    public static void playerJoinsWorld(PlayerEvent.PlayerLoggedInEvent event)
    {
        CultivatorStats.getCultivatorStats(event.getPlayer()).setDisconnected(false);

        if (!event.getEntity().getEntityWorld().isRemote)
        {
            ServerItemControl.sendPlayerStats(event.getPlayer(), (PlayerEntity) event.getPlayer());
            SkillHotbarServer.addPlayer(event.getPlayer().getUniqueID());
        }
    }

    // Fired off when an player respawns into the world
    @SubscribeEvent
    public static void playerRespawns(PlayerEvent.PlayerRespawnEvent event)
    {
        CultivatorStats.getCultivatorStats(event.getPlayer()).setDisconnected(false);

        if (!event.getEntity().getEntityWorld().isRemote)
            ServerItemControl.sendPlayerStats(event.getPlayer(), (PlayerEntity)event.getPlayer());
    }

    // Fired off when an player changes dimension
    @SubscribeEvent
    public static void playerChangesDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        CultivatorStats.getCultivatorStats(event.getPlayer()).setDisconnected(false);

        if (!event.getEntity().getEntityWorld().isRemote)
            ServerItemControl.sendPlayerStats(event.getPlayer(), (PlayerEntity)event.getPlayer());
    }

    // Fired off when an player starts tracking a target
    @SubscribeEvent
    public static void playerStartsTracking(PlayerEvent.StartTracking event)
    {
        if (!event.getEntity().getEntityWorld().isRemote)
            if (event.getTarget() instanceof PlayerEntity)
                ServerItemControl.sendPlayerStats(event.getPlayer(), (PlayerEntity)event.getTarget());
    }

    // Fired off when an player starts watching a chunk
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event)
    {
        if (!event.getWorld().isRemote)
            PacketHandler.sendChunkQiSourcesToClient(event.getWorld().getChunk(event.getPos().x, event.getPos().z), event.getPlayer());
    }

    @SubscribeEvent
    public static void playerDisconnects(PlayerEvent.PlayerLoggedOutEvent event)
    {
        CultivatorStats.getCultivatorStats(event.getPlayer()).setDisconnected(true);

        if (!event.getPlayer().getEntityWorld().isRemote)
            SkillHotbarServer.removePlayer(event.getPlayer().getUniqueID());
    }

    @SubscribeEvent
    public static void playerInteract(PlayerInteractEvent.RightClickBlock event)
    {
        cancelPlacement(event);
    }

    @SubscribeEvent
    public static void playerInteract(PlayerInteractEvent.RightClickItem event)
    {
        cancelPlacement(event);
    }

    private static void cancelPlacement(PlayerInteractEvent event)
    {
        // Cancel placing item if the SkillHotbar is active
        if (event.getWorld().isRemote)
        {
            if (SkillHotbarOverlay.isActive())
                event.setCanceled(true);
        }
        else
        if (SkillHotbarServer.isActive(event.getPlayer().getUniqueID()))
            event.setCanceled(true);
    }

    // Fired off when an entity joins the world, this happens on both the client and the server
    @SubscribeEvent
    public void entityJoinWorld(EntityJoinWorldEvent event)
    {
    }
}