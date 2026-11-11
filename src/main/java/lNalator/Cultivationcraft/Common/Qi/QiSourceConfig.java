package lNalator.Cultivationcraft.Common.Qi;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class QiSourceConfig {

    public final static int MaxSources = 1;
    public final static int MaxSize = 64;
    public final static int MinSize = 16;
    // Old Qi source reserves, still the unit of body cultivation (Qi absorb and costs, blood healing)
    public final static int MaxStorage = 100000000;
    public final static int MinStorage = 1000000;
    // Qi source reserves: the smallest holds about 30 minutes of absorption for a starting Qi Condenser (10 Qi a second)
    public final static int MaxReserve = 1800000;
    public final static int MinReserve = 18000;
    // Body cultivation draws from the smaller reserves at the same relative rate as from the old ones
    public final static double BODY_QI_SCALE = (double) MaxStorage / MaxReserve;

    protected final static int defaultRarity = 40;
    protected static HashMap<ResourceKey<Level>, Integer> dimensionRarity = new HashMap<>();

    // Ticks to fully regen, linear: from 1 Minecraft day (24000 ticks) to 3 days
    public final static int MaxRegen = 24000;
    public final static int MinRegen = 72000;

    public static void init() {
        QiSourceConfig.addDimensionRarity(Level.NETHER, 100);
    }

    // Return the number of QiSources to spawn
    // Much more likely to be 0, but has a chance to be up to MaxSources
    public static int getQiSourceInLevelChunk(ResourceKey<Level> dimension) {
        return (int) Math.floor(Math.pow(Math.random(), getRarity(dimension)) * (MaxSources + 1));
    }

    public static void addDimensionRarity(ResourceKey<Level> dimension, int rarity) {
        dimensionRarity.put(dimension, rarity);
    }

    public static int getRarity(ResourceKey<Level> dimension) {
        if (dimensionRarity.containsKey(dimension)) {
            return dimensionRarity.get(dimension);
        }

        return defaultRarity;
    }

    // Return a randomly generated size for a Qi Source
    // Much more likely to be MinSize, but has a chance to be up to MaxSize
    public static int generateRandomSize() {
        return (int) (Math.pow(Math.random(), 5) * (MaxSize - MinSize)) + MinSize;
    }

    // Return a randomly generated double
    // 0 = MinStorage, 1 = MaxStorage
    public static double generateRandomQiStorage() {
        return Math.pow(Math.random(), 10);
    }

    // Return a randomly generated int
    // Specifies the number of ticks it takes to fully regen the qi source
    public static int generateRandomQiRegen() {
        return (int) (Math.pow(Math.random(), 0.25) * (MinRegen - MaxRegen)) + MaxRegen;
    }
}
