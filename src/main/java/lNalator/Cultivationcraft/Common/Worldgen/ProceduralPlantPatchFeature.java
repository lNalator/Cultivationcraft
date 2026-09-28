package lNalator.Cultivationcraft.Common.Worldgen;

import com.mojang.serialization.Codec;
import lNalator.Cultivationcraft.Common.Blocks.BlockRegister;
import lNalator.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import lNalator.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import lNalator.Cultivationcraft.Common.Blocks.Plants.utils.Seeds;
import lNalator.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import lNalator.Cultivationcraft.Common.Blocks.Plants.world.ProceduralPlantElementConditions;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProceduralPlantPatchFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceLocation[] SURFACE_ELEMENTS = {
            Elements.earthElement, Elements.woodElement, Elements.windElement,
            Elements.waterElement, Elements.iceElement, Elements.lightningElement
    };

    public ProceduralPlantPatchFeature() {
        super(Codec.unit(NoneFeatureConfiguration.INSTANCE));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        var server = level.getLevel();
        boolean nether = server.dimension() == Level.NETHER;
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        PlantCatalogSavedData catalog = PlantCatalogSavedData.getOrCreate(server, Config.Server.procPlantCatalogSize());
        int regionSize = Config.Server.procPlantRegionSizeChunks() * 16;
        int remaining = Config.Server.procPlantPlacementBudget();
        int placed = 0;
        // Invocation-local caches never retain chunks or leak across generation workers.
        Map<Long, Optional<BlockPos>> surfaces = new HashMap<>();
        Map<Long, BlockState> environment = new HashMap<>();

        for (int attempt = 0; attempt < Config.Server.procPlantPatchAttempts() && remaining > 0; attempt++) {
            BlockPos column = origin.offset(random.nextInt(13) - 6, 0, random.nextInt(13) - 6);
            ResourceLocation element = random.nextFloat() < 0.16f ? Elements.noElement
                    : nether ? Elements.fireElement : SURFACE_ELEMENTS[random.nextInt(SURFACE_ELEMENTS.length)];
            boolean underground = nether || element.equals(Elements.earthElement);
            BlockPos center;
            if (underground) {
                int minY = level.getMinBuildHeight() + 4;
                int maxY = nether
                        ? Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + server.dimensionType().logicalHeight()) - 5
                        : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()) - 4;
                if (maxY < minY) {
                    continue;
                }
                center = findCaveFloor(level, new BlockPos(column.getX(), minY + random.nextInt(maxY - minY + 1), column.getZ()),
                        Config.Server.procPlantCaveScanSteps());
            } else {
                center = surface(level, column, surfaces);
                if (center != null && level.getFluidState(center).is(FluidTags.WATER)) {
                    element = Elements.waterElement;
                }
            }
            if (center == null || elementSpawnMult(element) <= 0) {
                continue;
            }
            if (!ProceduralPlantElementConditions.canSpawn(server, level, center, element, environment)) {
                continue;
            }
            var candidates = catalog.entriesForElement(element);
            if (candidates.isEmpty()) {
                continue;
            }
            BlockPos region = new BlockPos(Math.floorDiv(center.getX(), regionSize), 0, Math.floorDiv(center.getZ(), regionSize));
            var speciesRandom = Seeds.forPos(server.getSeed(), region, 0xC0FFEE ^ element.getPath().hashCode());
            var entry = candidates.get(speciesRandom.nextInt(candidates.size()));
            int tier = choosePatchTier(random);
            int count = tier == 3 ? 1 : tier == 2 ? 2 + random.nextInt(3) : 6 + random.nextInt(5);
            int cap = tier == 3 ? 1 : tier == 2 ? 4 : Config.Server.procPlantPatchCapT1();
            if (element.equals(Elements.noElement)) {
                cap = Math.min(cap, 6);
            }
            // Fractional multipliers also affect single plants, and zero disables every tier.
            double desired = Math.min(cap, count * elementSpawnMult(element));
            count = (int) desired + (random.nextDouble() < desired - (int) desired ? 1 : 0);
            int radius = tier == 3 ? 0 : tier == 2 ? 5 + random.nextInt(3) : 3 + random.nextInt(2);
            for (int plantIndex = 0; plantIndex < count && remaining > 0; plantIndex++, remaining--) {
                // The first specimen uses the already validated center.
                BlockPos candidate = plantIndex == 0 ? center
                        : center.offset(random.nextInt(radius * 2 + 1) - radius, 0, random.nextInt(radius * 2 + 1) - radius);
                BlockPos pos = plantIndex == 0 ? center : underground
                        ? findCaveFloor(level, candidate.above(3), 7) : surface(level, candidate, surfaces);
                if (pos == null || !plantable(level, pos)
                        || !ProceduralPlantElementConditions.canSpawn(server, level, pos, element, environment)) {
                    continue;
                }
                int growth = tier == 3 ? 1000 + random.nextInt(9000) : tier == 2 ? 100 + random.nextInt(900) : random.nextInt(100);
                BlockState state = BlockRegister.PROCEDURAL_PLANT.get().defaultBlockState()
                        .setValue(ProceduralPlantBlock.SPECIES, entry.id)
                        .setValue(ProceduralPlantBlock.TIER, tier)
                        .setValue(ProceduralPlantBlock.HOST_QI, tier == 3)
                        .setValue(ProceduralPlantBlock.WATERLOGGED, level.getFluidState(pos).is(FluidTags.WATER));
                if (level.setBlock(pos, state, Block.UPDATE_CLIENTS)) {
                    if (level.getBlockEntity(pos) instanceof ProceduralPlantBlockEntity plant) {
                        plant.initializeWorldgenGrowth(growth);
                    }
                    environment.put(pos.asLong(), state);
                    placed++;
                }
            }
        }
        return placed > 0;
    }

    private static int choosePatchTier(RandomSource random) {
        double roll = random.nextDouble() * 100;
        double tier3 = Config.Server.procPlantPatchTier3Percent();
        return roll < tier3 ? 3 : roll < Math.min(100, tier3 + Config.Server.procPlantPatchTier2Percent()) ? 2 : 1;
    }

    private static BlockPos surface(WorldGenLevel level, BlockPos column, Map<Long, Optional<BlockPos>> cache) {
        long key = BlockPos.asLong(column.getX(), 0, column.getZ());
        return cache.computeIfAbsent(key, ignored -> {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            // One-block pools are allowed; deeper water has no solid floor here.
            if (level.getBlockState(pos.below()).is(Blocks.WATER)) {
                pos = pos.below();
            }
            return plantable(level, pos) ? Optional.of(pos) : Optional.empty();
        }).orElse(null);
    }

    private static BlockPos findCaveFloor(WorldGenLevel level, BlockPos start, int steps) {
        var cursor = start.mutable();
        for (int i = 0; i < steps && cursor.getY() > level.getMinBuildHeight(); i++, cursor.move(Direction.DOWN)) {
            if (level.isEmptyBlock(cursor) && plantable(level, cursor) && !level.canSeeSkyFromBelowWater(cursor)) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static boolean plantable(WorldGenLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        boolean shallowWater = state.is(Blocks.WATER) && state.getFluidState().isSource()
                && !level.getFluidState(pos.above()).is(FluidTags.WATER);
        if (!state.isAir() && !shallowWater) {
            return false;
        }
        BlockPos floor = pos.below();
        BlockState ground = level.getBlockState(floor);
        return ground.isFaceSturdy(level, floor, Direction.UP)
                && Block.isFaceFull(ground.getCollisionShape(level, floor), Direction.UP);
    }

    private static double elementSpawnMult(ResourceLocation element) {
        if (element.equals(Elements.fireElement)) return Config.Server.spawnMultFire();
        if (element.equals(Elements.earthElement)) return Config.Server.spawnMultEarth();
        if (element.equals(Elements.woodElement)) return Config.Server.spawnMultWood();
        if (element.equals(Elements.windElement)) return Config.Server.spawnMultWind();
        if (element.equals(Elements.waterElement)) return Config.Server.spawnMultWater();
        if (element.equals(Elements.iceElement)) return Config.Server.spawnMultIce();
        if (element.equals(Elements.lightningElement)) return Config.Server.spawnMultLightning();
        return Config.Server.spawnMultNone();
    }
}
