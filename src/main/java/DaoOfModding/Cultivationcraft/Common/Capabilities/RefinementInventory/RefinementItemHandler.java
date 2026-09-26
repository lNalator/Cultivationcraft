package DaoOfModding.Cultivationcraft.Common.Capabilities.RefinementInventory;

import DaoOfModding.Cultivationcraft.Common.Refinement.RefinementHandlers;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class RefinementItemHandler extends ItemStackHandler {
    public RefinementItemHandler() { super(1); }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        return RefinementHandlers.find(stack) != null;
    }
}
