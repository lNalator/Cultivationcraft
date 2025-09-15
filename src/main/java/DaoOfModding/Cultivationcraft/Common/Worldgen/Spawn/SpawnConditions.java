package DaoOfModding.Cultivationcraft.Common.Worldgen.Spawn;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SpawnConditions {
    private SpawnConditions() {}

    public static ElementSpawnCondition forPath(String elementPath) {
        if (contains(elementPath, "fire")) return FIRE;
        if (contains(elementPath, "earth")) return EARTH;
        if (contains(elementPath, "wood")) return WOOD;
        if (contains(elementPath, "wind") || contains(elementPath, "air")) return WIND;
        if (contains(elementPath, "water")) return WATER;
        if (contains(elementPath, "ice")) return ICE;
        if (contains(elementPath, "lightning")) return LIGHTNING;
        return NONE;
    }

    private static boolean contains(String path, String key) { return path != null && path.contains(key); }

    public static final ElementSpawnCondition NONE = new ElementSpawnCondition() {
        public boolean canPlaceOn(WorldGenLevel level, BlockPos pos, PlantGenome g) { 
            BlockState below = level.getBlockState(pos.below());
            boolean ground = below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.ROOTED_DIRT)
                    || below.is(Blocks.PODZOL) || below.is(Blocks.MOSS_BLOCK);
            return ground && below.isFaceSturdy(level, pos.below(), Direction.UP);
         }
        public boolean extraRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g) {
            int sea = level.getLevel().getSeaLevel();
            // Only underground and not sky-exposed
            if (pos.getY() >= sea - 5) return false;
            if (level.canSeeSkyFromBelowWater(pos)) return false;
            return true;
        }
        public double environmentBoost(WorldGenLevel level, BlockPos pos, PlantGenome g) { return 1.0; }
    };

    public static final ElementSpawnCondition EARTH = new ElementSpawnCondition() {
        public boolean canPlaceOn(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            BlockState below = level.getBlockState(pos.below());
            // Spawn on stone-like and groundy materials with sturdy top surface
            boolean ground = below.is(Blocks.STONE) || below.is(Blocks.GRANITE) || below.is(Blocks.DIORITE) || below.is(Blocks.ANDESITE)
                    || below.is(Blocks.DEEPSLATE) || below.is(Blocks.GRAVEL) || below.is(Blocks.SAND) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.ROOTED_DIRT) || below.is(Blocks.TUFF);
            return ground && below.isFaceSturdy(level, pos.below(), Direction.UP);
        }
        public boolean extraRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g) { return true; }
        public double environmentBoost(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            int sea = level.getLevel().getSeaLevel();
            double mult = 1.0;
            if (pos.getY() < sea - 10) mult *= 1.25;
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (biome.contains("lush_caves")) mult *= 1.5;
            return mult;
        }
    };

    public static final ElementSpawnCondition ICE = new ElementSpawnCondition() {
        public boolean canPlaceOn(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            BlockState below = level.getBlockState(pos.below());
            return (below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE) || below.is(Blocks.BLUE_ICE) || below.is(Blocks.FROSTED_ICE) || below.is(Blocks.SNOW_BLOCK))
                    && below.isFaceSturdy(level, pos.below(), Direction.UP);
        }
        public boolean extraRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g) {
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            return biome.contains("frozen") || biome.contains("snow");
        }
        public double environmentBoost(WorldGenLevel level, BlockPos pos, PlantGenome g) { return 1.3; }
    };

    public static final ElementSpawnCondition FIRE = new ElementSpawnCondition() {
        public boolean canPlaceOn(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            BlockState below = level.getBlockState(pos.below());
            return (below.is(Blocks.NETHERRACK) || below.is(Blocks.CRIMSON_NYLIUM) || below.is(Blocks.WARPED_NYLIUM)
                    || below.is(Blocks.BASALT) || below.is(Blocks.BLACKSTONE) || below.is(Blocks.SOUL_SAND) || below.is(Blocks.SOUL_SOIL))
                    && below.isFaceSturdy(level, pos.below(), Direction.UP);
        }
        public boolean extraRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g) {
            return server.dimension() == Level.NETHER;
        }
        public double environmentBoost(WorldGenLevel level, BlockPos pos, PlantGenome g) { return 1.0; }
    };

    public static final ElementSpawnCondition WIND = new ElementSpawnCondition() {
        public boolean canPlaceOn(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            BlockState below = level.getBlockState(pos.below());
            boolean ground = below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.SAND) || below.is(Blocks.GRAVEL)
                    || below.is(Blocks.STONE) || below.is(Blocks.DEEPSLATE);
            return ground && below.isFaceSturdy(level, pos.below(), Direction.UP);
        }
        public boolean extraRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g) { return isOpenArea(level, pos); }
        public double environmentBoost(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            int sea = level.getLevel().getSeaLevel();
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            double mult = 1.0;
            if (biome.contains("windswept") || biome.contains("plains")) mult *= 1.3;
            if (pos.getY() > sea + 40) mult *= 1.25; else if (isOpenArea(level, pos)) mult *= 1.1;
            return mult;
        }
    };

    public static final ElementSpawnCondition LIGHTNING = new ElementSpawnCondition() {
        public boolean canPlaceOn(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            BlockState below = level.getBlockState(pos.below());
            boolean ground = below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.SAND) || below.is(Blocks.GRAVEL)
                    || below.is(Blocks.STONE) || below.is(Blocks.DEEPSLATE);
            return ground && below.isFaceSturdy(level, pos.below(), Direction.UP) && isOpenArea(level, pos);
        }
        public boolean extraRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g) {
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            return biome.contains("mountain") || biome.contains("peaks") || biome.contains("hills") || biome.contains("windswept");
        }
        public double environmentBoost(WorldGenLevel level, BlockPos pos, PlantGenome g) { return 1.2; }
    };

    public static final ElementSpawnCondition WOOD = new ElementSpawnCondition() {
        public boolean canPlaceOn(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            BlockState below = level.getBlockState(pos.below());
            boolean ground = below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.ROOTED_DIRT)
                    || below.is(Blocks.PODZOL) || below.is(Blocks.MOSS_BLOCK);
            return ground && below.isFaceSturdy(level, pos.below(), Direction.UP);
        }
        public boolean extraRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g) {
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            return biome.contains("forest") || biome.contains("jungle");
        }
        public double environmentBoost(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (biome.contains("dark_forest") || biome.contains("jungle")) return 1.5;
            if (biome.contains("forest")) return 1.3;
            return 1.0;
        }
    };

    public static final ElementSpawnCondition WATER = new ElementSpawnCondition() {
        public boolean canPlaceOn(WorldGenLevel level, BlockPos pos, PlantGenome g) {
            BlockState below = level.getBlockState(pos.below());
            boolean ground = below.is(Blocks.SAND) || below.is(Blocks.CLAY) || below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.MUD);
            return ground && below.isFaceSturdy(level, pos.below(), Direction.UP);
        }
        public boolean extraRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g) {
            // near water or in water biomes
            String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
            return biome.contains("ocean") || biome.contains("river") || biome.contains("swamp") || biome.contains("beach") || nearWater(level, pos, 3);
        }
        public double environmentBoost(WorldGenLevel level, BlockPos pos, PlantGenome g) { return nearWater(level, pos, 5) ? 1.2 : 1.0; }
    };

    // Utilities
    private static boolean isOpenArea(WorldGenLevel level, BlockPos pos) {
        int solid = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                m.set(pos.getX()+dx, pos.getY()+1, pos.getZ()+dz);
                if (!level.isEmptyBlock(m)) solid++;
            }
        return solid <= 2;
    }

    private static boolean nearWater(WorldGenLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                for (int dy = -1; dy <= 1; dy++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var st = level.getBlockState(m);
                    if (st.getBlock() == Blocks.WATER || st.getFluidState().isSource()) return true;
                }
        return false;
    }
}
