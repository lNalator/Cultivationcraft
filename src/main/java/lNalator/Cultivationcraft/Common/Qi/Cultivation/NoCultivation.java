package lNalator.Cultivationcraft.Common.Qi.Cultivation;

import lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.CultivationPassives.BasePassive;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;

public class NoCultivation extends CultivationType {

    public static final ResourceLocation ID = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.cultivation.none");

    public NoCultivation() {
        super(0);

        passive = new BasePassive();
    }

}
