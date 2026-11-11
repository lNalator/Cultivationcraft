package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Qi.Elements.ElementalStates;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Field axis: the impact pulls the entities around it in. Repulsion is the opposite pole
public class GravityModifier extends TechniqueModifier {

    // Each rank: +1 block of radius and +25% force
    protected static final double BASE_RADIUS = 3;
    protected static final double BASE_FORCE = 0.5;

    public GravityModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.gravity");
        CATEGORY = FIELD_CATEGORY;

        unlockQuest = new Quest(Quest.DAMAGE_TAKEN, 50, DamageSource.FALL.msgId);
        stabiliseQuest = new Quest(Quest.FIELD_MOVED, 1000);
    }

    // 1 pulls towards the impact, -1 pushes away
    protected double direction() {
        return 1;
    }

    @Override
    public void onHitAll(Player owner, Vec3 pos, float damage, ResourceLocation element) {
        if (owner.level.isClientSide) {
            return;
        }

        int rank = getRank(owner);
        double radius = BASE_RADIUS + (rank - 1);
        double force = BASE_FORCE * (1 + 0.25 * (rank - 1));

        for (Entity target : owner.level.getEntities(owner, new AABB(pos, pos).inflate(radius))) {
            double distance = target.position().distanceTo(pos);

            if (distance > radius) {
                continue;
            }

            affect(owner, target, pos, distance, radius, force);
        }
    }

    protected void affect(Player owner, Entity target, Vec3 pos, double distance, double radius, double force) {
        // Rooted targets hold their ground, like against Wind
        if (!(target instanceof LivingEntity living) || living.hasEffect(ElementalStates.ROOTED.get()) || distance < 0.5) {
            return;
        }

        // Stronger near the impact
        Vec3 push = pos.subtract(target.position()).normalize().scale(direction() * force * (1 - distance / (radius + 1)));
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, push.y * 0.5 + 0.1, push.z));
        target.hurtMarked = true;

        QuestHandler.progressQuest(owner, Quest.FIELD_MOVED, 1);
    }

    protected static boolean isEnemyProjectile(Player owner, Entity entity) {
        return entity instanceof Projectile projectile && projectile.getOwner() != owner;
    }
}
