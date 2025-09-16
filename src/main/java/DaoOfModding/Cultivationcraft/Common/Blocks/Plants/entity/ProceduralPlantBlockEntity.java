package DaoOfModding.Cultivationcraft.Common.Blocks.Plants.entity;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSource;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSourceConfig;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.Connection;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ProceduralPlantBlockEntity extends BlockEntity {
    public static final int MAX_SPIRITUAL_GROWTH = 9999;
    private static final int TIER_TWO_GROWTH = 100;
    private static final int TIER_THREE_GROWTH = 1000;

    private CompoundTag qiHostData; // Serialized QiSource data
    private int spiritualGrowth; // dynamic growth stat that drives tier/qi changes

    public ProceduralPlantBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegister.PROCEDURAL_PLANT_ENTITY.get(), pos, state);
    }

    private void markUpdated() {
        setChanged();
        if (level instanceof ServerLevel server) {
            BlockState state = getBlockState();
            server.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            server.getChunkSource().blockChanged(worldPosition);
        }
    }

    public void setQiHostData(CompoundTag tag) {
        this.qiHostData = tag == null ? null : tag.copy();
        markUpdated();
    }

    public CompoundTag getQiHostData() {
        return qiHostData == null ? null : qiHostData.copy();
    }

    public int getSpiritualGrowth() {
        return spiritualGrowth;
    }

    public void setSpiritualGrowth(int newGrowth) {
        int clamped = Mth.clamp(newGrowth, 0, MAX_SPIRITUAL_GROWTH);
        if (clamped != this.spiritualGrowth) {
            this.spiritualGrowth = clamped;
            markUpdated();
        }
    }

    public void incrementSpiritualGrowth(int amount) {
        if (amount == 0) {
            return;
        }
        int clamped = Mth.clamp(this.spiritualGrowth + amount, 0, MAX_SPIRITUAL_GROWTH);
        if (clamped != this.spiritualGrowth) {
            this.spiritualGrowth = clamped;
            markUpdated();
        }
    }

    public int getTier() {
        return growthToTier(spiritualGrowth);
    }

    public static int growthToTier(int growth) {
        if (growth >= TIER_THREE_GROWTH) {
            return 3;
        }
        if (growth >= TIER_TWO_GROWTH) {
            return 2;
        }
        return 1;
    }

    public int dynamicTier() {
        return getTier();
    }

    public void attachQiSourceIfMissing(ServerLevel srv, net.minecraft.resources.ResourceLocation element) {
        if (qiHostData != null) return;
        var state = getBlockState();
        if (!(state.getBlock() instanceof ProceduralPlantBlock)) return;
        var source = new QiSource(worldPosition, QiSourceConfig.generateRandomSize(), element, QiSourceConfig.generateRandomQiStorage(), QiSourceConfig.generateRandomQiRegen());
        var cap = ChunkQiSources.getChunkQiSources(srv.getChunkAt(worldPosition));
        cap.getQiSources().add(source);
        this.qiHostData = source.SerializeNBT();
        markUpdated();
        PacketHandler.sendChunkQiSourcesToClient(srv.getChunkAt(worldPosition));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        var state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof ProceduralPlantBlock)) return;
        if (!state.hasProperty(ProceduralPlantBlock.HOST_QI)) return;

        // Enforce: Only Tier 3 (growth milestone reached) may host a Qi Source
        if (state.getValue(ProceduralPlantBlock.HOST_QI) && getTier() < 3) {
            level.setBlock(worldPosition, state.setValue(ProceduralPlantBlock.HOST_QI, false), 3);
            return;
        }
        if (!state.getValue(ProceduralPlantBlock.HOST_QI)) return;

        // Only create once: if no stored data (worldgen), create and persist one now
        if (qiHostData == null) {
            if (!state.hasProperty(ProceduralPlantBlock.SPECIES)) return;
            int species = state.getValue(ProceduralPlantBlock.SPECIES);
            var srv = (ServerLevel) level;
            var genome = PlantGenomes.getById(srv, species);
            if (genome == null) return;

            var source = new QiSource(worldPosition, QiSourceConfig.generateRandomSize(), genome.qiElement(), QiSourceConfig.generateRandomQiStorage(), QiSourceConfig.generateRandomQiRegen());
            var cap = ChunkQiSources.getChunkQiSources(srv.getChunkAt(worldPosition));
            cap.getQiSources().add(source);
            this.qiHostData = source.SerializeNBT();
            markUpdated();
            PacketHandler.sendChunkQiSourcesToClient(srv.getChunkAt(worldPosition));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (qiHostData != null) tag.put("QiHostData", qiHostData);
        tag.putInt("SpiritualGrowth", spiritualGrowth);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("QiHostData")) this.qiHostData = tag.getCompound("QiHostData");
        else this.qiHostData = null;
        int storedGrowth;
        if (tag.contains("SpiritualGrowth")) {
            storedGrowth = tag.getInt("SpiritualGrowth");
        } else if (tag.contains("Age")) {
            storedGrowth = tag.getInt("Age");
        } else {
            storedGrowth = 0;
        }
        this.spiritualGrowth = Mth.clamp(storedGrowth, 0, MAX_SPIRITUAL_GROWTH);
    }

    // Client sync for HUD usage
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(pkt.getTag());
    }
}





