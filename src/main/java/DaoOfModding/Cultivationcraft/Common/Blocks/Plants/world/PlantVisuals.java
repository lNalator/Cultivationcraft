package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Central catalog for procedural plant visual pieces. Server and client share the
 * same definitions so a species id always picks the same assets.
 */
public final class PlantVisuals {
    public static final int NO_FRUIT = -1;

    private static final String MODID = "cultivationcraft";

    private static final List<ResourceLocation> STEM_TEXTURES = List.of(
        new ResourceLocation(MODID, "block/procplant/stems/stem_0"),
        new ResourceLocation(MODID, "block/procplant/stems/stem_1")
    );

    private static final List<ResourceLocation> FOLIAGE_TEXTURES = List.of(
        new ResourceLocation(MODID, "block/procplant/foliage/foliage_0"),
        new ResourceLocation(MODID, "block/procplant/foliage/foliage_1")
    );

    private static final List<ResourceLocation> FRUIT_TEXTURES = List.of(
        new ResourceLocation(MODID, "block/procplant/fruits/fruit_0"),
        new ResourceLocation(MODID, "block/procplant/fruits/fruit_1")
    );

    private PlantVisuals() {}

    public static int stemVariantCount() {
        return STEM_TEXTURES.size();
    }

    public static ResourceLocation stemTexture(int index) {
        return STEM_TEXTURES.get(index);
    }

    public static int foliageVariantCount() {
        return FOLIAGE_TEXTURES.size();
    }

    public static ResourceLocation foliageTexture(int index) {
        return FOLIAGE_TEXTURES.get(index);
    }

    public static int fruitVariantCount() {
        return FRUIT_TEXTURES.size();
    }

    public static ResourceLocation fruitTexture(int index) {
        return FRUIT_TEXTURES.get(index);
    }

    public static boolean isValidStem(int index) {
        return index >= 0 && index < stemVariantCount();
    }

    public static boolean isValidFoliage(int index) {
        return index >= 0 && index < foliageVariantCount();
    }

    public static boolean isValidFruit(int index) {
        return index == NO_FRUIT || (index >= 0 && index < fruitVariantCount());
    }
}
