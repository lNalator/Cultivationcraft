package DaoOfModding.Cultivationcraft.Common.Worldgen;

import com.mojang.serialization.Codec;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.utils.Seeds;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import DaoOfModding.Cultivationcraft.Common.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.tags.BlockTags;

public class ProceduralPlantPatchFeature extends Feature<NoneFeatureConfiguration> {

    public ProceduralPlantPatchFeature() {
        super(Codec.unit(NoneFeatureConfiguration.INSTANCE));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        RandomSource rng = ctx.random();
        BlockPos origin = ctx.origin();

        PlantCatalogSavedData catalog = PlantCatalogSavedData.getOrCreate(server, Config.Server.procPlantCatalogSize());
        if (catalog.size() <= 0) return false;

        int placed = 0;
        int tries = 64; // attempts around origin to find valid spots
        int radiusXZ = 6;
        int ySpread = 3;

        for (int i = 0; i < tries; i++) {
            int dx = rng.nextInt(radiusXZ * 2 + 1) - radiusXZ;
            int dz = rng.nextInt(radiusXZ * 2 + 1) - radiusXZ;
            int dy = rng.nextInt(ySpread * 2 + 1) - ySpread;

            BlockPos base = origin.offset(dx, dy, dz);

            BlockPos pos = findSurfaceAirAboveSolid(level, base);
            if (pos == null) continue;

            int regionSize = Config.Server.procPlantRegionSizeChunks() * 16;
            int rx = Math.floorDiv(pos.getX(), regionSize);
            int rz = Math.floorDiv(pos.getZ(), regionSize);
            BlockPos regionPos = new BlockPos(rx, 0, rz);
            int id = Seeds.forPos(server.getSeed(), regionPos, 0xC0FFEE).nextInt(Math.max(1, catalog.size()));
            var entry = catalog.getById(id);
            if (entry == null) continue;
            var g = entry.genome;

            // If EARTH plant, bias search to find an underground cavity with solid floor
            if (contains(g.qiElement().getPath(), "earth")) {
                BlockPos cave = findUndergroundPlacementNear(level, pos);
                if (cave != null) pos = cave; else continue;
            }

            if (!level.isEmptyBlock(pos)) continue; // must place into air

            if (!ElementRules.canSpawnAt(server, level, pos, g.qiElement().getPath())) continue;

            // Choose a pre-aging pattern for this patch
            int patchTier = choosePatchTier(rng); // 1,2,3 represent age ranges

            // Base patch size tuned by element multiplier; then reshaped by patch tier type
            int baseCap = Config.Server.procPlantPatchCapT1();
            int patchCap = Math.max(1, (int)Math.round(baseCap * elementSpawnMult(g.qiElement().getPath())));
            int count;
            int radius;
            if (patchTier == 3) { // T3: isolated singles
                count = 1;
                radius = 0;
            } else if (patchTier == 2) { // T2: small group, wider spread
                count = 2 + rng.nextInt(3); // 2..4
                radius = 6 + rng.nextInt(5); // 6..10
            } else { // T1: large clumps near anchor
                count = 1 + rng.nextInt(Math.max(1, patchCap));
                radius = 2 + rng.nextInt(2); // 2..3
            }
            for (int n = 0; n < count; n++) {
                int ox = rng.nextInt(radius * 2 + 1) - radius;
                int oz = rng.nextInt(radius * 2 + 1) - radius;
                int oy = rng.nextInt(2) - 1;
                BlockPos p = pos.offset(ox, oy, oz);

                // Re-adjust to surface for non-earth plants; earth stays underground
                BlockPos pp = contains(g.qiElement().getPath(), "earth") ? adjustToCaveAir(level, p) : findSurfaceAirAboveSolid(level, p);
                if (pp == null) continue;
                if (!ElementRules.canSpawnAt(server, level, pp, g.qiElement().getPath())) continue;

                // HOST_QI: always true for T3; otherwise use config chance
                boolean none = contains(g.qiElement().getPath(), "none");
                boolean host = (patchTier == 3) || rng.nextDouble() < (none ? Config.Server.procPlantHostChanceNone() : Config.Server.procPlantHostChanceElement());

                var state = BlockRegister.PROCEDURAL_PLANT.get()
                        .defaultBlockState()
                        .setValue(ProceduralPlantBlock.SPECIES, id)
                        .setValue(ProceduralPlantBlock.HOST_QI, host);

                if (level.isEmptyBlock(pp)) {
                    level.setBlock(pp, state, 2);
                    // Pre-aging: set BE dynamic age by patch type. For T3, HOST_QI is set; BE onLoad will attach source.
                    if (level.getBlockEntity(pp) instanceof ProceduralPlantBlockEntity be) {
                        int initAge = switch (patchTier) { case 3 -> 100 + rng.nextInt(60); case 2 -> 50 + rng.nextInt(50); default -> rng.nextInt(50); };
                        be.setAge(initAge);
                    }
                    placed++;
                }
            }
        }

        return placed > 0;
    }

    private int choosePatchTier(RandomSource rng) {
        int r = rng.nextInt(100);
        if (r < 10) return 3;   // 10% T3
        if (r < 40) return 2;   // 30% T2
        return 1;               // 60% T1
    }

    private static boolean contains(String s, String k) { return s != null && s.contains(k); }

    private BlockPos findSurfaceAirAboveSolid(WorldGenLevel level, BlockPos start) {
        MutableBlockPos m = new MutableBlockPos(start.getX(), start.getY(), start.getZ());
        // walk down some steps to find ground
        for (int i = 0; i < 16 && m.getY() > level.getMinBuildHeight(); i++) {
            if (!level.isEmptyBlock(m)) {
                // climb to first air above the solid stack
                while (!level.isEmptyBlock(m.above())) {
                    m.move(Direction.UP);
                    if (m.getY() >= level.getMaxBuildHeight()) return null;
                }
                BlockPos place = m.above();
                BlockState below = level.getBlockState(place.below());
                if (below.isFaceSturdy(level, place.below(), Direction.UP)) return place.immutable();
                return null;
            }
            m.move(Direction.DOWN);
        }
        return null;
    }

    private BlockPos findUndergroundPlacementNear(WorldGenLevel level, BlockPos start) {
        final int minY = level.getMinBuildHeight() + 4;
        final int maxSteps = 196;
        MutableBlockPos m = new MutableBlockPos(start.getX(), start.getY(), start.getZ());
        int steps = 0;
        // go down through air
        while (steps++ < 32 && m.getY() > minY && level.isEmptyBlock(m)) m.move(Direction.DOWN);
        // scan down to find underground air above solid floor
        for (; steps < maxSteps && m.getY() > minY; steps++) {
            boolean air = level.isEmptyBlock(m);
            if (air && !level.canSeeSkyFromBelowWater(m)) {
                BlockState below = level.getBlockState(m.below());
                if (!below.isAir() && below.isFaceSturdy(level, m.below(), Direction.UP)) return m.immutable();
            }
            m.move(Direction.DOWN);
        }
        return null;
    }

    private BlockPos adjustToCaveAir(WorldGenLevel level, BlockPos pos) {
        // If already a valid underground air above sturdy floor, keep
        if (level.isEmptyBlock(pos) && !level.canSeeSkyFromBelowWater(pos)) {
            var below = level.getBlockState(pos.below());
            if (!below.isAir() && below.isFaceSturdy(level, pos.below(), Direction.UP)) return pos;
        }
        return findUndergroundPlacementNear(level, pos);
    }

    private double elementSpawnMult(String path) {
        if (path == null) return 1.0;
        if (path.contains("fire")) return Config.Server.spawnMultFire();
        if (path.contains("earth")) return Config.Server.spawnMultEarth();
        if (path.contains("wood")) return Config.Server.spawnMultWood();
        if (path.contains("wind") || path.contains("air")) return Config.Server.spawnMultWind();
        if (path.contains("water")) return Config.Server.spawnMultWater();
        if (path.contains("ice")) return Config.Server.spawnMultIce();
        if (path.contains("lightning")) return Config.Server.spawnMultLightning();
        if (path.contains("none")) return Config.Server.spawnMultNone();
        return 1.0;
    }

    // Element placement rules per the new spec
    private static final class ElementRules {
        static boolean canSpawnAt(ServerLevel server, WorldGenLevel level, BlockPos pos, String path) {
            if (path == null) return true; // none
            path = path.toLowerCase();
            if (path.contains("fire")) return fire(server, level, pos);
            if (path.contains("earth")) return earth(level, pos);
            if (path.contains("ice")) return ice(level, pos);
            if (path.contains("wind") || path.contains("air")) return wind(level, pos);
            if (path.contains("lightning")) return lightning(level, pos);
            if (path.contains("wood")) return wood(level, pos);
            if (path.contains("water")) return water(level, pos);
            return true; // none
        }

        private static boolean fire(ServerLevel server, WorldGenLevel level, BlockPos pos) {
            if (server.dimension() != Level.NETHER) return false;
            BlockState below = level.getBlockState(pos.below());
            boolean ok = below.is(Blocks.NETHERRACK) || below.is(Blocks.CRIMSON_NYLIUM) || below.is(Blocks.WARPED_NYLIUM)
                    || below.is(Blocks.BASALT) || below.is(Blocks.BLACKSTONE) || below.is(Blocks.SOUL_SAND) || below.is(Blocks.SOUL_SOIL);
            return ok && below.isFaceSturdy(level, pos.below(), Direction.UP);
        }

        private static boolean earth(WorldGenLevel level, BlockPos pos) {
            if (level.canSeeSkyFromBelowWater(pos)) return false; // underground-only
            BlockState below = level.getBlockState(pos.below());
            boolean ok = below.is(Blocks.STONE) || below.is(Blocks.GRANITE) || below.is(Blocks.DIORITE) || below.is(Blocks.ANDESITE)
                    || below.is(Blocks.DEEPSLATE) || below.is(Blocks.GRAVEL) || below.is(Blocks.SAND)
                    || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.ROOTED_DIRT) || below.is(Blocks.TUFF);
            return ok && below.isFaceSturdy(level, pos.below(), Direction.UP);
        }

        private static boolean ice(WorldGenLevel level, BlockPos pos) {
            BlockState below = level.getBlockState(pos.below());
            boolean icyGround = below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE) || below.is(Blocks.BLUE_ICE)
                    || below.is(Blocks.FROSTED_ICE) || below.is(Blocks.SNOW_BLOCK);
            if (icyGround) return below.isFaceSturdy(level, pos.below(), Direction.UP);
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            return biome.contains("frozen") || biome.contains("snow");
        }

        private static boolean wind(WorldGenLevel level, BlockPos pos) {
            if (!isOpenArea(level, pos, 1)) return false;
            int sea = level.getLevel().getSeaLevel();
            if (pos.getY() < sea + 75) return false;
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            return biome.contains("mountain") || biome.contains("windswept") || biome.contains("peaks") || biome.contains("hills");
        }

        private static boolean lightning(WorldGenLevel level, BlockPos pos) {
            if (!isOpenArea(level, pos, 1)) return false;
            int sea = level.getLevel().getSeaLevel();
            if (pos.getY() < sea + 100) return false;
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            return biome.contains("mountain") || biome.contains("windswept") || biome.contains("peaks") || biome.contains("hills");
        }

        private static boolean wood(WorldGenLevel level, BlockPos pos) {
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (!(biome.contains("forest") || biome.contains("jungle") || biome.contains("dark_forest"))) return false;
            return nearWoodOrPlants(level, pos, 4);
        }

        private static boolean water(WorldGenLevel level, BlockPos pos) {
            BlockState below = level.getBlockState(pos.below());
            boolean ground = below.is(Blocks.SAND) || below.is(Blocks.CLAY) || below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.MUD);
            if (!ground || !below.isFaceSturdy(level, pos.below(), Direction.UP)) return false;
            if (nearWater(level, pos, 4)) return true;
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            return biome.contains("ocean") || biome.contains("river") || biome.contains("swamp") || biome.contains("beach");
        }

        private static boolean isOpenArea(WorldGenLevel level, BlockPos pos, int clearance) {
            // Checks that there is minimal obstruction above and can see sky
            if (!level.canSeeSkyFromBelowWater(pos)) return false;
            int solid = 0;
            MutableBlockPos m = new MutableBlockPos();
            for (int dx = -clearance; dx <= clearance; dx++)
                for (int dz = -clearance; dz <= clearance; dz++) {
                    m.set(pos.getX() + dx, pos.getY() + 1, pos.getZ() + dz);
                    if (!level.isEmptyBlock(m)) solid++;
                }
            return solid <= 2;
        }

        private static boolean nearWater(WorldGenLevel level, BlockPos center, int radius) {
            MutableBlockPos m = new MutableBlockPos();
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++)
                    for (int dy = -1; dy <= 1; dy++) {
                        m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        var st = level.getBlockState(m);
                        if (st.getBlock() == Blocks.WATER || st.getFluidState().isSource()) return true;
                    }
            return false;
        }

        private static boolean nearWoodOrPlants(WorldGenLevel level, BlockPos center, int radius) {
            MutableBlockPos m = new MutableBlockPos();
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++)
                    for (int dy = -1; dy <= 1; dy++) {
                        m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        var st = level.getBlockState(m);
                        if (st.is(BlockTags.LOGS) || st.is(BlockTags.LEAVES) || st.is(BlockTags.SAPLINGS) || st.is(BlockTags.FLOWERS)) return true;
                    }
            return false;
        }
    }
}
