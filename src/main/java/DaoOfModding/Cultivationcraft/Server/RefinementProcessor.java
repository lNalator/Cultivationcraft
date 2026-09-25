package DaoOfModding.Cultivationcraft.Server;

import DaoOfModding.Cultivationcraft.Common.Capabilities.RefinementInventory.RefinementInventory;
import DaoOfModding.Cultivationcraft.Common.Refinement.RefinementHandlers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class RefinementProcessor {
    public static void tick(long elapsedNanos) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (!player.isAlive()) continue;
            var stack = RefinementInventory.getCapability(player).getItemStackHandler().getStackInSlot(0);
            RefinementHandlers.tick(player, stack, elapsedNanos);
        }
    }
}
