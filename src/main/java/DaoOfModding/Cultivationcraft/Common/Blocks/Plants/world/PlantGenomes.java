package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import java.util.List;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.utils.Seeds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

public class PlantGenomes {
   private static final long SALT_SPECIES = "cultivationcraftplantsspecies".hashCode();
    private static final long SALT_TRAITS  = "cultivationcraftplanttraits".hashCode();

    private PlantGenomes() {}

    public static PlantGenome forWorldPos(Level level, BlockPos pos) {
        long worldSeed = level.getServer().getLevel(level.dimension()).getSeed();
        // Chunk-scale assignment: one species per ~8x8 chunks to create patches.
        int regionSize = 8 * 16;
        BlockPos regionPos = new BlockPos(
            Mth.floor((double)pos.getX() / regionSize),
            0,
            Mth.floor((double)pos.getZ() / regionSize)
        );

        RandomSource speciesRng = Seeds.forPos(worldSeed, regionPos, SALT_SPECIES);
        int speciesId = speciesRng.nextInt(16); // 16 species to start

        // Traits derived from species (not per-block → stable within the region)
        RandomSource traitRng = Seeds.forPos(worldSeed ^ speciesId, BlockPos.ZERO, SALT_TRAITS);

        // Example: biome-influenced color hue
        float baseHue = traitRng.nextFloat(); // 0..1
        float sat = Mth.lerp(traitRng.nextFloat(), 0.6f, 1.0f);
        float bri = Mth.lerp(traitRng.nextFloat(), 0.7f, 1.0f);

        // bias by biome temperature
        ResourceKey<Biome> biomeKey = level.getBiome(pos).unwrapKey().orElse(null);
        float tempBias = 0.0f;
        if (biomeKey != null) {
            float temp = level.getBiome(pos).value().getBaseTemperature();
            tempBias = Mth.clamp((temp - 0.5f) * 0.2f, -0.1f, 0.1f);
        }
        float hue = (baseHue + tempBias) % 1.0f;
        int color = hsbToRgb(hue, sat, bri);

        int maxAge = 3 + traitRng.nextInt(3); // 3..5
        float growthChance = 0.05f + traitRng.nextFloat() * 0.10f; // 5%..15% on random tick
        int height = 10 + traitRng.nextInt(6) * 2; // visual hint
        boolean prefersShade = traitRng.nextBoolean();
        boolean spawnsInCold = traitRng.nextBoolean();

        return new PlantGenome(speciesId, color, maxAge, growthChance, height, prefersShade, spawnsInCold);
    }

    private static int hsbToRgb(float h, float s, float b) {
        // returns 0xRRGGBB (Minecraft tint expects RGB, no alpha)
        return java.awt.Color.HSBtoRGB(h, s, b) & 0xFFFFFF;
    }
}
