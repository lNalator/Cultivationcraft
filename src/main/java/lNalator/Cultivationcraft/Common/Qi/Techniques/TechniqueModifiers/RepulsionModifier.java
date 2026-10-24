package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

// Field axis, opposite of Gravity: the impact pushes everything away and slows enemy projectiles around it
public class RepulsionModifier extends GravityModifier {

    // TODO core design: shockwaves pushing particles away from the core
    public RepulsionModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.repulsion");

        unlockQuest = new Quest(Quest.KNOCKBACK_TAKEN, 50);
        stabiliseQuest = new Quest(Quest.FIELD_MOVED, 1000);
    }

    @Override
    protected double direction() {
        return -1;
    }

    @Override
    protected void affect(Player owner, Entity target, Vec3 pos, double distance, double radius, double force) {
        if (isEnemyProjectile(owner, target)) {
            target.setDeltaMovement(target.getDeltaMovement().scale(0.3));
            target.hurtMarked = true;
            return;
        }

        super.affect(owner, target, pos, distance, radius, force);
    }
}
