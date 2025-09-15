package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import java.util.ArrayList;
import java.util.List;

import DaoOfModding.Cultivationcraft.Common.Config;
import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
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

    private final List<Entry> entries = new ArrayList<>();

    public List<Entry> entries() {
        return entries;
    }

    public Entry getById(int id) {
        if (id < 0 || id >= entries.size()) return null;
        return entries.get(id);
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
            ct.putInt("maxAge", e.genome.maxAge());
            ct.putFloat("growthChance", e.genome.growthChance());
            ct.putInt("height", e.genome.heightPixels());
            ct.putBoolean("prefersShade", e.genome.prefersShade());
            ct.putBoolean("spawnsInCold", e.genome.spawnsInCold());
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
            PlantGenome g = new PlantGenome(
                ct.getInt("speciesId"),
                ct.getInt("color"),
                ct.getInt("maxAge"),
                ct.getFloat("growthChance"),
                ct.getInt("height"),
                ct.getBoolean("prefersShade"),
                ct.getBoolean("spawnsInCold"),
                new net.minecraft.resources.ResourceLocation(ct.getString("element")),
                ct.getInt("tier")
            );
            String name = ct.getString("name");
            data.entries.add(new Entry(id, g, name));
        }
        return data;
    }

    public static PlantCatalogSavedData getOrCreate(ServerLevel level, int desiredSize) {
        return level.getDataStorage().computeIfAbsent(PlantCatalogSavedData::load, () -> create(level, desiredSize), DATA_NAME);
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
        int size = Math.max(minRequired, desiredSize);
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
            int tier = 1;
            int roll = rng.nextInt(10);
            if (roll < 1) tier = 3; else if (roll < 3) tier = 2; // 10% chance to be T2+, heavier bias to T1
            idCounter = addOne(level, rng, data, none, tier, idCounter);
        }

        data.setDirty();
        return data;
    }

    private static int generateBatchForElement(ServerLevel level, RandomSource rng, PlantCatalogSavedData data,
                                               ResourceLocation element, int c1, int c2, int c3, int idStart) {
        int id = idStart;
        for (int i = 0; i < c1; i++) id = addOne(level, rng, data, element, 1, id);
        for (int i = 0; i < c2; i++) id = addOne(level, rng, data, element, 2, id);
        for (int i = 0; i < c3; i++) id = addOne(level, rng, data, element, 3, id);
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

        int ageMin = Config.Server.procPlantAgeMin();
        int ageMax = Config.Server.procPlantAgeMax();
        if (ageMax < ageMin) ageMax = ageMin;
        int maxAge = ageMin + rng.nextInt(Math.max(1, (ageMax - ageMin + 1)));

        double gMin = Config.Server.procPlantGrowthMin();
        double gMax = Config.Server.procPlantGrowthMax();
        if (gMax < gMin) gMax = gMin;
        float growthChance = (float)(gMin + rng.nextDouble() * (gMax - gMin));

        int hMin = Config.Server.procPlantHeightMin();
        int hMax = Config.Server.procPlantHeightMax();
        if (hMax < hMin) hMax = hMin;
        int height = hMin + rng.nextInt(Math.max(1, (hMax - hMin + 1)));
        boolean prefersShade = rng.nextBoolean();

        // Derive spawn preferences from element
        boolean cold = isColdFavored(element);

        PlantGenome genome = new PlantGenome(id, color, maxAge, growthChance, height, prefersShade, cold, element, tier);
        String name = generateName(rng, genome);
        data.entries.add(new Entry(id, genome, name));
        return id + 1;
    }

    private static boolean isColdFavored(ResourceLocation element) {
        var el = Elements.getElement(element);
        if (el == null) return false;
        String path = element.getPath();
        // favor cold for water/ice/wind-like
        if (path.contains("water") || path.contains("ice") || path.contains("wind")) return true;
        if (path.contains("fire")) return false;
        return false;
    }

    private static int rollTier(RandomSource rng) {
        int t3 = Config.Server.procPlantTier3ChancePercent();
        int t2 = Config.Server.procPlantTier2ChancePercent();
        if (t3 < 0) t3 = 0; if (t3 > 100) t3 = 100;
        if (t2 < 0) t2 = 0; if (t2 > 100) t2 = 100;
        int r = rng.nextInt(100);
        if (r < t3) return 3;
        if (r < t3 + t2) return 2;
        return 1;
    }

    private static String generateName(RandomSource rng, PlantGenome g) {
        return PlantNamePools.pickName(rng, g.qiElement());
    }
}
