package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import DaoOfModding.Cultivationcraft.Common.Config;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.utils.Seeds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Facade for accessing per-world plant genomes.
 */
public class PlantGenomes {
    private static final long SALT_PICK = "cultivationcraft_catalog_pick".hashCode();

    private PlantGenomes() {}

    public static PlantGenome forWorldPos(Level level, BlockPos pos) {
        // Map world position to a catalog entry to create region patches while keeping a fixed set.
        if (level.isClientSide) {
            // Client without catalog – fallback
            return new PlantGenome(0, 0x88CC44, 0, 0, PlantVisuals.NO_FRUIT, 4, 0.08f, 14, false, false, new ResourceLocation("cultivationcraft","cultivationcraft.elements.none"), 1);
        }
        ServerLevel srv = level.getServer().getLevel(level.dimension());
        PlantCatalogSavedData catalog = PlantCatalogSavedData.getOrCreate(srv, Config.Server.procPlantCatalogSize());

        int regionSize = Config.Server.procPlantRegionSizeChunks() * 16; // N x N chunks regions
        BlockPos regionPos = new BlockPos(
            Mth.floor((double)pos.getX() / regionSize),
            0,
            Mth.floor((double)pos.getZ() / regionSize)
        );

        long worldSeed = srv.getSeed();
        RandomSource rng = Seeds.forPos(worldSeed, regionPos, SALT_PICK);
        int id = rng.nextInt(Math.max(1, catalog.size()));
        PlantCatalogSavedData.Entry e = catalog.getById(id);
        return e != null ? e.genome : new PlantGenome(0, 0x88CC44, 0, 0, PlantVisuals.NO_FRUIT, 4, 0.08f, 14, false, false, new ResourceLocation("cultivationcraft","cultivationcraft.elements.none"), 1);
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

    /** Uses the saved catalog name; a null level reads the synced client catalog for item titles. */
    @Nullable
    public static Component getDisplayName(@Nullable Level level, int id) {
        String name;
        int color;
        if (level instanceof ServerLevel server) {
            var entry = PlantCatalogSavedData.getOrCreate(server, Config.Server.procPlantCatalogSize()).getById(id);
            if (entry == null) return null;
            name = entry.displayName;
            color = entry.genome.colorRGB();
        } else {
            var entry = ClientPlantCatalog.get(id);
            if (entry == null) return null;
            name = entry.name;
            color = entry.color;
        }
        return name == null || name.isBlank() ? null : Component.literal(name).withStyle(style -> style.withColor(color));
    }
}
