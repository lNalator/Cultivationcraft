package DaoOfModding.Cultivationcraft.Client.Textures;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import DaoOfModding.Cultivationcraft.Common.Items.ItemRegister;
import DaoOfModding.Cultivationcraft.Common.Items.ProceduralPlantItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ClientColors {
    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block e) {
        e.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || state == null || !state.hasProperty(ProceduralPlantBlock.SPECIES)) {
                return 0xFFFFFF;
            }
            int species = state.getValue(ProceduralPlantBlock.SPECIES);
            var entry = ClientPlantCatalog.get(species);
            return entry != null ? entry.color : 0xFFFFFF;
        }, BlockRegister.PROCEDURAL_PLANT.get());
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFF;
            }
            int species = ProceduralPlantItem.readSpecies(stack);
            var entry = ClientPlantCatalog.get(species);
            return entry != null ? entry.color : 0xFFFFFF;
        }, ItemRegister.PROCEDURAL_PLANT_ITEM.get());
    }
}
