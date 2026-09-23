package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache of the per-world plant catalog for tooltips/rendering.
 */
public final class ClientPlantCatalog {
    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

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
        ENTRIES.clear();
    }

    public static void put(int id, String name, int color, String element, int tier, int stemVariant, int foliageVariant, int fruitVariant) {
        ENTRIES.put(id, new Entry(name, color, element, tier, stemVariant, foliageVariant, fruitVariant));
    }

    public static Entry get(int id) {
        return ENTRIES.get(id);
    }
}
