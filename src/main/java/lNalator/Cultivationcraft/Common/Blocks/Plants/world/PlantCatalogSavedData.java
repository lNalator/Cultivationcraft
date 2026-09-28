package lNalator.Cultivationcraft.Common.Blocks.Plants.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;

import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Stores a per-world catalog of plant genomes with display names.
 */
public class PlantCatalogSavedData extends SavedData {

    public static final String DATA_NAME = "cultivationcraft_plant_catalog";

    private static final float FRUIT_PRESENT_CHANCE = 0.6f;

    public static class Entry {

        public final int id;
        public final PlantGenome genome;
        public final String displayName;

        public Entry(int id, PlantGenome genome, String displayName) {
            this.id = id;
            this.genome = genome;
            this.displayName = displayName;
        }
    }

    private static final Map<MinecraftServer, PlantCatalogSavedData> CATALOGS = new ConcurrentHashMap<>();
    private List<Entry> entries = new ArrayList<>();
    private Map<Integer, Entry> byId = Map.of();
    private Map<ResourceLocation, List<Entry>> byElement = Map.of();

    private void freeze() {
        entries = List.copyOf(entries);
        Map<Integer, Entry> ids = new HashMap<>();
        Map<ResourceLocation, List<Entry>> elements = new HashMap<>();
        for (Entry entry : entries) {
            if (entry.id < 0 || entry.id > 63 || ids.put(entry.id, entry) != null) {
                throw new IllegalArgumentException("Invalid or duplicate plant species ID: " + entry.id);
            }
            elements.computeIfAbsent(entry.genome.qiElement(), key -> new ArrayList<>()).add(entry);
        }
        elements.replaceAll((key, value) -> List.copyOf(value));
        byId = Map.copyOf(ids);
        byElement = Map.copyOf(elements);
    }

    public List<Entry> entriesForElement(ResourceLocation element) {
        return byElement.getOrDefault(element, List.of());
    }

    public static void clear(MinecraftServer server) {
        CATALOGS.remove(server);
    }

    public List<Entry> entries() {
        return entries;
    }

    public Entry getById(int id) {
        return byId.get(id);
    }

    public int size() {
        return entries.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag ct = new CompoundTag();
            ct.putInt("id", e.id);
            ct.putInt("speciesId", e.genome.speciesId());
            ct.putInt("color", e.genome.colorRGB());
            ct.putInt("stemVariant", e.genome.stemVariant());
            ct.putInt("foliageVariant", e.genome.foliageVariant());
            ct.putInt("fruitVariant", e.genome.fruitVariant());
            ct.putString("element", e.genome.qiElement().toString());
            ct.putInt("tier", e.genome.tier());
            ct.putString("name", e.displayName);
            list.add(ct);
        }
        tag.put("entries", list);
        return tag;
    }

    public static PlantCatalogSavedData load(CompoundTag tag) {
        PlantCatalogSavedData data = new PlantCatalogSavedData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ct = list.getCompound(i);
            int id = ct.getInt("id");
            int stemVariant = ct.contains("stemVariant", Tag.TAG_INT) ? ct.getInt("stemVariant") : 0;
            int foliageVariant = ct.contains("foliageVariant", Tag.TAG_INT) ? ct.getInt("foliageVariant") : 0;
            int fruitVariant = ct.contains("fruitVariant", Tag.TAG_INT) ? ct.getInt("fruitVariant") : PlantVisuals.NO_FRUIT;
            if (!PlantVisuals.isValidStem(stemVariant)) {
                stemVariant = 0;
            }
            if (!PlantVisuals.isValidFoliage(foliageVariant)) {
                foliageVariant = 0;
            }
            if (!PlantVisuals.isValidFruit(fruitVariant)) {
                fruitVariant = PlantVisuals.NO_FRUIT;
            }
            PlantGenome g = new PlantGenome(
                    ct.getInt("speciesId"),
                    ct.getInt("color"),
                    stemVariant,
                    foliageVariant,
                    fruitVariant,
                    new net.minecraft.resources.ResourceLocation(ct.getString("element")),
                    ct.getInt("tier")
            );
            String name = ct.getString("name");
            data.entries.add(new Entry(id, g, name));
        }
        data.freeze();
        return data;
    }

    public static PlantCatalogSavedData getOrCreate(ServerLevel level, int desiredSize) {
        // Level load initializes this before chunk generation; workers only read the frozen catalog.
        return CATALOGS.computeIfAbsent(level.getServer(), server -> {
            ServerLevel overworld = server.overworld();
            return overworld.getDataStorage().computeIfAbsent(PlantCatalogSavedData::load,
                    () -> create(overworld, desiredSize), DATA_NAME);
        });
    }

    private static PlantCatalogSavedData create(ServerLevel level, int desiredSize) {
        PlantCatalogSavedData data = new PlantCatalogSavedData();
        long seed = level.getSeed();
        RandomSource rng = RandomSource.create(seed ^ 0x91E10DA5L);

        List<ResourceLocation> core = new ArrayList<>();
        core.add(Elements.fireElement);
        core.add(Elements.earthElement);
        core.add(Elements.woodElement);
        core.add(Elements.windElement);
        core.add(Elements.waterElement);
        core.add(Elements.iceElement);
        core.add(Elements.lightningElement);
        ResourceLocation none = Elements.noElement;

        int E = core.size();
        // Ensure at least 6 per element (3 T1, 2 T2, 1 T3)
        int minRequired = 6 * E;
        int size = Mth.clamp(desiredSize, minRequired + 1, 64);
        int idCounter = 0;

        // Decide base counts per element and tier
        int perElemT3 = 1;
        int perElemT2 = 2;
        int perElemT1 = 3;

        // Generate per-element entries with balanced tiers
        for (ResourceLocation element : core) {
            idCounter = generateBatchForElement(level, rng, data, element, perElemT1, perElemT2, perElemT3, idCounter);
        }

        // Fill remaining with none-element (bias to T1)
        while (data.entries.size() < size) {
            int roll = rng.nextInt(100);
            int tier3 = Config.Server.procPlantTier3ChancePercent();
            int tier2 = Math.min(100 - tier3, Config.Server.procPlantTier2ChancePercent());
            int tier = roll < tier3 ? 3 : roll < tier3 + tier2 ? 2 : 1;
            idCounter = addOne(level, rng, data, none, tier, idCounter);
        }

        data.setDirty();
        data.freeze();
        return data;
    }

    private static int generateBatchForElement(ServerLevel level, RandomSource rng, PlantCatalogSavedData data,
            ResourceLocation element, int c1, int c2, int c3, int idStart) {
        int id = idStart;
        for (int i = 0; i < c1; i++) {
            id = addOne(level, rng, data, element, 1, id);
        }
        for (int i = 0; i < c2; i++) {
            id = addOne(level, rng, data, element, 2, id);
        }
        for (int i = 0; i < c3; i++) {
            id = addOne(level, rng, data, element, 3, id);
        }
        return id;
    }

    private static int addOne(ServerLevel level, RandomSource rng, PlantCatalogSavedData data,
            ResourceLocation element, int tier, int id) {
        // Visual/color based on element, with slight variation
        java.awt.Color elemC = Elements.getElement(element).color;
        float[] hsb = java.awt.Color.RGBtoHSB(elemC.getRed(), elemC.getGreen(), elemC.getBlue(), null);
        float hueJitter = (rng.nextFloat() - 0.5f) * 0.08f;
        float sat = Mth.clamp(hsb[1] + (rng.nextFloat() - 0.5f) * 0.2f, 0f, 1f);
        float bri = Mth.clamp(hsb[2] + (rng.nextFloat() - 0.5f) * 0.2f, 0f, 1f);
        int color = java.awt.Color.HSBtoRGB((hsb[0] + hueJitter + 1f) % 1f, sat, bri) & 0xFFFFFF;

        int stemVariant = rng.nextInt(Math.max(1, PlantVisuals.stemVariantCount()));
        int foliageVariant = rng.nextInt(Math.max(1, PlantVisuals.foliageVariantCount()));
        int fruitVariant = PlantVisuals.NO_FRUIT;
        int fruitCount = PlantVisuals.fruitVariantCount();
        if (fruitCount > 0 && rng.nextFloat() < FRUIT_PRESENT_CHANCE) {
            fruitVariant = rng.nextInt(fruitCount);
        }

        PlantGenome genome = new PlantGenome(id, color, stemVariant, foliageVariant, fruitVariant, element, tier);
        String name = generateName(rng, genome);
        data.entries.add(new Entry(id, genome, name));
        return id + 1;
    }

    private static String generateName(RandomSource rng, PlantGenome g) {
        return PlantNamePools.pickName(rng, g.qiElement());
    }
}
