package DaoOfModding.Cultivationcraft.Common.Items;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public class ProceduralPlantItem extends BlockItem {
    public ProceduralPlantItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // Try to read species from BlockStateTag written by getCloneItemStack
        int species = 0;
        if (stack.hasTag() && stack.getTag().contains("BlockStateTag")) {
            var bst = stack.getTag().getCompound("BlockStateTag");
            if (bst.contains("species")) {
                try { species = Integer.parseInt(bst.getString("species")); } catch (NumberFormatException ignored) {}
            }
        }

        String name = null;
        if (level instanceof ServerLevel server) {
            name = PlantGenomes.getNameById(server, species);
        } else {
            var e = ClientPlantCatalog.get(species);
            if (e != null) name = e.name;
        }

        if (name != null && !name.isEmpty()) {
            tooltip.add(Component.literal("Species: " + name + " (#" + species + ")"));
        } else {
            tooltip.add(Component.literal("Species #" + species));
        }
    }
}
