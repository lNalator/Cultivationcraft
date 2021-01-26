package DaoOfModding.Cultivationcraft.Common.Qi.BodyParts;

import DaoOfModding.Cultivationcraft.Common.Capabilities.BodyModifications.BodyModifications;
import DaoOfModding.Cultivationcraft.Common.Qi.PlayerStatModifications;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.UUID;

public class BodyPartStatControl
{
    private static HashMap<UUID, PlayerStatModifications> stats = new HashMap<UUID, PlayerStatModifications>();

    public static void addStats(UUID playerID, PlayerStatModifications statsToAdd)
    {
        if (!stats.containsKey(playerID))
            stats.put(playerID, new PlayerStatModifications());

        stats.get(playerID).combine(statsToAdd);
    }

    public static PlayerStatModifications getStats(UUID playerID)
    {
        if (!stats.containsKey(playerID))
            stats.put(playerID, new PlayerStatModifications());

        return stats.get(playerID);
    }

    public static void updateStats(PlayerEntity player)
    {
        // Clear the existing stats
        stats.put(player.getUniqueID(), new PlayerStatModifications());

        // Add all existing body part stats to the players stats
        for (BodyPart part : BodyModifications.getBodyModifications(player).getModifications().values())
            BodyPartStatControl.addStats(player.getUniqueID(), part.getStatChanges());
    }
}
