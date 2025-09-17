package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Centralizes elemental placement and growth rules so both worldgen and runtime growth stay in sync.
 */
public final class ProceduralPlantElementConditions {
    private static final int WATER_RADIUS = 5;
    private static final int WOOD_RADIUS = 6;

    private ProceduralPlantElementConditions() {}

    public static boolean canSpawn(ServerLevel server, LevelAccessor level, BlockPos pos, ResourceLocation element) {
        String key = elementKey(element);
        if (key.contains("none")) {
            return noneSpawn(level, pos);
        }
        if (key.contains("fire")) {
            return fireSpawn(server, level, pos);
        }
        if (key.contains("earth")) {
            return earthSpawn(level, pos);
        }
        if (key.contains("ice")) {
            return iceSpawn(level, pos);
        }
        if (key.contains("wind")) {
            return windSpawn(level, pos);
        }
        if (key.contains("lightning")) {
            return lightningSpawn(level, pos);
        }
        if (key.contains("wood")) {
            return woodSpawn(level, pos);
        }
        if (key.contains("water")) {
            return waterSpawn(level, pos);
        }
        return true;
    }

    public static float growthModifier(Level level, BlockPos pos, PlantGenome genome) {
        if (genome == null) {
            return 1.0f;
        }
        String key = elementKey(genome.qiElement());
        if (key.contains("none")) {
            return noneGrowth(level, pos);
        }
        if (key.contains("fire")) {
            return fireGrowth(level, pos);
        }
        if (key.contains("earth")) {
            return earthGrowth(level, pos);
        }
        if (key.contains("ice")) {
            return iceGrowth(level, pos);
        }
        if (key.contains("wind")) {
            return windGrowth(level, pos);
        }
        if (key.contains("lightning")) {
            return lightningGrowth(level, pos);
        }
        if (key.contains("wood")) {
            return woodGrowth(level, pos);
        }
        if (key.contains("water")) {
            return waterGrowth(level, pos);
        }
        return 1.0f;
    }

    private static String elementKey(ResourceLocation element) {
        return element == null ? "" : element.getPath().toLowerCase();
    }

    private static boolean fireSpawn(ServerLevel server, LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        boolean preferred = isFireGround(below);
        return (preferred || server.dimensionType().ultraWarm()) && below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static boolean earthSpawn(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (level.canSeeSkyFromBelowWater(pos)) {
            return false;
        }
        return isEarthGround(below) && below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static boolean iceSpawn(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        boolean icyGround = isIceGround(below) || below.is(Blocks.SNOW_BLOCK);
        if (icyGround) {
            return below.isFaceSturdy(level, pos.below(), Direction.UP);
        }
        String biome = biomeKey(level, pos);
        return biome.contains("frozen") || biome.contains("snow");
    }

    private static boolean windSpawn(LevelAccessor level, BlockPos pos) {
        return isOpenArea(level, pos, 1) && pos.getY() > 85;
    }

    private static boolean lightningSpawn(LevelAccessor level, BlockPos pos) {
        return isOpenArea(level, pos, 1) && pos.getY() > 130;
    }

    private static boolean woodSpawn(LevelAccessor level, BlockPos pos) {
        String biome = biomeKey(level, pos);
        if (!(biome.contains("forest") || biome.contains("jungle") || biome.contains("dark_forest"))) {
            return false;
        }
        return nearWood(level, pos, WOOD_RADIUS);
    }

    private static boolean waterSpawn(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        boolean ground = isWaterGround(below);
        if (!ground || !below.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        return nearWater(level, pos, WATER_RADIUS - 1);
    }

    private static boolean noneSpawn(LevelAccessor level, BlockPos pos) {
        if (pos.getY() > 85) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        boolean ground = below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK);
        return ground && below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static float fireGrowth(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (isFireGround(below)) {
            return 1.35f;
        }
        if (level.dimensionType().ultraWarm()) {
            return 1.0f;
        }
        return 0.0f;
    }

    private static float earthGrowth(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        boolean underground = !level.canSeeSkyFromBelowWater(pos);
        if (underground && isEarthGround(below)) {
            return 1.3f;
        }
        if (isEarthGround(below)) {
            return 0.8f;
        }
        return underground ? 0.6f : 0.4f;
    }

    private static float iceGrowth(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (isIceGround(below)) {
            return 1.4f;
        }
        String biome = biomeKey(level, pos);
        if (biome.contains("frozen") || biome.contains("snow") || biome.contains("cold")) {
            return 1.1f;
        }
        return 0.5f;
    }

    private static float windGrowth(Level level, BlockPos pos) {
        boolean open = isOpenArea(level, pos, 1);
        if (open && pos.getY() > 125) {
            return 1.4f;
        }
        if (open && pos.getY() > 80) {
            return 1.0f;
        }
        return open ? 0.7f : 0.4f;
    }

    private static float lightningGrowth(Level level, BlockPos pos) {
        boolean open = isOpenArea(level, pos, 1);
        if (open && pos.getY() > 160) {
            return 1.5f;
        }
        if (open && pos.getY() > 120) {
            return 1.1f;
        }
        return open ? 0.6f : 0.3f;
    }

    private static float woodGrowth(Level level, BlockPos pos) {
        if (nearWood(level, pos, WOOD_RADIUS)) {
            return 1.35f;
        }
        String biome = biomeKey(level, pos);
        if (biome.contains("forest") || biome.contains("jungle") || biome.contains("dark_forest")) {
            return 1.05f;
        }
        return 0.6f;
    }

    private static float waterGrowth(Level level, BlockPos pos) {
        if (nearWater(level, pos, WATER_RADIUS)) {
            return 1.35f;
        }
        BlockState below = level.getBlockState(pos.below());
        if (isWaterGround(below)) {
            return 1.1f;
        }
        return 0.55f;
    }

    private static float noneGrowth(Level level, BlockPos pos) {
        return pos.getY() <= 85 ? 1.0f : 0.6f;
    }

    private static boolean isFireGround(BlockState state) {
        return state.is(Blocks.NETHERRACK) || state.is(Blocks.NETHER_BRICKS) || state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT);
    }

    private static boolean isEarthGround(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DEEPSLATE) || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND) || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.TUFF);
    }

    private static boolean isIceGround(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static boolean isWaterGround(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.CLAY) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MUD) || state.is(Blocks.STONE);
    }

    private static boolean isOpenArea(LevelAccessor level, BlockPos pos, int clearance) {
        if (!level.canSeeSkyFromBelowWater(pos)) {
            return false;
        }
        MutableBlockPos cursor = new MutableBlockPos();
        int solid = 0;
        for (int dx = -clearance; dx <= clearance; dx++) {
            for (int dz = -clearance; dz <= clearance; dz++) {
                cursor.set(pos.getX() + dx, pos.getY() + 1, pos.getZ() + dz);
                if (!level.isEmptyBlock(cursor)) {
                    solid++;
                    if (solid > 2) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean nearWater(LevelAccessor level, BlockPos center, int radius) {
        MutableBlockPos cursor = new MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    FluidState fluid = level.getFluidState(cursor);
                    if (!fluid.isEmpty() && (fluid.isSource())) {
                        return true;
                    }
                    if (level.getBlockState(cursor).getBlock() == Blocks.WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean nearWood(LevelAccessor level, BlockPos center, int radius) {
        MutableBlockPos cursor = new MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String biomeKey(LevelAccessor level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> key.location().getPath()).orElse("");
    }
}


