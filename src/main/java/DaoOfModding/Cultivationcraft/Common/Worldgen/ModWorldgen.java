package DaoOfModding.Cultivationcraft.Common.Worldgen;

import java.util.List;

import DaoOfModding.Cultivationcraft.Cultivationcraft;
import DaoOfModding.Cultivationcraft.Common.Register;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModWorldgen {
    public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED =
            DeferredRegister.create(Registry.CONFIGURED_FEATURE_REGISTRY, Cultivationcraft.MODID);

    public static final DeferredRegister<PlacedFeature> PLACED =
            DeferredRegister.create(Registry.PLACED_FEATURE_REGISTRY, Cultivationcraft.MODID);

    public static final RegistryObject<ConfiguredFeature<?, ?>> CF_PROC_PLANT_PATCH =
            CONFIGURED.register("procedural_plant_patch",
                    () -> new ConfiguredFeature<>(Register.PROCEDURAL_PLANT_PATCH.get(),
                            NoneFeatureConfiguration.INSTANCE));

    public static final RegistryObject<PlacedFeature> PF_PROC_PLANT_PATCH =
            PLACED.register("procedural_plant_patch",
                    () -> new PlacedFeature(
                            Holder.hackyErase(CF_PROC_PLANT_PATCH.getHolder().orElseThrow()),
                            List.of(
                                RarityFilter.onAverageOnceEvery(8), // ~1/8 chunks
                                InSquarePlacement.spread(),         // spread across the chunk
                                HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                                BiomeFilter.biome()
                            )
                    ));

    public static void init() {
        CONFIGURED.register(FMLJavaModLoadingContext.get().getModEventBus());
        PLACED.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}