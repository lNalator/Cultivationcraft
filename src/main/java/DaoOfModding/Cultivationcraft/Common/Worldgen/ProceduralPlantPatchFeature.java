package DaoOfModding.Cultivationcraft.Common.Worldgen;

import com.mojang.serialization.Codec;

import DaoOfModding.Cultivationcraft.Common.Config;
import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSource;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSourceConfig;
import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenome;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.utils.Seeds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
        int tries = 64;                 // base attempts per patch
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
            if (!canPlaceHereForElement(level, pos, g)) continue;
            if (!passesElementRules(server, level, pos, g)) continue;

            // Rarity weighting by tier (skip QiSource proximity to avoid cross-chunk loads during worldgen)
            double baseChance = switch (g.tier()) { case 3 -> Config.Server.procPlantRarityT3(); case 2 -> Config.Server.procPlantRarityT2(); default -> Config.Server.procPlantRarityT1(); };
            // Per-element spawn multiplier
            baseChance *= elementSpawnMult(g.qiElement().getPath());
            // Environmental boosts by element and biome/height
            baseChance *= environmentSpawnBoost(level, pos, g);
            if (rng.nextDouble() > baseChance) continue;

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

    private boolean canPlaceHereForElement(WorldGenLevel level, BlockPos pos, PlantGenome g) {
        // must be air at pos
        if (!level.isEmptyBlock(pos)) return false;
        var below = level.getBlockState(pos.below());
        String path = g.qiElement().getPath();
        boolean soilOk = below.is(BlockTags.DIRT) || below.is(Blocks.FARMLAND);
        if (path.contains("earth")) soilOk = soilOk || below.is(Blocks.STONE) || below.is(Blocks.DEEPSLATE) || below.is(Blocks.GRAVEL) || below.is(Blocks.SAND);
        if (path.contains("water")) soilOk = soilOk || below.is(Blocks.SAND) || below.is(Blocks.CLAY);
        if (!soilOk) return false;
        int light = level.getMaxLocalRawBrightness(pos);
        return light >= 7;
    }

    private boolean passesElementRules(ServerLevel server, WorldGenLevel level, BlockPos pos, PlantGenome g) {
        var el = Elements.getElement(g.qiElement());
        String path = g.qiElement().getPath();

        // Fire: Nether only
        if (path.contains("fire")) {
            if (server.dimension() != Level.NETHER) return false;
        }

        // Cold preference: skip hot biomes for cold-favored
        float temp = level.getBiome(pos).value().getBaseTemperature();
        if (g.spawnsInCold() && temp > 0.8f) return false;
        if (!g.spawnsInCold() && path.contains("fire") && temp < 0.5f) return false;

        // Proximity/location rules by element
        if (path.contains("water")) {
            if (!nearWater(level, pos, 3)) return false;
        }
        if (path.contains("ice")) {
            if (temp > 0.3f) return false;
        }
        if (path.contains("wind") || path.contains("air") || path.contains("lightning")) {
            int sea = level.getLevel().getSeaLevel();
            if (pos.getY() < sea + 40) return false;
            int sky = level.getMaxLocalRawBrightness(pos);
            if (path.contains("lightning") && sky < 12) return false;
        }
        if (path.contains("earth")) {
            // prefer stone-like; allow placement only if block below is stone/stone-like
            var below = level.getBlockState(pos.below());
            if (!(below.is(Blocks.STONE) || below.is(Blocks.DEEPSLATE) || below.is(Blocks.GRAVEL) || below.is(Blocks.SAND) || below.is(BlockTags.DIRT))) return false;
        }
        if (path.contains("wood")) {
            // mild preference: require moderate light
            if (level.getMaxLocalRawBrightness(pos) < 9) return false;
        }
        return true;
    }

    private boolean nearWater(WorldGenLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var st = level.getBlockState(m);
                    if (st.getBlock() == Blocks.WATER || st.getFluidState().isSource()) return true;
                }
            }
        }
        return false;
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

    private double environmentSpawnBoost(WorldGenLevel level, BlockPos pos, PlantGenome g) {
        String path = g.qiElement().getPath();
        double mult = 1.0;
        var biomeKeyOpt = level.getBiome(pos).unwrapKey();
        String biomePath = biomeKeyOpt.map(k -> k.location().getPath()).orElse("");
        int sea = level.getLevel().getSeaLevel();
        // Earth: better underground and in lush caves
        if (path.contains("earth")) {
            if (pos.getY() < sea - 10) mult *= 1.25;
            if (biomePath.contains("lush_caves")) mult *= 1.5;
        }
        // Ice: better in frozen/snowy
        if (path.contains("ice")) {
            if (biomePath.contains("frozen") || biomePath.contains("snow")) mult *= 1.5;
        }
        // Wind/Air: prefer windswept/plains and higher altitude, but allow low if open
        if (path.contains("wind") || path.contains("air")) {
            if (biomePath.contains("windswept") || biomePath.contains("plains")) mult *= 1.3;
            if (pos.getY() > sea + 40) mult *= 1.25; else if (isOpenArea(level, pos)) mult *= 1.1;
        }
        // Lightning: small mountain bias
        if (path.contains("lightning")) {
            if (biomePath.contains("mountain") || biomePath.contains("peaks") || biomePath.contains("hills")) mult *= 1.3;
        }
        // Wood: forests, jungles, dark forests
        if (path.contains("wood")) {
            if (biomePath.contains("forest")) mult *= 1.3;
            if (biomePath.contains("jungle") || biomePath.contains("dark_forest")) mult *= 1.5;
        }
        // Water: ocean/river/swamp/beach; near large ponds
        if (path.contains("water")) {
            if (biomePath.contains("ocean") || biomePath.contains("river") || biomePath.contains("swamp") || biomePath.contains("beach")) mult *= 1.5;
            if (nearWater(level, pos, 5)) mult *= 1.2;
        }
        return mult;
    }

    private boolean isOpenArea(WorldGenLevel level, BlockPos pos) {
        int solid = 0; int checked = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                m.set(pos.getX()+dx, pos.getY()+1, pos.getZ()+dz);
                checked++;
                if (!level.isEmptyBlock(m)) solid++;
            }
        return solid <= 2; // mostly open
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
