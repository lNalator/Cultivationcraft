package lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.CultivationPassives;

import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultCultivationStatIDs;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.TechniqueStatModification;

// Qi Condensation stats plus the absorption range: +1 block every 500 points
public class CoreFormingPassive extends QiCondenserPassive {

    public CoreFormingPassive() {
        super();

        TechniqueStatModification rangeModification = new TechniqueStatModification(DefaultCultivationStatIDs.qiAbsorbRange);

        rangeModification.addStatChange(DefaultCultivationStatIDs.qiAbsorbRange, 0.002);

        addTechniqueStat(DefaultCultivationStatIDs.qiAbsorbRange, 0, rangeModification);
    }
}
