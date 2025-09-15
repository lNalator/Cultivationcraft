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
        public Entry(String name, int color, String element) {
            this.name = name;
            this.color = color;
            this.element = element;
        }
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static void put(int id, String name, int color, String element) {
        ENTRIES.put(id, new Entry(name, color, element));
    }

    public static Entry get(int id) {
        return ENTRIES.get(id);
    }
}
