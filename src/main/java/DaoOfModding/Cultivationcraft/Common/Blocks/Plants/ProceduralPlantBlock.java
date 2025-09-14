package DaoOfModding.Cultivationcraft.Common.Blocks.Plants;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;

public class ProceduralPlantBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3; // 0..3; adjust to genome at runtime

    public ProceduralPlantBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.DANDELION).noOcclusion().randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    protected boolean mayPlaceOn(BlockState state, LevelReader level, BlockPos pos) {
        // soil check: grass/dirt/farmland etc.
        return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true; // growth on random ticks
    }

    public void randomTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        if (level.isClientSide) return;

        int age = state.getValue(AGE);
        if (age >= 3) return;

        var g = PlantGenomes.forWorldPos(level, pos);

        // simple growth rules
        int light = level.getMaxLocalRawBrightness(pos);
        boolean okLight = g.prefersShade() ? light >= 6 : light >= 9;

        if (okLight && level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
            if (rng.nextFloat() < g.growthChance()) {
                level.setBlock(pos, state.setValue(AGE, age + 1), 2);
            }
        }
    }

    @Override
    public boolean isBonemealSuccess(Level lvl, RandomSource rng, BlockPos pos, BlockState state) {
        return true;
    }

    public void performBonemeal(Level level, RandomSource rng, BlockPos pos, BlockState state) {
        
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState,
            boolean isClient) {
        return blockState.getValue(AGE) < 3;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource source, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        if (age < 3) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }
}
