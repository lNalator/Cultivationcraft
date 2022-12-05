package DaoOfModding.Cultivationcraft.Client;

import DaoOfModding.Cultivationcraft.Common.Qi.Stats.BodyPartStatControl;
import DaoOfModding.Cultivationcraft.Common.Qi.Stats.PlayerStatControl;
import DaoOfModding.Cultivationcraft.Common.Qi.Stats.PlayerStatModifications;
import DaoOfModding.Cultivationcraft.Common.Qi.Stats.StatIDs;
import DaoOfModding.mlmanimator.Client.Poses.PoseHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;

public class Physics
{
    private static HashMap<UUID, Double> fallSpeed = new HashMap<>();

    // PoseHandlers as client only
    public static Vec3 getDelta(Player player)
    {
        Vec3 currentMotion = player.getDeltaMovement();

        if (player.level.isClientSide)
            currentMotion = PoseHandler.getPlayerPoseHandler(player.getUUID()).getDeltaMovement();

        return currentMotion;
    }

    // Increase player jump speed based on the jump height
    public static void applyJump(Player player)
    {
        PlayerStatControl stats = BodyPartStatControl.getPlayerStatControl(player.getUUID());
        float jumpHeight = stats.getStats().getStat(StatIDs.jumpHeight);

        Vec3 currentMotion = getDelta(player);

        // Increase not only the height jump but also multiply X and Z momentum
        player.setDeltaMovement(currentMotion.x + (currentMotion.x * jumpHeight * 0.2f) * stats.getLegWeightModifier(), (0.42f + jumpHeight * 0.1f) * stats.getLegWeightModifier(), currentMotion.z + (currentMotion.z * jumpHeight * 0.2f) * stats.getLegWeightModifier());
    }

    public static void Bounce(Player player)
    {
        float bounceHeight = BodyPartStatControl.getStats(player.getUUID()).getStat(StatIDs.bounceHeight);

        // Do nothing if the player has no bounce stat
        if (bounceHeight == 0)
            return;

        // Do nothing if the player is in water
        if (player.isInWater())
        {
            fallSpeed.remove(player.getUUID());
            return;
        }

        Vec3 delta = getDelta(player);

        // If the player is on the ground then bounce if they have been falling, otherwise do nothing
        if (player.isOnGround())
        {
            if (fallSpeed.containsKey(player.getUUID()))
            {
                double bounce = fallSpeed.get(player.getUUID()) * -bounceHeight;

                // Only bounce if above a certain threshold, to stop infinite micro-bounces
                if (bounce > 0.25)
                    player.setDeltaMovement(delta.x, bounce, delta.z);

                fallSpeed.remove(player.getUUID());
            }

            return;
        }

        // If the player is falling place their fall speed into the fallSpeed hashmap
        double fall = delta.y;

        if (fall < 0)
            fallSpeed.put(player.getUUID(), fall);
        else
            fallSpeed.remove(player.getUUID());
    }

    // Increase the distance you can fall without taking damage by the fall height
    public static float reduceFallDistance(Player player, float distance)
    {
        PlayerStatControl stats = BodyPartStatControl.getPlayerStatControl(player.getUUID());

        distance -= (stats.getStats().getStat(StatIDs.fallHeight) - 1) * stats.getLegWeightModifier();

        // Adjust the vanilla fall distance of 1 by the legWeightModifier
        distance += 1 * (1 - stats.getLegWeightModifier());

        if (distance < 0)
            distance = 0;

        return distance;
    }
}
