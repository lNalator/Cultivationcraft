package lNalator.Cultivationcraft.Common.Capabilities.ChunkQiSources;

import lNalator.Cultivationcraft.Common.Qi.QiSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;

public interface IChunkQiSources {

    public void setChunkPos(ChunkPos position);

    public ChunkPos getChunkPos();

    public void setDimension(ResourceLocation dim);

    public ResourceLocation getDimension();

    public List<QiSource> getQiSources();

    public void setQiSources(List<QiSource> sources);

    public int countQiSources();

    public boolean tick(Level level);

    public void generateQiSources(Level level);

    public CompoundTag writeNBT();

    public void readNBT(CompoundTag nbt);
}
