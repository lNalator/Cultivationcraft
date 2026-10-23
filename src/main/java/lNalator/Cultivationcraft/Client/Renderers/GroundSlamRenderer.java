package lNalator.Cultivationcraft.Client.Renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Falling ground slam: fake copies of the ground blocks lift and tilt away from the impact, then settle back,
 * over cracks and block particles. Purely visual, the world is never changed.
 */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT)
public final class GroundSlamRenderer {

    private record Piece(BlockPos pos, BlockState state, double distance, int crackId) {

    }

    private record Slam(Vec3 center, float radius, long started, List<Piece> pieces) {

    }

    private static final int DURATION = 30;
    private static final double MAX_LIFT = 0.45;
    private static final float MAX_TILT = 20;
    // Negative ids so the cracks never collide with an entity breaking a block
    private static int nextCrackId = -1000;

    private static final List<Slam> slams = new ArrayList<>();
    private static final MultiBufferSource.BufferSource BLOCKS = MultiBufferSource.immediate(new BufferBuilder(16384));

    public static void add(Vec3 center, float radius) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        List<Piece> pieces = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        BlockPos ground = new BlockPos(center).below();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance > radius) {
                    continue;
                }

                // The ground surface can be a block higher or lower than the landing spot
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos pos = ground.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (state.getRenderShape() == RenderShape.MODEL && state.isCollisionShapeFullBlock(level, pos) && level.getBlockState(pos.above()).isAir()) {
                        int crackId = nextCrackId--;
                        level.destroyBlockProgress(crackId, pos, (int) (6 * (1 - distance / (radius + 1))) + 3);
                        pieces.add(new Piece(pos, state, distance, crackId));

                        for (int i = 0; i < 3; i++) {
                            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + Math.random(), pos.getY() + 1.05, pos.getZ() + Math.random(),
                                    dx * 0.05, 0.15 + Math.random() * 0.15, dz * 0.05);
                        }

                        break;
                    }
                }
            }
        }

        slams.add(new Slam(center, radius, level.getGameTime(), pieces));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        ClientLevel level = Minecraft.getInstance().level;

        if (event.phase != TickEvent.Phase.END || slams.isEmpty()) {
            return;
        }

        slams.removeIf(slam -> {
            if (level != null && level.getGameTime() - slam.started <= DURATION + 10) {
                return false;
            }

            if (level != null) {
                slam.pieces.forEach(piece -> level.destroyBlockProgress(piece.crackId, piece.pos, -1));
            }

            return true;
        });
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || slams.isEmpty() || mc.level == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        double time = mc.level.getGameTime() + event.getPartialTick();

        // Same matrix handling as RefinementItemRenderer: the event's pose already holds the view rotation
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        for (Slam slam : slams) {
            double progress = (time - slam.started) / DURATION;

            if (progress < 0 || progress > 1) {
                continue;
            }

            // Fast rise, slow settle
            double rise = Math.sin(Math.PI * Math.sqrt(progress));

            for (Piece piece : slam.pieces) {
                double falloff = 1 - piece.distance / (slam.radius + 1);
                double lift = MAX_LIFT * rise * falloff;
                Vec3 away = new Vec3(piece.pos.getX() + 0.5 - slam.center.x, 0, piece.pos.getZ() + 0.5 - slam.center.z);
                Vec3 axis = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : new Vec3(-away.z, 0, away.x).normalize();

                pose.pushPose();
                pose.translate(piece.pos.getX() + 0.5 - camera.x, piece.pos.getY() + lift - camera.y, piece.pos.getZ() + 0.5 - camera.z);
                // Tilt the outer edge up, away from the impact
                pose.mulPose(new Vector3f((float) axis.x, 0, (float) axis.z).rotationDegrees((float) (MAX_TILT * rise * falloff)));
                pose.translate(-0.5, 0, -0.5);
                mc.getBlockRenderer().renderSingleBlock(piece.state, pose, BLOCKS, LevelRenderer.getLightColor(mc.level, piece.pos.above()), OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }

        BLOCKS.endBatch();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }
}
