package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;

public class WoodModifier extends TechniqueModifier {

    // TODO core texture (animated coreTexture shown in the cultivation screen): bark and leaves, with vines sprouting from the core (no texture yet, uses blank.png)
    public WoodModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.wood");
        CATEGORY = ELEMENTAL_CATEGORY;

        Element = Elements.woodElement;

        unlockQuest = new Quest(Quest.QI_SOURCE_MEDITATION, 1, Element.toString());
        stabiliseQuest = new Quest(Quest.DAMAGE_DEALT, 1000, Element.toString());
    }
}
