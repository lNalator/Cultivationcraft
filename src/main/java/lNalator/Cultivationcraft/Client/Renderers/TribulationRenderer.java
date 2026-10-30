package lNalator.Cultivationcraft.Client.Renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.Tribulation;
import lNalator.Cultivationcraft.Common.Qi.Techniques.DivineSenseTechnique;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Tribulation storm: black clouds swirling into a vortex above the tribulating player, with no rain. Before each strike
 * a light blue ring runs through the clouds from the edge to the eye, and the lightning strikes when it gets there.
 * Seen by every player around (see TribulationPacket), the sky turns black under the storm. Players with an active
 * Divine Sense are synced from further away: beyond the storm's range they only see a marker, through walls.
 */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT)
public final class TribulationRenderer {

    private static final class Storm {
        int entityId;
        Vec3 center;
        int tick;
        int strikes;
        int sinceUpdate = 0;
        float intensity = 0;
        // Height of the clouds, eased towards cloudTarget so they don't jump with the terrain
        double base = Double.NaN;

        // Phase of the Divine Sense marker's pulse, faster while a strike charges
        double pulse = 0;

        // Diffuse lightning flickering somewhere in the clouds
        int flickerAge = -1;
        double flickerAngle;
        double flickerRadius;
    }

    // The clouds float at least this high above the player, and above the ground when they are underground
    private static final double ABOVE_PLAYER = 100;
    private static final double ABOVE_GROUND = 60;
    private static final double RADIUS = 192;
    private static final double EYE = 3;
    // The clouds sink towards the eye, like a funnel
    private static final double FUNNEL_DEPTH = 12;

    private static final int GATHER_TICKS = 160;
    private static final int DISSIPATE_TICKS = 60;
    private static final int CHARGE_TICKS = 40;
    // The server syncs every 10 ticks: a storm not heard of for longer has ended
    private static final int LOST_TICKS = 30;
    private static final int FLICKER_TICKS = 3;

    // Size of a cloud block, how thick it is, and the height of each step down into the funnel
    private static final double CELL = 12;
    private static final double THICKNESS = 4;
    private static final double STEP = 2;

    private static final float[] RING_COLOR = {0.75f, 0.9f, 1f};
    private static final float[] FLASH_COLOR = {1f, 1f, 1f};

    // Divine Sense marker: far markers are drawn this close along the same line of sight, at the same size on screen,
    // so the render distance does not cut them
    private static final double MARKER_DISTANCE = 96;
    private static final int FLASH_TICKS = 6;
    private static final double PULSE_SPEED = 0.15;
    private static final double CHARGED_PULSE_SPEED = 0.9;

    private static final Map<Integer, Storm> storms = new HashMap<>();
    private static final Random random = new Random();

    public static void update(int entityId, Vec3 center, int tick, int strikes) {
        Storm storm = storms.get(entityId);

        if (storm == null) {
            storm = new Storm();
            storm.entityId = entityId;
            storms.put(entityId, storm);

            if (inStormRange(center)) {
                play(center, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 0.5f);
            }
        }

        storm.center = center;
        storm.tick = tick;
        storm.strikes = strikes;
        storm.sinceUpdate = 0;
    }

    private static void play(Vec3 center, SoundEvent sound, float volume, float pitch) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level != null) {
            level.playLocalSound(center.x, cloudTarget(center), center.z, sound, SoundSource.WEATHER, volume, pitch, false);
        }
    }

    // Further away, the storm is only synced for Divine Sense (see Tribulation.sync): no clouds, sky or sounds
    private static boolean inStormRange(Vec3 center) {
        var player = Minecraft.getInstance().player;

        return player != null && player.position().distanceToSqr(center) <= Tribulation.VISIBLE_RANGE * Tribulation.VISIBLE_RANGE;
    }

    private static boolean lost(Storm storm) {
        return storm.sinceUpdate > LOST_TICKS;
    }

    private static int ticksToStrike(Storm storm) {
        return Tribulation.STRIKE_INTERVAL - storm.tick % Tribulation.STRIKE_INTERVAL;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || storms.isEmpty()) {
            return;
        }

        if (Minecraft.getInstance().level == null) {
            storms.clear();
            return;
        }

        if (Minecraft.getInstance().isPaused()) {
            return;
        }

        storms.values().removeIf(storm -> {
            storm.sinceUpdate++;

            if (lost(storm)) {
                storm.intensity -= 1f / DISSIPATE_TICKS;
                return storm.intensity <= 0;
            }

            storm.tick++;

            double target = cloudTarget(center(storm, 1));
            storm.base = Double.isNaN(storm.base) ? target : storm.base + (target - storm.base) * 0.05;
            storm.pulse += ticksToStrike(storm) <= CHARGE_TICKS ? CHARGED_PULSE_SPEED : PULSE_SPEED;

            // Out of range the storm fades away instead of vanishing, and keeps ticking for the Divine Sense marker
            if (!inStormRange(storm.center)) {
                storm.intensity = Math.max(0, storm.intensity - 1f / DISSIPATE_TICKS);
                return false;
            }

            storm.intensity = Math.min(1, storm.intensity + 1f / GATHER_TICKS);

            if (ticksToStrike(storm) == CHARGE_TICKS) {
                play(storm.center, SoundEvents.BEACON_ACTIVATE, 2f, 1.6f);
            }

            // Low rumbles while the storm lasts
            if (random.nextInt(100) == 0) {
                play(storm.center, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.3f, 0.4f);
            }

            if (storm.flickerAge >= 0 && ++storm.flickerAge > FLICKER_TICKS) {
                storm.flickerAge = -1;
            }

            if (storm.flickerAge < 0 && random.nextInt(25) == 0) {
                storm.flickerAge = 0;
                storm.flickerAngle = random.nextDouble() * Math.PI * 2;
                storm.flickerRadius = EYE + random.nextDouble() * (RADIUS - EYE);
            }

            return false;
        });
    }

    // Follows the player when they are loaded on this client
    private static Vec3 center(Storm storm, float partialTick) {
        Entity entity = Minecraft.getInstance().level.getEntity(storm.entityId);

        return entity != null ? entity.getPosition(partialTick) : storm.center;
    }

    private static double cloudTarget(Vec3 center) {
        int ground = Minecraft.getInstance().level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(center.x), Mth.floor(center.z));

        return Math.max(center.y + ABOVE_PLAYER, ground + ABOVE_GROUND);
    }

    // Radius of the charging ring, or -1 when it is not charging
    private static double ringRadius(Storm storm, float partialTick) {
        double left = ticksToStrike(storm) - partialTick;

        if (lost(storm) || left > CHARGE_TICKS || left < 0) {
            return -1;
        }

        double progress = 1 - left / CHARGE_TICKS;

        // Slow at first, then rushing into the eye
        return EYE + (RADIUS - EYE) * (1 - progress * progress);
    }

    private static double outerRadius(Storm storm) {
        // The clouds converge from further away while the storm gathers
        return RADIUS * (1.5 - 0.5 * storm.intensity);
    }

    private static double cloudY(double r, double outer) {
        double fromEye = 1 - Mth.clamp(r / outer, 0, 1);

        return -FUNNEL_DEPTH * fromEye * fromEye;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (storms.isEmpty() || mc.level == null) {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            darkenSky(event);
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            renderSenseMarkers(event);
            return;
        }

        // Before particles and weather: the clouds write their depth, so rain and particles closer than them stay in front
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        Matrix4f matrix = event.getPoseStack().last().pose();

        // Same matrix handling as GroundSlamRenderer: the event's pose already holds the view rotation
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        for (Storm storm : storms.values()) {
            if (storm.intensity <= 0) {
                continue;
            }

            Vec3 center = center(storm, partialTick);
            double base = Double.isNaN(storm.base) ? cloudTarget(center) : storm.base;
            double time = storm.tick + partialTick;
            double ring = ringRadius(storm, partialTick);
            // Each strike is stronger: so is its ring
            double ringWidth = 10 + 2 * storm.strikes;

            // Same two passes as vanilla clouds: depth first, then colour, so only the nearest faces of the blocks show
            RenderSystem.defaultBlendFunc();
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(true);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            cloudBlocks(buffer, matrix, storm, center, base, camera, time, ring, ringWidth);
            tesselator.end();

            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(false);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            cloudBlocks(buffer, matrix, storm, center, base, camera, time, ring, ringWidth);
            tesselator.end();
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    // Divine Sense: a column of light from the tribulating player up to the clouds and a diamond around them, through walls.
    // It pulses faster and faster while a strike charges, and flashes white when it strikes
    private static void renderSenseMarkers(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        DivineSenseTechnique sense = DivineSenseBlockRenderer.activeSense(mc);

        if (sense == null || !sense.senses(DivineSenseTechnique.SENSE_TRIBULATIONS) || mc.options.hideGui) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        Matrix4f matrix = event.getPoseStack().last().pose();

        // Same state handling as DivineSenseBlockRenderer, drawn at the same stage
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        float lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        var shader = RenderSystem.getShader();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.lineWidth(2);

        try {
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

            for (Storm storm : storms.values()) {
                if (lost(storm) || storm.entityId == mc.player.getId()) {
                    continue;
                }

                Vec3 center = center(storm, partialTick);
                double top = Double.isNaN(storm.base) ? cloudTarget(center) : storm.base;
                double distance = center.distanceTo(camera);
                double scale = distance > MARKER_DISTANCE ? MARKER_DISTANCE / distance : 1;

                boolean flash = storm.tick >= Tribulation.STRIKE_INTERVAL && storm.tick % Tribulation.STRIKE_INTERVAL < FLASH_TICKS;
                float[] color = flash ? FLASH_COLOR : RING_COLOR;
                float alpha = flash ? 1 : (float) (0.55 + 0.3 * Math.sin(storm.pulse + partialTick * PULSE_SPEED));

                // The column, four lines around the player up to the base of the clouds
                double half = 0.6;
                for (int corner = 0; corner < 4; corner++) {
                    double x = center.x + ((corner & 1) == 0 ? -half : half);
                    double z = center.z + ((corner & 2) == 0 ? -half : half);
                    markerLine(buffer, matrix, camera, scale, new Vec3(x, center.y, z), new Vec3(x, top, z), color, alpha);
                }

                // The diamond, around the player's body
                Vec3 middle = center.add(0, 1, 0);
                double size = 1.5;
                Vec3[] tips = {middle.add(size, 0, 0), middle.add(0, 0, size), middle.add(-size, 0, 0), middle.add(0, 0, -size)};
                Vec3 up = middle.add(0, size * 1.3, 0), down = middle.add(0, -size * 1.3, 0);

                for (int tip = 0; tip < tips.length; tip++) {
                    markerLine(buffer, matrix, camera, scale, tips[tip], tips[(tip + 1) % tips.length], color, alpha);
                    markerLine(buffer, matrix, camera, scale, tips[tip], up, color, alpha);
                    markerLine(buffer, matrix, camera, scale, tips[tip], down, color, alpha);
                }
            }

            BufferUploader.drawWithShader(buffer.end());
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(depthWrite);
            if (depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            if (blend) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            RenderSystem.lineWidth(lineWidth);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    // Positions relative to the camera, brought closer by scale
    private static void markerLine(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, double scale, Vec3 start, Vec3 end, float[] color, float alpha) {
        Vec3 from = start.subtract(camera).scale(scale), to = end.subtract(camera).scale(scale);

        buffer.vertex(matrix, (float) from.x, (float) from.y, (float) from.z).color(color[0], color[1], color[2], alpha).endVertex();
        buffer.vertex(matrix, (float) to.x, (float) to.y, (float) to.z).color(color[0], color[1], color[2], alpha).endVertex();
    }

    // How thick the clouds are at this point, from the centre of the storm: spiral arms, the eye, the frayed edge
    private static double density(double dx, double dz, double outer, double time) {
        double r = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
        // Three spiral arms turning around the eye
        double swirl = 0.5 + 0.5 * Math.sin(3 * Math.atan2(dz, dx) + 5 * Math.log(r) - time * 0.08);
        double edge = Mth.clamp((outer - r) / (outer * 0.35), 0, 1);
        double eye = Mth.clamp((r - EYE) / 4, 0, 1);

        return edge * eye * (0.55 + 0.45 * swirl);
    }

    // The clouds are blocks on a world-aligned grid, like vanilla clouds, stepping down into the funnel
    private static void cloudBlocks(BufferBuilder buffer, Matrix4f matrix, Storm storm, Vec3 center, double base, Vec3 camera, double time, double ring, double ringWidth) {
        double outer = outerRadius(storm);
        int minX = Mth.floor((center.x - outer) / CELL), maxX = Mth.floor((center.x + outer) / CELL);
        int minZ = Mth.floor((center.z - outer) / CELL), maxZ = Mth.floor((center.z + outer) / CELL);

        for (int i = minX; i <= maxX; i++) {
            for (int k = minZ; k <= maxZ; k++) {
                double dx = (i + 0.5) * CELL - center.x;
                double dz = (k + 0.5) * CELL - center.z;
                double thick = density(dx, dz, outer, time);

                if (thick < 0.4) {
                    continue;
                }

                double r = Math.sqrt(dx * dx + dz * dz);
                // Grey at the edge of the storm, darker inwards, black around the eye
                double shade = 0.03 + 0.4 * Math.pow(Mth.clamp(r / outer, 0, 1), 0.8) + 0.03 * thick;

                // Light carried through the clouds: the charging ring and the flickers
                double glow = 0;

                if (ring >= 0) {
                    double d = (r - ring) / (ringWidth * 1.5);
                    glow += 0.8 * Math.exp(-d * d);
                }

                if (storm.flickerAge >= 0) {
                    double fx = dx - Math.cos(storm.flickerAngle) * storm.flickerRadius;
                    double fz = dz - Math.sin(storm.flickerAngle) * storm.flickerRadius;
                    glow += 0.6 * Math.exp(-(fx * fx + fz * fz) / 1024) * (1 - storm.flickerAge / (double) (FLICKER_TICKS + 1));
                }

                glow = Math.min(1, glow);

                float[] color = {
                        (float) (shade + (RING_COLOR[0] - shade) * glow),
                        (float) (shade + (RING_COLOR[1] - shade) * glow),
                        (float) (shade * 1.2 + (RING_COLOR[2] - shade * 1.2) * glow),
                        (float) (storm.intensity * (0.95 + 0.05 * glow))
                };

                float x0 = (float) (i * CELL - camera.x), x1 = (float) (x0 + CELL);
                float z0 = (float) (k * CELL - camera.z), z1 = (float) (z0 + CELL);
                float y0 = (float) (base + Math.round(cloudY(r, outer) / STEP) * STEP - camera.y), y1 = (float) (y0 + THICKNESS);

                // Lit from above, like vanilla clouds
                face(buffer, matrix, color, 1f, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
                face(buffer, matrix, color, 0.7f, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
                face(buffer, matrix, color, 0.85f, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
                face(buffer, matrix, color, 0.85f, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
                face(buffer, matrix, color, 0.8f, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
                face(buffer, matrix, color, 0.8f, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
            }
        }
    }

    private static void face(BufferBuilder buffer, Matrix4f matrix, float[] color, float light, float... corners) {
        for (int c = 0; c < 12; c += 3) {
            buffer.vertex(matrix, corners[c], corners[c + 1], corners[c + 2])
                    .color(color[0] * light, color[1] * light, color[2] * light, color[3]).endVertex();
        }
    }

    // How dark the sky is for this camera: full under the storm, fading out up to 2.5 radii away
    private static float darkness(Vec3 camera) {
        float dark = 0;

        for (Storm storm : storms.values()) {
            double distance = Math.sqrt(Math.pow(camera.x - storm.center.x, 2) + Math.pow(camera.z - storm.center.z, 2));
            dark = Math.max(dark, storm.intensity * (float) Mth.clamp(1 - (distance - RADIUS) / (RADIUS * 1.5), 0, 1));
        }

        return dark;
    }

    // Near the storm the sky turns dark grey to black: a dark box drawn around the camera over the sky, before the terrain
    private static void darkenSky(RenderLevelStageEvent event) {
        float dark = darkness(event.getCamera().getPosition());

        if (dark <= 0) {
            return;
        }

        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = event.getPoseStack().last().pose();
        float[] color = {0.05f, 0.05f, 0.06f, 0.92f * dark};
        float d = 64;

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        face(buffer, matrix, color, 1f, -d, d, -d, d, d, -d, d, d, d, -d, d, d);
        face(buffer, matrix, color, 1f, -d, -d, -d, -d, -d, d, d, -d, d, d, -d, -d);
        face(buffer, matrix, color, 1f, -d, -d, -d, d, -d, -d, d, d, -d, -d, d, -d);
        face(buffer, matrix, color, 1f, -d, -d, d, -d, d, d, d, d, d, d, -d, d);
        face(buffer, matrix, color, 1f, -d, -d, -d, -d, d, -d, -d, d, d, -d, -d, d);
        face(buffer, matrix, color, 1f, d, -d, -d, d, -d, d, d, d, d, d, d, -d);
        Tesselator.getInstance().end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    // The fog follows the sky, so the horizon darkens too
    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (storms.isEmpty()) {
            return;
        }

        float keep = 1 - 0.9f * darkness(event.getCamera().getPosition());
        event.setRed(event.getRed() * keep);
        event.setGreen(event.getGreen() * keep);
        event.setBlue(event.getBlue() * keep);
    }
}
