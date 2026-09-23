package DaoOfModding.Cultivationcraft.Common.Events;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlantCatalogEvents {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());
        PacketHandler.sendPlantCatalogToClient(sp, data.entries());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());
        PacketHandler.sendPlantCatalogToClient(sp, data.entries());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());
        PacketHandler.sendPlantCatalogToClient(sp, data.entries());
    }
}
