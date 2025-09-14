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

        public static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec spec;

        static
        {
            builder.push("First Person Rendering");
            qiSourceElementalEffects = builder.comment("Enable Qi Sources to apply their elemental effects to the world around them").define("Enable Qi Source Elemental Effects", true);
            builder.pop();

            builder.push("Procedural Plants");
            procPlantCatalogSize = builder.comment("Number of plant entries generated per world")
                    .defineInRange("catalog_size", 50, 1, 256);
            procPlantRegionSizeChunks = builder.comment("Region size in chunks used to map world positions to a catalog entry")
                    .defineInRange("region_size_chunks", 8, 1, 64);
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
    }
}
