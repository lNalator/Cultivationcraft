package DaoOfModding.Cultivationcraft.Common.Capabilities.RefinementInventory;

import net.minecraft.nbt.CompoundTag;

public interface IRefinementInventory
{
    public RefinementItemHandler getItemStackHandler();
    public void setItemStackHandler(RefinementItemHandler newItem);

    public CompoundTag writeNBT();
    public void readNBT(CompoundTag NBT);
}
