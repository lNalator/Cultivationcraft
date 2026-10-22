package lNalator.Cultivationcraft.Server;

import lNalator.Cultivationcraft.Common.CultivationNotifier;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.RefinementInventory.RefinementInventoryProvider;
import lNalator.Cultivationcraft.Common.Qi.Techniques.MeditateTechnique;
import lNalator.Cultivationcraft.Common.Refinement.RefinementHandlers;
import lNalator.Cultivationcraft.Network.PacketHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Refinement only progresses while meditating:
// placing an item starts Meditate, finishing or removing it stands the player up,
// and turning Meditate off ejects the item from the slot.
public final class RefinementProcessor {

    // Players whose current meditation is refining an item
    private static final Set<UUID> refining = new HashSet<>();
    // Players already told they need Meditate equipped
    private static final Set<UUID> warned = new HashSet<>();

    public static void tick(long elapsedNanos) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isRemoved() || player.isSpectator()) {
                continue;
            }
            var inventory = player.getCapability(RefinementInventoryProvider.INSTANCE).orElse(null);
            if (inventory == null) {
                continue;
            }
            var handler = inventory.getItemStackHandler();
            var stack = handler.getStackInSlot(0);
            MeditateTechnique meditate = findMeditate(player);
            UUID id = player.getUUID();

            if (!RefinementHandlers.isActive(player, stack)) {
                warned.remove(id);
                // Refinement finished or item removed: stand up
                if (refining.remove(id) && meditate != null && meditate.isActive()) {
                    meditate.deactivate(player);
                    PacketHandler.sendCultivatorTechniquesToClient(player);
                }
                RefinementHandlers.tick(player, stack, elapsedNanos);
                continue;
            }

            if (meditate == null) {
                if (warned.add(id)) {
                    player.displayClientMessage(Component.translatable("cultivationcraft.refinement.needs_meditate"), true);
                }
                RefinementHandlers.tick(player, ItemStack.EMPTY, elapsedNanos);
                continue;
            }
            warned.remove(id);

            if (!meditate.isActive()) {
                if (refining.remove(id)) {
                    // Meditate was turned off mid-refinement: cancel and eject the item
                    handler.setStackInSlot(0, ItemStack.EMPTY);
                    player.getInventory().placeItemBackInInventory(stack);
                    player.containerMenu.broadcastChanges();
                    CultivationNotifier.notify(player, CultivationNotifier.Type.ALERT, Component.translatable("cultivationcraft.notify.refinement_cancelled"), stack.getHoverName());
                    RefinementHandlers.tick(player, ItemStack.EMPTY, elapsedNanos);
                    continue;
                }
                // Item just placed: start meditating
                meditate.activate(player);
                PacketHandler.sendCultivatorTechniquesToClient(player);
                if (!meditate.isActive()) {
                    RefinementHandlers.tick(player, ItemStack.EMPTY, elapsedNanos);
                    continue;
                }
            }

            refining.add(id);
            RefinementHandlers.tick(player, stack, elapsedNanos);
        }
    }

    private static MeditateTechnique findMeditate(ServerPlayer player) {
        ICultivatorTechniques techs = CultivatorTechniques.getCultivatorTechniques(player);
        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            if (techs.getTechnique(i) instanceof MeditateTechnique meditate) {
                return meditate;
            }
        }
        return null;
    }
}
