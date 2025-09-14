package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
                new net.minecraft.resources.ResourceLocation(ct.getString("element"))
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

        for (int i = 0; i < desiredSize; i++) {
            // Build a genome deterministically
            int speciesId = i; // stable per-entry
            // Visual/color
            float baseHue = rng.nextFloat();
            float sat = Mth.lerp(rng.nextFloat(), 0.6f, 1.0f);
            float bri = Mth.lerp(rng.nextFloat(), 0.7f, 1.0f);
            int color = java.awt.Color.HSBtoRGB(baseHue, sat, bri) & 0xFFFFFF;

            int ageMin = DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantAgeMin();
            int ageMax = DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantAgeMax();
            if (ageMax < ageMin) ageMax = ageMin;
            int maxAge = ageMin + rng.nextInt(Math.max(1, (ageMax - ageMin + 1)));

            double gMin = DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantGrowthMin();
            double gMax = DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantGrowthMax();
            if (gMax < gMin) gMax = gMin;
            float growthChance = (float)(gMin + rng.nextDouble() * (gMax - gMin));

            int hMin = DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantHeightMin();
            int hMax = DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantHeightMax();
            if (hMax < hMin) hMax = hMin;
            int height = hMin + rng.nextInt(Math.max(1, (hMax - hMin + 1)));
            boolean prefersShade = rng.nextBoolean();
            boolean spawnsInCold = rng.nextBoolean();

            // Pick element from dimension rules
            java.util.ArrayList<net.minecraft.resources.ResourceLocation> rules = DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements.getDimensionRules(level.dimension());
            net.minecraft.resources.ResourceLocation element = rules.get(rng.nextInt(Math.max(1, rules.size())));

            PlantGenome genome = new PlantGenome(speciesId, color, maxAge, growthChance, height, prefersShade, spawnsInCold, element);
            String name = generateName(rng, genome);
            data.entries.add(new Entry(i, genome, name));
        }
        data.setDirty();
        return data;
    }

    private static String generateName(RandomSource rng, PlantGenome g) {
        return DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantNamePools.pickName(rng, g.qiElement());
    }
}
