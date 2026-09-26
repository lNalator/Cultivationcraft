package DaoOfModding.Cultivationcraft.Server;

import DaoOfModding.Cultivationcraft.Common.Capabilities.RefinementInventory.RefinementInventoryProvider;
import DaoOfModding.Cultivationcraft.Common.Refinement.RefinementHandlers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class RefinementProcessor {
    public static void tick(long elapsedNanos) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isRemoved() || player.isSpectator()) continue;
            var inventory = player.getCapability(RefinementInventoryProvider.INSTANCE).orElse(null);
            if (inventory == null) continue;
            var stack = inventory.getItemStackHandler().getStackInSlot(0);
            RefinementHandlers.tick(player, stack, elapsedNanos);
        }
    }
}
