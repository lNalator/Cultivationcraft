package DaoOfModding.Cultivationcraft.Common.Alchemy;

import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;
import DaoOfModding.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import net.minecraft.world.entity.player.Player;

public final class PillPotency {
    public static double restorationMultiplier(Player player, int pillTier) {
        if (player == null) return 1;
        var cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();
        int realm = cultivation instanceof CoreFormingCultivation ? 3 : cultivation instanceof QiCondenserCultivation ? 2 : 1;
        return Math.pow(.5, Math.max(0, realm - Math.max(1, pillTier)));
    }
}
