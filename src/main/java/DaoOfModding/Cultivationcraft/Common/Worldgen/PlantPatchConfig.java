package DaoOfModding.Cultivationcraft.Common.Worldgen;

public record PlantPatchConfig(
    int tries,            // attempts per placement
    int radiusXZ,
    int ySpread,
    boolean onlyColdBiomes // example toggle
) {}
