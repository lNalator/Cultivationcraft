package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.FlyingSwordEntity;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.QiProjectile;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class WarpModifier extends TechniqueModifier {

    int range = 6;

    // Qi projectiles appear this many blocks ahead on the line of sight, then fly normally
    public static final double PROJECTILE_WARP_DISTANCE = 6;
    // Kept between the warped projectile and a block in the way, so it never appears inside a wall
    protected static final double PROJECTILE_WARP_MARGIN = 1;
    // How far into an entity on the way the projectile arrives, so it hits it on its first tick
    protected static final double PROJECTILE_WARP_HIT_DEPTH = 0.1;

    public WarpModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.warp");
        unique = true;
        CATEGORY = MODIFIER_CATEGORY;

        unlockQuest = new Quest(Quest.ENDER_TELEPORT, 1);
        stabiliseQuest = new Quest(Quest.ENDER_TELEPORT, 1000);

        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/warp.png"), 20, 4);

        allowSameCategory = true;
    }

    @Override
    public boolean flyingSwordMovementOverride(FlyingSwordEntity sword, Vec3 targetPos) {
        // Do nothing if the sword is already in range of the targetPos
        if (sword.level.isClientSide() || sword.position().subtract(targetPos).length() < range || sword.getOwningPlayer() == null) {
            return false;
        }

        return teleport(sword.getOwningPlayer(), sword, randomRangeValue(targetPos, range / 2));
    }

    public void onHitTaken(Player owner, float damage, ResourceLocation defensiveElement, QiDamageSource source) {
        if (damage <= 0 || source.isInternal() || source.msgId.compareTo(DamageSource.FALL.getMsgId()) == 0) {
            return;
        }

        float rand = (float) Math.random();
        float modifier = 1;
        float negmodifier = 1;

        Vec3 destination = new Vec3(owner.position().x + rand * damage / 2f, (int) owner.position().y, owner.position().z + (1 - rand) * damage / 2f);
        Vec3 originalDestination = destination;

        // Change the Y position until there is an air block on and above the position, but not below
        while (!owner.level.getBlockState(new BlockPos(destination)).isAir() || !owner.level.getBlockState(new BlockPos(destination.x, destination.y + 1, destination.z)).isAir() || owner.level.getBlockState(new BlockPos(destination.x, destination.y - 1, destination.z)).isAir()) {
            destination = originalDestination.add(0, (int) modifier * negmodifier, 0);

            negmodifier *= -1;
            modifier += 0.5f;

            // No ground within 64 blocks (in the air above the world, over the void...): would loop forever
            if (modifier > 64) {
                return;
            }
        }

        teleport(owner, owner, destination);
    }

    // The projectile is warped ahead of the player on their line of sight, then flies normally: it still has to be aimed and can still be dodged
    @Override
    public Entity QiProjectileSpawnOverride(Player owner, Entity inputProjectile) {
        if (owner.level.isClientSide()) {
            return inputProjectile;
        }

        Vec3 start = inputProjectile.position();
        Vec3 direction = owner.getLookAngle().normalize();
        double distance = PROJECTILE_WARP_DISTANCE;

        // Stop before the first block on the way, never through a wall
        BlockHitResult blockHit = owner.level.clip(new ClipContext(start, start.add(direction.scale(distance)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, inputProjectile));
        if (blockHit.getType() != HitResult.Type.MISS) {
            distance = Math.min(distance, start.distanceTo(blockHit.getLocation()) - PROJECTILE_WARP_MARGIN);
        }

        // A target between the player and the warp distance is still hit: the projectile arrives on the first entity on the way.
        // The projectile's own width counts, so a big projectile (Grow) does not jump over a target it would have touched
        Vec3 end = start.add(direction.scale(distance));
        double hitRadius = inputProjectile.getBbWidth() / 2 + 0.3;
        for (Entity entity : owner.level.getEntities(owner, inputProjectile.getBoundingBox().expandTowards(direction.scale(distance)).inflate(hitRadius + 1), candidate -> candidate.isPickable() && !candidate.isSpectator() && candidate.isAlive() && !(candidate instanceof QiProjectile))) {
            AABB box = entity.getBoundingBox().inflate(hitRadius);
            Optional<Vec3> entry = box.contains(start) ? Optional.of(start) : box.clip(start, end);
            if (entry.isPresent() && start.distanceTo(entry.get()) + PROJECTILE_WARP_HIT_DEPTH < distance) {
                distance = start.distanceTo(entry.get()) + PROJECTILE_WARP_HIT_DEPTH;
            }
        }

        // Too close to be worth a warp: the projectile flies normally (and still hits a target right in front of the player)
        if (distance < 1) {
            return inputProjectile;
        }

        // Same cost as the other Warp teleports: 10 Qi per block. Without enough Qi, the projectile is shot normally
        if (!CultivatorStats.getCultivatorStats(owner).getCultivation().consumeQi(owner, distance * 10)) {
            return inputProjectile;
        }

        Vec3 destination = start.add(direction.scale(distance));
        ServerLevel level = (ServerLevel) owner.level;

        // Teleport effects where the projectile leaves and where it arrives, so the target sees where it comes from
        level.sendParticles(ParticleTypes.PORTAL, start.x, start.y, start.z, 16, 0.2, 0.2, 0.2, 0.5);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, destination.x, destination.y, destination.z, 16, 0.2, 0.2, 0.2, 0.05);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.ENDERMAN_TELEPORT, owner.getSoundSource(), 0.5f, 1.5f);
        level.playSound(null, destination.x, destination.y, destination.z, SoundEvents.ENDERMAN_TELEPORT, owner.getSoundSource(), 0.5f, 1.5f);

        // The projectile is not in the world yet: moving it is enough, and it does not count for the teleport quests.
        // When it arrives on an entity, it hits it on its first tick like any projectile touching a target
        inputProjectile.setPos(destination.x, destination.y, destination.z);

        return inputProjectile;
    }

    public Vec3 randomRangeValue(Vec3 input, float amount) {
        return new Vec3(randomRangeValue(amount) + input.x, randomRangeValue(amount) + input.y, randomRangeValue(amount) + input.z);
    }

    public float randomRangeValue(float amount) {
        return (float) (Math.random() - 0.5f) * amount;
    }

    public boolean teleport(Player owner, Entity entity, Vec3 pos) {
        if (owner == null) {
            return false;
        }

        // Spend Qi equal to the distance teleported * 10
        double distance = entity.position().subtract(pos).length();
        if (!CultivatorStats.getCultivatorStats(owner).getCultivation().consumeQi(owner, distance * 10)) {
            return false;
        }

        QuestHandler.progressQuest(owner, Quest.ENDER_TELEPORT, 1);

        for (int i = 0; i < 32; ++i) {
            ((ServerLevel) entity.level).sendParticles(ParticleTypes.PORTAL, entity.position().x, entity.position().y + owner.getRandom().nextDouble() * 2.0D, entity.position().z, 1, owner.getRandom().nextGaussian(), 0.0D, owner.getRandom().nextGaussian(), 0);
        }

        entity.level.playSound(null, entity, SoundEvents.ENDERMAN_TELEPORT, entity.getSoundSource(), 1, 1);

        entity.teleportTo(pos.x, pos.y, pos.z);
        entity.resetFallDistance();

        return true;
    }

    // Flight with Warp jumps instead of gliding: see FlightTechnique
}
