package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.Cultivationcraft.Network.Packets.GroundSlamPacket;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Elements.ElementalStates;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;
import lNalator.Cultivationcraft.Common.FlyingSwordEntity;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.FlyingSwordFormationTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.QiEmission;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FallModifier extends TechniqueModifier {

    public FallModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.fall");
        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/fall.png"), 6, 2);
        CATEGORY = MASS_CATEGORY;

        unlockQuest = new Quest(Quest.DAMAGE_TAKEN, 1, DamageSource.FALL.msgId);
        stabiliseQuest = new Quest(Quest.DAMAGE_DEALT, 10000);

        damageMult = 2;
    }

    public float getDamageMultiplier(Player owner, Technique tech) {
        // Only apply damage multipliers to techniques that can fall; each rank adds +50%
        if (tech instanceof QiEmission || tech instanceof FlyingSwordFormationTechnique) {
            return damageMult + 0.5f * (getRank(owner) - 1);
        }

        return 1;
    }

    // Falls 1.5x faster: vanilla gravity is 0.08 per tick
    @Override
    public void playerTick(Player player) {
        if (massApplies(player)) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, -0.04, 0));
        }
    }

    public Vec3 modifyMovement(Vec3 move) {
        // Extra gravity, capped so it doesn't keep accelerating forever
        if (move.y > -1) {
            move = move.add(0, -0.02, 0);
        }

        return move;
    }

    public void onHitTaken(Player owner, float damage, ResourceLocation defensiveElement, QiDamageSource source) {
        if (owner.level.isClientSide || source.msgId.compareTo(DamageSource.FALL.getMsgId()) != 0 || damage <= 0) {
            return;
        }

        // Deal damage to entities standing on ground at the same level as the player
        // Range and damage based on fall damage taken
        float range = 1 + damage / 4;

        // The box needs some height: a flat box at the player's feet never intersected mobs standing exactly
        // on the same level, so only some of them were hit. Use the same circle as the ground slam visual,
        // one block up or down like the ground it lifts.
        Vec3 center = owner.position();
        AABB area = new AABB(center.x - range, center.y - 1, center.z - range, center.x + range, center.y + 1, center.z + range);

        for (Entity hit : owner.level.getEntities(owner, area)) {
            double dx = hit.getX() - center.x;
            double dz = hit.getZ() - center.z;

            if (hit.isOnGround() && dx * dx + dz * dz <= range * range) {
                hit.hurt(new QiDamageSource(ID.toString(), owner, defensiveElement, false), damage);
            }
        }

        // Falling + Earth synergy stuns and knocks back, Falling + Wood roots (a player never has both elements)
        boolean earth = CoreSynergies.has(owner, CoreSynergies.FALL_EARTH);
        boolean wood = CoreSynergies.has(owner, CoreSynergies.FALL_WOOD);

        if (earth || wood) {
            slamSynergy(owner, center, earth);
        }

        // Ground slam visual for everyone around, and a heavy impact sound. Earth makes the ground rise higher
        PacketHandler.channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> owner), new GroundSlamPacket(owner.position(), range, earth ? 2 : 1));
        owner.level.playSound(null, owner.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 0.5f);
    }

    // Fixed radius around the landing, whatever the fall: the slam is seen and heard, so it can be jumped over
    protected void slamSynergy(Player owner, Vec3 center, boolean earth) {
        double radius = Config.Server.fallingSlamRadius();
        ServerLevel level = (ServerLevel) owner.level;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 1, radius),
                e -> e != owner && e.isAlive() && e.isOnGround() && !e.isSpectator() && !(e instanceof TamableAnimal pet && pet.isOwnedBy(owner)))) {
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;

            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }

            if (earth) {
                target.addEffect(new MobEffectInstance(ElementalStates.STUNNED.get(), Config.Server.fallingEarthStunTicks()), owner);

                // Rooted targets hold their ground, like against Gravity
                if (!target.hasEffect(ElementalStates.ROOTED.get())) {
                    Vec3 away = new Vec3(dx, 0, dz);
                    away = away.lengthSqr() < 1.0E-4 ? new Vec3(Math.random() - 0.5, 0, Math.random() - 0.5) : away;
                    away = away.normalize().scale(Config.Server.fallingEarthKnockback());
                    target.setDeltaMovement(target.getDeltaMovement().add(away.x, 0.35, away.z));
                    target.hurtMarked = true;
                }
            } else {
                target.addEffect(new MobEffectInstance(ElementalStates.ROOTED.get(), Config.Server.fallingWoodRootTicks()), owner);

                // Roots coming out of the ground around the target's feet
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), target.getX(), target.getY() + 0.1, target.getZ(), 20, 0.3, 0.1, 0.3, 0.1);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HANGING_ROOTS.defaultBlockState()), target.getX(), target.getY() + 0.4, target.getZ(), 12, 0.25, 0.3, 0.25, 0.05);
            }
        }

        if (!earth) {
            level.playSound(null, owner.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1, 0.6f);
        }
    }

    public Vec3 flyingSwordTargetOverride(FlyingSwordEntity sword, Entity target, Vec3 targetPos) {
        // Make the sword fall faster if it is currently above the entity
        if (Math.abs(target.position().x - sword.position().x) < 1 && Math.abs(target.position().z - sword.position().z) < 1) {
            sword.movement = modifyMovement(sword.movement);
            return targetPos;
        }

        // Move the sword above the entity
        return targetPos.add(0, 5, 0);
    }

    // TODO: Slowly fall when flying
}
