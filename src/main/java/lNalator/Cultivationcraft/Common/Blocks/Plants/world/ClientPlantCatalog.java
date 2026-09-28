package lNalator.Cultivationcraft.Common.Blocks.Plants.world;

import java.util.Map;

/**
 * Client-side cache of the per-world plant catalog for tooltips/rendering.
 */
public final class ClientPlantCatalog {

    private static volatile Map<Integer, Entry> entries = Map.of();

    public static class Entry {

        public final String name;
        public final int color; // genome color
        public final String element; // ResourceLocation string
        public final int tier; // 1..3
        public final int stemVariant;
        public final int foliageVariant;
        public final int fruitVariant; // -1 = none

        public Entry(String name, int color, String element, int tier, int stemVariant, int foliageVariant, int fruitVariant) {
            this.name = name;
            this.color = color;
            this.element = element;
            this.tier = tier;
            this.stemVariant = stemVariant;
            this.foliageVariant = foliageVariant;
            this.fruitVariant = fruitVariant;
        }
    }

    public static void clear() {
        entries = Map.of();
    }

    public static void replace(Map<Integer, Entry> catalog) {
        entries = Map.copyOf(catalog);
    }

    public static Entry get(int id) {
        return entries.get(id);
    }
}
