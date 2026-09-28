package lNalator.Cultivationcraft.Common;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    public static class Server {

        protected static final ForgeConfigSpec.ConfigValue<Boolean> qiSourceElementalEffects;
        private static ForgeConfigSpec.IntValue procPlantCatalogSize;
        private static ForgeConfigSpec.IntValue procPlantRegionSizeChunks;
        private static ForgeConfigSpec.IntValue procPlantTier3ChancePercent;
        private static ForgeConfigSpec.IntValue procPlantTier2ChancePercent;
        private static ForgeConfigSpec.IntValue procPlantPatchCapT1;
        private static ForgeConfigSpec.IntValue procPlantPatchAttempts;
        private static ForgeConfigSpec.IntValue procPlantPlacementBudget;
        private static ForgeConfigSpec.IntValue procPlantCaveScanSteps;
        private static ForgeConfigSpec.DoubleValue procPlantPatchTier2Percent;
        private static ForgeConfigSpec.DoubleValue procPlantPatchTier3Percent;
        private static ForgeConfigSpec.DoubleValue procPlantGrowthBoostQiAny;
        private static ForgeConfigSpec.DoubleValue procPlantGrowthBoostQiMatch;
        private static ForgeConfigSpec.IntValue procPlantQiGrowthRadius;

        // Per-element spawn multipliers
        private static ForgeConfigSpec.DoubleValue spawnMultFire;
        private static ForgeConfigSpec.DoubleValue spawnMultEarth;
        private static ForgeConfigSpec.DoubleValue spawnMultWood;
        private static ForgeConfigSpec.DoubleValue spawnMultWind;
        private static ForgeConfigSpec.DoubleValue spawnMultWater;
        private static ForgeConfigSpec.DoubleValue spawnMultIce;
        private static ForgeConfigSpec.DoubleValue spawnMultLightning;
        private static ForgeConfigSpec.DoubleValue spawnMultNone;

        public static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec spec;

        static {
            builder.push("First Person Rendering");
            qiSourceElementalEffects = builder.comment("Enable Qi Sources to apply their elemental effects to the world around them")
                    .define("Enable Qi Source Elemental Effects", true);
            builder.pop();

            builder.push("Procedural Plants");
            builder.push("catalog");
            procPlantCatalogSize = builder.comment("Number of plant entries generated per world")
                    .defineInRange("catalog_size", 50, 43, 64);
            procPlantRegionSizeChunks = builder.comment("Region size in chunks used to map world positions to a catalog entry")
                    .defineInRange("region_size_chunks", 8, 1, 64);
            builder.pop();
            builder.push("catalog_tiers");
            procPlantTier3ChancePercent = builder.comment("Percent chance of tier 3 neutral catalog entries; elemental entries have guaranteed tier coverage")
                    .defineInRange("tier3_percent", 5, 0, 100);
            procPlantTier2ChancePercent = builder.comment("Percent chance of tier 2 neutral catalog entries; remaining entries are tier 1")
                    .defineInRange("tier2_percent", 25, 0, 100);
            builder.pop();
            builder.push("patches");
            procPlantPatchCapT1 = builder.comment("Max plants placed per patch for tier 1")
                    .defineInRange("patch_cap_t1", 12, 1, 16);
            procPlantPatchAttempts = builder.comment("Maximum patch centers tried per feature invocation")
                    .defineInRange("patch_attempts", 10, 1, 16);
            procPlantPlacementBudget = builder.comment("Maximum individual plant placement attempts across all patches in one invocation")
                    .defineInRange("placement_budget", 48, 1, 128);
            procPlantCaveScanSteps = builder.comment("Maximum downward steps when finding a cave or Nether patch center")
                    .defineInRange("cave_scan_steps", 32, 1, 64);
            procPlantPatchTier2Percent = builder.defineInRange("patch_tier2_percent", 5.0D, 0.0D, 100.0D);
            procPlantPatchTier3Percent = builder.defineInRange("patch_tier3_percent", 0.5D, 0.0D, 100.0D);
            builder.pop();
            builder.push("growth");
            procPlantGrowthBoostQiAny = builder.comment("Additional growth per plant tier when any Qi Source is nearby")
                    .defineInRange("growth_boost_qi_any", 4.0D, 0.0D, 10.0D);
            procPlantGrowthBoostQiMatch = builder.comment("Additional growth per plant tier when a matching Qi Source is nearby")
                    .defineInRange("growth_boost_qi_match", 6.0D, 0.0D, 10.0D);
            procPlantQiGrowthRadius = builder.comment("Radius (blocks) to search for Qi Sources to boost growth")
                    .defineInRange("qi_growth_radius", 16, 1, 128);
            builder.pop();
            builder.push("Element Spawn Multipliers");
            spawnMultFire = builder.defineInRange("fire", 1.0D, 0.0D, 10.0D);
            spawnMultEarth = builder.defineInRange("earth", 1.0D, 0.0D, 10.0D);
            spawnMultWood = builder.defineInRange("wood", 1.0D, 0.0D, 10.0D);
            spawnMultWind = builder.defineInRange("wind", 1.0D, 0.0D, 10.0D);
            spawnMultWater = builder.defineInRange("water", 1.0D, 0.0D, 10.0D);
            spawnMultIce = builder.defineInRange("ice", 1.0D, 0.0D, 10.0D);
            spawnMultLightning = builder.defineInRange("lightning", 1.0D, 0.0D, 10.0D);
            spawnMultNone = builder.defineInRange("none", 0.50D, 0.0D, 10.0D);
            builder.pop();
            builder.pop();

            spec = builder.build();
        }

        public static boolean qiSourceElementalEffectsOn() {
            if (qiSourceElementalEffects.get()) {
                return true;
            }

            return false;
        }

        public static int procPlantCatalogSize() {
            return procPlantCatalogSize.get();
        }

        public static int procPlantRegionSizeChunks() {
            return procPlantRegionSizeChunks.get();
        }

        public static int procPlantTier3ChancePercent() {
            return procPlantTier3ChancePercent.get();
        }

        public static int procPlantTier2ChancePercent() {
            return procPlantTier2ChancePercent.get();
        }

        public static int procPlantPatchAttempts() { return procPlantPatchAttempts.get(); }
        public static int procPlantPlacementBudget() { return procPlantPlacementBudget.get(); }
        public static int procPlantCaveScanSteps() { return procPlantCaveScanSteps.get(); }
        public static double procPlantPatchTier2Percent() { return procPlantPatchTier2Percent.get(); }
        public static double procPlantPatchTier3Percent() { return procPlantPatchTier3Percent.get(); }

        public static int procPlantPatchCapT1() {
            return procPlantPatchCapT1.get();
        }

        public static double procPlantGrowthBoostQiAny() {
            return procPlantGrowthBoostQiAny.get();
        }

        public static double procPlantGrowthBoostQiMatch() {
            return procPlantGrowthBoostQiMatch.get();
        }

        public static int procPlantQiGrowthRadius() {
            return procPlantQiGrowthRadius.get();
        }

        // Element spawn multipliers getters
        public static double spawnMultFire() {
            return spawnMultFire.get();
        }

        public static double spawnMultEarth() {
            return spawnMultEarth.get();
        }

        public static double spawnMultWood() {
            return spawnMultWood.get();
        }

        public static double spawnMultWind() {
            return spawnMultWind.get();
        }

        public static double spawnMultWater() {
            return spawnMultWater.get();
        }

        public static double spawnMultIce() {
            return spawnMultIce.get();
        }

        public static double spawnMultLightning() {
            return spawnMultLightning.get();
        }

        public static double spawnMultNone() {
            return spawnMultNone.get();
        }
    }
}
