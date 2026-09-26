package DaoOfModding.Cultivationcraft.Common.Alchemy;

import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Register alchemy data listeners together so each runs exactly once per reload. */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AlchemyReloadListeners {
    private AlchemyReloadListeners() {}

    @SubscribeEvent
    public static void registerAlchemyReloadListeners(AddReloadListenerEvent event) {
        // Forge's event bus 6.0.3 names handlers using the package and simple class name.
        // Separate nested Loader.register handlers in this package would collide.
        event.addListener(new PillDefinition.Loader());
        event.addListener(new AlchemyFailureExplosion.Loader());
    }
}
