package DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources;

import DaoOfModding.Cultivationcraft.Common.Qi.QiSource;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSourceConfig;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;

public class ChunkQiSources implements IChunkQiSources
{
    // TODO: Different dimension spawning

    ChunkPos chunkPos;
    List<QiSource> QiSources = new ArrayList<QiSource>();

    public void setChunkPos(ChunkPos position)
    {
        chunkPos = position;
    }

    public ChunkPos getChunkPos()
    {
        return chunkPos;
    }

    public List<QiSource> getQiSources()
    {
        return QiSources;
    }

    public void setQiSources(List<QiSource> sources)
    {
        QiSources = sources;
    }

    public void generateQiSources()
    {
        for (int i = 0; i < QiSourceConfig.getQiSourceInChunk(); i++)
            generateQiSource();
    }

    private void generateQiSource()
    {
        // Generate a random xPos and zPos somewhere within the chunk
        int xPos = (int)(Math.random() * 15) + (chunkPos.x << 4);
        int zPos = (int)(Math.random() * 15) + (chunkPos.z << 4);

        // Generate a random yPos, more likely to be at ground level (0.32)
        double x = Math.random();

        int yPos = (int)(((4 * Math.pow(x, 3)) - (5.28 * Math.pow(x, 2)) + (2.28 * x)) * 200);

        QiSources.add(new QiSource(new BlockPos(xPos, yPos, zPos)));
    }

    // Return a specified players CultivatorStats
    public static IChunkQiSources getChunkQiSources(Chunk chunk) {
        return chunk.getCapability(ChunkQiSourcesCapability.CULTIVATOR_STATS_CAPABILITY).orElseThrow(() -> new IllegalArgumentException("getting chunk Qi sources"));
    }
}