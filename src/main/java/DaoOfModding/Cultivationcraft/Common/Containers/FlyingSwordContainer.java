package DaoOfModding.Cultivationcraft.Common.Containers;

import DaoOfModding.Cultivationcraft.Common.BasicContainer;
import DaoOfModding.Cultivationcraft.Common.Capabilities.FlyingSwordContainerItemStack.FlyingSwordContainerItemHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import DaoOfModding.Cultivationcraft.Common.Register;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class FlyingSwordContainer extends BasicContainer
{
    public static final int FLYING_SWORD_ITEM_YPOS = 43;
    public static final int FLYING_SWORD_ITEM_XPOS = 80;

    protected final FlyingSwordContainerItemHandler itemStackHandler;
    private final net.minecraft.world.inventory.ContainerData pillProgress = new net.minecraft.world.inventory.SimpleContainerData(1);
    private final Player owner;

    public static FlyingSwordContainer createContainerServerSide(int windowID, Inventory playerInventory, FlyingSwordContainerItemHandler handler)
    {
        return new FlyingSwordContainer(windowID, playerInventory, handler);
    }


    public static FlyingSwordContainer createContainerClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf extraData)
    {
        return new FlyingSwordContainer(windowID, playerInventory, new FlyingSwordContainerItemHandler());
    }

    protected FlyingSwordContainer(int windowId, Inventory playerInv, FlyingSwordContainerItemHandler handler)
    {
        super(Register.ContainerTypeFlyingSword.get(), windowId);

        itemStackHandler = handler;
        owner = playerInv.player;
        addDataSlots(pillProgress);

        // Add the players inventory slots into the container
        addPlayerInventory(playerInv);

        addSlot(new SlotItemHandler(handler, 0, FLYING_SWORD_ITEM_XPOS, FLYING_SWORD_ITEM_YPOS));
    }

    @Override
    public void broadcastChanges() {
        if (!owner.level.isClientSide) pillProgress.set(0, isRefiningPill()
                ? DaoOfModding.Cultivationcraft.Common.Alchemy.PillEffects.analysisProgress(owner, itemStackHandler.getStackInSlot(0)) : 0);
        super.broadcastChanges();
    }

    public boolean isRefiningPill() {
        return itemStackHandler.getStackInSlot(0).getItem() instanceof DaoOfModding.Cultivationcraft.Common.Items.AlchemyPillItem;
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

    public float getBindTime()
    {
        return isRefiningPill() ? 5 * (1 - getBindPercent()) : getNbt("BindRemaining");
    }

    public float getBindPercent()
    {
        return isRefiningPill() ? pillProgress.get(0) / 1000f : getNbt("BindPercent");
    }

    protected float getNbt(String tag)
    {
        if (itemStackHandler.getStackInSlot(0) == ItemStack.EMPTY)
            return 0;

        if (!itemStackHandler.getStackInSlot(0).hasTag() || !itemStackHandler.getStackInSlot(0).getTag().contains(tag))
            return 0;

        return itemStackHandler.getStackInSlot(0).getTag().getFloat(tag);
    }

    @Override
    // Return true if player can interact with this container, false if not
    public boolean stillValid(@Nonnull Player player)
    {
        //TODO: Check if player cultivation high enough to use flying swords
        return true;
    }
}
