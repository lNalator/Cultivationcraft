package DaoOfModding.Cultivationcraft.Common.Capabilities.RefinementInventory;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;

public class RefinementInventory implements IRefinementInventory
{
    protected RefinementItemHandler item = new RefinementItemHandler();

    public RefinementItemHandler getItemStackHandler()
    {
        return item;
    }

    public void setItemStackHandler(RefinementItemHandler newItem)
    {
        item = newItem;
    }

    // Return the player's persistent refinement inventory
    public static IRefinementInventory getCapability(Player player)
    {
        return player.getCapability(RefinementInventoryProvider.INSTANCE).orElseThrow(() -> new IllegalArgumentException("Missing player refinement inventory"));
    }

    public CompoundTag writeNBT()
    {
        CompoundTag nbt = getItemStackHandler().serializeNBT();

        return nbt;
    }

    public void readNBT(CompoundTag nbt)
    {
        getItemStackHandler().deserializeNBT((CompoundTag)nbt);
    }
}
