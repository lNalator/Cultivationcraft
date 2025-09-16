package DaoOfModding.Cultivationcraft.Common;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config
{
    public static class Server
    {
        protected static final ForgeConfigSpec.ConfigValue<Boolean> qiSourceElementalEffects;
        private static ForgeConfigSpec.IntValue procPlantCatalogSize;
        private static ForgeConfigSpec.IntValue procPlantRegionSizeChunks;
        private static ForgeConfigSpec.IntValue procPlantAgeMin;
        private static ForgeConfigSpec.IntValue procPlantAgeMax;
        private static ForgeConfigSpec.DoubleValue procPlantGrowthMin;
        private static ForgeConfigSpec.DoubleValue procPlantGrowthMax;
        private static ForgeConfigSpec.IntValue procPlantHeightMin;
        private static ForgeConfigSpec.IntValue procPlantHeightMax;
        private static ForgeConfigSpec.IntValue procPlantTier3ChancePercent;
        private static ForgeConfigSpec.IntValue procPlantTier2ChancePercent;
        private static ForgeConfigSpec.IntValue procPlantPatchCapT1;
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

        static
        {
            builder.push("First Person Rendering");
            qiSourceElementalEffects = builder.comment("Enable Qi Sources to apply their elemental effects to the world around them")
                        .define("Enable Qi Source Elemental Effects", true);
            builder.pop();

            builder.push("Procedural Plants");
            builder.push("catalog");
                procPlantCatalogSize = builder.comment("Number of plant entries generated per world")
                        .defineInRange("catalog_size", 50, 1, 256);
                procPlantRegionSizeChunks = builder.comment("Region size in chunks used to map world positions to a catalog entry")
                        .defineInRange("region_size_chunks", 8, 1, 64);
            builder.pop();
            builder.push("genome");
                procPlantAgeMin = builder.comment("Minimum maxAge for plants")
                        .defineInRange("age_min", 3, 1, 10);
                procPlantAgeMax = builder.comment("Maximum maxAge for plants")
                        .defineInRange("age_max", 5, 1, 15);
                procPlantGrowthMin = builder.comment("Minimum per-tick growth chance")
                        .defineInRange("growth_chance_min", 0.05D, 0.0D, 1.0D);
                procPlantGrowthMax = builder.comment("Maximum per-tick growth chance")
                        .defineInRange("growth_chance_max", 0.15D, 0.0D, 1.0D);
                procPlantHeightMin = builder.comment("Minimum visual height in pixels")
                        .defineInRange("height_min", 10, 1, 64);
                procPlantHeightMax = builder.comment("Maximum visual height in pixels")
                        .defineInRange("height_max", 22, 1, 64);
            builder.pop();
            builder.push("catalog_tiers");
                procPlantTier3ChancePercent = builder.comment("Percent chance of tier 3 (epic) entries in catalog")
                        .defineInRange("tier3_percent", 5, 0, 100);
                procPlantTier2ChancePercent = builder.comment("Percent chance of tier 2 (rare) entries in catalog; rest are tier 1")
                        .defineInRange("tier2_percent", 25, 0, 100);
            builder.pop();
            builder.push("patches");
                procPlantPatchCapT1 = builder.comment("Max plants placed per patch for tier 1")
                        .defineInRange("patch_cap_t1", 3, 1, 16);
            builder.pop();
            builder.push("growth");
                procPlantGrowthBoostQiAny = builder.comment("Growth multiplier when any Qi Source is nearby")
                        .defineInRange("growth_boost_qi_any", 1.2D, 1.0D, 10.0D);
                procPlantGrowthBoostQiMatch = builder.comment("Growth multiplier when matching-element Qi Source is nearby")
                        .defineInRange("growth_boost_qi_match", 1.8D, 1.0D, 10.0D);
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

        public static boolean qiSourceElementalEffectsOn()
        {
            if (qiSourceElementalEffects.get())
                return true;

            return false;
        }

        public static int procPlantCatalogSize() { return procPlantCatalogSize.get(); }
        public static int procPlantRegionSizeChunks() { return procPlantRegionSizeChunks.get(); }
        public static int procPlantAgeMin() { return procPlantAgeMin.get(); }
        public static int procPlantAgeMax() { return procPlantAgeMax.get(); }
        public static double procPlantGrowthMin() { return procPlantGrowthMin.get(); }
        public static double procPlantGrowthMax() { return procPlantGrowthMax.get(); }
        public static int procPlantHeightMin() { return procPlantHeightMin.get(); }
        public static int procPlantHeightMax() { return procPlantHeightMax.get(); }
        public static int procPlantTier3ChancePercent() { return procPlantTier3ChancePercent.get(); }
        public static int procPlantTier2ChancePercent() { return procPlantTier2ChancePercent.get(); }
        public static int procPlantPatchCapT1() { return procPlantPatchCapT1.get(); }
        public static double procPlantGrowthBoostQiAny() { return procPlantGrowthBoostQiAny.get(); }
        public static double procPlantGrowthBoostQiMatch() { return procPlantGrowthBoostQiMatch.get(); }
        public static int procPlantQiGrowthRadius() { return procPlantQiGrowthRadius.get(); }

        // Element spawn multipliers getters
        public static double spawnMultFire() { return spawnMultFire.get(); }
        public static double spawnMultEarth() { return spawnMultEarth.get(); }
        public static double spawnMultWood() { return spawnMultWood.get(); }
        public static double spawnMultWind() { return spawnMultWind.get(); }
        public static double spawnMultWater() { return spawnMultWater.get(); }
        public static double spawnMultIce() { return spawnMultIce.get(); }
        public static double spawnMultLightning() { return spawnMultLightning.get(); }
        public static double spawnMultNone() { return spawnMultNone.get(); }
    }
}
