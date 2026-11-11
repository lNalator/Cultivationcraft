package lNalator.Cultivationcraft.Client.Renderers;

import lNalator.Cultivationcraft.Client.Renderer;
import lNalator.Cultivationcraft.Client.genericClientFunctions;
import lNalator.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import lNalator.Cultivationcraft.Common.Qi.Elements.Element;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.QiSource;
import lNalator.Cultivationcraft.Common.Register;
import lNalator.mlmanimator.Client.Poses.PoseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class QiSourceRenderer {

    // At the base absorption speed (BASE_FLOW Qi a tick), a source sends one particle every STREAM_INTERVAL ticks at
    // STREAM_SPEED blocks a tick. Faster absorption gives a denser and faster stream, so the stream follows the player's absorption
    protected static final double BASE_FLOW = 0.5;
    protected static final int STREAM_INTERVAL = 2;
    protected static final int MAX_STREAM_PARTICLES = 4;
    protected static final double STREAM_SPEED = 0.5;
    protected static final double MIN_STREAM_SPEED = 0.3;
    protected static final double MAX_STREAM_SPEED = 1.5;
    // Particles thrown around the player when a stream particle arrives
    protected static final int ARRIVAL_PARTICLES = 2;
    protected static final float ARRIVAL_DISTANCE = 0.6f;
    // While a source is absorbed its random drift is rarer and shorter
    protected static final int ABSORBED_DRIFT_INTERVAL = 3;
    protected static final float ABSORBED_DRIFT_DISTANCE = 0.4f;

    public static QiSource qisource = null;
    public static Entity target = null;
    public static Element element = Elements.getElement(Elements.noElement);
    public static float distanceScale = 1;
    public static double streamSpeed = STREAM_SPEED;

    // Source shown in the Divine Sense HUD: the one the player looks at
    public static QiSource focused = null;
    // A source is looked at within about 8 degrees of the view direction
    protected static final double FOCUS_DOT = 0.99;

    public static void renderQiSources(float partialTick) {
        int LevelChunks = Minecraft.getInstance().options.renderDistance().get() - 1;
        int LevelChunkX = genericClientFunctions.getPlayer().blockPosition().getX() >> 4;
        int LevelChunkZ = genericClientFunctions.getPlayer().blockPosition().getZ() >> 4;

        // Spawning follows game ticks: it used to add up the frame's partial tick, so the rate depended on the frame rate
        // and could almost stop (no visible stream) at some frame rates
        long gameTime = Minecraft.getInstance().level.getGameTime();

        LevelChunk QiLevelChunk;

        Player player = genericClientFunctions.getPlayer();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        double focusRange = 4 * Renderer.QiSenseRadius;
        QiSource lookedFocus = null;
        double bestDot = FOCUS_DOT;

        // Cycle through all the LevelChunks in render distance, rendering all the Qi sources they contain
        for (int x = -LevelChunks; x < LevelChunks; x++) {
            for (int z = -LevelChunks; z < LevelChunks; z++) {
                QiLevelChunk = Minecraft.getInstance().level.getChunk(LevelChunkX + x, LevelChunkZ + z);

                for (QiSource source : ChunkQiSources.getChunkQiSources(QiLevelChunk).getQiSources()) {
                    QiSourceRenderer.render(source, gameTime);

                    Vec3 toSource = Vec3.atCenterOf(source.getPos()).subtract(eye);
                    double dot = toSource.length() > focusRange ? -1 : toSource.normalize().dot(look);

                    if (dot > bestDot) {
                        bestDot = dot;
                        lookedFocus = source;
                    }
                }
            }
        }

        focused = lookedFocus;
    }

    public static void render(QiSource source, long gameTime) {
        // Several frames share one tick: spawn at most once per tick
        if (source.spawnedTick == gameTime) {
            return;
        }

        source.spawnedTick = gameTime;

        boolean absorbed = !source.getAbsorbingPlayers().isEmpty();

        // While absorbed: a continuous stream towards every absorbing player, as dense and fast as their absorption
        for (Map.Entry<UUID, Double> entry : source.getAbsorbingPlayers().entrySet()) {
            Player absorbing = Minecraft.getInstance().level.getPlayerByUUID(entry.getKey());

            if (absorbing == null) {
                continue;
            }

            double flow = entry.getValue() / BASE_FLOW;
            double perTick = flow / STREAM_INTERVAL;
            int count = perTick >= 1 ? Math.min(MAX_STREAM_PARTICLES, (int) Math.round(perTick)) : 1;

            if (perTick < 1 && gameTime % Math.max(1, Math.round(1 / perTick)) != 0) {
                continue;
            }

            streamSpeed = Mth.clamp(STREAM_SPEED * Math.sqrt(flow), MIN_STREAM_SPEED, MAX_STREAM_SPEED);
            float height = PoseHandler.getHeightAdjustment(absorbing.getUUID());

            for (int i = 0; i < count; i++) {
                spawn(source, absorbing, absorbing.position().add(0, height, 0).subtract(Vec3.atLowerCornerOf(source.getPos())), 1);
            }

            streamSpeed = STREAM_SPEED;
        }

        // Always drift in random directions, more often the fuller the source is, rarer and shorter while absorbed
        int interval = (source.getSpawnTick() + 1 + QiSource.maxSpawnTime) * (absorbed ? ABSORBED_DRIFT_INTERVAL : 1);

        if (gameTime % interval != 0) {
            return;
        }

        Random random = new Random();
        spawn(source, null, new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5), absorbed ? ABSORBED_DRIFT_DISTANCE : 1);
    }

    protected static void spawn(QiSource source, Player absorbing, Vec3 direction, float distance) {
        Random random = new Random();

        double x = source.getPos().getX() + (random.nextDouble() - 0.5) * 0.2;
        double y = source.getPos().getY() + (random.nextDouble() - 0.5) * 0.2;
        double z = source.getPos().getZ() + (random.nextDouble() - 0.5) * 0.2;

        direction = direction.normalize();

        qisource = source;
        target = absorbing;
        element = Elements.getElement(source.getElement());
        distanceScale = distance;

        Minecraft.getInstance().level.addParticle(Register.qiParticleType.get(), x, y, z, direction.x, direction.y, direction.z);

        qisource = null;
        target = null;
        element = Elements.getElement(Elements.noElement);
        distanceScale = 1;
    }

    // A stream particle reached its player: a small puff of the source's element around them shows the Qi arriving
    public static void arrive(double x, double y, double z, Element arriving) {
        element = arriving;
        distanceScale = ARRIVAL_DISTANCE;

        for (int i = 0; i < ARRIVAL_PARTICLES; i++) {
            Minecraft.getInstance().level.addParticle(Register.qiParticleType.get(), x, y, z, 0, 0, 0);
        }

        element = Elements.getElement(Elements.noElement);
        distanceScale = 1;
    }
}
