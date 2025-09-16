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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ProceduralPlantBlockEntity extends BlockEntity {
    private CompoundTag qiHostData; // Serialized QiSource data
    private int age; // increments on plant random ticks (dynamic tier source)

    public ProceduralPlantBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegister.PROCEDURAL_PLANT_ENTITY.get(), pos, state);
    }

    public void setQiHostData(CompoundTag tag) {
        this.qiHostData = tag == null ? null : tag.copy();
        setChanged();
    }

    public CompoundTag getQiHostData() {
        return qiHostData == null ? null : qiHostData.copy();
    }

    public int getAge() { return age; }
    public void incrementAge(int amount) { this.age += amount; if (this.age < 0) this.age = 0; setChanged(); }
    public int dynamicTier() { if (age >= 1000) return 3; if (age >= 100) return 2; return 1; }
    public void setAge(int newAge) { this.age = Math.max(0, newAge); setChanged(); }

    public void attachQiSourceIfMissing(ServerLevel srv, net.minecraft.resources.ResourceLocation element) {
        if (qiHostData != null) return;
        var state = getBlockState();
        if (!(state.getBlock() instanceof ProceduralPlantBlock)) return;
        var source = new QiSource(worldPosition, QiSourceConfig.generateRandomSize(), element, QiSourceConfig.generateRandomQiStorage(), QiSourceConfig.generateRandomQiRegen());
        var cap = ChunkQiSources.getChunkQiSources(srv.getChunkAt(worldPosition));
        cap.getQiSources().add(source);
        this.qiHostData = source.SerializeNBT();
        setChanged();
        PacketHandler.sendChunkQiSourcesToClient(srv.getChunkAt(worldPosition));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        var state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof ProceduralPlantBlock)) return;
        if (!state.hasProperty(ProceduralPlantBlock.HOST_QI)) return;

        // Enforce: Only Tier 3 (age 100+) may host a Qi Source
        if (state.getValue(ProceduralPlantBlock.HOST_QI) && dynamicTier() < 3) {
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
            setChanged();
            PacketHandler.sendChunkQiSourcesToClient(srv.getChunkAt(worldPosition));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (qiHostData != null) tag.put("QiHostData", qiHostData);
        tag.putInt("Age", age);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("QiHostData")) this.qiHostData = tag.getCompound("QiHostData");
        else this.qiHostData = null;
        this.age = tag.getInt("Age");
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
