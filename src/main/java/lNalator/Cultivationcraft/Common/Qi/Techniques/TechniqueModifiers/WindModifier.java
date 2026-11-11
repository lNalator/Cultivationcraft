package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;

public class WindModifier extends TechniqueModifier {

    public WindModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.wind");
        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/wind.png"), 8, 2);
        CATEGORY = ELEMENTAL_CATEGORY;

        Element = Elements.windElement;

        unlockQuest = new Quest(Quest.QI_SOURCE_MEDITATION, 1, Element.toString());
        stabiliseQuest = new Quest(Quest.DAMAGE_DEALT, 1000, Element.toString());
    }
}
