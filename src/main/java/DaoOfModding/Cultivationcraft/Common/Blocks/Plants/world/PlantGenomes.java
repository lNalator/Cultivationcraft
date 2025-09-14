package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.utils.Seeds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

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
            return new PlantGenome(0, 0x88CC44, 4, 0.08f, 14, false, false, new net.minecraft.resources.ResourceLocation("cultivationcraft","cultivationcraft.elements.none"));
        }
        ServerLevel srv = level.getServer().getLevel(level.dimension());
        PlantCatalogSavedData catalog = PlantCatalogSavedData.getOrCreate(srv, DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());

        int regionSize = DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantRegionSizeChunks() * 16; // N x N chunks regions
        BlockPos regionPos = new BlockPos(
            Mth.floor((double)pos.getX() / regionSize),
            0,
            Mth.floor((double)pos.getZ() / regionSize)
        );

        long worldSeed = srv.getSeed();
        RandomSource rng = Seeds.forPos(worldSeed, regionPos, SALT_PICK);
        int id = rng.nextInt(Math.max(1, catalog.size()));
        PlantCatalogSavedData.Entry e = catalog.getById(id);
        return e != null ? e.genome : new PlantGenome(0, 0x88CC44, 4, 0.08f, 14, false, false, new net.minecraft.resources.ResourceLocation("cultivationcraft","cultivationcraft.elements.none"));
    }

    public static PlantGenome getById(ServerLevel level, int id) {
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());
        PlantCatalogSavedData.Entry e = data.getById(id);
        return e != null ? e.genome : null;
    }

    public static String getNameById(ServerLevel level, int id) {
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, DaoOfModding.Cultivationcraft.Common.Config.Server.procPlantCatalogSize());
        PlantCatalogSavedData.Entry e = data.getById(id);
        return e != null ? e.displayName : null;
    }
}
