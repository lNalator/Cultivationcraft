package DaoOfModding.Cultivationcraft.Common.Worldgen;

import com.mojang.serialization.Codec;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.utils.Seeds;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.ProceduralPlantElementConditions;
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
        int tries = 10; // few attempts to reduce overall density
        int radiusXZ = 6;
        int ySpread = 2;

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
            String elemKey = chooseElementKeyForPatch(server, rng);
            int id = pickRandomIdByElementStable(catalog, elemKey, server.getSeed(), regionPos);
            if (id < 0) {
                id = Seeds.forPos(server.getSeed(), regionPos, 0xC0FFEE).nextInt(Math.max(1, catalog.size()));
            }
            var entry = catalog.getById(id);
            if (entry == null) continue;
            var g = entry.genome;

            // If EARTH plant, bias search to find an underground cavity with solid floor
            if (contains(g.qiElement().getPath(), "earth")) {
                BlockPos cave = findUndergroundPlacementNear(level, pos);
                if (cave != null) pos = cave; else continue;
            }

            if (!level.isEmptyBlock(pos)) continue; // must place into air

            if (!ProceduralPlantElementConditions.canSpawn(server, level, pos, g.qiElement())) continue;

            // Choose a pre-aging pattern for this patch, with optional bias near QiSources
            int patchTier = choosePatchTier(rng); // 1,2,3 represent age ranges

            // Patch size policy: smaller overall for performance
            int count;
            int radius;
            boolean isNone = g.qiElement() != null && g.qiElement().getPath().contains("none");
            if (patchTier == 3) { // T3: isolated singles
                count = 1;
                radius = 0;
            } else if (patchTier == 2) { // T2: small group, wider spread
                count = 2 + rng.nextInt(3); // 2..4
                radius = 5 + rng.nextInt(3); // 5..7
            } else { // T1: compact cluster ~6..10
                int target = 6 + rng.nextInt(5); // 6..10
                double mult = elementSpawnMult(g.qiElement().getPath());
                count = (int)Math.round(target * mult);
                if (isNone) { if (count < 2) count = 2; if (count > 6) count = 6; }
                else { if (count < 4) count = 4; if (count > 12) count = 12; }
                radius = 3 + rng.nextInt(2); // 3..4
            }
            for (int n = 0; n < count; n++) {
                int ox = rng.nextInt(radius * 2 + 1) - radius;
                int oz = rng.nextInt(radius * 2 + 1) - radius;
                int oy = rng.nextInt(2) - 1;
                BlockPos p = pos.offset(ox, oy, oz);

                // Re-adjust to surface for non-earth plants; earth stays underground
                BlockPos pp = contains(g.qiElement().getPath(), "earth") ? adjustToCaveAir(level, p) : findSurfaceAirAboveSolid(level, p);
                if (pp == null) continue;
                if (!ProceduralPlantElementConditions.canSpawn(server, level, pp, g.qiElement())) continue;

                if (level.isEmptyBlock(pp)) {
                    int initGrowth = switch (patchTier) {
                        case 3 -> 1000 + rng.nextInt(9000);      // 1000..9999 (Tier 3)
                        case 2 -> 100 + rng.nextInt(900);       // 100..999  (Tier 2)
                        default -> rng.nextInt(100);            // 0..99     (Tier 1)
                    };
                    int tier = ProceduralPlantBlockEntity.growthToTier(initGrowth);
                    boolean host = tier >= 3;

                    BlockState state = BlockRegister.PROCEDURAL_PLANT.get()
                            .defaultBlockState()
                            .setValue(ProceduralPlantBlock.SPECIES, id)
                            .setValue(ProceduralPlantBlock.TIER, tier)
                            .setValue(ProceduralPlantBlock.HOST_QI, host);

                    level.setBlock(pp, state, 2);
                    // Pre-growth: set BE dynamic spiritual growth; Tier 3 hosts pick up Qi on load.
                    if (level.getBlockEntity(pp) instanceof ProceduralPlantBlockEntity be) {
                        be.setSpiritualGrowth(initGrowth);
                    }
                    placed++;
                }
            }
        }

        return placed > 0;
    }

    private int choosePatchTier(RandomSource rng) {
        double r = rng.nextDouble();
        if (r < 0.005) return 3;    // 0.5% T3 (epic)
        if (r < 0.055) return 2;   // 5.5% T2 (rare)
        return 1;               // 94% T1 (common)
    }

    private static boolean contains(String s, String k) { return s != null && s.contains(k); }

    private String chooseElementKeyForPatch(ServerLevel server, RandomSource rng) {
        boolean inNether = server.dimension() == Level.NETHER;
        if (rng.nextFloat() >= 0.84f) return "none";
        String[] elems = inNether
                ? new String[]{"fire"}
                : new String[]{"earth", "wood", "wind", "water", "ice", "lightning"};
        return elems[rng.nextInt(elems.length)];
    }

    private int pickRandomIdByElementStable(PlantCatalogSavedData catalog, String elementKey, long worldSeed, BlockPos regionPos) {
        java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
        for (var e : catalog.entries()) {
            String path = e.genome.qiElement().getPath();
            if (elementKey == null || elementKey.isEmpty() || path.contains(elementKey)) ids.add(e.id);
        }
        if (ids.isEmpty()) return -1;
        int salt = elementKey == null ? 0 : elementKey.hashCode();
        RandomSource srng = Seeds.forPos(worldSeed, regionPos, 0xC0FFEE ^ salt);
        return ids.get(srng.nextInt(ids.size()));
    }

    private BlockPos findSurfaceAirAboveSolid(WorldGenLevel level, BlockPos start) {
        MutableBlockPos m = new MutableBlockPos(start.getX(), start.getY(), start.getZ());
        // walk down some steps to find ground
        for (int i = 0; i < 64 && m.getY() > level.getMinBuildHeight(); i++) {
            if (!level.isEmptyBlock(m)) {
                // climb to first air above the solid stack
                while (!level.isEmptyBlock(m.above())) {
                    m.move(Direction.UP);
                    if (m.getY() >= level.getMaxBuildHeight()) return null;
                }
                BlockPos place = m.above();
                BlockState below = level.getBlockState(place.below());
                if (below.isFaceSturdy(level, place.below(), Direction.UP)
                        && net.minecraft.world.level.block.Block.isFaceFull(below.getCollisionShape(level, place.below()), Direction.UP))
                    return place.immutable();
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
                if (!below.isAir() && below.isFaceSturdy(level, m.below(), Direction.UP)
                        && net.minecraft.world.level.block.Block.isFaceFull(below.getCollisionShape(level, m.below()), Direction.UP)) return m.immutable();
            }
            m.move(Direction.DOWN);
        }
        return null;
    }

    private BlockPos adjustToCaveAir(WorldGenLevel level, BlockPos pos) {
        // If already a valid underground air above sturdy floor, keep
        if (level.isEmptyBlock(pos) && !level.canSeeSkyFromBelowWater(pos)) {
            var below = level.getBlockState(pos.below());
            if (!below.isAir() && below.isFaceSturdy(level, pos.below(), Direction.UP)
                    && net.minecraft.world.level.block.Block.isFaceFull(below.getCollisionShape(level, pos.below()), Direction.UP)) return pos;
        }
        return findUndergroundPlacementNear(level, pos);
    }

    private double elementSpawnMult(String path) {
        if (path == null) return 1.0;
        if (path.contains("fire")) return Config.Server.spawnMultFire();
        if (path.contains("earth")) return Config.Server.spawnMultEarth();
        if (path.contains("wood")) return Config.Server.spawnMultWood();
        if (path.contains("wind")) return Config.Server.spawnMultWind();
        if (path.contains("water")) return Config.Server.spawnMultWater();
        if (path.contains("ice")) return Config.Server.spawnMultIce();
        if (path.contains("lightning")) return Config.Server.spawnMultLightning();
        if (path.contains("none")) return Config.Server.spawnMultNone();
        return 1.0;
    }
}





