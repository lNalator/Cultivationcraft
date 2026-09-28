package lNalator.Cultivationcraft.Common.Blocks.Plants.world;

import net.minecraft.resources.ResourceLocation;

public record PlantGenome(
        int speciesId, // e.g. 0..N-1
        int colorRGB, // 0xRRGGBB; client tint
        int stemVariant, // index into stem texture catalog
        int foliageVariant, // index into foliage texture catalog
        int fruitVariant, // index into fruit texture catalog; -1 = none
        ResourceLocation qiElement, // element type
        int tier // default growth tier for command-given specimens; placed plants derive tier from growth
        ) {

}
