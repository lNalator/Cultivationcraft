package lNalator.Cultivationcraft.Common.Blocks.custom;

import lNalator.Cultivationcraft.Common.Blocks.entity.FrozenBlockEntity;
import lNalator.Cultivationcraft.Common.Blocks.util.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrozenBlock extends AbstractGlassBlock implements EntityBlock {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty IS_SECOND_BLOCK = BooleanProperty.create("is_second_block");

    public FrozenBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(IS_SECOND_BLOCK, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, IS_SECOND_BLOCK);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    // Breaks as fast as the block it froze (design E2)
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FrozenBlockEntity frozen && frozen.getOldBlockState().getDestroySpeed(level, pos) >= 0) {
            return frozen.getOldBlockState().getDestroyProgress(player, level, pos);
        }

        return 0;
    }

    // Breaking a frozen block thaws it, then breaks the original block normally: its usual drops,
    // and a container's contents, are dropped exactly once since the frozen block holds the only copy
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof FrozenBlockEntity frozen)) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }

        frozen.thawBlock();

        BlockState original = level.getBlockState(pos);
        BlockEntity originalEntity = level.getBlockEntity(pos);

        if (!player.isCreative() && player.hasCorrectToolForDrops(original)) {
            Block.dropResources(original, level, pos, originalEntity, player, player.getMainHandItem());
        }

        level.removeBlock(pos, false);
        return true;
    }

    /* BLOCK ENTITY */
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FrozenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> entityType) {
        return TickableBlockEntity.getTickerHelper(level);
    }
}
