package DaoOfModding.Cultivationcraft.Common.Worldgen;

import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.core.HolderSet;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers.AddFeaturesBiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBiomeModifiers {

    // The registry for biome modifiers
    public static final DeferredRegister<BiomeModifier> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIERS, Cultivationcraft.MODID);

    public static final RegistryObject<BiomeModifier> PROC_PLANT_IN_OVERWORLD =
            BIOME_MODIFIERS.register("proc_plant_in_overworld", () -> {
                // 1) Select the biomes (Overworld tag)
                HolderSet.Named<Biome> overworldBiomes =
                        BuiltinRegistries.BIOME.getOrCreateTag(BiomeTags.IS_OVERWORLD);

                // 2) The placed feature we created earlier (step 3)
                HolderSet<PlacedFeature> features =
                        HolderSet.direct(ModWorldgen.PF_PROC_PLANT_PATCH.getHolder().orElseThrow());

                // 3) Add our feature at the vegetation decoration step
                return new AddFeaturesBiomeModifier(
                        overworldBiomes,
                        features,
                        GenerationStep.Decoration.VEGETAL_DECORATION
                );
            });

    public static void init() {
        BIOME_MODIFIERS.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
    }
}
