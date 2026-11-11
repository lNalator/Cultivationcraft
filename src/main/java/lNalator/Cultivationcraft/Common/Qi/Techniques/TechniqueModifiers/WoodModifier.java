package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;

public class WoodModifier extends TechniqueModifier {

    public WoodModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.wood");
        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/wood.png"), 8, 4);
        CATEGORY = ELEMENTAL_CATEGORY;

        Element = Elements.woodElement;

        unlockQuest = new Quest(Quest.QI_SOURCE_MEDITATION, 1, Element.toString());
        stabiliseQuest = new Quest(Quest.DAMAGE_DEALT, 1000, Element.toString());
    }
}
