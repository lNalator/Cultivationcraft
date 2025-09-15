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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ProceduralPlantBlockEntity extends BlockEntity {
    private CompoundTag qiHostData; // Serialized QiSource data

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

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        var state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof ProceduralPlantBlock)) return;
        if (!state.hasProperty(ProceduralPlantBlock.HOST_QI)) return;
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("QiHostData")) this.qiHostData = tag.getCompound("QiHostData");
        else this.qiHostData = null;
    }
}
