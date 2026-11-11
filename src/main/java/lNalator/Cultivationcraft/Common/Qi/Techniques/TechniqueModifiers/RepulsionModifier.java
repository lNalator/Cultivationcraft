package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

// Field axis, opposite of Gravity: the impact pushes everything away and slows enemy projectiles around it
public class RepulsionModifier extends GravityModifier {

    public RepulsionModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.repulsion");
        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/repulsion.png"), 8, 2);

        unlockQuest = new Quest(Quest.KNOCKBACK_TAKEN, 50);
        stabiliseQuest = new Quest(Quest.FIELD_MOVED, 1000);
    }

    protected void freeze(Entity projectile) {
        projectile.setDeltaMovement(0, -0.2, 0);
        projectile.setNoGravity(false);

        // Self-propelled projectiles (fireballs, Qi projectiles) would keep accelerating: they only fall now
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.xPower = 0;
            hurting.yPower = -0.05;
            hurting.zPower = 0;
        }

        projectile.hurtMarked = true;

        if (projectile.level instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, projectile.getX(), projectile.getY(), projectile.getZ(), 10, 0.15, 0.15, 0.15, 0.02);
            level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.4f, 1.8f);
        }
    }

    @Override
    protected double direction() {
        return -1;
    }

    @Override
    protected void affect(Player owner, Entity target, Vec3 pos, double distance, double radius, double force) {
        if (isEnemyProjectile(owner, target)) {
            // Repulsion + Ice synergy: a projectile slowed right at the impact freezes and drops
            if (distance <= Config.Server.repulsionIceDistance() && CoreSynergies.has(owner, CoreSynergies.REPULSION_ICE)) {
                freeze(target);
                return;
            }

            target.setDeltaMovement(target.getDeltaMovement().scale(0.3));
            target.hurtMarked = true;
            return;
        }

        super.affect(owner, target, pos, distance, radius, force);
    }
}
