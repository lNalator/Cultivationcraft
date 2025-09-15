package DaoOfModding.Cultivationcraft.Common.Worldgen.Spawn;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;

public interface ElementSpawnCondition {
    // Check substrate and immediate placement constraints (air check is handled by the feature before calling this)
    boolean canPlaceOn(WorldGenLevel level, BlockPos pos, PlantGenome g);

    // Additional biome/dimension rules
    boolean extraRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g);

    // Chance multiplier based on broader environment context
    double environmentBoost(WorldGenLevel level, BlockPos pos, PlantGenome g);
}

