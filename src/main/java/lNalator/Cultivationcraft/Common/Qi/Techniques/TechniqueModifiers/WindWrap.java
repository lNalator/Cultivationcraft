package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Techniques.DivineSenseTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.FlightTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.Cultivationcraft.Network.Packets.WindWrapPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Levitation + Wind synergy: while flying, the player wraps up to 3 entities in wind (dedicated "Wind Wrap" key) and carries them along.
 * Looking at an entity within 4 blocks wraps or releases it, looking at nothing releases everyone. Each wrapped entity costs Qi
 * every second, more for a higher realm (mobs are realm 0). A wrapped player breaks free by sneaking for 1 s, at once when their realm
 * is higher than the carrier's. Everyone is released with a few seconds of Slow Falling when the flight stops or the Qi runs out.
 * Server only; the carrier's client gets the counter shown near the Qi orb.
 */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
public final class WindWrap {

    private static final class Wrapped {
        final Entity entity;
        int sneakTicks = 0;

        Wrapped(Entity entity) {
            this.entity = entity;
        }
    }

    // Wrapped entities beyond this distance from the carrier (teleport, lag) are released
    private static final double LOST_DISTANCE = 16;
    // Where the wrapped entities float, around and behind the carrier (degrees from the way the carrier looks)
    private static final double[] SLOT_ANGLES = {180, 130, 230, 90, 270, 155, 205, 110, 250, 180};
    private static final double SLOT_DISTANCE = 1.8;

    private static final Map<UUID, List<Wrapped>> carried = new HashMap<>();

    private WindWrap() {
    }

    public static boolean isFlying(Player player) {
        ICultivatorTechniques techniques = CultivatorTechniques.getCultivatorTechniques(player);

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            Technique technique = techniques.getTechnique(i);

            if (technique instanceof FlightTechnique && technique.isActive()) {
                return true;
            }
        }

        return false;
    }

    // The "Wind Wrap" key, sent by the client
    public static void keyPressed(ServerPlayer player) {
        if (!CoreSynergies.has(player, CoreSynergies.LEVITATION_WIND) || !isFlying(player) || player.isOnGround()) {
            return;
        }

        List<Wrapped> list = carried.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        Entity looked = lookedAt(player, Config.Server.windWrapRange());

        if (looked == null) {
            releaseAll(player, false);
            return;
        }

        for (Wrapped wrapped : list) {
            if (wrapped.entity == looked) {
                list.remove(wrapped);
                release(wrapped.entity, false);
                sync(player);
                return;
            }
        }

        if (list.size() >= Config.Server.windWrapMax() || isWrapped(looked) || isCarrying(looked)) {
            return;
        }

        list.add(new Wrapped(looked));
        looked.fallDistance = 0;
        player.level.playSound(null, looked.getX(), looked.getY(), looked.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.3f, 1.6f);
        sync(player);
    }

    // First entity on the line of sight within range, not behind a block
    private static Entity lookedAt(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));

        HitResult block = player.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }

        AABB area = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, area, WindWrap::canWrap, eye.distanceToSqr(end));

        return hit == null ? null : hit.getEntity();
    }

    private static boolean canWrap(Entity entity) {
        return entity instanceof LivingEntity && entity.isAlive() && entity.isPickable() && !entity.isSpectator()
                && !(entity instanceof EnderDragon) && !(entity instanceof WitherBoss);
    }

    public static boolean isWrapped(Entity entity) {
        return carried.values().stream().anyMatch(list -> list.stream().anyMatch(wrapped -> wrapped.entity == entity));
    }

    private static boolean isCarrying(Entity entity) {
        List<Wrapped> list = carried.get(entity.getUUID());
        return list != null && !list.isEmpty();
    }

    private static int realm(Entity entity) {
        return entity instanceof Player player ? DivineSenseTechnique.getRealm(player) : 0;
    }

    private static void release(Entity entity, boolean slowFalling) {
        entity.fallDistance = 0;

        if (slowFalling && entity instanceof LivingEntity living && entity.isAlive()) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, Config.Server.windWrapSlowFallingTicks()));
        }
    }

    public static void releaseAll(Player carrier, boolean slowFalling) {
        List<Wrapped> list = carried.remove(carrier.getUUID());

        if (list == null) {
            return;
        }

        list.forEach(wrapped -> release(wrapped.entity, slowFalling));

        if (carrier instanceof ServerPlayer player) {
            sync(player);
        }
    }

    private static void sync(ServerPlayer player) {
        List<Wrapped> list = carried.get(player.getUUID());
        PacketHandler.channel.send(PacketDistributor.PLAYER.with(() -> player), new WindWrapPacket(list == null ? 0 : list.size(), Config.Server.windWrapMax()));
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) {
            return;
        }

        Player carrier = event.player;
        List<Wrapped> list = carried.get(carrier.getUUID());

        if (list == null || list.isEmpty()) {
            return;
        }

        // The flight stopped (or the synergy is gone): everyone is let down gently
        if (!carrier.isAlive() || !CoreSynergies.has(carrier, CoreSynergies.LEVITATION_WIND) || !isFlying(carrier)) {
            releaseAll(carrier, true);
            return;
        }

        int before = list.size();
        int carrierRealm = realm(carrier);

        list.removeIf(wrapped -> {
            Entity entity = wrapped.entity;

            if (!entity.isAlive() || entity.isRemoved() || entity.level != carrier.level || entity.distanceTo(carrier) > LOST_DISTANCE) {
                release(entity, false);
                return true;
            }

            // An unwilling player breaks free by sneaking, at once when their realm is higher
            if (entity instanceof Player player) {
                wrapped.sneakTicks = player.isShiftKeyDown() ? wrapped.sneakTicks + 1 : 0;

                if (wrapped.sneakTicks > 0 && (realm(player) > carrierRealm || wrapped.sneakTicks >= Config.Server.windWrapBreakFreeTicks())) {
                    release(entity, false);
                    return true;
                }
            }

            return false;
        });

        if (list.isEmpty()) {
            carried.remove(carrier.getUUID());
            sync((ServerPlayer) carrier);
            return;
        }

        // Qi per second for each entity, growing with its realm
        double cost = 0;
        for (Wrapped wrapped : list) {
            cost += Config.Server.windWrapCost() + Config.Server.windWrapCostPerRealm() * realm(wrapped.entity);
        }

        if (cost > 0 && !CultivatorStats.getCultivatorStats(carrier).getCultivation().consumeQi(carrier, cost / 20)) {
            releaseAll(carrier, true);
            return;
        }

        ServerLevel level = (ServerLevel) carrier.level;

        for (int i = 0; i < list.size(); i++) {
            Entity entity = list.get(i).entity;
            double angle = Math.toRadians(carrier.getYRot() + 90 + SLOT_ANGLES[i % SLOT_ANGLES.length]);
            double distance = SLOT_DISTANCE + entity.getBbWidth() / 2;
            Vec3 slot = carrier.position().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);

            // Pulled towards its place, a little stronger than gravity so it floats instead of sagging
            Vec3 move = slot.subtract(entity.position()).scale(0.35);
            if (move.length() > 1.5) {
                move = move.normalize().scale(1.5);
            }

            entity.setDeltaMovement(move.add(0, entity.isNoGravity() ? 0 : 0.08, 0));
            entity.fallDistance = 0;
            entity.hurtMarked = true;

            // Wind swirling around the wrapped entity
            if (carrier.tickCount % 2 == 0) {
                double swirl = carrier.tickCount * 0.4 + i * 2.1;
                double radius = entity.getBbWidth() / 2 + 0.4;

                for (int k = 0; k < 2; k++) {
                    double a = swirl + k * Math.PI;
                    double x = entity.getX() + Math.cos(a) * radius;
                    double z = entity.getZ() + Math.sin(a) * radius;
                    double y = entity.getY() + (0.2 + 0.6 * ((carrier.tickCount / 2 + k * 5) % 10) / 10.0) * entity.getBbHeight();
                    // Count 0: the offset is the particle's velocity, tangent to the swirl
                    level.sendParticles(ParticleTypes.CLOUD, x, y, z, 0, -Math.sin(a), 0.1, Math.cos(a), 0.08);
                }
            }
        }

        if (list.size() != before) {
            sync((ServerPlayer) carrier);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // No counter to send to a player who left
        List<Wrapped> list = carried.remove(event.getEntity().getUUID());

        if (list != null) {
            list.forEach(wrapped -> release(wrapped.entity, true));
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        releaseAll(event.getEntity(), true);
    }
}
