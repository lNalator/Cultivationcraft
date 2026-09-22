package DaoOfModding.Cultivationcraft.Common.Containers;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import DaoOfModding.Cultivationcraft.Common.Items.ProceduralPlantItem;
import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Ingredient qi only. QiHostData is reserved for future recipe requirements. */
public final class AlchemyQi {
    public static final List<ResourceLocation> ELEMENTS = List.of(Elements.noElement, Elements.fireElement,
            Elements.earthElement, Elements.woodElement, Elements.windElement, Elements.waterElement,
            Elements.iceElement, Elements.lightningElement);

    private AlchemyQi() {}

    public static int contribution(ItemStack stack) {
        if (!(stack.getItem() instanceof ProceduralPlantItem) || !stack.hasTag()) return 0;
        var tag = stack.getTag();
        int growth = tag.contains("SpiritualGrowth", Tag.TAG_ANY_NUMERIC)
                ? tag.getInt("SpiritualGrowth") : tag.getInt("PlantAge");
        return Mth.clamp(growth, 0, ProceduralPlantBlockEntity.MAX_SPIRITUAL_GROWTH) * stack.getCount();
    }

    public static int[] totals(Container container, ServerLevel level) {
        int[] totals = new int[ELEMENTS.size()];
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            int qi = contribution(stack);
            if (qi == 0) continue;
            var genome = PlantGenomes.getById(level, ProceduralPlantItem.readSpecies(stack));
            if (genome == null) continue;
            int element = ELEMENTS.indexOf(genome.qiElement());
            if (element >= 0) totals[element] += qi;
        }
        return totals;
    }
}
