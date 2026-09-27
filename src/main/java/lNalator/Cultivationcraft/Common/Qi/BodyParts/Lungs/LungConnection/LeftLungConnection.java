package lNalator.Cultivationcraft.Common.Qi.BodyParts.Lungs.LungConnection;

import lNalator.Cultivationcraft.Common.Qi.BodyParts.Lungs.Breath.Breath;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.Lungs.Lung.Lung;
import lNalator.Cultivationcraft.Common.Qi.Stats.BodyPartStatControl;
import lNalator.Cultivationcraft.Common.Qi.Stats.StatIDs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class LeftLungConnection extends LungConnection {

    public static final ResourceLocation location = new ResourceLocation("leftlung");

    public LeftLungConnection(Lung newLung) {
        super(newLung);

        loc = location;
    }

    public void renderLungs(int x, int y) {
        lung.render(x, y, false);
    }

    public void calculateCapacity(Player player) {
        lung.setCapacity(BodyPartStatControl.getPlayerStatControl(player).getStats().getStat(StatIDs.lungCapacity) / 2);
    }

    // Returns the amount of air of the current breath type remaining in the lungs
    public float getCurrent(Breath breath) {
        if (!lung.canBreath(breath)) {
            return 0;
        }

        return lung.getCurrent();
    }
}
