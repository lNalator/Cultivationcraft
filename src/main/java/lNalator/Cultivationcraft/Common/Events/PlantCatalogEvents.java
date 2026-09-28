package lNalator.Cultivationcraft.Common.Events;

import lNalator.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import lNalator.Cultivationcraft.Network.PacketHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlantCatalogEvents {

    @SubscribeEvent
    public static void onLevelLoad(net.minecraftforge.event.level.LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            PlantCatalogSavedData.getOrCreate(level, lNalator.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        PlantCatalogSavedData.clear(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        ServerLevel level = sp.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, lNalator.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());
        PacketHandler.sendPlantCatalogToClient(sp, data.entries());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        ServerLevel level = sp.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, lNalator.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());
        PacketHandler.sendPlantCatalogToClient(sp, data.entries());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        ServerLevel level = sp.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, lNalator.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());
        PacketHandler.sendPlantCatalogToClient(sp, data.entries());
    }
}
