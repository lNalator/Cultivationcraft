package lNalator.Cultivationcraft.Common.Blocks.Plants.world;

import lNalator.Cultivationcraft.Common.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Facade for accessing per-world plant genomes.
 */
public class PlantGenomes {

    private PlantGenomes() {
    }

    public static PlantGenome getById(ServerLevel level, int id) {
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, Config.Server.procPlantCatalogSize());
        PlantCatalogSavedData.Entry e = data.getById(id);
        return e != null ? e.genome : null;
    }

    public static String getNameById(ServerLevel level, int id) {
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, Config.Server.procPlantCatalogSize());
        PlantCatalogSavedData.Entry e = data.getById(id);
        return e != null ? e.displayName : null;
    }

    /**
     * Uses the saved catalog name; a null level reads the synced client catalog
     * for item titles.
     */
    @Nullable
    public static Component getDisplayName(@Nullable Level level, int id) {
        String name;
        int color;
        if (level instanceof ServerLevel server) {
            var entry = PlantCatalogSavedData.getOrCreate(server, Config.Server.procPlantCatalogSize()).getById(id);
            if (entry == null) {
                return null;
            }
            name = entry.displayName;
            color = entry.genome.colorRGB();
        } else {
            var entry = ClientPlantCatalog.get(id);
            if (entry == null) {
                return null;
            }
            name = entry.name;
            color = entry.color;
        }
        return name == null || name.isBlank() ? null : Component.literal(name).withStyle(style -> style.withColor(color));
    }
}
