package lNalator.Cultivationcraft.Client.GUI.Screens.CultivationTypeScreens;

import lNalator.Cultivationcraft.Client.GUI.Screens.ConceptScreen;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.FoundationEstablishmentCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import net.minecraft.client.Minecraft;

// The screens of each cultivation type live here rather than in the cultivation classes,
// so a dedicated server never loads client classes when it creates a player
public class CultivationScreens {

    public static CultivationTypeScreen create(CultivationType cultivation) {
        if (cultivation instanceof CoreFormingCultivation)
            return new CoreFormingScreen();
        if (cultivation instanceof QiCondenserCultivation)
            return new QiCondenserScreen();
        if (cultivation instanceof FoundationEstablishmentCultivation)
            return new FoundationEstablishmentScreen();
        return new CultivationTypeScreen();
    }

    public static void openAdvancementOption(CultivationType cultivation) {
        if (cultivation instanceof CoreFormingCultivation)
            Minecraft.getInstance().forceSetScreen(new ConceptScreen(true));
    }
}
