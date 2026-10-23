package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;

// Choosing Aura as a core unlocks the Aura technique (design D4). It no longer adds a passive
// effect around other techniques, and no longer needs an elemental core (Pure gives a neutral aura).
public class AuraModifier extends TechniqueModifier {

    public static final ResourceLocation AURA_ID = new ResourceLocation(Cultivationcraft.MODID, "concept.aura");

    public AuraModifier() {
        ID = AURA_ID;
        unique = true;
        CATEGORY = MODIFIER_CATEGORY;

        // Qi overflow: 30 s meditating in a Qi source with full Qi
        unlockQuest = new Quest(Quest.QI_OVERFLOW, 30);
        // ponytail: placeholder threshold until the per-minute measurement (design C1)
        stabiliseQuest = new Quest(Quest.AURA_HIT, 200);

        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/aura.png"), 4, 2);

        allowSameCategory = true;
    }
}
