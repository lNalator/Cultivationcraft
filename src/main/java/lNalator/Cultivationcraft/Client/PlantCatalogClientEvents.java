package lNalator.Cultivationcraft.Client;

import lNalator.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlantCatalogClientEvents {
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPlantCatalog.clear();
    }
}
