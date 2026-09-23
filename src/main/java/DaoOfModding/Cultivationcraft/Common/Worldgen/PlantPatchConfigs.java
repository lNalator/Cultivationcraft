package DaoOfModding.Cultivationcraft.Common.Worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class PlantPatchConfigs {
    public static final Codec<PlantPatchConfig> CODEC =
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("tries").forGetter(PlantPatchConfig::tries),
            Codec.INT.fieldOf("radius_xz").forGetter(PlantPatchConfig::radiusXZ),
            Codec.INT.fieldOf("y_spread").forGetter(PlantPatchConfig::ySpread),
            Codec.BOOL.fieldOf("only_cold").forGetter(PlantPatchConfig::onlyColdBiomes)
        ).apply(instance, PlantPatchConfig::new));
}
