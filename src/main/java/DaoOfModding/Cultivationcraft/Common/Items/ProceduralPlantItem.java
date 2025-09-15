package DaoOfModding.Cultivationcraft.Common.Items;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ProceduralPlantItem extends BlockItem {
    public ProceduralPlantItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // Species id from BlockStateTag (set by pick block)
        int species = 0;
        if (stack.hasTag() && stack.getTag().contains("BlockStateTag")) {
            var bst = stack.getTag().getCompound("BlockStateTag");
            if (bst.contains("species")) {
                try { species = Integer.parseInt(bst.getString("species")); } catch (NumberFormatException ignored) {}
            }
        }

        // Name and element
        String name = null;
        if (level instanceof ServerLevel server) name = PlantGenomes.getNameById(server, species);
        else {
            var e = ClientPlantCatalog.get(species);
            if (e != null) name = e.name;
        }

        int nameColor = 0xFFFFFF;
        String elementStr = null;
        if (level instanceof ServerLevel server) {
            var g = PlantGenomes.getById(server, species);
            if (g != null) { nameColor = g.colorRGB(); elementStr = g.qiElement().toString(); }
        } else {
            var e = ClientPlantCatalog.get(species);
            if (e != null) { nameColor = e.color; elementStr = e.element; }
        }

        if (name == null || name.isEmpty()) name = "Species";
        Style style = Style.EMPTY.withColor(nameColor);
        tooltip.add(Component.literal(name).setStyle(style));

        if (elementStr != null) {
            var rl = new ResourceLocation(elementStr);
            var elem = Elements.getElement(rl);
            if (elem != null) {
                int elemRGB = (elem.color.getRed() << 16) | (elem.color.getGreen() << 8) | elem.color.getBlue();
                tooltip.add(Component.literal("Element: " + elem.getName()).withStyle(s -> s.withColor(elemRGB)));
            }
        }

        // Dynamic Tier (age), only if PlantAge is present on the item NBT
        if (stack.hasTag() && stack.getTag().contains("PlantAge")) {
            int plantAge = stack.getTag().getInt("PlantAge");
            int dynTier = plantAge >= 100 ? 3 : (plantAge >= 50 ? 2 : 1);
            String stars = dynTier >= 3 ? "★★★" : (dynTier == 2 ? "★★" : "★");
            tooltip.add(Component.literal("Tier (age): " + stars).withStyle(s -> s.withColor(0xFFD700)));
        }

        // Host info
        boolean hostFlag = false;
        if (stack.hasTag() && stack.getTag().contains("QiHostData")) hostFlag = true;
        else if (stack.hasTag() && stack.getTag().contains("BlockStateTag")) {
            var bst = stack.getTag().getCompound("BlockStateTag");
            if ("true".equalsIgnoreCase(bst.getString("host_qi"))) hostFlag = true;
        }
        if (hostFlag) tooltip.add(Component.literal("Hosts Qi Source").withStyle(s -> s.withColor(0x00FFFF)));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        int species = 0;
        if (stack.hasTag() && stack.getTag().contains("BlockStateTag")) {
            var bst = stack.getTag().getCompound("BlockStateTag");
            if (bst.contains("species")) {
                try { species = Integer.parseInt(bst.getString("species")); } catch (NumberFormatException ignored) {}
            }
        }
        boolean host = false;
        if (stack.hasTag() && stack.getTag().contains("QiHostData")) host = true;
        else if (stack.hasTag() && stack.getTag().contains("BlockStateTag")) {
            var bst = stack.getTag().getCompound("BlockStateTag");
            if ("true".equalsIgnoreCase(bst.getString("host_qi"))) host = true;
        }
        int dynTier = 0;
        if (stack.hasTag() && stack.getTag().contains("PlantAge")) {
            int plantAge = stack.getTag().getInt("PlantAge");
            dynTier = plantAge >= 100 ? 3 : (plantAge >= 50 ? 2 : 1);
        }
        return Optional.of(new DaoOfModding.Cultivationcraft.Client.Tooltip.PlantBadgeTooltipData(species, host, dynTier));
    }
}

