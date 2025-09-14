package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.util.RandomSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads element-specific name pools from data packs at data/cultivationcraft/plant_names/*.json
 * Each JSON: { "prefixes":[], "cores":[], "suffixes":[] }
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlantNamePools {
    private static final Map<ResourceLocation, NamePool> POOLS = new HashMap<>();
    private static final NamePool DEFAULT = new NamePool(
            List.of("Dusky", "Luminous", "Bitter", "Verdant", "Crimson", "Azure", "Gleaming", "Mellow", "Stormy", "Silent"),
            List.of("Bramble", "Bloom", "Spore", "Thorn", "Petal", "Reed", "Fern", "Moss", "Sedge", "Vetch"),
            List.of("wort", "root", "leaf", "flower", "weed", "shrub", "grass", "vine", "cap", "bud")
    );

    public record NamePool(List<String> prefixes, List<String> cores, List<String> suffixes) {}

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener((barrier, manager, prepProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
                java.util.concurrent.CompletableFuture.supplyAsync(() -> Boolean.TRUE, backgroundExecutor)
                        .thenCompose(barrier::wait)
                        .thenAcceptAsync(v -> load(manager), gameExecutor)
        );
    }

    private static void load(ResourceManager manager) {
        POOLS.clear();
        var gson = new Gson();
        String base = "plant_names"; // folder under data/<ns>/plant_names
        for (var resLoc : manager.listResources(base, s -> s.toString().endsWith(".json")).keySet()) {
            if (!"cultivationcraft".equals(resLoc.getNamespace())) continue;
            try {
                Resource res = manager.getResource(resLoc).orElse(null);
                if (res == null) continue;
                try (var reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    List<String> p = toList(obj.get("prefixes"));
                    List<String> c = toList(obj.get("cores"));
                    List<String> s = toList(obj.get("suffixes"));
                    if (!p.isEmpty() && !c.isEmpty() && !s.isEmpty()) {
                        // element key from filename (e.g., plant_names/cultivationcraft.elements.fire.json)
                        String path = resLoc.getPath();
                        String file = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
                        ResourceLocation key = new ResourceLocation("cultivationcraft", file);
                        POOLS.put(key, new NamePool(p, c, s));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private static List<String> toList(JsonElement elem) {
        if (elem == null || !elem.isJsonArray()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        elem.getAsJsonArray().forEach(e -> out.add(e.getAsString()));
        return out;
    }

    public static String pickName(RandomSource rng, ResourceLocation element) {
        NamePool pool = POOLS.getOrDefault(element, DEFAULT);
        String p = pool.prefixes().get(rng.nextInt(pool.prefixes().size()));
        String c = pool.cores().get(rng.nextInt(pool.cores().size()));
        String s = pool.suffixes().get(rng.nextInt(pool.suffixes().size()));
        return p + " " + c + s;
    }
}
