package DaoOfModding.Cultivationcraft.Common.Containers;

import DaoOfModding.Cultivationcraft.Common.BasicContainer;
import DaoOfModding.Cultivationcraft.Common.Refinement.RefinementHandlers;
import DaoOfModding.Cultivationcraft.Common.Capabilities.RefinementInventory.RefinementItemHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import DaoOfModding.Cultivationcraft.Common.Register;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class RefinementContainer extends BasicContainer
{
    public static final int REFINEMENT_SLOT_Y = 43;
    public static final int REFINEMENT_SLOT_X = 80;

    protected final RefinementItemHandler itemStackHandler;
    private final net.minecraft.world.inventory.ContainerData refinementProgress = new net.minecraft.world.inventory.SimpleContainerData(2);
    private final Player owner;

    public static RefinementContainer createContainerServerSide(int windowID, Inventory playerInventory, RefinementItemHandler handler)
    {
        return new RefinementContainer(windowID, playerInventory, handler);
    }


    public static RefinementContainer createContainerClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf extraData)
    {
        return new RefinementContainer(windowID, playerInventory, new RefinementItemHandler());
    }

    protected RefinementContainer(int windowId, Inventory playerInv, RefinementItemHandler handler)
    {
        super(Register.REFINEMENT_MENU.get(), windowId);

        itemStackHandler = handler;
        owner = playerInv.player;
        addDataSlots(refinementProgress);

        // Add the players inventory slots into the container
        addPlayerInventory(playerInv);

        addSlot(new SlotItemHandler(handler, 0, REFINEMENT_SLOT_X, REFINEMENT_SLOT_Y));
    }

    @Override
    public void broadcastChanges() {
        if (!owner.level.isClientSide) {
            ItemStack stack = itemStackHandler.getStackInSlot(0);
            var handler = RefinementHandlers.find(stack);
            refinementProgress.set(0, handler == null ? 0 : Math.round(handler.progress(owner, stack) * 1000));
            refinementProgress.set(1, handler == null ? 0 : Math.min(32767, (int) Math.ceil(handler.remainingSeconds(owner, stack))));
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < FIRST_FREE_SLOT_INDEX) {
            if (!moveItemStackTo(stack, FIRST_FREE_SLOT_INDEX, slots.size(), false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, FIRST_FREE_SLOT_INDEX, true)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    public int getRemainingSeconds() { return refinementProgress.get(1); }

    public float getRefinementProgress() { return refinementProgress.get(0) / 1000f; }

    @Override
    // Return true if player can interact with this container, false if not
    public boolean stillValid(@Nonnull Player player)
    {
        return true;
    }
}
