package DaoOfModding.Cultivationcraft.Common.Worldgen;

import com.mojang.serialization.Codec;

import DaoOfModding.Cultivationcraft.Common.Config;
import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Worldgen.Spawn.ElementSpawnCondition;
import DaoOfModding.Cultivationcraft.Common.Worldgen.Spawn.SpawnConditions;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.utils.Seeds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.Direction;
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
        int tries = 64;                 // scan multiple positions; place at most one patch per chunk
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

            // Choose a species id from per-world catalog, stable per region
            var server = level.getLevel();
            PlantCatalogSavedData catalog = PlantCatalogSavedData.getOrCreate(server, Config.Server.procPlantCatalogSize());
            int regionSize = Config.Server.procPlantRegionSizeChunks() * 16;
            BlockPos regionPos = new BlockPos(pos.getX() / regionSize, 0, pos.getZ() / regionSize);
            int id = Seeds.forPos(server.getSeed(), regionPos, 0xC0FFEE).nextInt(Math.max(1, catalog.size()));
            var entry = catalog.getById(id);
            if (entry == null) continue;
            var g = entry.genome;
            ElementSpawnCondition cond = SpawnConditions.forPath(g.qiElement().getPath());
            boolean allowDark = g.qiElement().getPath().contains("earth") || g.qiElement().getPath().contains("wood");
            if (!basicAirAndLight(level, pos, allowDark)) continue;
            if (!cond.canPlaceOn(level, pos, g)) continue;
            if (!cond.extraRules(server, level, pos, g)) continue;

            // Guaranteed spawn if conditions are met: skip chance gating
            // We still allow per-element environmentBoost to influence patch size later if needed.

            // Patch cap 1-4 based on tier (higher tier smaller patches). Non-element (none) can be larger
            int patchCapBase = switch (g.tier()) { case 3 -> Config.Server.procPlantPatchCapT3(); case 2 -> Config.Server.procPlantPatchCapT2(); default -> Config.Server.procPlantPatchCapT1(); };
            boolean isNone = g.qiElement().getPath().contains("none");
            int patchCap = isNone ? patchCapBase + Config.Server.procPlantPatchCapNoneBonus() : patchCapBase;
            if (placed >= patchCap) break;

            // Mutation: small chance for wind → lightning swap
            if (g.qiElement().getPath().contains("wind") || g.qiElement().getPath().contains("air")) {
                if (rng.nextDouble() < 0.05) {
                    int lightningId = pickRandomIdByElement(catalog, "lightning");
                    if (lightningId >= 0) id = lightningId;
                }
            }

            // place plant block at pos if air
            if (level.isEmptyBlock(pos)) {
                boolean host = false;
                if (g.tier() == 3) {
                    boolean none = g.qiElement().getPath().contains("none");
                    double chance = none ? Config.Server.procPlantHostChanceNone() : Config.Server.procPlantHostChanceElement();
                    host = rng.nextDouble() < chance;
                }
                var state = BlockRegister.PROCEDURAL_PLANT.get().defaultBlockState().setValue(ProceduralPlantBlock.SPECIES, id).setValue(ProceduralPlantBlock.HOST_QI, host);
                level.setBlock(pos, state, 2);
                // Host QiSource will be created on block entity load or on player placement.
                placed++;
            }
            if (placed > 0) break; // one patch max per chunk
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

    private boolean basicAirAndLight(WorldGenLevel level, BlockPos pos, boolean allowDark) {
        if (!level.isEmptyBlock(pos)) return false;
        if (allowDark) return true;
        int light = level.getMaxLocalRawBrightness(pos);
        return light >= 7;
    }

    private double elementSpawnMult(String path) {
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

    private int pickRandomIdByElement(PlantCatalogSavedData catalog, String elementSubstr) {
        java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
        for (var e : catalog.entries()) {
            if (e.genome.qiElement().getPath().contains(elementSubstr)) ids.add(e.id);
        }
        if (ids.isEmpty()) return -1;
        return ids.get((int)(Math.random() * ids.size()));
    }
}
