package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;


public final class Seeds {
    // A salt to ensure that the seed is different from other uses of the same technique
    public static final long PLANT_SALT = "cultivationcraftplants".hashCode();
    public static RandomSource forPos(long worldSeed, BlockPos pos, long extraSalt) {
        long mixed = Mth.getSeed(pos) ^ worldSeed ^ PLANT_SALT ^ extraSalt * 0x9E3779B97F4A7C15L;
        return RandomSource.create(mixed);
    }
}


