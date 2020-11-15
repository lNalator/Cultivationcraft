package DaoOfModding.Cultivationcraft.Server;

import DaoOfModding.Cultivationcraft.Common.FlyingSwordController;
import DaoOfModding.Cultivationcraft.Common.FlyingSwordEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerListeners
{
    public static long lastServerTickTime = System.nanoTime();

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            if(ServerItemControl.loaded)
            {
                ServerItemControl.checkForRecalls();
                FlyingSwordBindProgresser.bindFlyingSword(System.nanoTime() - lastServerTickTime);
            }

            lastServerTickTime = System.nanoTime();
        }
    }

    // Fired off when an item is thrown
    @SubscribeEvent
    public static void entityJoinWorld(ItemTossEvent event)
    {
        // Check if the entity is an item
        if (event.getEntity() instanceof ItemEntity)
        {
            ItemEntity item = (ItemEntity) event.getEntity();

            // If the item is a flying sword, spawn a flying sword item and cancel the current spawn
            if (FlyingSwordController.isFlying(item.getItem()))
            {
                // Only do this if the entity is not already a FlyingSwordEntity
                if (!(event.getEntity() instanceof FlyingSwordEntity))
                {
                    event.setCanceled(true);
                    FlyingSwordController.spawnFlyingSword(item);
                }
            }
        }
    }
}
