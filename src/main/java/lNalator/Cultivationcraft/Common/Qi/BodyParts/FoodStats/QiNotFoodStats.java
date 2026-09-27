package lNalator.Cultivationcraft.Common.Qi.BodyParts.FoodStats;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.PlayerHealthManager;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.QiSource;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultCultivationStatIDs;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.math.Vector3f;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class QiNotFoodStats extends QiFoodStats {

    public QiNotFoodStats() {
        super();

        staminaColor = new Vector3f(0.2f, 0.8f, 0.8f);
    }

    // Server side only
    @Override
    public void tick(Player player) {
        // Do nothing is player is dead
        if (!player.isAlive()) {
            return;
        }

        // Update the max food to equal the maxQi
        setMaxFood((int) CultivatorStats.getCultivatorStats(player).getCultivation().getCultivationStat(player, DefaultCultivationStatIDs.maxQi));

        // Qi doesn't have saturation
        setSaturation(0);

        // Handle stomach food drain here
        drainFood(player);

        // Get the player's blood and let it handle passive regen
        PlayerHealthManager.getBlood(player).regen(player);

        // Ensure that health and Qi don't go above max values
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        if (getFoodLevel() > getMaxFood()) {
            setFoodLevel(getMaxFood());
        }
    }

    public boolean isEdible(ItemStack item) {
        return false;
    }

    public boolean canEatMeat() {
        return false;
    }

    @Override
    public double meditation(double QiRemaining, Player player) {
        double room = Math.max(0, getMaxFood() - getTrueFoodLevel());
        if (room == 0) {
            return QiRemaining;
        }
        CultivationType cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();
        double passive = Math.min(room, Math.max(0,
                cultivation.getCultivationStat(player, DefaultCultivationStatIDs.qiPassiveAbsorbSpeed) / 20.0));
        double requested = Math.min(Math.max(0, QiRemaining), room - passive);
        double absorbed = requested > 0 ? cultivation.absorbFromQiSource(requested, player) : 0;
        setFoodLevel((float) Math.min(getMaxFood(), getTrueFoodLevel() + passive + absorbed));
        return Math.max(0, QiRemaining - absorbed);
    }

    public QiNotFoodStats clone() {
        QiNotFoodStats clone = new QiNotFoodStats();
        clone.maxFood = maxFood;
        clone.exhaustionLevel = exhaustionLevel;
        clone.foodLevel = foodLevel;
        clone.tickTimer = tickTimer;

        return clone;
    }
}
