package DaoOfModding.Cultivationcraft.Common.Containers;

import DaoOfModding.Cultivationcraft.Common.Capabilities.RefinementInventory.RefinementInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;

public class RefinementMenuProvider implements MenuProvider
{
    protected final ServerPlayer owner;

    public RefinementMenuProvider(ServerPlayer player)
    {
        owner = player;
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("cultivationcraft.gui.refinement");
    }

    // Creating container on the server
    @Override
    public RefinementContainer createMenu(int windowID, Inventory playerInventory, Player player)
    {
        return RefinementContainer.createContainerServerSide(windowID, playerInventory, RefinementInventory.getCapability(owner).getItemStackHandler());
    }
}
