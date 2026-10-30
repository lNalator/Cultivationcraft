package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Qi.QiProjectile;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;

// Mass axis, opposite of Falling: slow falls without fall damage (sneak to fall normally), Qi projectiles fly further
public class LevitationModifier extends TechniqueModifier {

    public static final ResourceLocation LEVITATION_ID = new ResourceLocation(Cultivationcraft.MODID, "concept.levitation");

    // Slow Falling potion speed
    protected static final double MAX_FALL_SPEED = 0.1;

    // TODO core texture (animated coreTexture shown in the cultivation screen): a core gently bobbing up and down, with light wisps beneath it (no texture yet, uses blank.png)
    public LevitationModifier() {
        ID = LEVITATION_ID;
        CATEGORY = MASS_CATEGORY;

        unlockQuest = new Quest(Quest.SLOW_FALL, 60);
        stabiliseQuest = new Quest(Quest.SLOW_FALL, 1000);
    }

    // Fall distance when the player started sneaking: only the part of the fall from there to the ground hurts (server only)
    protected static final HashMap<UUID, Float> sneakStart = new HashMap<>();

    // The player still accumulates fall distance; the landing damage is reduced in CommonListeners
    @Override
    public void playerTick(Player player) {
        if (!player.level().isClientSide) {
            if (!player.isShiftKeyDown() || player.onGround()) {
                sneakStart.remove(player.getUUID());
            } else if (player.fallDistance > 0) {
                sneakStart.putIfAbsent(player.getUUID(), player.fallDistance);
            }
        }

        if (!massApplies(player) || player.isShiftKeyDown()) {
            return;
        }

        Vec3 move = player.getDeltaMovement();

        if (move.y < -MAX_FALL_SPEED) {
            player.setDeltaMovement(move.x, -MAX_FALL_SPEED, move.z);
        }
    }

    // Damaging fall distance: none while levitating, and when sneaking only what was fallen since the player started sneaking
    public static float damagingFall(Player player, float distance) {
        if (!hasLevitation(player)) {
            return distance;
        }

        Float start = sneakStart.remove(player.getUUID());

        if (!player.isShiftKeyDown()) {
            return 0;
        }

        return start == null ? distance : Math.max(0, distance - start);
    }

    public static boolean hasLevitation(Player player) {
        return lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats.getCultivatorStats(player).getCultivation().getModifierRank(LEVITATION_ID) > 0;
    }

    // Each rank adds 50% lifetime to Qi projectiles
    @Override
    public Entity QiProjectileSpawnOverride(Player owner, Entity inputProjectile) {
        if (inputProjectile instanceof QiProjectile projectile) {
            projectile.scaleLife(1 + 0.5f * getRank(owner));
        }

        return inputProjectile;
    }
}
