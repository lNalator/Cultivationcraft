package lNalator.Cultivationcraft.Common;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    public static class Server {

        protected static final ForgeConfigSpec.ConfigValue<Boolean> qiSourceElementalEffects;
        private static final ForgeConfigSpec.IntValue stabiliseGoalBase;
        private static final ForgeConfigSpec.IntValue stabiliseGoalStep;
        private static final ForgeConfigSpec.DoubleValue pressureDamageMultiplier;
        private static final ForgeConfigSpec.IntValue echoStep;
        private static final ForgeConfigSpec.DoubleValue ownElementEfficiency;
        private static final ForgeConfigSpec.DoubleValue variantOwnElementEfficiency;
        private static final ForgeConfigSpec.DoubleValue otherElementEfficiency;
        private static final ForgeConfigSpec.DoubleValue pressingElementEfficiency;
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
        private static ForgeConfigSpec.DoubleValue qiSourceMinBonus;
        private static ForgeConfigSpec.DoubleValue qiSourceMaxBonus;

        public static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec spec;

        static {
            builder.push("First Person Rendering");
            qiSourceElementalEffects = builder.comment("Enable Qi Sources to apply their elemental effects to the world around them")
                    .define("Enable Qi Source Elemental Effects", true);
            builder.pop();

            builder.push("Qi Sources");
            qiSourceMinBonus = builder.comment("Lowest absorption speed multiplier of a nearly empty Qi source")
                    .defineInRange("min_absorption_bonus", 0.25, 0.01, 10.0);
            qiSourceMaxBonus = builder.comment("Highest absorption speed multiplier of a large, full Qi source")
                    .defineInRange("max_absorption_bonus", 3.0, 0.01, 100.0);
            builder.pop();

            builder.push("Core Forming");
            stabiliseGoalBase = builder.comment("Stabilisation quest goal of the first Core Forming stage, shared by every core")
                    .defineInRange("stabilise_goal_base", 1000, 1, 1000000);
            stabiliseGoalStep = builder.comment("Added to the stabilisation goal at each following stage")
                    .defineInRange("stabilise_goal_step", 500, 0, 1000000);
            pressureDamageMultiplier = builder.comment("Damage multiplier when the attacker's core element presses the target's core element (Golden Core)")
                    .defineInRange("pressure_damage_multiplier", 1.25, 1.0, 10.0);
            echoStep = builder.comment("Echo concept: each echo is this many percent weaker than the previous one (the first is 40%)")
                    .defineInRange("echo_step", 5, 0, 40);
            ownElementEfficiency = builder.comment("Cultivation efficiency of an elemental core in a Qi source of its own element")
                    .defineInRange("own_element_efficiency", 1.25, 0.01, 10.0);
            variantOwnElementEfficiency = builder.comment("Cultivation efficiency of Ice and Lightning cores in a Qi source of their own element")
                    .defineInRange("variant_own_element_efficiency", 1.5, 0.01, 10.0);
            otherElementEfficiency = builder.comment("Cultivation efficiency of an elemental core in a Qi source of another element")
                    .defineInRange("other_element_efficiency", 0.5, 0.01, 10.0);
            pressingElementEfficiency = builder.comment("Cultivation efficiency of an elemental core in a Qi source of an element that presses it")
                    .defineInRange("pressing_element_efficiency", 0.1, 0.01, 10.0);
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

        public static int stabiliseGoal(int stage) {
            return stabiliseGoalBase.get() + stabiliseGoalStep.get() * (stage - 1);
        }

        public static double qiSourceMinBonus() {
            return qiSourceMinBonus.get();
        }

        public static double qiSourceMaxBonus() {
            return qiSourceMaxBonus.get();
        }

        public static int echoStep() {
            return echoStep.get();
        }

        public static double ownElementEfficiency() {
            return ownElementEfficiency.get();
        }

        public static double variantOwnElementEfficiency() {
            return variantOwnElementEfficiency.get();
        }

        public static double otherElementEfficiency() {
            return otherElementEfficiency.get();
        }

        public static double pressingElementEfficiency() {
            return pressingElementEfficiency.get();
        }

        public static float pressureDamageMultiplier() {
            return pressureDamageMultiplier.get().floatValue();
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

    public static class Client {

        private static final ForgeConfigSpec.BooleanValue notifySuccess;
        private static final ForgeConfigSpec.BooleanValue notifyProgress;
        private static final ForgeConfigSpec.BooleanValue notifyAlert;

        public static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec spec;

        static {
            builder.push("Notifications");
            notifySuccess = builder.comment("Show success toasts (quest completed, concept learnt...)").define("success", true);
            notifyProgress = builder.comment("Show progress toasts (technique mastered, stage full...)").define("progress", true);
            notifyAlert = builder.comment("Show alert toasts (refinement cancelled...)").define("alert", true);
            builder.pop();

            spec = builder.build();
        }

        public static boolean enabled(CultivationNotifier.Type type) {
            return switch (type) {
                case SUCCESS -> notifySuccess.get();
                case PROGRESS -> notifyProgress.get();
                case ALERT -> notifyAlert.get();
            };
        }
    }
}
