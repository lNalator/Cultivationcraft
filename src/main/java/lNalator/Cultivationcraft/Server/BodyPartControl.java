package lNalator.Cultivationcraft.Server;

import lNalator.Cultivationcraft.Client.genericClientFunctions;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPartNames;
import lNalator.Cultivationcraft.Common.Capabilities.BodyModifications.BodyModifications;
import lNalator.Cultivationcraft.Common.Capabilities.BodyModifications.IBodyModifications;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPart;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPartOption;
import lNalator.Cultivationcraft.Common.Qi.Stats.BodyPartStatControl;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.Stats.StatIDs;
import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.Cultivationcraft.debug;
import net.minecraft.world.entity.player.Player;

public class BodyPartControl {

    public static void checkForgeProgress(Player player) {
        ICultivatorStats stats = CultivatorStats.getCultivatorStats(player);

        if (stats.getCultivationType() == CultivationTypes.BODY_CULTIVATOR) {
            IBodyModifications modifications = BodyModifications.getBodyModifications(player);
            BodyPart toComplete = BodyPartNames.getPartOrOption(modifications.getSelection());

            if (toComplete == null) {
                return;
            }

            // If the QI progress is higher than the QI needed for this part
            // Add this part onto player's body modifications and clear the selection
            if (BodyPartStatControl.getStats(player).getStat(StatIDs.qiCost) < modifications.getProgress()) {
                if (toComplete instanceof BodyPartOption) {
                    modifications.setOption((BodyPartOption) toComplete); 
                }else {
                    modifications.setModification(toComplete);
                }

                if (!debug.skipQuest) {
                    modifications.setLastForged(modifications.getSelection());
                    modifications.setQuestProgress(0);
                }

                modifications.setSelection("");
                modifications.setProgress(0);

                PacketHandler.sendBodyModificationsToClient(player);

                CultivatorTechniques.getCultivatorTechniques(player).determinePassives(player);

                BodyPartStatControl.addStats(player, toComplete.getStatChanges());
                BodyPartStatControl.updateStats(player);
            }
        }
    }
}
