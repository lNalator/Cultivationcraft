package lNalator.Cultivationcraft.Client.Renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Echo + Lightning synergy: a short jagged arc between the two targets of an echo, flickering for a few ticks.
 * Our own geometry with the lightning render type, not a vanilla lightning bolt.
 */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT)
public final class LightningArcRenderer {

    private record Arc(Vec3 from, Vec3 to, long started, long seed) {

    }

    private static final int DURATION = 8;
    private static final int SEGMENTS = 8;
    private static final double JITTER = 0.35;
    private static final float CORE_WIDTH = 0.04f;
    private static final float GLOW_WIDTH = 0.12f;

    private static final List<Arc> arcs = new ArrayList<>();
    private static final Random random = new Random();

    public static void add(Vec3 from, Vec3 to) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        arcs.add(new Arc(from, to, level.getGameTime(), random.nextLong()));

        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, (Math.random() - 0.5) * 0.4, Math.random() * 0.3, (Math.random() - 0.5) * 0.4);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        ClientLevel level = Minecraft.getInstance().level;

        if (event.phase != TickEvent.Phase.END || arcs.isEmpty()) {
            return;
        }

        arcs.removeIf(arc -> level == null || level.getGameTime() - arc.started > DURATION);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || arcs.isEmpty() || mc.level == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        double time = mc.level.getGameTime() + event.getPartialTick();

        // Same matrix handling as GroundSlamRenderer: the event's pose already holds the view rotation
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();

        for (Arc arc : arcs) {
            double age = time - arc.started;

            if (age < 0 || age > DURATION) {
                continue;
            }

            float alpha = (float) (1 - age / DURATION);
            // A new shape every 2 ticks, so the arc flickers
            Random shape = new Random(arc.seed + (long) (age / 2));
            Vec3[] points = new Vec3[SEGMENTS + 1];

            for (int i = 0; i <= SEGMENTS; i++) {
                Vec3 point = arc.from.lerp(arc.to, i / (double) SEGMENTS);

                // The ends stay on the targets
                if (i > 0 && i < SEGMENTS) {
                    point = point.add((shape.nextDouble() - 0.5) * 2 * JITTER, (shape.nextDouble() - 0.5) * 2 * JITTER, (shape.nextDouble() - 0.5) * 2 * JITTER);
                }

                points[i] = point.subtract(camera);
            }

            for (int i = 0; i < SEGMENTS; i++) {
                segment(consumer, matrix, points[i], points[i + 1], GLOW_WIDTH, 0.45f, 0.55f, 1f, 0.35f * alpha);
                segment(consumer, matrix, points[i], points[i + 1], CORE_WIDTH, 0.9f, 0.95f, 1f, 0.9f * alpha);
            }
        }

        buffers.endBatch(RenderType.lightning());

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    // Two crossed quads along the segment, each drawn on both sides (the render type culls back faces)
    private static void segment(VertexConsumer consumer, Matrix4f matrix, Vec3 a, Vec3 b, float width, float red, float green, float blue, float alpha) {
        Vec3 direction = b.subtract(a);

        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }

        Vec3 side = direction.cross(new Vec3(0, 1, 0));

        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }

        side = side.normalize().scale(width);
        Vec3 up = direction.cross(side).normalize().scale(width);

        quad(consumer, matrix, a, b, side, red, green, blue, alpha);
        quad(consumer, matrix, a, b, up, red, green, blue, alpha);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 offset, float red, float green, float blue, float alpha) {
        Vec3[] corners = {a.subtract(offset), a.add(offset), b.add(offset), b.subtract(offset)};

        for (int i = 0; i < 4; i++) {
            vertex(consumer, matrix, corners[i], red, green, blue, alpha);
        }

        for (int i = 3; i >= 0; i--) {
            vertex(consumer, matrix, corners[i], red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 pos, float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).color(red, green, blue, alpha).endVertex();
    }
}
