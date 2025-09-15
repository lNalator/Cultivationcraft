package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import net.minecraft.resources.ResourceLocation;

public record PlantGenome(
    int speciesId,        // e.g. 0..N-1
    int colorRGB,         // 0xRRGGBB; client tint
    int maxAge,           // growth stages (e.g., 3 or 7)
    float growthChance,   // per random tick when conditions met
    int heightPixels,     // for hitbox/shape choice if you like
    boolean prefersShade, // growth rule example
    boolean spawnsInCold, // worldgen rule example
    ResourceLocation qiElement, // element type
    int tier              // 1..3 rarity tier (3 = max)
) {}
