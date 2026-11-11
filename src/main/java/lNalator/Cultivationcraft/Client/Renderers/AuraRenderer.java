package lNalator.Cultivationcraft.Client.Renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStatsCapability;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniquesCapability;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Common.Qi.Elements.Element;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Techniques.AuraTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aura zone: a translucent bubble in the aura's element colour, brighter at its silhouette, and a ring on the ground
 * at the edge of the zone, so every player nearby sees where the aura hits. Purely visual.
 */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT)
public final class AuraRenderer {

    private static final class Zone {
        float fade;
        float previousFade;
        double range;
        float red;
        float green;
        float blue;
    }

    private static final int FADE_TICKS = 8;
    private static final int LATITUDES = 16;
    private static final int LONGITUDES = 48;
    private static final double RING_WIDTH = 0.25;
    private static final double RING_GLOW = 1.5;
    // Distance around the bubble's wall over which the view switches between the outside and inside looks
    private static final double INSIDE_BLEND = 1;

    private static final Map<UUID, Zone> zones = new HashMap<>();

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (mc.level == null) {
            zones.clear();
            return;
        }

        if (mc.isPaused()) {
            return;
        }

        for (Zone zone : zones.values()) {
            zone.previousFade = zone.fade;
        }

        for (Player player : mc.level.players()) {
            AuraTechnique aura = activeAura(player);

            if (aura == null) {
                continue;
            }

            Zone zone = zones.computeIfAbsent(player.getUUID(), id -> new Zone());
            zone.range = aura.getRange(player);

            // Lightened element colour, so dark elements (Earth...) stay visible
            Element element = Elements.getElement(element(player));
            zone.red = 0.25f + 0.75f * element.color.getRed() / 255f;
            zone.green = 0.25f + 0.75f * element.color.getGreen() / 255f;
            zone.blue = 0.25f + 0.75f * element.color.getBlue() / 255f;

            zone.fade = Math.min(1, zone.fade + 1f / FADE_TICKS);
        }

        // Stopped auras fade out, the ones of players who left are dropped
        zones.entrySet().removeIf(entry -> {
            Player player = mc.level.getPlayerByUUID(entry.getKey());

            if (player == null) {
                return true;
            }

            if (activeAura(player) != null) {
                return false;
            }

            Zone zone = entry.getValue();
            zone.fade = Math.max(0, zone.fade - 1f / FADE_TICKS);
            return zone.fade <= 0 && zone.previousFade <= 0;
        });
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || zones.isEmpty() || mc.level == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        double time = mc.level.getGameTime() + partialTick;
        Matrix4f matrix = event.getPoseStack().last().pose();

        // Same matrix handling as GroundSlamRenderer: the event's pose already holds the view rotation
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Map.Entry<UUID, Zone> entry : zones.entrySet()) {
            Player player = mc.level.getPlayerByUUID(entry.getKey());
            Zone zone = entry.getValue();
            float fade = zone.previousFade + (zone.fade - zone.previousFade) * partialTick;

            if (player == null || fade <= 0) {
                continue;
            }

            // The aura hits entities whose feet are within range of the owner's feet: the zone is centred on them
            Vec3 center = player.getPosition(partialTick).subtract(camera);
            // 1 when the camera is inside the bubble (its owner included), 0 outside
            double inside = 1 - smoothstep(zone.range - INSIDE_BLEND, zone.range + INSIDE_BLEND, center.length());

            bubble(buffer, matrix, center, zone, fade, inside, time);
            groundRing(buffer, matrix, center, zone, fade, time);
        }

        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    // Sphere of the zone's radius, with faint bands slowly rising along it. Seen from outside: nearly clear face on,
    // brighter towards its silhouette (like a soap bubble). Seen from inside, where every wall faces the camera:
    // a light veil all around, brighter towards the horizon, so the zone reads as well from both sides
    private static void bubble(BufferBuilder buffer, Matrix4f matrix, Vec3 center, Zone zone, float strength, double inside, double time) {
        for (int i = 0; i < LATITUDES; i++) {
            double lat0 = Math.PI * i / LATITUDES - Math.PI / 2;
            double lat1 = Math.PI * (i + 1) / LATITUDES - Math.PI / 2;

            for (int j = 0; j < LONGITUDES; j++) {
                double lon0 = Math.PI * 2 * j / LONGITUDES;
                double lon1 = Math.PI * 2 * (j + 1) / LONGITUDES;

                bubbleVertex(buffer, matrix, center, zone, strength, inside, time, lat0, lon0);
                bubbleVertex(buffer, matrix, center, zone, strength, inside, time, lat1, lon0);
                bubbleVertex(buffer, matrix, center, zone, strength, inside, time, lat1, lon1);
                bubbleVertex(buffer, matrix, center, zone, strength, inside, time, lat0, lon1);
            }
        }
    }

    private static void bubbleVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 center, Zone zone, float strength, double inside, double time, double lat, double lon) {
        Vec3 normal = new Vec3(Math.cos(lat) * Math.cos(lon), Math.sin(lat), Math.cos(lat) * Math.sin(lon));
        Vec3 position = center.add(normal.scale(zone.range));

        // Positions are camera relative: the view direction is the position itself
        double facing = position.lengthSqr() < 1.0E-6 ? 1 : Math.abs(normal.dot(position.normalize()));
        double outsideAlpha = 0.03 + 0.3 * Math.pow(1 - facing, 3);
        double insideAlpha = 0.08 + 0.22 * Math.pow(1 - Math.abs(Math.sin(lat)), 2);
        double bands = 0.75 + 0.25 * Math.sin(lat * 8 - time * 0.12);
        float alpha = (float) (strength * (outsideAlpha + (insideAlpha - outsideAlpha) * inside) * bands);

        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(zone.red, zone.green, zone.blue, alpha).endVertex();
    }

    // Bright line on the ground at the edge of the zone, with a glow fading towards the inside; it breathes slowly
    private static void groundRing(BufferBuilder buffer, Matrix4f matrix, Vec3 center, Zone zone, float strength, double time) {
        double y = center.y + 0.05;
        double outer = zone.range;
        double edge = Math.max(0, outer - RING_WIDTH);
        double inner = Math.max(0, edge - RING_GLOW);
        float lineAlpha = (float) (strength * (0.5 + 0.1 * Math.sin(time * 0.2)));
        float glowAlpha = strength * 0.15f;

        for (int j = 0; j < LONGITUDES; j++) {
            double lon0 = Math.PI * 2 * j / LONGITUDES;
            double lon1 = Math.PI * 2 * (j + 1) / LONGITUDES;

            ringQuad(buffer, matrix, center, y, zone, lon0, lon1, edge, lineAlpha, outer, lineAlpha);
            ringQuad(buffer, matrix, center, y, zone, lon0, lon1, inner, 0, edge, glowAlpha);
        }
    }

    private static void ringQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 center, double y, Zone zone, double lon0, double lon1,
                                 double innerRadius, float innerAlpha, double outerRadius, float outerAlpha) {
        ringVertex(buffer, matrix, center, y, zone, lon0, innerRadius, innerAlpha);
        ringVertex(buffer, matrix, center, y, zone, lon0, outerRadius, outerAlpha);
        ringVertex(buffer, matrix, center, y, zone, lon1, outerRadius, outerAlpha);
        ringVertex(buffer, matrix, center, y, zone, lon1, innerRadius, innerAlpha);
    }

    private static void ringVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 center, double y, Zone zone, double lon, double radius, float alpha) {
        buffer.vertex(matrix, (float) (center.x + Math.cos(lon) * radius), (float) y, (float) (center.z + Math.sin(lon) * radius))
                .color(zone.red, zone.green, zone.blue, alpha).endVertex();
    }

    // Radius of a player's aura zone while it is shown (fading out included), -1 without aura
    public static double zoneRange(Player player) {
        Zone zone = zones.get(player.getUUID());
        return zone != null && zone.fade > 0 ? zone.range : -1;
    }

    // How visible a player's aura zone is, from 0 to 1 while it fades in and out
    public static float zoneFade(Player player, float partialTick) {
        Zone zone = zones.get(player.getUUID());
        return zone == null ? 0 : zone.previousFade + (zone.fade - zone.previousFade) * partialTick;
    }

    private static double smoothstep(double start, double end, double value) {
        double t = Math.max(0, Math.min(1, (value - start) / (end - start)));
        return t * t * (3 - 2 * t);
    }

    private static AuraTechnique activeAura(Player player) {
        ICultivatorTechniques techs = player.getCapability(CultivatorTechniquesCapability.INSTANCE).resolve().orElse(null);

        if (techs == null) {
            return null;
        }

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            Technique tech = techs.getTechnique(i);

            if (tech instanceof AuraTechnique aura && aura.isActive()) {
                return aura;
            }
        }

        return null;
    }

    // Same element as AuraTechnique.getElement: the one of the elemental core, none otherwise
    private static ResourceLocation element(Player player) {
        ICultivatorStats stats = player.getCapability(CultivatorStatsCapability.INSTANCE).resolve().orElse(null);

        if (stats != null) {
            for (TechniqueModifier mod : stats.getCultivation().getModifiers()) {
                if (mod.hasElement()) {
                    return mod.getElement();
                }
            }
        }

        return Elements.noElement;
    }
}
