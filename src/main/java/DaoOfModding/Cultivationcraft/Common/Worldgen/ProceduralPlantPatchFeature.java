package DaoOfModding.Cultivationcraft.Common.Worldgen;

import com.mojang.serialization.Codec;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;

public class ProceduralPlantPatchFeature extends Feature<NoneFeatureConfiguration> {

    public ProceduralPlantPatchFeature() {
        super(Codec.unit(NoneFeatureConfiguration.INSTANCE));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rng = ctx.random();
        BlockPos origin = ctx.origin();

        // Try multiple attempts around origin to make a patch
        int placed = 0;
        int tries = 64;                 // density – tweakable
        int radiusXZ = 6;               // spread – tweakable
        int ySpread = 2;

        for (int i = 0; i < tries; i++) {
            int dx = rng.nextInt(radiusXZ * 2 + 1) - radiusXZ;
            int dz = rng.nextInt(radiusXZ * 2 + 1) - radiusXZ;
            int dy = rng.nextInt(ySpread * 2 + 1) - ySpread;

            BlockPos pos = origin.offset(dx, dy, dz);

            // place only on “ground surface” – nudge down to the first air above solid
            pos = findSurface(level, pos);
            if (pos == null) continue;

            if (!canPlaceHere(level, pos)) continue;

            // optional: species/biome gating via your genome
            var g = PlantGenomes.forWorldPos(level.getLevel(), pos);
            if (g.spawnsInCold()) {
                // tiny example: skip if biome is hot
                float temp = level.getBiome(pos).value().getBaseTemperature();
                if (temp > 0.9f) continue;
            }

            // place plant block at pos if air
            if (level.isEmptyBlock(pos)) {
                level.setBlock(pos, BlockRegister.PROCEDURAL_PLANT.get().defaultBlockState(), 2);
                placed++;
            }
        }

        return placed > 0; // tell the engine if we placed anything
    }

    private BlockPos findSurface(WorldGenLevel level, BlockPos start) {
        BlockPos.MutableBlockPos m = start.mutable();

        // slide down a bit to avoid floating
        for (int i = 0; i < 8 && m.getY() > level.getMinBuildHeight(); i++) {
            if (!level.isEmptyBlock(m)) {
                // step up until the first air
                while (!level.isEmptyBlock(m.above())) {
                    m.move(Direction.UP);
                    if (m.getY() >= level.getMaxBuildHeight()) return null;
                }
                return m.above().immutable();
            }
            m.move(Direction.DOWN);
        }
        return null;
    }

    private boolean canPlaceHere(WorldGenLevel level, BlockPos pos) {
        // must be air at pos and dirt/grass-like underneath
        if (!level.isEmptyBlock(pos)) return false;

        var below = level.getBlockState(pos.below());
        if (!(below.is(BlockTags.DIRT) || below.is(Blocks.FARMLAND))) return false;

        // light check example
        int light = level.getMaxLocalRawBrightness(pos);
        return light >= 7;
    }
}
