package lNalator.Cultivationcraft.Client.Renderers;

import lNalator.Cultivationcraft.Client.genericClientFunctions;
import lNalator.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import lNalator.Cultivationcraft.Common.Qi.Elements.Element;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.QiSource;
import lNalator.Cultivationcraft.Common.Register;
import lNalator.mlmanimator.Client.Poses.PoseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class QiSourceRenderer {

    // A source being absorbed sends one particle to each absorbing player every STREAM_INTERVAL ticks
    protected static final int STREAM_INTERVAL = 2;
    public static final double STREAM_SPEED = 0.5;

    public static QiSource qisource = null;
    public static Entity target = null;
    public static Element element = Elements.getElement(Elements.noElement);

    public static void renderQiSources(float partialTick) {
        int LevelChunks = Minecraft.getInstance().options.renderDistance().get() - 1;
        int LevelChunkX = genericClientFunctions.getPlayer().blockPosition().getX() >> 4;
        int LevelChunkZ = genericClientFunctions.getPlayer().blockPosition().getZ() >> 4;

        // Spawning follows game ticks: it used to add up the frame's partial tick, so the rate depended on the frame rate
        // and could almost stop (no visible stream) at some frame rates
        long gameTime = Minecraft.getInstance().level.getGameTime();

        LevelChunk QiLevelChunk;

        // Cycle through all the LevelChunks in render distance, rendering all the Qi sources they contain
        for (int x = -LevelChunks; x < LevelChunks; x++) {
            for (int z = -LevelChunks; z < LevelChunks; z++) {
                QiLevelChunk = Minecraft.getInstance().level.getChunk(LevelChunkX + x, LevelChunkZ + z);

                for (QiSource source : ChunkQiSources.getChunkQiSources(QiLevelChunk).getQiSources()) {
                    QiSourceRenderer.render(source, gameTime);
                }
            }
        }
    }

    public static void render(QiSource source, long gameTime) {
        // Several frames share one tick: spawn at most once per tick
        if (source.spawnedTick == gameTime) {
            return;
        }

        source.spawnedTick = gameTime;

        // While absorbed: a continuous stream towards every absorbing player
        if (!source.getAbsorbingPlayers().isEmpty()) {
            if (gameTime % STREAM_INTERVAL != 0) {
                return;
            }

            for (UUID id : source.getAbsorbingPlayers().keySet()) {
                Player absorbing = Minecraft.getInstance().level.getPlayerByUUID(id);

                if (absorbing != null) {
                    float height = PoseHandler.getPlayerPoseHandler(absorbing.getUUID()).getPlayerModel().getHeightAdjustment();
                    spawn(source, absorbing, absorbing.position().add(0, height, 0).subtract(Vec3.atLowerCornerOf(source.getPos())));
                }
            }

            return;
        }

        // Otherwise drift in random directions, more often the fuller the source is
        if (gameTime % (source.getSpawnTick() + 1 + QiSource.maxSpawnTime) != 0) {
            return;
        }

        Random random = new Random();
        spawn(source, null, new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5));
    }

    protected static void spawn(QiSource source, Player absorbing, Vec3 direction) {
        Random random = new Random();

        double x = source.getPos().getX() + (random.nextDouble() - 0.5) * 0.2;
        double y = source.getPos().getY() + (random.nextDouble() - 0.5) * 0.2;
        double z = source.getPos().getZ() + (random.nextDouble() - 0.5) * 0.2;

        direction = direction.normalize();

        qisource = source;
        target = absorbing;
        element = Elements.getElement(source.getElement());

        Minecraft.getInstance().level.addParticle(Register.qiParticleType.get(), x, y, z, direction.x, direction.y, direction.z);

        qisource = null;
        target = null;
        element = Elements.getElement(Elements.noElement);
    }
}
