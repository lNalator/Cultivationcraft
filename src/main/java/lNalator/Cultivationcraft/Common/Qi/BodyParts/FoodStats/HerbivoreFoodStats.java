package lNalator.Cultivationcraft.Common.Qi.BodyParts.FoodStats;

import net.minecraft.world.level.block.state.BlockState;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class HerbivoreFoodStats extends QiFoodStats {

    // Blocks of the 1.19 GRASS material (grass block, mycelium, hay, dried kelp)
    public static final TagKey<Block> GRASS_LIKE = TagKey.create(Registries.BLOCK, new ResourceLocation(Cultivationcraft.MODID, "grass_like"));
    // Blocks of the 1.19 GRASS, LEAVES, REPLACEABLE_PLANT, REPLACEABLE_FIREPROOF_PLANT and VEGETABLE materials
    public static final TagKey<Block> HERBIVORE_FOOD = TagKey.create(Registries.BLOCK, new ResourceLocation(Cultivationcraft.MODID, "herbivore_food"));

    @Override
    public void eat(int p_75122_1_, float p_75122_2_) {
        // Food gives double nutrition value
        super.eat(p_75122_1_ * 2, p_75122_2_);
    }

    @Override
    public int getNutrition(BlockState block) {
        if (block.is(HERBIVORE_FOOD)) {
            return 1;
        }

        return 0;
    }

    @Override
    public boolean canEatMeat() {
        return false;
    }

    @Override
    public HerbivoreFoodStats clone() {
        HerbivoreFoodStats clone = new HerbivoreFoodStats();
        clone.maxFood = maxFood;
        clone.exhaustionLevel = exhaustionLevel;
        clone.foodLevel = foodLevel;
        clone.tickTimer = tickTimer;

        return clone;
    }
}
