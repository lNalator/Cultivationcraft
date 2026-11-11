package lNalator.Cultivationcraft.Client.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Vigilance: when someone senses this player with Divine Sense, an arc on the edge of the screen points towards them,
 * like the damage indicator of a shooter, and fades out in 2 s (see DivineSensePingPacket).
 */
public final class DivineSensePingOverlay {

    private record Ping(int observerId, Vec3 position, long start) {

    }

    private static final int DURATION = 40;
    private static final int FADE_IN = 3;
    // Half the angle covered by the arc, in degrees
    private static final float HALF_ARC = 18;
    private static final int SEGMENTS = 12;
    private static final float THICKNESS = 8;
    private static final int COLOR = 0xD8C8FF;

    private static final List<Ping> pings = new ArrayList<>();
    private static ClientLevel pingLevel;

    public static void add(int observerId, Vec3 position) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        if (level != pingLevel) {
            pings.clear();
            pingLevel = level;
        }

        // A new ping from the same observer replaces the old one
        pings.removeIf(ping -> ping.observerId() == observerId);
        pings.add(new Ping(observerId, position, level.getGameTime()));
    }

    public static void render(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        if (pings.isEmpty() || mc.player == null || mc.level == null) {
            return;
        }

        if (mc.level != pingLevel) {
            pings.clear();
            return;
        }

        double now = mc.level.getGameTime() + partialTick;
        pings.removeIf(ping -> now - ping.start() >= DURATION);

        if (pings.isEmpty()) {
            return;
        }

        float centerX = mc.getWindow().getGuiScaledWidth() / 2f;
        float centerY = mc.getWindow().getGuiScaledHeight() / 2f;
        float radius = Math.min(centerX, centerY) * 0.6f;
        Vec3 eye = mc.player.getEyePosition(partialTick);
        float yaw = mc.player.getViewYRot(partialTick);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Ping ping : pings) {
            // Follows the observer while they are loaded on this client
            Entity observer = mc.level.getEntity(ping.observerId());
            Vec3 from = observer != null ? observer.getEyePosition(partialTick) : ping.position();
            double age = now - ping.start();
            float alpha = (float) (Math.min(1, age / FADE_IN) * (1 - age / DURATION));

            // Angle on screen, clockwise from the top: 0 straight ahead, 180 behind
            float targetYaw = (float) (Mth.atan2(-(from.x - eye.x), from.z - eye.z) * Mth.RAD_TO_DEG);
            float angle = Mth.wrapDegrees(targetYaw - yaw);

            arc(buffer, matrix, centerX, centerY, radius, angle, alpha);
        }

        Tesselator.getInstance().end();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void arc(BufferBuilder buffer, Matrix4f matrix, float centerX, float centerY, float radius, float angle, float alpha) {
        int red = (COLOR >> 16) & 255, green = (COLOR >> 8) & 255, blue = COLOR & 255;

        for (int segment = 0; segment < SEGMENTS; segment++) {
            float start = (float) segment / SEGMENTS, end = (float) (segment + 1) / SEGMENTS;
            // Fades towards both ends of the arc
            int startAlpha = Math.round(255 * 0.8f * alpha * Mth.sin(start * Mth.PI));
            int endAlpha = Math.round(255 * 0.8f * alpha * Mth.sin(end * Mth.PI));
            double a0 = Math.toRadians(angle - HALF_ARC + 2 * HALF_ARC * start);
            double a1 = Math.toRadians(angle - HALF_ARC + 2 * HALF_ARC * end);

            vertex(buffer, matrix, centerX, centerY, radius, a0, red, green, blue, startAlpha);
            vertex(buffer, matrix, centerX, centerY, radius + THICKNESS, a0, red, green, blue, 0);
            vertex(buffer, matrix, centerX, centerY, radius + THICKNESS, a1, red, green, blue, 0);
            vertex(buffer, matrix, centerX, centerY, radius, a1, red, green, blue, endAlpha);
        }
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float centerX, float centerY, float radius, double angle,
            int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, centerX + (float) Math.sin(angle) * radius, centerY - (float) Math.cos(angle) * radius, 0)
                .color(red, green, blue, alpha).endVertex();
    }
}
