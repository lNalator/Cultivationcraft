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
        // Synergies between cores: fixed values, they never grow with the cores' ranks
        private static final ForgeConfigSpec.IntValue auraExplosionPulses;
        private static final ForgeConfigSpec.DoubleValue auraExplosionRadius;
        private static final ForgeConfigSpec.DoubleValue warpInfusionRadius;
        private static final ForgeConfigSpec.IntValue warpInfusionTicks;
        private static final ForgeConfigSpec.IntValue gravityExplosionDelay;
        private static final ForgeConfigSpec.DoubleValue repulsionIceDistance;
        private static final ForgeConfigSpec.DoubleValue echoLightningRange;
        private static final ForgeConfigSpec.IntValue poisonInfusionTicks;
        private static final ForgeConfigSpec.DoubleValue fallingSlamRadius;
        private static final ForgeConfigSpec.IntValue fallingEarthStunTicks;
        private static final ForgeConfigSpec.DoubleValue fallingEarthKnockback;
        private static final ForgeConfigSpec.IntValue fallingWoodRootTicks;
        private static final ForgeConfigSpec.IntValue windWrapMax;
        private static final ForgeConfigSpec.DoubleValue windWrapRange;
        private static final ForgeConfigSpec.DoubleValue windWrapCost;
        private static final ForgeConfigSpec.DoubleValue windWrapCostPerRealm;
        private static final ForgeConfigSpec.IntValue windWrapBreakFreeTicks;
        private static final ForgeConfigSpec.IntValue windWrapSlowFallingTicks;
        private static final ForgeConfigSpec.DoubleValue growAuraRange;
        private static final ForgeConfigSpec.DoubleValue shrinkWarpCost;
        private static final ForgeConfigSpec.IntValue bounceMax;
        private static final ForgeConfigSpec.DoubleValue bounceLife;

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
            bounceMax = builder.comment("Elasticity concept: most bounces of a Qi projectile on blocks; it disappears on the next block hit")
                    .defineInRange("bounce_max", 3, 0, 100);
            bounceLife = builder.comment("Elasticity concept: a Qi projectile keeps this fraction of its remaining lifetime at each bounce")
                    .defineInRange("bounce_life", 0.6, 0.0, 1.0);
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

            builder.push("Core Synergies");
            auraExplosionPulses = builder.comment("Aura + Explosion: one aura pulse in this many explodes at the player's position, without breaking blocks")
                    .defineInRange("aura_explosion_pulses", 3, 1, 100);
            auraExplosionRadius = builder.comment("Aura + Explosion: explosion radius, as a fraction of the aura's range")
                    .defineInRange("aura_explosion_radius", 0.5, 0.05, 2.0);
            warpInfusionRadius = builder.comment("Infusion + Warp: radius of the elemental zone left where a teleport starts")
                    .defineInRange("warp_infusion_radius", 2.0, 0.5, 10.0);
            warpInfusionTicks = builder.comment("Infusion + Warp: how long the zone lasts, in ticks")
                    .defineInRange("warp_infusion_ticks", 60, 1, 1200);
            gravityExplosionDelay = builder.comment("Gravity + Explosion: ticks between the Gravity pull and the explosion")
                    .defineInRange("gravity_explosion_delay", 10, 1, 100);
            repulsionIceDistance = builder.comment("Repulsion + Ice: enemy projectiles slowed this close to the impact are frozen and drop")
                    .defineInRange("repulsion_ice_distance", 1.5, 0.1, 10.0);
            echoLightningRange = builder.comment("Echo + Lightning: the echo jumps to another target within this many blocks")
                    .defineInRange("echo_lightning_range", 4.0, 1.0, 16.0);
            poisonInfusionTicks = builder.comment("Poison + Infusion: regeneration stays blocked this many ticks after leaving an Infusion zone")
                    .defineInRange("poison_infusion_ticks", 20, 1, 600);
            fallingSlamRadius = builder.comment("Falling + Earth / Falling + Wood: radius of the ground slam's stun or roots")
                    .defineInRange("falling_slam_radius", 2.0, 0.5, 10.0);
            fallingEarthStunTicks = builder.comment("Falling + Earth: stun duration in ticks")
                    .defineInRange("falling_earth_stun_ticks", 20, 1, 200);
            fallingEarthKnockback = builder.comment("Falling + Earth: knockback strength")
                    .defineInRange("falling_earth_knockback", 0.8, 0.0, 5.0);
            fallingWoodRootTicks = builder.comment("Falling + Wood: roots duration in ticks")
                    .defineInRange("falling_wood_root_ticks", 20, 1, 200);
            windWrapMax = builder.comment("Levitation + Wind: most entities wrapped in wind at once")
                    .defineInRange("wind_wrap_max", 3, 1, 10);
            windWrapRange = builder.comment("Levitation + Wind: how far an entity can be wrapped, in blocks")
                    .defineInRange("wind_wrap_range", 4.0, 1.0, 16.0);
            windWrapCost = builder.comment("Levitation + Wind: Qi per second for each wrapped entity")
                    .defineInRange("wind_wrap_cost", 2.0, 0.0, 1000.0);
            windWrapCostPerRealm = builder.comment("Levitation + Wind: extra Qi per second for each realm of the wrapped entity (mobs are realm 0)")
                    .defineInRange("wind_wrap_cost_per_realm", 2.0, 0.0, 1000.0);
            windWrapBreakFreeTicks = builder.comment("Levitation + Wind: ticks a wrapped player must sneak to break free (at once when their realm is higher)")
                    .defineInRange("wind_wrap_break_free_ticks", 20, 1, 200);
            windWrapSlowFallingTicks = builder.comment("Levitation + Wind: Slow Falling given to everyone released when the flight stops or the Qi runs out")
                    .defineInRange("wind_wrap_slow_falling_ticks", 100, 0, 1200);
            growAuraRange = builder.comment("Grow + Aura: blocks added to the aura's range")
                    .defineInRange("grow_aura_range", 1.0, 0.0, 10.0);
            shrinkWarpCost = builder.comment("Shrink + Warp: Qi cost multiplier of teleports and Warp flight jumps")
                    .defineInRange("shrink_warp_cost", 0.7, 0.0, 1.0);
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

        public static int bounceMax() { return bounceMax.get(); }
        public static double bounceLife() { return bounceLife.get(); }
        public static int auraExplosionPulses() { return auraExplosionPulses.get(); }
        public static double auraExplosionRadius() { return auraExplosionRadius.get(); }
        public static double warpInfusionRadius() { return warpInfusionRadius.get(); }
        public static int warpInfusionTicks() { return warpInfusionTicks.get(); }
        public static int gravityExplosionDelay() { return gravityExplosionDelay.get(); }
        public static double repulsionIceDistance() { return repulsionIceDistance.get(); }
        public static double echoLightningRange() { return echoLightningRange.get(); }
        public static int poisonInfusionTicks() { return poisonInfusionTicks.get(); }
        public static double fallingSlamRadius() { return fallingSlamRadius.get(); }
        public static int fallingEarthStunTicks() { return fallingEarthStunTicks.get(); }
        public static double fallingEarthKnockback() { return fallingEarthKnockback.get(); }
        public static int fallingWoodRootTicks() { return fallingWoodRootTicks.get(); }
        public static int windWrapMax() { return windWrapMax.get(); }
        public static double windWrapRange() { return windWrapRange.get(); }
        public static double windWrapCost() { return windWrapCost.get(); }
        public static double windWrapCostPerRealm() { return windWrapCostPerRealm.get(); }
        public static int windWrapBreakFreeTicks() { return windWrapBreakFreeTicks.get(); }
        public static int windWrapSlowFallingTicks() { return windWrapSlowFallingTicks.get(); }
        public static double growAuraRange() { return growAuraRange.get(); }
        public static double shrinkWarpCost() { return shrinkWarpCost.get(); }

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
