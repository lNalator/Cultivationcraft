package lNalator.Cultivationcraft.Common.Blocks.entity;

import lNalator.Cultivationcraft.Common.Blocks.BlockRegister;
import lNalator.Cultivationcraft.Common.Blocks.custom.FrozenBlock;
import lNalator.Cultivationcraft.Common.Blocks.util.TickableBlockEntity;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.QiSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FrozenBlockEntity extends BlockEntity implements TickableBlockEntity {

    protected int FREEZE_PROGRESS_TICKS = 0;
    // Stored here rather than in the block state, where a 0-1000 property multiplied the block's states
    protected int FREEZE_DURATION_TICKS = 50;
    protected ItemStack itemStack;
    protected BlockPos blockPos;
    protected BlockState oldBlockState;
    protected BlockEntity oldBlockEntity;
    protected CompoundTag oldBlockEntityData;
    protected boolean isSecondBlock;

    protected FrozenBlockEntity connectedEntity;

    public FrozenBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockRegister.FROZEN_BLOCK_ENTITY.get(), blockPos, blockState);
        this.blockPos = blockPos;
        this.oldBlockState = Blocks.AIR.defaultBlockState();
        this.oldBlockEntity = null;
        this.oldBlockEntityData = null;
        this.itemStack = new ItemStack(this.oldBlockState.getBlock().asItem());

        this.isSecondBlock = blockState.getValue(FrozenBlock.IS_SECOND_BLOCK);
    }

    public FrozenBlockEntity(BlockPos blockPos, BlockState blockState, BlockState oldBlockState, BlockEntity oldBlockEntity, CompoundTag oldBlockEntityData, boolean isSecondBlock) {
        super(BlockRegister.FROZEN_BLOCK_ENTITY.get(), blockPos, blockState);
        this.blockPos = blockPos;
        this.oldBlockState = oldBlockState;
        this.oldBlockEntity = oldBlockEntity;
        this.oldBlockEntityData = oldBlockEntityData;
        this.itemStack = new ItemStack(this.oldBlockState.getBlock().asItem());
        this.isSecondBlock = isSecondBlock;
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);

        CompoundTag cultivationCraftData = nbt.getCompound(Cultivationcraft.MODID);
        FREEZE_PROGRESS_TICKS = cultivationCraftData.getInt("unfreezeTicks");
        if (cultivationCraftData.contains("freezeDuration")) {
            FREEZE_DURATION_TICKS = cultivationCraftData.getInt("freezeDuration");
        }

        this.oldBlockState = NbtUtils.readBlockState(cultivationCraftData.getCompound("FrozenBlockState"));
        this.blockPos = NbtUtils.readBlockPos(cultivationCraftData.getCompound("FrozenBlockPos"));
        this.itemStack = new ItemStack(this.oldBlockState.getBlock().asItem());
        this.isSecondBlock = cultivationCraftData.getBoolean("isSecondBlock");

        // The original block entity's data (container contents...) is the only copy while frozen
        this.oldBlockEntityData = cultivationCraftData.contains("FrozenBlockEntityData") ? cultivationCraftData.getCompound("FrozenBlockEntityData") : null;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);

        var cultivationCraftData = new CompoundTag();
        cultivationCraftData.putInt("unfreezeTicks", FREEZE_PROGRESS_TICKS);
        cultivationCraftData.putInt("freezeDuration", FREEZE_DURATION_TICKS);

        cultivationCraftData.put("FrozenBlockState", NbtUtils.writeBlockState(oldBlockState));
        cultivationCraftData.put("FrozenBlockPos", NbtUtils.writeBlockPos(blockPos));
        cultivationCraftData.putBoolean("isSecondBlock", isSecondBlock);

        if (oldBlockEntityData != null) {
            cultivationCraftData.put("FrozenBlockEntityData", oldBlockEntityData);
        }
        nbt.put(Cultivationcraft.MODID, cultivationCraftData);
    }

    @Override
    public @NotNull
    CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();
        saveAdditional(nbt);
        return nbt;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void setOldBlockFields(BlockState oldBlockState, BlockEntity oldBlockEntity, CompoundTag oldBlockEntityData) {
        this.oldBlockState = oldBlockState;
        this.oldBlockEntity = oldBlockEntity;
        this.oldBlockEntityData = oldBlockEntityData;
        setChanged();
    }

    public void setDuration(int duration) {
        FREEZE_DURATION_TICKS = duration;
        setChanged();
    }

    // A new freeze on an already frozen block extends it to at least the new duration
    public void refresh(int duration) {
        FREEZE_DURATION_TICKS = Math.max(FREEZE_DURATION_TICKS - FREEZE_PROGRESS_TICKS, duration);
        FREEZE_PROGRESS_TICKS = 0;
        setChanged();
    }

    public void setConnected(FrozenBlockEntity second) {
        connectedEntity = second;
    }

    public boolean isSecondBlock() {
        return isSecondBlock;
    }

    public BlockState getOldBlockState() {
        return oldBlockState;
    }

    public ItemStack getRenderStack() {
        ItemStack stack;
        if (oldBlockState != null && !isSecondBlock) {
            stack = itemStack.getItem().getDefaultInstance();
        } else {
            stack = new ItemStack(Blocks.AIR.asItem());
        }
        return stack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void thawBlock() {
        Level world = this.getLevel();
        if (world != null && this.oldBlockState != null && this.blockPos != null) {
            // Revert the block to its previous state
            CompoundTag data = oldBlockEntityData;
            BlockPos pos = this.blockPos;
            world.setBlockAndUpdate(pos, this.oldBlockState);

            // Restore the saved data into the block entity the original block just created
            if (data != null) {
                BlockEntity restored = world.getBlockEntity(pos);
                if (restored != null) {
                    restored.load(data);
                    restored.setChanged();
                }
            }

            if (connectedEntity != null) {
                connectedEntity.thawBlock();
            }
        }
    }

    protected static boolean iceSourceNearby(Level level, BlockPos pos) {
        for (QiSource source : ChunkQiSources.getQiSourcesInRange(level, Vec3.atCenterOf(pos), 0)) {
            if (Elements.iceElement.equals(source.getElement())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void tick(Level level, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        this.FREEZE_PROGRESS_TICKS++;

        // Frozen water stays frozen while an Ice Qi source reaches it (design D7)
        if (FREEZE_PROGRESS_TICKS % 40 == 0 && oldBlockState.is(Blocks.WATER) && iceSourceNearby(level, blockPos)) {
            FREEZE_PROGRESS_TICKS = 0;
        }

        if (this.FREEZE_PROGRESS_TICKS >= FREEZE_DURATION_TICKS) {
            this.thawBlock();
            this.FREEZE_PROGRESS_TICKS = 0;
            setChanged(level, blockPos, blockState);
        }
    }

}
