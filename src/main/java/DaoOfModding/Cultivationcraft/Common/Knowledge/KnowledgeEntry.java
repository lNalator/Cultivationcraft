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
                             String recipe, Set<String> lootPools, int weight, List<String> category, boolean unlockedByDefault,
                             boolean operatorOnly, int cultivationType, String translationKey, List<String> aliases) {
    private static Map<ResourceLocation, KnowledgeEntry> entries = Map.of();
    public static Collection<KnowledgeEntry> all() { return entries.values(); }
    public static KnowledgeEntry get(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return null;
        KnowledgeEntry exact = entries.get(key);
        if (exact != null) return exact;
        return entries.values().stream().filter(entry -> entry.aliases.contains(key.toString())).findFirst().orElse(null);
    }
    /** Command shorthand is separate from persistent keys: ambiguous names never pick a random entry. */
    public static KnowledgeEntry resolveSlip(ResourceLocation requested) {
        String namespace = requested.getNamespace().equals("minecraft") ? Cultivationcraft.MODID : requested.getNamespace();
        String path = requested.getPath();
        if (path.startsWith("knowledge/")) path = path.substring("knowledge/".length());
        ResourceLocation key = new ResourceLocation(namespace, path);
        KnowledgeEntry exact = get(key.toString());
        if (exact != null) return exact;
        String name = path;
        var matches = all().stream().filter(KnowledgeEntry::hasSlip)
                .filter(entry -> entry.id.getNamespace().equals(namespace))
                .filter(entry -> entry.id.getPath().substring(entry.id.getPath().lastIndexOf('/') + 1).equals(name)
                        || entry.recipe.equals(key.toString())).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }
    public boolean hasSlip() { return !unlockedByDefault && !titles.isEmpty(); }
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
                if (object.has("titles")) object.getAsJsonArray("titles").forEach(value -> titles.add(value.getAsString()));
                Set<String> pools = new HashSet<>();
                if (object.has("loot_pools")) object.getAsJsonArray("loot_pools").forEach(value -> pools.add(value.getAsString()));
                String recipe = object.has("recipe") ? object.get("recipe").getAsString() : "";
                List<String> category = new ArrayList<>();
                if (object.has("category")) object.getAsJsonArray("category").forEach(value -> category.add(value.getAsString()));
                if (!object.has("category")) category.add("Knowledge");
                if (category.size() > 4 || category.stream().anyMatch(String::isBlank))
                    throw new JsonParseException("Invalid knowledge category: " + id);
                int weight = object.has("weight") ? object.get("weight").getAsInt() : 1;
                boolean unlocked = object.has("unlocked_by_default") && object.get("unlocked_by_default").getAsBoolean();
                boolean operatorOnly = object.has("operator_only") && object.get("operator_only").getAsBoolean();
                int cultivationType = object.has("cultivation_type") ? object.get("cultivation_type").getAsInt() : -1;
                String translationKey = object.has("translation_key") ? object.get("translation_key").getAsString() : "";
                List<String> aliases = new ArrayList<>();
                if (object.has("aliases")) object.getAsJsonArray("aliases").forEach(value -> aliases.add(value.getAsString()));
                if ((!unlocked && titles.isEmpty()) || cultivationType < -1 || cultivationType > 1
                        || aliases.stream().anyMatch(alias -> ResourceLocation.tryParse(alias) == null)
                        || titles.stream().anyMatch(String::isBlank) || weight < 1 || weight > 10000
                        || (!recipe.isEmpty() && ResourceLocation.tryParse(recipe) == null))
                    throw new JsonParseException("Invalid knowledge entry: " + id);
                loaded.put(id, new KnowledgeEntry(id, object.get("title").getAsString(), object.get("text").getAsString(),
                        List.copyOf(titles), recipe, Set.copyOf(pools), weight, List.copyOf(category), unlocked, operatorOnly, cultivationType, translationKey, List.copyOf(aliases)));
            });
            Set<String> keys = new HashSet<>();
            loaded.keySet().forEach(id -> keys.add(id.toString()));
            for (KnowledgeEntry entry : loaded.values()) for (String alias : entry.aliases)
                if (!keys.add(alias)) throw new JsonParseException("Duplicate knowledge alias: " + alias);
            entries = Collections.unmodifiableMap(loaded);
        }
    }
}
