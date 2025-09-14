package DaoOfModding.Cultivationcraft.Client.Textures;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ClientColors {
    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block e) {
        e.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null) {
                var g = PlantGenomes.forWorldPos((net.minecraft.world.level.Level) level, pos);
                return g.colorRGB();
            }
            return 0xFFFFFF;
        }, BlockRegister.PROCEDURAL_PLANT.get());
    }
}
