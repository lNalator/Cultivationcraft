package DaoOfModding.Cultivationcraft.Common.Alchemy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import DaoOfModding.Cultivationcraft.Common.Blocks.entity.AlchemyCauldronBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Containers.AlchemyQi;
import DaoOfModding.Cultivationcraft.Common.Items.ItemRegister;
import DaoOfModding.Cultivationcraft.Common.Items.ProceduralPlantItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** One batch consumes all nine input stacks, exactly as counted by the displayed scores. */
public record AlchemyBatch(PillDefinition definition, int[] scores, boolean valid, double impurity, double neutralBonus) {
    public static AlchemyBatch inspect(Container inventory, ServerLevel level) {
        int[] scores = AlchemyQi.totals(inventory, level);
        int total = Arrays.stream(scores).sum();
        int elemental = total - scores[0];
        Set<Integer> species = new HashSet<>();
        boolean occupied = false, valid = true, tierPlant = false;
        for (int i = 0; i < AlchemyCauldronBlockEntity.SLOT_COUNT; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            occupied = true;
            int id = ProceduralPlantItem.readSpecies(stack);
            if (!(stack.getItem() instanceof ProceduralPlantItem) || AlchemyQi.contribution(stack) <= 0
                    || PlantGenomes.getById(level, id) == null) { valid = false; continue; }
            species.add(id);
            tierPlant |= ProceduralPlantBlockEntity.growthToTier(AlchemyQi.contribution(stack) / stack.getCount()) >= 1;
        }
        if (!occupied) return null;
        // A recognisable elemental pair takes precedence, even when under the minimum.
        // Otherwise cultivation is the blind, total-score recipe. This prevents partial
        // healing recipes from silently becoming successful cultivation pills.
        PillDefinition selected = null;
        double closest = Double.MAX_VALUE;
        for (PillDefinition candidate : PillDefinition.all()) {
            if (candidate.cultivation()) continue;
            boolean represented = true;
            double distance = 0;
            int targetTotal = Arrays.stream(candidate.targets()).sum();
            for (int i = 0; i < scores.length; i++) {
                int target = candidate.targets()[i];
                if (target > 0 && scores[i] == 0) represented = false;
                if (i == 0 && target == 0) continue; // Optional neutral stabilizer.
                distance += Math.abs(scores[i] - target) / (double) targetTotal;
            }
            if (represented && distance < closest) { selected = candidate; closest = distance; }
        }
        if (selected == null) {
            // Highest reached cultivation threshold; below all thresholds, fail the cheapest.
            for (PillDefinition candidate : PillDefinition.all()) {
                if (!candidate.cultivation()) continue;
                if (selected == null || (candidate.minimumScore() <= elemental
                        && (selected.minimumScore() > elemental || candidate.minimumScore() > selected.minimumScore()))
                        || (selected.minimumScore() > elemental && candidate.minimumScore() < selected.minimumScore()))
                    selected = candidate;
            }
        }
        if (selected == null) return null;
        valid &= tierPlant && species.size() >= 2 && species.size() <= 3;
        double waste = 0;
        if (selected.cultivation()) {
            valid &= elemental >= selected.minimumScore();
            waste = Math.max(0, elemental - selected.minimumScore() - Math.max(10, selected.minimumScore() * .25));
        } else {
            for (int i = 0; i < scores.length; i++) {
                int target = selected.targets()[i];
                valid &= scores[i] >= target;
                if (i == 0 && target == 0) continue;
                waste += target == 0 ? scores[i] : Math.max(0, scores[i] - target - Math.max(10, target * .25));
            }
        }
        double bonus = 0;
        if (selected.targets()[0] == 0) {
            int required = selected.cultivation() ? selected.minimumScore() : Arrays.stream(selected.targets()).sum();
            double ideal = Math.max(10, required * .1), neutral = scores[0];
            bonus = neutral < ideal * .75 ? neutral / (ideal * .75)
                    : neutral <= ideal * 1.25 ? 1 : Math.max(0, (2 * ideal - neutral) / (.75 * ideal));
            waste += Math.max(0, neutral - ideal * 1.25);
        }
        return new AlchemyBatch(selected, scores, valid, total == 0 ? 1 : Mth.clamp(waste / total, 0, 1), bonus);
    }

    public int purity() { return (int) Math.round(Mth.clamp(95 - 50 * impurity + 5 * neutralBonus, 1, 100)); }

    public ItemStack refine(ServerLevel level, int cauldronTier) {
        // D = 1 for these starter definitions. Invalid mandatory requirements always ruin a batch.
        double failure = Mth.clamp(5 + 8 * (1 - cauldronTier) + 6 * impurity - 3 * neutralBonus, 2, 90);
        if (!valid || level.random.nextDouble() * 100 < failure) return new ItemStack(ItemRegister.ALCHEMY_REMNANTS.get());
        net.minecraft.resources.ResourceLocation affinity = null;
        if (definition.cultivation()) {
            List<Integer> dominant = new ArrayList<>();
            int highest = 0;
            for (int i = 1; i < scores.length; i++) {
                if (scores[i] > highest) { highest = scores[i]; dominant.clear(); }
                if (scores[i] == highest && highest > 0) dominant.add(i);
            }
            affinity = AlchemyQi.ELEMENTS.get(dominant.get(level.random.nextInt(dominant.size())));
        }
        return PillStacks.create(level, definition, purity(), affinity);
    }
}
