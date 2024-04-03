package DaoOfModding.Cultivationcraft.Common.Blocks.custom;

import DaoOfModding.Cultivationcraft.Common.Blocks.entity.FrozenBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Blocks.util.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrozenBlock extends AbstractGlassBlock implements EntityBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty IS_SECOND_BLOCK = BooleanProperty.create("is_second_block");
    BlockState oldBlockState = null;
    BlockEntity oldBlockEntity = null;
    CompoundTag oldBlockEntityData = null;

    public FrozenBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(IS_SECOND_BLOCK, Boolean.valueOf(false)));

    }

    public void setOldBlockFields(BlockState oldBlockState, BlockEntity oldBlockEntity, CompoundTag oldBlockEntityData) {
        this.oldBlockState = oldBlockState;
        this.oldBlockEntity = oldBlockEntity;
        this.oldBlockEntityData = oldBlockEntityData;
    }

    public BlockState getOldBlockState() {
        return oldBlockState;
    }

    public BlockEntity getOldBlockEntity() {
        return oldBlockEntity;
    }

    public CompoundTag getOldBlockEntityData() {
        return oldBlockEntityData;
    }

    public boolean getIsSecondBlock() {
        return defaultBlockState().getValue(IS_SECOND_BLOCK);
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

    /* BLOCK ENTITY */
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FrozenBlockEntity(pos, state, oldBlockState, oldBlockEntity, oldBlockEntityData, IS_SECOND_BLOCK);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> entityType) {
        return TickableBlockEntity.getTickerHelper(level);
    }
}