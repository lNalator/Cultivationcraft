package lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.CultivationPassives;

import lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.PassiveTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultCultivationStatIDs;


public class BasePassive extends PassiveTechnique {

    public BasePassive() {
        super();

        addTechniqueStat(DefaultCultivationStatIDs.maxQi, 0);
        addTechniqueStat(DefaultCultivationStatIDs.qiAbsorbRange, 0);
        addTechniqueStat(DefaultCultivationStatIDs.qiAbsorbSpeed, 10);
        addTechniqueStat(DefaultCultivationStatIDs.qiPassiveAbsorbSpeed, 1);
        addTechniqueStat(DefaultCultivationStatIDs.qiPassiveSpeed, 0);
    }
}
