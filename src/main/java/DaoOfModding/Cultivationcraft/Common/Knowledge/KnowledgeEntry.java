package DaoOfModding.Cultivationcraft.Common.Knowledge;

import DaoOfModding.Cultivationcraft.Common.Alchemy.PillCatalog;
import DaoOfModding.Cultivationcraft.Common.Alchemy.PillDefinition;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

public record KnowledgeEntry(ResourceLocation id, String title, String text, List<String> titles,
                             String recipe, Set<String> lootPools, int weight) {
    private static Map<ResourceLocation, KnowledgeEntry> entries = Map.of();
    public static Collection<KnowledgeEntry> all() { return entries.values(); }
    public static KnowledgeEntry get(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : entries.get(key);
    }
    public boolean available() { return recipe.isEmpty() || PillDefinition.get(recipe) != null; }
    public String resolveTitle(String value, ServerLevel level) {
        PillDefinition pill = PillDefinition.get(recipe);
        return pill == null ? value : value.replace("{pill}", PillCatalog.get(level).name(pill, level));
    }

    @Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() { super(new Gson(), "knowledge"); }
        @SubscribeEvent public static void register(AddReloadListenerEvent event) { event.addListener(new Loader()); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, KnowledgeEntry> loaded = new TreeMap<>();
            files.forEach((id, json) -> {
                JsonObject object = json.getAsJsonObject();
                List<String> titles = new ArrayList<>();
                object.getAsJsonArray("titles").forEach(value -> titles.add(value.getAsString()));
                Set<String> pools = new HashSet<>();
                object.getAsJsonArray("loot_pools").forEach(value -> pools.add(value.getAsString()));
                String recipe = object.has("recipe") ? object.get("recipe").getAsString() : "";
                int weight = object.has("weight") ? object.get("weight").getAsInt() : 1;
                if (titles.isEmpty() || titles.stream().anyMatch(String::isBlank) || weight < 1 || weight > 10000
                        || (!recipe.isEmpty() && ResourceLocation.tryParse(recipe) == null))
                    throw new JsonParseException("Invalid knowledge entry: " + id);
                loaded.put(id, new KnowledgeEntry(id, object.get("title").getAsString(), object.get("text").getAsString(),
                        List.copyOf(titles), recipe, Set.copyOf(pools), weight));
            });
            entries = Collections.unmodifiableMap(loaded);
        }
    }
}
