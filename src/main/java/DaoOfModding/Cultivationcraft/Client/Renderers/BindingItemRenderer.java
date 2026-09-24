package DaoOfModding.Cultivationcraft.Client.Renderers;

import DaoOfModding.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A cosmetic projection, with a small continuous stream of chest-to-item motes. */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT)
public final class BindingItemRenderer {
    private record Visual(ItemStack item, int color, long started, long updated) {}
    private static final Map<UUID, Visual> visuals = new HashMap<>();
    private static final MultiBufferSource.BufferSource ITEMS = MultiBufferSource.immediate(new BufferBuilder(4096));
    private static final BufferBuilder MOTES = new BufferBuilder(16384);
    private static ClientLevel world;

    private static void checkWorld(ClientLevel current) {
        if (world != current) {
            visuals.clear();
            world = current;
        }
    }

    public static void update(UUID player, ResourceLocation dimension, ItemStack item, int color) {
        ClientLevel current = Minecraft.getInstance().level;
        checkWorld(current);
        if (current == null || !current.dimension().location().equals(dimension)) return;
        if (item.isEmpty()) {
            visuals.remove(player);
            return;
        }
        long time = current.getGameTime();
        Visual previous = visuals.get(player);
        long started = previous != null && ItemStack.matches(previous.item, item) ? previous.started : time;
        visuals.put(player, new Visual(item.copy(), color, started, time));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientLevel current = Minecraft.getInstance().level;
        checkWorld(current);
        if (current == null) return;
        // Expire projections after an entity leaves tracking range or disconnects.
        visuals.entrySet().removeIf(entry -> current.getGameTime() - entry.getValue().updated > 100);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != world || visuals.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        double time = mc.level.getGameTime() + (double) partialTick;
        PoseStack pose = event.getPoseStack();
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        var shader = RenderSystem.getShader();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        try {
            for (var entry : visuals.entrySet()) {
                Player player = mc.level.getPlayerByUUID(entry.getKey());
                if (!visible(player, mc, camera)) continue;
                Vec3 forward = forward(player, partialTick);
                Vec3 chest = chest(player, partialTick, forward);
                Vec3 target = target(chest, forward, time - entry.getValue().started);
                pose.pushPose();
                try {
                    pose.translate(target.x - camera.x, target.y - camera.y, target.z - camera.z);
                    pose.mulPose(Vector3f.YP.rotationDegrees((float) ((time - entry.getValue().started) * 1.2 % 360)));
                    pose.scale(.65f, .65f, .65f);
                    mc.getItemRenderer().renderStatic(entry.getValue().item, ItemTransforms.TransformType.FIXED,
                            LevelRenderer.getLightColor(mc.level, new BlockPos(target)), OverlayTexture.NO_OVERLAY,
                            pose, ITEMS, player.getId());
                } finally {
                    pose.popPose();
                }
            }
            ITEMS.endBatch();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            Vector3f rightAxis = new Vector3f(1, 0, 0);
            Vector3f upAxis = new Vector3f(0, 1, 0);
            rightAxis.transform(event.getCamera().rotation());
            upAxis.transform(event.getCamera().rotation());
            Vec3 right = new Vec3(rightAxis.x(), rightAxis.y(), rightAxis.z());
            Vec3 up = new Vec3(upAxis.x(), upAxis.y(), upAxis.z());
            MOTES.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            for (var entry : visuals.entrySet()) {
                Player player = mc.level.getPlayerByUUID(entry.getKey());
                if (!visible(player, mc, camera)) continue;
                Visual visual = entry.getValue();
                Vec3 forward = forward(player, partialTick);
                Vec3 chest = chest(player, partialTick, forward);
                double elapsed = time - visual.started;
                drawMotes(pose.last().pose(), chest, target(chest, forward, elapsed), forward,
                        camera, right, up, elapsed, visual.color);
            }
            BufferUploader.drawWithShader(MOTES.end());
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(depthWrite);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private static boolean visible(Player player, Minecraft mc, Vec3 camera) {
        return player != null && player.isAlive() && !player.isSpectator()
                && !player.isInvisibleTo(mc.player) && player.position().distanceToSqr(camera) < 48 * 48;
    }

    private static Vec3 forward(Player player, float partialTick) {
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        // In first person the body may lag behind the direction the player is facing.
        if (player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson())
            yaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
    }

    private static Vec3 chest(Player player, float partialTick, Vec3 forward) {
        return player.getPosition(partialTick).add(0, player.getBbHeight() * .65, 0).add(forward.scale(.18));
    }

    private static Vec3 target(Vec3 chest, Vec3 forward, double time) {
        return chest.add(forward.scale(.95)).add(0, .1 + Math.sin(time * .065) * .045, 0);
    }

    private static void drawMotes(Matrix4f matrix, Vec3 chest, Vec3 target, Vec3 forward, Vec3 camera,
                                  Vec3 right, Vec3 up, double elapsed, int color) {
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        // One mote every three ticks; each takes 1.8 seconds to reach the item.
        // Analytic paths stay smooth at any frame rate and follow the moving player.
        for (int i = 0; i < 12; i++) {
            double age = elapsed - i * 3;
            if (age < 0) continue;
            double progress = (age % 36) / 36;
            double envelope = Math.pow(Math.sin(progress * Math.PI), 1.3);
            double angle = i * 2.39996 + progress * Math.PI;
            Vec3 position = chest.lerp(target, progress)
                    .add(side.scale(Math.sin(angle) * .09 * Math.sin(progress * Math.PI)))
                    .add(0, Math.cos(angle) * .055 * Math.sin(progress * Math.PI), 0).subtract(camera);
            glow(matrix, position, right, up, .045 * envelope, color, (float) (.8 * envelope));
        }
    }

    private static void glow(Matrix4f matrix, Vec3 center, Vec3 right, Vec3 up, double radius, int color, float alpha) {
        // Soft round, camera-facing particles: transparent edges rather than square sprites.
        for (int segment = 0; segment < 12; segment++) {
            double a = segment * Math.PI / 6, b = (segment + 1) * Math.PI / 6;
            vertex(matrix, center, color, alpha);
            vertex(matrix, center.add(right.scale(Math.cos(a) * radius)).add(up.scale(Math.sin(a) * radius)), color, 0);
            vertex(matrix, center.add(right.scale(Math.cos(b) * radius)).add(up.scale(Math.sin(b) * radius)), color, 0);
        }
    }

    private static void vertex(Matrix4f matrix, Vec3 point, int color, float alpha) {
        MOTES.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (int) (alpha * 255)).endVertex();
    }
}
