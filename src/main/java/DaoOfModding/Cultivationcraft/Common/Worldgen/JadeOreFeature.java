package DaoOfModding.Cultivationcraft.Common.Worldgen;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Collect a face-connected deposit before placing it; never leave a tiny partial vein. */
public final class JadeOreFeature extends Feature<NoneFeatureConfiguration> {
    public JadeOreFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!replaceable(level, origin)) return false;

        int size = 5 + context.random().nextInt(5);
        List<BlockPos> vein = new ArrayList<>();
        List<BlockPos> frontier = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(origin);
        visited.add(origin);
        while (!frontier.isEmpty() && vein.size() < size) {
            BlockPos pos = frontier.remove(context.random().nextInt(frontier.size()));
            if (!replaceable(level, pos)) continue;
            vein.add(pos);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                // Bound the deposit to avoid reaching outside the worldgen write region.
                if (Math.abs(next.getX() - origin.getX()) <= 3 && Math.abs(next.getZ() - origin.getZ()) <= 3
                        && Math.abs(next.getY() - origin.getY()) <= 3 && visited.add(next)) frontier.add(next);
            }
        }
        if (vein.size() < 5) return false;
        for (BlockPos pos : vein) level.setBlock(pos, BlockRegister.JADE_ORE.get().defaultBlockState(), 2);
        return true;
    }

    private static boolean replaceable(WorldGenLevel level, BlockPos pos) {
        return pos.getY() < 0 && !level.isOutsideBuildHeight(pos) && level.ensureCanWrite(pos)
                && level.getBlockState(pos).is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }
}
