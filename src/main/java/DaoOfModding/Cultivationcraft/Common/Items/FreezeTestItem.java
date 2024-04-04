package DaoOfModding.Cultivationcraft.Common.Items;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.custom.FrozenBlock;
import DaoOfModding.Cultivationcraft.Common.Blocks.entity.FrozenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

public class FreezeTestItem extends Item {
    public FreezeTestItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext pContext) {
        if (!pContext.getLevel().isClientSide() && pContext.getHand() == InteractionHand.MAIN_HAND) {
            BlockPos blockPos = pContext.getClickedPos();
            BlockPos secondBlockPos = getMultiblockPos(blockPos, pContext.getLevel().getBlockState(blockPos));
            Level level = pContext.getLevel();
            if (secondBlockPos != null) {
                createFrozenBlock(level, secondBlockPos, true);
            }
            createFrozenBlock(level, blockPos, false);
            return InteractionResult.SUCCESS;
        }
        return super.useOn(pContext);
    }

    public static void createFrozenBlock(Level world, BlockPos blockPos, boolean isSecondBlock) {
        BlockState oldState = world.getBlockState(blockPos);
        BlockEntity oldBlockEntity = null;
        CompoundTag oldBlockEntityData = null;
        if (oldState.hasBlockEntity()) {
            oldBlockEntity = world.getBlockEntity(blockPos);
            oldBlockEntityData = oldBlockEntity.serializeNBT();
            world.removeBlockEntity(blockPos);
        }

        if (!world.isClientSide()) {
            BlockState FrozenBlock = setFrozenBlock(oldState, isSecondBlock);
            world.setBlockAndUpdate(blockPos, FrozenBlock);
            ((FrozenBlockEntity) world.getBlockEntity(blockPos)).setOldBlockFields(oldState, oldBlockEntity, oldBlockEntityData);
        }
    }

    public static BlockPos getMultiblockPos(BlockPos blockPos, BlockState blockState) {
        String name = blockState.getBlock().getName().getString();

        if (name.contains("Door")) {
            Property<?> half = blockState.getBlock().getStateDefinition().getProperty("half");
            return blockState.getValue(half).toString().equals("upper") ? blockPos.below() : blockPos.above();
        } else if (name.contains("Piston")) {
            Property<?> facing = blockState.getBlock().getStateDefinition().getProperty("facing");
            if (name.equals("Piston Head")) {
                return blockPos.relative(((Direction) blockState.getValue(facing)).getOpposite());
            } else {
                if (blockState.getValue((Property<Boolean>) blockState.getBlock().getStateDefinition().getProperty("extended"))) {
                    return blockPos.relative((Direction) blockState.getValue(facing));
                }
            }
        } else if (name.contains("Bed")) {
            Property<?> facing = blockState.getBlock().getStateDefinition().getProperty("facing");
            Property<?> part = blockState.getBlock().getStateDefinition().getProperty("part");
            if (blockState.getValue(part).toString().equals("foot")) {
                return blockPos.relative((Direction) blockState.getValue(facing));
            } else {
                return blockPos.relative(((Direction) blockState.getValue(facing)).getOpposite());
            }
        }

        return null;
    }

    public static BlockState setFrozenBlock(BlockState oldBlockState, boolean isSecondBlock) {
        BlockState frozenBlock = BlockRegister.FROZEN_BLOCK.get().defaultBlockState();
        Property<Direction> facing = oldBlockState.getBlock().getStateDefinition().getProperty("facing") != null
                ? (Property<Direction>) oldBlockState.getBlock().getStateDefinition().getProperty("facing")
                : null;
        if (
                facing != null
                        && !oldBlockState.getValue(facing).equals(Direction.UP)
                        && !oldBlockState.getValue(facing).equals(Direction.DOWN)
        ) {
            frozenBlock = frozenBlock.setValue(FrozenBlock.FACING, oldBlockState.getValue(facing));
            frozenBlock = frozenBlock.setValue(FrozenBlock.IS_SECOND_BLOCK, isSecondBlock);
        }
        return frozenBlock;
    }
}
