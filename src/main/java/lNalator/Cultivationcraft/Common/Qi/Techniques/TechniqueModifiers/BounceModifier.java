package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Client.Physics;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.QiProjectile;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BounceModifier extends TechniqueModifier {

    public BounceModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.bounce");
        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/bounce.png"), 8, 2);
        unique = true;
        CATEGORY = MODIFIER_CATEGORY;

        unlockQuest = new Quest(Quest.BOUNCE, 1);
        stabiliseQuest = new Quest(Quest.BOUNCE, 1000);

        allowSameCategory = true;
        qiMult = 1.5f;
    }

    public boolean onHitBlock(Player owner, BlockHitResult hit, float damage, ResourceLocation element, Entity projectile) {
        super.onHitBlock(owner, hit, damage, element, projectile);

        // A limited number of bounces, each one shortening the projectile's remaining lifetime
        if (projectile instanceof QiProjectile qiProjectile) {
            if (qiProjectile.getBounces() >= Config.Server.bounceMax()) {
                return false;
            }

            qiProjectile.addBounce();
            qiProjectile.scaleLife((float) Config.Server.bounceLife());
        }

        Vec3 normal = new Vec3(hit.getDirection().getNormal().getX(), hit.getDirection().getNormal().getY(), hit.getDirection().getNormal().getZ());
        Vec3 bounceDirection = bounce(projectile.getDeltaMovement().normalize(), normal);

        // Set the new delta movement of the projectile to be the bounce direction multiplied by the previous speed
        projectile.setDeltaMovement(bounceDirection.scale(projectile.getDeltaMovement().length()));

        // Its propulsion bounces too, otherwise it keeps pushing towards the wall
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            Vec3 power = new Vec3(hurting.xPower, hurting.yPower, hurting.zPower);

            if (power.lengthSqr() > 0) {
                power = bounce(power.normalize(), normal).scale(power.length());
                hurting.xPower = power.x;
                hurting.yPower = power.y;
                hurting.zPower = power.z;
            }
        }

        return true;
    }

    protected Vec3 bounce(Vec3 direction, Vec3 normal) {
        Vec3 bounce = normal.scale(normal.dot(direction) * 2);
        bounce = bounce.subtract(direction);
        bounce = bounce.scale(-1);

        return bounce;
    }

    public void onHitTaken(Player owner, float damage, ResourceLocation defensiveElement, QiDamageSource source) {
        if (source.isInternal() || damage <= 0) {
            return;
        }

        // Bouncing costs half the damage taken in Qi; do nothing if the player does not have enough
        if (!CultivatorStats.getCultivatorStats(owner).getCultivation().consumeQi(owner, damage / 2)) {
            return;
        }

        if (source.msgId.compareTo(DamageSource.FALL.getMsgId()) == 0) {
            Physics.Bounce(owner, 0.75f);
        }
    }
}
